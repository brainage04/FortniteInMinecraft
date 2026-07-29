package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


public final class PortAFortItem extends Item {
    private static final float THROW_POWER = 1.5F;
    private static final float THROW_INACCURACY = 1.0F;

    private static BuildWorldState buildState;
    private static BuildRules buildRules;
    private static WorldBuildMaterializer buildMaterializer;

    private final Definition definition;
    private final Item clientItem;

    public PortAFortItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings,
                Component.literal("Utility / " + definition.rarity().label()),
                Component.literal("Throwable that snaps impact to the build grid."),
                Component.literal("Spawns tracked metal build pieces: " + format(buildTileBlocks())
                        + "x" + format(buildTileBlocks()) + " tiles, " + format(buildWallHeightBlocks()) + "-block walls."),
                Component.literal("Source: " + definition.sourceItemId())
        ));
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public static void configureBuildPlacement(BuildWorldState state, BuildRules rules, WorldBuildMaterializer materializer) {
        buildState = Objects.requireNonNull(state, "state");
        buildRules = Objects.requireNonNull(rules, "rules");
        buildMaterializer = Objects.requireNonNull(materializer, "materializer");
    }

    public Definition definition() {
        return definition;
    }

    Item clientItem() {
        return clientItem;
    }

    static UseCooldown cooldownComponent(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(definition.cooldownTicks() / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!configured()) {
            serverPlayer.sendSystemMessage(Component.literal("Port-A-Fort deployment is not configured."), true);
            return InteractionResult.FAIL;
        }

        ItemStack projectileStack = new ItemStack(clientItem);
        PortAFortProjectile projectile = new PortAFortProjectile(
                serverLevel,
                serverPlayer,
                projectileStack,
                definition,
                orientationFrom(serverPlayer.getDirection()),
                serverPlayer.getUUID()
        );
        projectile.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F, THROW_POWER, THROW_INACCURACY);
        if (!serverLevel.addFreshEntity(projectile)) {
            return InteractionResult.FAIL;
        }

        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean configured() {
        return buildState != null && buildRules != null && buildMaterializer != null;
    }

    static int deployFort(ServerLevel level, Definition definition, UUID owner, Orientation orientation, HitResult impact) {
        if (!configured()) {
            return 0;
        }
        BlockPos anchorBlock = impactAnchor(impact);
        String dimension = level.dimension().identifier().toString();
        SnapGrid snapGrid = new SnapGrid(buildRules);
        BuildGridPos anchor = snapGrid.snap(dimension, anchorBlock.getX(), anchorBlock.getY(), anchorBlock.getZ());
        FootprintProjector footprints = new FootprintProjector(buildRules);
        List<BuildPieceState> pieces = fortPieces(anchor, definition, owner, orientation, level.getGameTime());
        ArrayList<PlacedPiece> placed = new ArrayList<>(pieces.size());

        for (BuildPieceState piece : pieces) {
            BuildSlot slot = piece.slot();
            PieceFootprint footprint = footprints.project(piece);
            List<BlockOffset> absoluteBlocks = footprint.absoluteBlocks(snapGrid.blockOrigin(slot.gridPos()));
            if (!buildState.addIfNotConflicting(piece, absoluteBlocks)) {
                rollback(level, placed);
                FortniteInMinecraft.LOGGER.debug("Port-A-Fort skipped {} at {}: build footprint overlaps an occupied build piece", slot.pieceType(), slot.gridPos());
                return 0;
            }
            WorldBuildWriteResult writeResult = buildMaterializer.place(level, piece, footprint);
            if (!writeResult.success()) {
                buildState.remove(slot);
                rollback(level, placed);
                FortniteInMinecraft.LOGGER.warn("Port-A-Fort materialization failed for {}: {}", slot, writeResult.message());
                return 0;
            }
            placed.add(new PlacedPiece(piece));
        }

        Vec3 impactLocation = impact.getLocation();
        level.playSound(null, impactLocation.x(), impactLocation.y(), impactLocation.z(),
                SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.9F, 1.25F);
        level.sendParticles(ParticleTypes.CLOUD, true, true,
                impactLocation.x(), impactLocation.y() + 0.5D, impactLocation.z(),
                32, definition.radius(), 1.2D, definition.radius(), 0.08D);
        return placed.size();
    }

    private static void rollback(ServerLevel level, List<PlacedPiece> placed) {
        for (int i = placed.size() - 1; i >= 0; i--) {
            BuildPieceState piece = placed.get(i).piece();
            buildMaterializer.clear(level, piece);
            buildState.remove(piece.slot());
        }
    }

    private static BlockPos impactAnchor(HitResult impact) {
        if (impact instanceof BlockHitResult blockHit && impact.getType() != HitResult.Type.MISS) {
            BlockPos clicked = blockHit.getBlockPos();
            return blockHit.getDirection() == Direction.UP ? clicked.above() : clicked.relative(blockHit.getDirection());
        }
        Vec3 location = impact.getLocation();
        return BlockPos.containing(location.x(), location.y(), location.z());
    }

    static List<BuildSlot> fortSlots(BuildGridPos anchor, int radius, int height, Orientation orientation) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(orientation, "orientation");
        if (radius < 1 || height < 1) {
            throw new IllegalArgumentException("fort radius and height must be positive");
        }

        int gridRadius = Math.max(0, radius - 1);
        ArrayList<BuildSlot> slots = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            addFloorDeck(slots, anchor, gridRadius, y);
            addPerimeterWalls(slots, anchor, gridRadius, y);
            if (y < height - 1) {
                slots.add(slot(anchor, 0, y, 0, PieceType.STAIR, orientation));
            }
        }
        for (int x = -gridRadius; x <= gridRadius; x++) {
            for (int z = -gridRadius; z <= gridRadius; z++) {
                slots.add(slot(anchor, x, height - 1, z, PieceType.ROOF, Orientation.NORTH));
            }
        }
        return List.copyOf(slots);
    }

    static List<BuildPieceState> fortPieces(BuildGridPos anchor, Definition definition, UUID owner, Orientation orientation, long firstTick) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(orientation, "orientation");
        ArrayList<BuildPieceState> pieces = new ArrayList<>();
        long tick = firstTick;
        for (BuildSlot slot : fortSlots(anchor, definition.radius(), definition.height(), orientation)) {
            pieces.add(BuildPieceState.placed(slot, MaterialType.METAL, owner, tick++));
        }
        return List.copyOf(pieces);
    }

    private static void addFloorDeck(List<BuildSlot> slots, BuildGridPos anchor, int gridRadius, int y) {
        for (int x = -gridRadius; x <= gridRadius; x++) {
            for (int z = -gridRadius; z <= gridRadius; z++) {
                slots.add(slot(anchor, x, y, z, PieceType.FLOOR, Orientation.NORTH));
            }
        }
    }

    private static void addPerimeterWalls(List<BuildSlot> slots, BuildGridPos anchor, int gridRadius, int y) {
        for (int x = -gridRadius; x <= gridRadius; x++) {
            slots.add(slot(anchor, x, y, -gridRadius, PieceType.WALL, Orientation.NORTH));
            slots.add(slot(anchor, x, y, gridRadius, PieceType.WALL, Orientation.SOUTH));
        }
        if (gridRadius == 0) {
            slots.add(slot(anchor, 0, y, 0, PieceType.WALL, Orientation.WEST));
            slots.add(slot(anchor, 0, y, 0, PieceType.WALL, Orientation.EAST));
            return;
        }
        for (int z = -gridRadius + 1; z <= gridRadius - 1; z++) {
            slots.add(slot(anchor, -gridRadius, y, z, PieceType.WALL, Orientation.WEST));
            slots.add(slot(anchor, gridRadius, y, z, PieceType.WALL, Orientation.EAST));
        }
    }

    private static BuildSlot slot(BuildGridPos anchor, int x, int y, int z, PieceType pieceType, Orientation orientation) {
        return BuildSlot.of(anchor.dimension(), anchor.x() + x, anchor.y() + y, anchor.z() + z, pieceType, orientation);
    }

    private static Orientation orientationFrom(Direction direction) {
        return switch (direction) {
            case EAST -> Orientation.EAST;
            case SOUTH -> Orientation.SOUTH;
            case WEST -> Orientation.WEST;
            default -> Orientation.NORTH;
        };
    }

    private static int buildTileBlocks() {
        return buildRules == null ? BuildRules.defaults().footprintSizeBlocks() : buildRules.footprintSizeBlocks();
    }

    private static int buildWallHeightBlocks() {
        return buildRules == null ? BuildRules.defaults().wallHeightBlocks() : buildRules.wallHeightBlocks();
    }

    public record Definition(
            String path,
            String displayName,
            FortniteRarity rarity,
            String sourceItemId,
            int cooldownTicks,
            int radius,
            int height
    ) {
        public Definition {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(rarity, "rarity");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks cannot be negative");
            }
            if (radius < 1 || height < 1) {
                throw new IllegalArgumentException("radius and height must describe a fort");
            }
        }
    }

    private record PlacedPiece(BuildPieceState piece) {
        private PlacedPiece {
            Objects.requireNonNull(piece, "piece");
        }
    }

    private static final class PortAFortProjectile extends Snowball {
        private final Definition definition;
        private final Orientation orientation;
        private final UUID owner;
        private boolean deployed;

        private PortAFortProjectile(Level level, Player ownerEntity, ItemStack stack, Definition definition, Orientation orientation, UUID owner) {
            super(level, ownerEntity, stack);
            this.definition = Objects.requireNonNull(definition, "definition");
            this.orientation = Objects.requireNonNull(orientation, "orientation");
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        @Override
        protected void onHit(HitResult result) {
            if (!deployed && !level().isClientSide() && level() instanceof ServerLevel serverLevel) {
                deployed = true;
                deployFort(serverLevel, definition, owner, orientation, result);
                discard();
            }
            super.onHit(result);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    private static ChatFormatting rarityColor(FortniteRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-4D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
