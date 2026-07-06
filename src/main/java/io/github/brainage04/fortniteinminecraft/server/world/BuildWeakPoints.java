package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BuildWeakPoints {
    public static final double WEAK_POINT_DAMAGE_MULTIPLIER = 4.5D;
    static final double HIT_RADIUS_BLOCKS = 0.75D;
    private static final double WEAK_POINT_SURFACE_CLEARANCE_BLOCKS = 0.25D;
    private static final double POSITION_EPSILON = 1.0E-6D;

    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final int TEXT_LINE_WIDTH = 40;
    private static final float DISPLAY_SCALE = 0.85F;
    private static final EntityType<Display.TextDisplay> TEXT_DISPLAY_TYPE = textDisplayType();
    private static final Map<BuildSlot, WeakPoint> WEAK_POINTS = new HashMap<>();
    private static final Map<TerrainKey, WeakPoint> TERRAIN_WEAK_POINTS = new HashMap<>();
    private static final Map<UUID, ActiveWeakPointView> ACTIVE_VIEWS = new HashMap<>();

    private static BuildWorldState state;
    private static WorldBuildMaterializer materializer;
    private static boolean registered;

    private BuildWeakPoints() {
    }

    public static void register(BuildWorldState buildWorld, WorldBuildMaterializer worldMaterializer) {
        state = Objects.requireNonNull(buildWorld, "buildWorld");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(BuildWeakPoints::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUUID()));
        registered = true;
    }

    public static Damage damageForHit(ServerLevel level, BuildSlot slot, Vec3 hitLocation, int baseDamage) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage cannot be negative");
        }
        if (materializer == null) {
            return new Damage(baseDamage, false);
        }
        return damageForHit(slot, visibleWeakPointPositions(level, slot), hitLocation, baseDamage);
    }

    public static Damage damageForHit(BuildSlot slot, Vec3 hitLocation, int baseDamage) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage cannot be negative");
        }
        if (materializer == null) {
            return new Damage(baseDamage, false);
        }
        return damageForHit(slot, materializer.trackedBlockPositions(slot), hitLocation, baseDamage);
    }

    static Damage damageForHit(BuildSlot slot, List<BlockPos> positions, Vec3 hitLocation, int baseDamage) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage cannot be negative");
        }
        if (positions.isEmpty()) {
            WEAK_POINTS.remove(slot);
            return new Damage(baseDamage, false);
        }

        WeakPoint weakPoint = weakPointFor(slot, positions);
        if (!hits(weakPoint.position(), hitLocation)) {
            return new Damage(baseDamage, false);
        }
        moveWeakPoint(slot, positions, weakPoint.sequence() + 1);
        return new Damage(scaledDamage(baseDamage), true);
    }

    public static Damage damageForTerrainHit(ServerLevel level, BlockPos pos, Vec3 hitLocation, int baseDamage) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage cannot be negative");
        }
        return damageForTerrainHit(
                level.dimension().identifier().toString(),
                pos,
                visibleTerrainWeakPointPositions(level, pos),
                hitLocation,
                baseDamage
        );
    }

    static Damage damageForTerrainHit(
            String dimension,
            BlockPos pos,
            List<Vec3> positions,
            Vec3 hitLocation,
            int baseDamage
    ) {
        dimension = requireDimension(dimension);
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(hitLocation, "hitLocation");
        if (baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage cannot be negative");
        }
        TerrainKey key = new TerrainKey(dimension, pos.immutable());
        if (positions.isEmpty()) {
            TERRAIN_WEAK_POINTS.remove(key);
            return new Damage(baseDamage, false);
        }

        WeakPoint weakPoint = terrainWeakPointFor(key, positions);
        if (!hits(weakPoint.position(), hitLocation)) {
            return new Damage(baseDamage, false);
        }
        moveTerrainWeakPoint(key, positions, weakPoint.sequence() + 1);
        return new Damage(scaledDamage(baseDamage), true);
    }

    public static void clearTerrain(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        clearTerrain(level.dimension().identifier().toString(), pos);
    }

    public static void clear(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        WEAK_POINTS.remove(slot);
        WeakPointTarget target = WeakPointTarget.build(slot);
        ACTIVE_VIEWS.entrySet().removeIf(entry -> {
            ActiveWeakPointView view = entry.getValue();
            if (!view.target().equals(target)) {
                return false;
            }
            view.display().discard();
            return true;
        });
    }

    public static void clearAll() {
        ACTIVE_VIEWS.values().forEach(view -> view.display().discard());
        ACTIVE_VIEWS.clear();
        WEAK_POINTS.clear();
        TERRAIN_WEAK_POINTS.clear();
    }

    static Vec3 weakPointPosition(BuildSlot slot, List<BlockPos> positions, int sequence) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(positions, "positions");
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be empty");
        }
        ArrayList<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort((left, right) -> {
            int y = Integer.compare(left.getY(), right.getY());
            if (y != 0) {
                return y;
            }
            int z = Integer.compare(left.getZ(), right.getZ());
            if (z != 0) {
                return z;
            }
            return Integer.compare(left.getX(), right.getX());
        });
        int index = Math.floorMod(slot.hashCode() + sequence * 7, sorted.size());
        return Vec3.atCenterOf(sorted.get(index));
    }

    static Vec3 terrainWeakPointPosition(String dimension, BlockPos pos, List<Vec3> positions, int sequence) {
        return terrainWeakPointPosition(new TerrainKey(requireDimension(dimension), pos.immutable()), positions, sequence);
    }

    private static Vec3 terrainWeakPointPosition(TerrainKey key, List<Vec3> positions, int sequence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(positions, "positions");
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be empty");
        }
        ArrayList<Vec3> sorted = new ArrayList<>(positions);
        sorted.sort((left, right) -> {
            int y = Double.compare(left.y(), right.y());
            if (y != 0) {
                return y;
            }
            int z = Double.compare(left.z(), right.z());
            if (z != 0) {
                return z;
            }
            return Double.compare(left.x(), right.x());
        });
        int index = Math.floorMod(key.hashCode() + sequence * 7, sorted.size());
        return sorted.get(index);
    }

    static boolean hits(Vec3 weakPoint, Vec3 hitLocation) {
        Objects.requireNonNull(weakPoint, "weakPoint");
        Objects.requireNonNull(hitLocation, "hitLocation");
        return weakPoint.distanceToSqr(hitLocation) <= HIT_RADIUS_BLOCKS * HIT_RADIUS_BLOCKS;
    }

    private static WeakPoint weakPointFor(BuildSlot slot, List<BlockPos> positions) {
        WeakPoint current = WEAK_POINTS.get(slot);
        if (current != null && containsPosition(positions, current.position())) {
            return current;
        }
        int nextSequence = current == null ? 0 : current.sequence() + 1;
        return moveWeakPoint(slot, positions, nextSequence);
    }

    private static boolean containsPosition(List<BlockPos> positions, Vec3 position) {
        for (BlockPos candidate : positions) {
            if (Vec3.atCenterOf(candidate).equals(position)) {
                return true;
            }
        }
        return false;
    }

    private static WeakPoint moveWeakPoint(BuildSlot slot, List<BlockPos> positions, int sequence) {
        WeakPoint next = new WeakPoint(sequence, weakPointPosition(slot, positions, sequence));
        WEAK_POINTS.put(slot, next);
        return next;
    }

    private static WeakPoint terrainWeakPointFor(TerrainKey key, List<Vec3> positions) {
        WeakPoint current = TERRAIN_WEAK_POINTS.get(key);
        if (current != null && containsSurfacePosition(positions, current.position())) {
            return current;
        }
        int nextSequence = current == null ? 0 : current.sequence() + 1;
        return moveTerrainWeakPoint(key, positions, nextSequence);
    }

    private static boolean containsSurfacePosition(List<Vec3> positions, Vec3 position) {
        for (Vec3 candidate : positions) {
            if (candidate.equals(position)) {
                return true;
            }
        }
        return false;
    }

    private static WeakPoint moveTerrainWeakPoint(TerrainKey key, List<Vec3> positions, int sequence) {
        WeakPoint next = new WeakPoint(sequence, terrainWeakPointPosition(key, positions, sequence));
        TERRAIN_WEAK_POINTS.put(key, next);
        return next;
    }

    private static int scaledDamage(int baseDamage) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(baseDamage * WEAK_POINT_DAMAGE_MULTIPLIER));
    }

    private static List<BlockPos> visibleWeakPointPositions(ServerLevel level, BuildSlot slot) {
        String dimension = slot.gridPos().dimension();
        ArrayList<BlockPos> visible = new ArrayList<>();
        for (BlockPos pos : materializer.trackedBlockPositions(slot)) {
            if (!slot.equals(materializer.topOwnerAt(dimension, pos))) {
                continue;
            }
            BlockState originalState = materializer.originalBlockState(dimension, pos.getX(), pos.getY(), pos.getZ());
            if (originalState != null && WorldObstructions.isBlockingCollision(level, pos, originalState)) {
                continue;
            }
            visible.add(pos);
        }
        return List.copyOf(visible);
    }

    private static List<Vec3> visibleTerrainWeakPointPositions(ServerLevel level, BlockPos pos) {
        if (!TerrainResourceHarvest.isHarvestable(level.getBlockState(pos))) {
            return List.of();
        }
        ArrayList<Vec3> visible = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (isAirFacing(level, pos, direction)) {
                visible.add(terrainSurfacePosition(pos, direction));
            }
        }
        return List.copyOf(visible);
    }

    static Vec3 terrainSurfacePosition(BlockPos pos, Direction direction) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(direction, "direction");
        Vec3 center = Vec3.atCenterOf(pos);
        return center.add(
                direction.getStepX() * 0.51D,
                direction.getStepY() * 0.51D,
                direction.getStepZ() * 0.51D
        );
    }

    static Vec3 visibleBuildWeakPointPosition(Vec3 blockCenter, Vec3 viewerPosition) {
        Objects.requireNonNull(blockCenter, "blockCenter");
        Vec3 unitTowardViewer = unitTowardViewer(blockCenter, viewerPosition);
        if (unitTowardViewer == null) {
            return blockCenter;
        }
        double offset = blockSurfaceDistance(unitTowardViewer) + WEAK_POINT_SURFACE_CLEARANCE_BLOCKS;
        return blockCenter.add(unitTowardViewer.scale(offset));
    }

    static Vec3 visibleSurfaceWeakPointPosition(Vec3 surfacePosition, Vec3 viewerPosition) {
        Objects.requireNonNull(surfacePosition, "surfacePosition");
        Vec3 unitTowardViewer = unitTowardViewer(surfacePosition, viewerPosition);
        if (unitTowardViewer == null) {
            return surfacePosition;
        }
        return surfacePosition.add(unitTowardViewer.scale(WEAK_POINT_SURFACE_CLEARANCE_BLOCKS));
    }

    private static Vec3 unitTowardViewer(Vec3 origin, Vec3 viewerPosition) {
        Objects.requireNonNull(viewerPosition, "viewerPosition");
        Vec3 towardViewer = viewerPosition.subtract(origin);
        double distance = towardViewer.length();
        if (distance <= POSITION_EPSILON) {
            return null;
        }
        return towardViewer.scale(1.0D / distance);
    }

    private static double blockSurfaceDistance(Vec3 unitDirection) {
        double dominantAxis = Math.max(
                Math.abs(unitDirection.x()),
                Math.max(Math.abs(unitDirection.y()), Math.abs(unitDirection.z()))
        );
        if (dominantAxis <= POSITION_EPSILON) {
            return 0.0D;
        }
        return 0.5D / dominantAxis;
    }

    private static boolean isAirFacing(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos adjacent = pos.relative(direction);
        if (!level.isInWorldBounds(adjacent)) {
            return false;
        }
        BlockState adjacentState = level.getBlockState(adjacent);
        return adjacentState.isAir() || adjacentState.canBeReplaced() && adjacentState.getFluidState().isEmpty();
    }

    private static void tickLevel(ServerLevel level) {
        if (state == null || materializer == null) {
            return;
        }
        String dimension = level.dimension().identifier().toString();
        WEAK_POINTS.keySet().removeIf(slot -> state.get(slot) == null);
        TERRAIN_WEAK_POINTS.keySet().removeIf(key -> key.dimension().equals(dimension)
                && !TerrainResourceHarvest.isHarvestable(level.getBlockState(key.pos())));
        for (ServerPlayer player : level.players()) {
            updatePlayerView(level, dimension, player);
        }
    }

    private static void updatePlayerView(ServerLevel level, String dimension, ServerPlayer player) {
        WeakPointTarget target = target(level, dimension, player);
        if (target == null) {
            clear(player.getUUID());
            return;
        }

        WeakPoint weakPoint;
        if (target.slot() != null) {
            if (state.get(target.slot()) == null) {
                clear(player.getUUID());
                return;
            }
            List<BlockPos> positions = visibleWeakPointPositions(level, target.slot());
            if (positions.isEmpty()) {
                clear(player.getUUID());
                return;
            }
            weakPoint = weakPointFor(target.slot(), positions);
        } else {
            List<Vec3> positions = visibleTerrainWeakPointPositions(level, target.terrain().pos());
            if (positions.isEmpty()) {
                clear(player.getUUID());
                return;
            }
            weakPoint = terrainWeakPointFor(target.terrain(), positions);
        }

        ActiveWeakPointView view = ACTIVE_VIEWS.get(player.getUUID());
        if (view == null || view.display().isRemoved() || !view.dimension().equals(dimension) || !view.target().equals(target)) {
            clear(player.getUUID());
            Display.TextDisplay display = new Display.TextDisplay(TEXT_DISPLAY_TYPE, level);
            display.setNoGravity(true);
            configure(display);
            if (!level.addFreshEntity(display)) {
                display.discard();
                return;
            }
            view = new ActiveWeakPointView(dimension, target, display);
            ACTIVE_VIEWS.put(player.getUUID(), view);
        }
        Vec3 visiblePosition = target.slot() != null
                ? visibleBuildWeakPointPosition(weakPoint.position(), player.getEyePosition())
                : visibleSurfaceWeakPointPosition(weakPoint.position(), player.getEyePosition());
        view.display().setPos(visiblePosition.x(), visiblePosition.y(), visiblePosition.z());
    }

    private static WeakPointTarget target(ServerLevel level, String dimension, ServerPlayer player) {
        HitResult hit = player.pick(BuildPieceHealthDisplays.LOOK_RANGE_BLOCKS, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        BuildSlot slot = materializer.topOwnerAt(dimension, pos);
        if (slot != null) {
            return WeakPointTarget.build(slot);
        }
        if (!TerrainResourceHarvest.isHarvestable(level.getBlockState(pos))
                || visibleTerrainWeakPointPositions(level, pos).isEmpty()) {
            return null;
        }
        return WeakPointTarget.terrain(new TerrainKey(dimension, pos.immutable()));
    }

    private static void configure(Display.TextDisplay display) {
        display.setText(Component.literal("X").withStyle(ChatFormatting.YELLOW));
        display.setLineWidth(TEXT_LINE_WIDTH);
        display.setBackgroundColor(TRANSPARENT_BACKGROUND);
        display.setTextOpacity((byte) 255);
        display.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.setViewRange(0.75F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(0.6F);
        display.setHeight(0.6F);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                new Quaternionf()
        ));
    }

    private static void clear(UUID playerId) {
        ActiveWeakPointView view = ACTIVE_VIEWS.remove(playerId);
        if (view != null) {
            view.display().discard();
        }
    }

    private static void clearTerrain(String dimension, BlockPos pos) {
        TerrainKey key = new TerrainKey(requireDimension(dimension), pos.immutable());
        TERRAIN_WEAK_POINTS.remove(key);
        WeakPointTarget target = WeakPointTarget.terrain(key);
        ACTIVE_VIEWS.entrySet().removeIf(entry -> {
            ActiveWeakPointView view = entry.getValue();
            if (!view.target().equals(target)) {
                return false;
            }
            view.display().discard();
            return true;
        });
    }

    private static String requireDimension(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank()) {
            throw new IllegalArgumentException("dimension cannot be blank");
        }
        return dimension;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.TextDisplay> textDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("text_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:text_display entity type");
        }
        return (EntityType<Display.TextDisplay>) type;
    }

    public record Damage(int amount, boolean weakPointHit) {
        public Damage {
            if (amount < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }
    }

    private record WeakPoint(int sequence, Vec3 position) {
        private WeakPoint {
            Objects.requireNonNull(position, "position");
        }
    }

    private record TerrainKey(String dimension, BlockPos pos) {
        private TerrainKey {
            dimension = requireDimension(dimension);
            Objects.requireNonNull(pos, "pos");
        }
    }

    private record WeakPointTarget(BuildSlot slot, TerrainKey terrain) {
        private static WeakPointTarget build(BuildSlot slot) {
            return new WeakPointTarget(Objects.requireNonNull(slot, "slot"), null);
        }

        private static WeakPointTarget terrain(TerrainKey terrain) {
            return new WeakPointTarget(null, Objects.requireNonNull(terrain, "terrain"));
        }

        private WeakPointTarget {
            if ((slot == null) == (terrain == null)) {
                throw new IllegalArgumentException("weak point target must be exactly one build slot or terrain block");
            }
        }
    }

    private record ActiveWeakPointView(String dimension, WeakPointTarget target, Display.TextDisplay display) {
        private ActiveWeakPointView {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(display, "display");
        }
    }
}
