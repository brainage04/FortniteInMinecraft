package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.mixin.DisplayAccessor;
import io.github.brainage04.fortniteinminecraft.mixin.TextDisplayAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BuildPieceHealthDisplays {
    static final double LOOK_RANGE_BLOCKS = 8.0D;
    private static final int BAR_SEGMENTS = 10;
    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final int TEXT_LINE_WIDTH = 160;
    private static final float DISPLAY_SCALE = 0.55F;
    private static final EntityType<Display.TextDisplay> TEXT_DISPLAY_TYPE = textDisplayType();
    private static final Map<UUID, ActiveHealthView> ACTIVE_VIEWS = new HashMap<>();
    private static BuildWorldState state;
    private static WorldBuildMaterializer materializer;
    private static boolean registered;

    private BuildPieceHealthDisplays() {
    }

    public static void register(BuildWorldState buildWorld, WorldBuildMaterializer worldMaterializer) {
        state = Objects.requireNonNull(buildWorld, "buildWorld");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(BuildPieceHealthDisplays::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUUID()));
        registered = true;
    }

    public static void clearAll() {
        ACTIVE_VIEWS.values().forEach(view -> view.display().discard());
        ACTIVE_VIEWS.clear();
    }

    public static String healthText(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        int filled = Math.max(0, Math.min(BAR_SEGMENTS, (int) Math.round(piece.healthRatio() * BAR_SEGMENTS)));
        String bar = "█".repeat(filled) + "░".repeat(BAR_SEGMENTS - filled);
        return piece.material().name().toLowerCase(java.util.Locale.ROOT)
                + " " + bar + " " + piece.currentHealth() + "/" + piece.maxHealth();
    }

    private static void tickLevel(ServerLevel level) {
        if (state == null || materializer == null) {
            return;
        }
        state.progressConstruction(level.getGameTime());
        String dimension = level.dimension().identifier().toString();
        for (ServerPlayer player : level.players()) {
            updatePlayerView(level, dimension, player);
        }
    }

    private static void updatePlayerView(ServerLevel level, String dimension, ServerPlayer player) {
        BuildSlot slot = targetSlot(player, dimension);
        if (slot == null) {
            clear(player.getUUID());
            return;
        }
        BuildPieceState piece = state.get(slot);
        if (piece == null) {
            clear(player.getUUID());
            return;
        }
        Vec3 origin = healthDisplayOrigin(slot);
        ActiveHealthView view = ACTIVE_VIEWS.get(player.getUUID());
        if (view == null || view.display().isRemoved() || !view.dimension().equals(dimension) || !view.slot().equals(slot)) {
            clear(player.getUUID());
            Display.TextDisplay display = new Display.TextDisplay(TEXT_DISPLAY_TYPE, level);
            display.setNoGravity(true);
            configure(display);
            if (!level.addFreshEntity(display)) {
                display.discard();
                return;
            }
            view = new ActiveHealthView(dimension, slot, display);
            ACTIVE_VIEWS.put(player.getUUID(), view);
        }
        view.display().setPos(origin.x(), origin.y(), origin.z());
        ((TextDisplayAccessor) view.display()).fortniteinminecraft$setText(
                Component.literal(healthText(piece)).withStyle(ChatFormatting.GREEN)
        );
    }

    private static BuildSlot targetSlot(ServerPlayer player, String dimension) {
        HitResult hit = player.pick(LOOK_RANGE_BLOCKS, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        return materializer.topOwnerAt(dimension, blockHit.getBlockPos());
    }

    private static Vec3 healthDisplayOrigin(BuildSlot slot) {
        List<BlockPos> positions = materializer.trackedBlockPositions(slot);
        if (positions.isEmpty()) {
            return Vec3.atCenterOf(new BlockPos(slot.gridPos().x(), slot.gridPos().y(), slot.gridPos().z()));
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1.0D);
            maxY = Math.max(maxY, pos.getY() + 1.0D);
            maxZ = Math.max(maxZ, pos.getZ() + 1.0D);
        }
        return new Vec3((minX + maxX) * 0.5D, maxY + 0.35D, (minZ + maxZ) * 0.5D);
    }

    private static void configure(Display.TextDisplay display) {
        TextDisplayAccessor textAccessor = (TextDisplayAccessor) display;
        textAccessor.fortniteinminecraft$setLineWidth(TEXT_LINE_WIDTH);
        textAccessor.fortniteinminecraft$setBackgroundColor(TRANSPARENT_BACKGROUND);
        textAccessor.fortniteinminecraft$setTextOpacity((byte) 255);
        textAccessor.fortniteinminecraft$setFlags((byte) Display.TextDisplay.FLAG_SHADOW);

        DisplayAccessor displayAccessor = (DisplayAccessor) display;
        displayAccessor.fortniteinminecraft$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        displayAccessor.fortniteinminecraft$setBrightnessOverride(Brightness.FULL_BRIGHT);
        displayAccessor.fortniteinminecraft$setViewRange(0.75F);
        displayAccessor.fortniteinminecraft$setShadowRadius(0.0F);
        displayAccessor.fortniteinminecraft$setShadowStrength(0.0F);
        displayAccessor.fortniteinminecraft$setWidth(3.0F);
        displayAccessor.fortniteinminecraft$setHeight(0.6F);
        displayAccessor.fortniteinminecraft$setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                new Quaternionf()
        ));
    }

    private static void clear(UUID playerId) {
        ActiveHealthView view = ACTIVE_VIEWS.remove(playerId);
        if (view != null) {
            view.display().discard();
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.TextDisplay> textDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("text_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:text_display entity type");
        }
        return (EntityType<Display.TextDisplay>) type;
    }

    private record ActiveHealthView(String dimension, BuildSlot slot, Display.TextDisplay display) {
        private ActiveHealthView {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(display, "display");
        }
    }
}
