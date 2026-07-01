package io.github.brainage04.fortniteinminecraft.client;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ClientBuildPreview {
    private static final BuildRules RULES = BuildRules.defaults();
    private static final FootprintProjector FOOTPRINTS = new FootprintProjector(RULES);
    private static final SnapGrid SNAP_GRID = new SnapGrid(RULES);
    private static final float PREVIEW_OUTSET_BLOCKS = 0.08F;
    private static final long SERVER_PREVIEW_TIMEOUT_TICKS = 5L;
    private static final EntityType<Display.BlockDisplay> BLOCK_DISPLAY_TYPE = blockDisplayType();
    private static final List<Display.BlockDisplay> ACTIVE_DISPLAYS = new ArrayList<>();

    private static boolean registered;
    private static Snapshot snapshot = Snapshot.inactive();
    private static ServerPreview serverPreview = ServerPreview.inactive();
    private static ClientLevel activeLevel;
    private static int nextLocalEntityId = -2_000_000_000;

    private ClientBuildPreview() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientBuildPreview::tick);
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void acceptServerPreview(BuildPreviewPayload payload) {
        Objects.requireNonNull(payload, "payload");
        Minecraft client = Minecraft.getInstance();
        long tick = client.level == null ? 0L : client.level.getGameTime();
        serverPreview = ServerPreview.from(payload, tick);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            activeLevel = null;
            clearDisplays();
            snapshot = Snapshot.inactive();
            serverPreview = ServerPreview.inactive();
            return;
        }

        Snapshot next = computeSnapshot(client);
        if (!next.active()) {
            clearDisplays();
            snapshot = next;
            return;
        }

        if (client.level != activeLevel || !next.sameVisual(snapshot)) {
            render(client.level, next);
            activeLevel = client.level;
        }
        snapshot = next;
    }

    private static Snapshot computeSnapshot(Minecraft client) {
        String dimension = client.level.dimension().identifier().toString();
        ServerPreview preview = serverPreview;
        if (!preview.matches(dimension, client.level.getGameTime())) {
            return Snapshot.inactive();
        }


        return Snapshot.active(
                preview.pieceType(),
                preview.material(),
                preview.slot(),
                preview.valid(),
                previewBoxes(preview.slot())
        );
    }

    private static void render(ClientLevel level, Snapshot snapshot) {
        clearDisplays();
        BlockState state = BuildVisualBlocks.previewState(snapshot.material(), snapshot.valid());
        for (PreviewBox box : snapshot.boxes()) {
            Display.BlockDisplay display = new Display.BlockDisplay(BLOCK_DISPLAY_TYPE, level);
            display.setId(nextLocalEntityId++);
            display.setUUID(UUID.randomUUID());
            display.setNoGravity(true);
            display.setSilent(true);
            configureDisplay(display, box, state);
            level.addEntity(display);
            ACTIVE_DISPLAYS.add(display);
        }
    }

    private static void clearDisplays() {
        for (Display.BlockDisplay display : ACTIVE_DISPLAYS) {
            display.discard();
        }
        ACTIVE_DISPLAYS.clear();
    }

    private static void configureDisplay(Display.BlockDisplay display, PreviewBox box, BlockState state) {
        BlockPos origin = box.origin();
        display.setPos(origin.getX(), origin.getY(), origin.getZ());
        display.setBlockState(state);
        display.setBrightnessOverride(Brightness.FULL_BRIGHT);
        float outset = PREVIEW_OUTSET_BLOCKS;
        display.setViewRange(1.0F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(Math.max(box.sizeX(), box.sizeZ()) + outset * 2.0F);
        display.setHeight(box.sizeY() + outset * 2.0F);
        display.setTransformation(new Transformation(
                new Vector3f(-outset, -outset, -outset),
                new Quaternionf(),
                new Vector3f(
                        box.sizeX() + outset * 2.0F,
                        box.sizeY() + outset * 2.0F,
                        box.sizeZ() + outset * 2.0F
                ),
                new Quaternionf()
        ));
    }

    private static List<PreviewBox> previewBoxes(BuildSlot slot) {
        PieceFootprint footprint = FOOTPRINTS.project(slot);
        BlockOffset origin = SNAP_GRID.blockOrigin(slot.gridPos());
        ArrayList<PreviewBox> boxes = new ArrayList<>(footprint.localBlocks().size());
        for (BlockOffset local : footprint.localBlocks()) {
            boxes.add(new PreviewBox(new BlockPos(origin.x() + local.x(), origin.y() + local.y(), origin.z() + local.z()), 1, 1, 1));
        }
        return List.copyOf(boxes);
    }


    @SuppressWarnings("unchecked")
    private static EntityType<Display.BlockDisplay> blockDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("block_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:block_display entity type");
        }
        return (EntityType<Display.BlockDisplay>) type;
    }

    public record PreviewBox(BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        public PreviewBox {
            Objects.requireNonNull(origin, "origin");
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                throw new IllegalArgumentException("preview box sizes must be positive");
            }
        }
    }

    private record ServerPreview(
            boolean active,
            String dimension,
            PieceType pieceType,
            MaterialType material,
            BuildSlot slot,
            boolean valid,
            long receivedTick
    ) {
        private static ServerPreview inactive() {
            return new ServerPreview(false, "", null, null, null, false, 0L);
        }

        private static ServerPreview from(BuildPreviewPayload payload, long receivedTick) {
            if (!payload.active()) {
                return inactive();
            }
            BuildSlot slot = new BuildSlot(
                    new BuildGridPos(payload.dimension(), payload.gridX(), payload.gridY(), payload.gridZ()),
                    payload.pieceType(),
                    payload.orientation()
            );
            return new ServerPreview(
                    true,
                    payload.dimension(),
                    payload.pieceType(),
                    payload.material(),
                    slot,
                    payload.valid(),
                    receivedTick
            );
        }

        private boolean matches(String currentDimension, long currentTick) {
            return active
                    && currentTick - receivedTick <= SERVER_PREVIEW_TIMEOUT_TICKS
                    && Objects.equals(dimension, currentDimension);
        }
    }

    public record Snapshot(
            boolean active,
            PieceType pieceType,
            MaterialType material,
            BuildSlot slot,
            boolean valid,
            List<PreviewBox> boxes
    ) {
        private static Snapshot inactive() {
            return new Snapshot(false, null, null, null, false, List.of());
        }

        private static Snapshot active(
                PieceType pieceType,
                MaterialType material,
                BuildSlot slot,
                boolean valid,
                List<PreviewBox> boxes
        ) {
            Objects.requireNonNull(pieceType, "pieceType");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(slot, "slot");
            return new Snapshot(true, pieceType, material, slot, valid, List.copyOf(boxes));
        }

        private boolean sameVisual(Snapshot other) {
            return active == other.active
                    && valid == other.valid
                    && Objects.equals(material, other.material)
                    && Objects.equals(slot, other.slot)
                    && Objects.equals(boxes, other.boxes);
        }
    }
}
