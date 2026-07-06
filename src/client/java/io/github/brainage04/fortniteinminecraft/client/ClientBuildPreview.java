package io.github.brainage04.fortniteinminecraft.client;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.core.edit.BuildEditGrids;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.EditModePayload;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ClientBuildPreview {
    private static final BuildRules RULES = BuildRules.defaults();
    private static final FootprintProjector FOOTPRINTS = new FootprintProjector(RULES);
    private static final SnapGrid SNAP_GRID = new SnapGrid(RULES);
    private static final float PREVIEW_CELL_SIZE_BLOCKS = 1.0F;
    private static final float PREVIEW_FACE_THICKNESS_BLOCKS = 0.025F;
    private static final float GRID_LINE_THICKNESS_BLOCKS = 0.045F;
    private static final float GRID_FACE_OFFSET_BLOCKS = 0.04F;
    private static final long SERVER_PREVIEW_TIMEOUT_TICKS = 5L;
    private static final EntityType<Display.BlockDisplay> BLOCK_DISPLAY_TYPE = blockDisplayType();
    private static final List<Display.BlockDisplay> ACTIVE_DISPLAYS = new ArrayList<>();

    private static boolean registered;
    private static Snapshot snapshot = Snapshot.inactive();
    private static ServerPreview serverPreview = ServerPreview.inactive();
    private static EditPreview editPreview = EditPreview.inactive();
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

    public static float previewCellSizeBlocks() {
        return PREVIEW_CELL_SIZE_BLOCKS;
    }

    public static void acceptServerPreview(BuildPreviewPayload payload) {
        Objects.requireNonNull(payload, "payload");
        Minecraft client = Minecraft.getInstance();
        long tick = client.level == null ? 0L : client.level.getGameTime();
        serverPreview = ServerPreview.from(payload, tick);
    }

    public static void acceptEditMode(EditModePayload payload) {
        Objects.requireNonNull(payload, "payload");
        editPreview = EditPreview.from(payload);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            activeLevel = null;
            clearDisplays();
            snapshot = Snapshot.inactive();
            serverPreview = ServerPreview.inactive();
            editPreview = EditPreview.inactive();
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
        EditPreview edit = editPreview;
        if (edit.matches(dimension)) {
            return Snapshot.active(
                    edit.pieceType(),
                    edit.material(),
                    edit.slot(),
                    true,
                    editBoxes(edit.slot()),
                    true
            );
        }

        ServerPreview preview = serverPreview;
        if (!preview.matches(dimension, client.level.getGameTime())) {
            return Snapshot.inactive();
        }

        List<PreviewBox> footprintBoxes = previewBoxes(preview.slot());
        List<PreviewBox> visibleBoxes = visiblePreviewBoxes(client.level, footprintBoxes);
        return Snapshot.active(
                preview.pieceType(),
                preview.material(),
                preview.slot(),
                preview.valid(),
                visibleBoxes
        );
    }

    private static void render(ClientLevel level, Snapshot snapshot) {
        clearDisplays();
        BlockState state = BuildVisualBlocks.previewState(snapshot.material(), snapshot.valid());
        renderDisplayBoxes(level, state, previewDisplayBoxes(snapshot.boxes()));
        if (snapshot.editing()) {
            renderDisplayBoxes(level, BuildVisualBlocks.previewState(snapshot.material(), false), editGridLineBoxes(snapshot.slot()));
        }
    }

    private static void renderDisplayBoxes(ClientLevel level, BlockState state, List<DisplayBox> boxes) {
        for (DisplayBox box : boxes) {
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

    private static void configureDisplay(Display.BlockDisplay display, DisplayBox box, BlockState state) {
        display.setPos(box.x(), box.y(), box.z());
        display.setBlockState(state);
        display.setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.setViewRange(1.0F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(Math.max(box.scaleX(), box.scaleZ()));
        display.setHeight(box.scaleY());
        display.setTransformation(new Transformation(
                new Vector3f(box.translateX(), box.translateY(), box.translateZ()),
                new Quaternionf(),
                new Vector3f(box.scaleX(), box.scaleY(), box.scaleZ()),
                new Quaternionf()
        ));
    }

    private static List<DisplayBox> previewDisplayBoxes(List<PreviewBox> boxes) {
        HashSet<BlockPos> occupied = occupiedPreviewCells(boxes);
        HashMap<FacePlane, HashSet<FaceCell>> facesByPlane = new HashMap<>();
        for (BlockPos cell : occupied) {
            for (Direction direction : Direction.values()) {
                if (!occupied.contains(cell.relative(direction))) {
                    FacePlane plane = facePlane(cell, direction);
                    facesByPlane.computeIfAbsent(plane, ignored -> new HashSet<>()).add(faceCell(cell, direction));
                }
            }
        }

        ArrayList<FacePlane> planes = new ArrayList<>(facesByPlane.keySet());
        planes.sort(Comparator
                .comparingInt((FacePlane plane) -> plane.direction().ordinal())
                .thenComparingInt(FacePlane::plane));

        ArrayList<DisplayBox> displayBoxes = new ArrayList<>();
        for (FacePlane plane : planes) {
            for (FaceRect rect : mergedFaceRects(facesByPlane.get(plane))) {
                displayBoxes.add(faceDisplayBox(plane, rect));
            }
        }
        return List.copyOf(displayBoxes);
    }

    private static HashSet<BlockPos> occupiedPreviewCells(List<PreviewBox> boxes) {
        HashSet<BlockPos> occupied = new HashSet<>();
        for (PreviewBox box : boxes) {
            BlockPos origin = box.origin();
            for (int x = 0; x < box.sizeX(); x++) {
                for (int y = 0; y < box.sizeY(); y++) {
                    for (int z = 0; z < box.sizeZ(); z++) {
                        occupied.add(new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z));
                    }
                }
            }
        }
        return occupied;
    }

    private static FacePlane facePlane(BlockPos cell, Direction direction) {
        return switch (direction) {
            case DOWN -> new FacePlane(direction, cell.getY());
            case UP -> new FacePlane(direction, cell.getY() + 1);
            case NORTH -> new FacePlane(direction, cell.getZ());
            case SOUTH -> new FacePlane(direction, cell.getZ() + 1);
            case WEST -> new FacePlane(direction, cell.getX());
            case EAST -> new FacePlane(direction, cell.getX() + 1);
        };
    }

    private static FaceCell faceCell(BlockPos cell, Direction direction) {
        return switch (direction) {
            case DOWN, UP -> new FaceCell(cell.getX(), cell.getZ());
            case NORTH, SOUTH -> new FaceCell(cell.getX(), cell.getY());
            case WEST, EAST -> new FaceCell(cell.getZ(), cell.getY());
        };
    }

    private static List<FaceRect> mergedFaceRects(Set<FaceCell> cells) {
        HashSet<FaceCell> remaining = new HashSet<>(cells);
        ArrayList<FaceRect> rects = new ArrayList<>();
        while (!remaining.isEmpty()) {
            FaceCell start = minFaceCell(remaining);
            int width = 1;
            while (remaining.contains(new FaceCell(start.u() + width, start.v()))) {
                width++;
            }

            int height = 1;
            while (containsFaceRow(remaining, start.u(), start.v() + height, width)) {
                height++;
            }

            for (int v = 0; v < height; v++) {
                for (int u = 0; u < width; u++) {
                    remaining.remove(new FaceCell(start.u() + u, start.v() + v));
                }
            }
            rects.add(new FaceRect(start.u(), start.v(), width, height));
        }
        return rects;
    }

    private static FaceCell minFaceCell(Set<FaceCell> cells) {
        FaceCell min = null;
        for (FaceCell cell : cells) {
            if (min == null || cell.v() < min.v() || cell.v() == min.v() && cell.u() < min.u()) {
                min = cell;
            }
        }
        return Objects.requireNonNull(min, "min");
    }

    private static boolean containsFaceRow(Set<FaceCell> cells, int u, int v, int width) {
        for (int offset = 0; offset < width; offset++) {
            if (!cells.contains(new FaceCell(u + offset, v))) {
                return false;
            }
        }
        return true;
    }

    private static DisplayBox faceDisplayBox(FacePlane plane, FaceRect rect) {
        float thickness = PREVIEW_FACE_THICKNESS_BLOCKS;
        return switch (plane.direction()) {
            case DOWN -> new DisplayBox(rect.u(), plane.plane(), rect.v(), 0.0F, 0.0F, 0.0F, rect.width(), thickness, rect.height());
            case UP -> new DisplayBox(rect.u(), plane.plane() - thickness, rect.v(), 0.0F, 0.0F, 0.0F, rect.width(), thickness, rect.height());
            case NORTH -> new DisplayBox(rect.u(), rect.v(), plane.plane(), 0.0F, 0.0F, 0.0F, rect.width(), rect.height(), thickness);
            case SOUTH -> new DisplayBox(rect.u(), rect.v(), plane.plane() - thickness, 0.0F, 0.0F, 0.0F, rect.width(), rect.height(), thickness);
            case WEST -> new DisplayBox(plane.plane(), rect.v(), rect.u(), 0.0F, 0.0F, 0.0F, thickness, rect.height(), rect.width());
            case EAST -> new DisplayBox(plane.plane() - thickness, rect.v(), rect.u(), 0.0F, 0.0F, 0.0F, thickness, rect.height(), rect.width());
        };
    }


    private static List<DisplayBox> editGridLineBoxes(BuildSlot slot) {
        BlockOffset origin = SNAP_GRID.blockOrigin(slot.gridPos());
        int tile = RULES.footprintSizeBlocks();
        int height = RULES.wallHeightBlocks();
        return slot.pieceType() == PieceType.WALL
                ? wallGridLineBoxes(slot, origin, tile, height)
                : flatGridLineBoxes(origin, tile, slot.pieceType());
    }

    private static List<DisplayBox> wallGridLineBoxes(BuildSlot slot, BlockOffset origin, int tile, int height) {
        int columns = BuildEditGrids.columns(slot.pieceType());
        int rows = BuildEditGrids.rows(slot.pieceType());
        int max = tile - 1;
        float line = GRID_LINE_THICKNESS_BLOCKS;
        ArrayList<DisplayBox> boxes = new ArrayList<>(columns + rows + 2);
        switch (slot.orientation()) {
            case NORTH -> {
                double z = origin.z() - GRID_FACE_OFFSET_BLOCKS;
                addNorthSouthWallGrid(boxes, origin.x(), origin.y(), z, tile, height, columns, rows, line);
            }
            case SOUTH -> {
                double z = origin.z() + max + 1.0D + GRID_FACE_OFFSET_BLOCKS;
                addNorthSouthWallGrid(boxes, origin.x(), origin.y(), z, tile, height, columns, rows, line);
            }
            case EAST -> {
                double x = origin.x() + max + 1.0D + GRID_FACE_OFFSET_BLOCKS;
                addEastWestWallGrid(boxes, x, origin.y(), origin.z(), tile, height, columns, rows, line);
            }
            case WEST -> {
                double x = origin.x() - GRID_FACE_OFFSET_BLOCKS;
                addEastWestWallGrid(boxes, x, origin.y(), origin.z(), tile, height, columns, rows, line);
            }
        }
        return List.copyOf(boxes);
    }

    private static void addNorthSouthWallGrid(
            ArrayList<DisplayBox> boxes,
            double originX,
            double originY,
            double planeZ,
            int tile,
            int height,
            int columns,
            int rows,
            float line
    ) {
        for (int column = 0; column <= columns; column++) {
            double x = originX + (double) column * tile / columns - line * 0.5D;
            boxes.add(new DisplayBox(x, originY, planeZ, 0.0F, 0.0F, 0.0F, line, height, line));
        }
        for (int row = 0; row <= rows; row++) {
            double y = originY + (double) row * height / rows - line * 0.5D;
            boxes.add(new DisplayBox(originX, y, planeZ, 0.0F, 0.0F, 0.0F, tile, line, line));
        }
    }

    private static void addEastWestWallGrid(
            ArrayList<DisplayBox> boxes,
            double planeX,
            double originY,
            double originZ,
            int tile,
            int height,
            int columns,
            int rows,
            float line
    ) {
        for (int column = 0; column <= columns; column++) {
            double z = originZ + (double) column * tile / columns - line * 0.5D;
            boxes.add(new DisplayBox(planeX, originY, z, 0.0F, 0.0F, 0.0F, line, height, line));
        }
        for (int row = 0; row <= rows; row++) {
            double y = originY + (double) row * height / rows - line * 0.5D;
            boxes.add(new DisplayBox(planeX, y, originZ, 0.0F, 0.0F, 0.0F, line, line, tile));
        }
    }

    private static List<DisplayBox> flatGridLineBoxes(BlockOffset origin, int tile, PieceType pieceType) {
        int columns = BuildEditGrids.columns(pieceType);
        int rows = BuildEditGrids.rows(pieceType);
        float line = GRID_LINE_THICKNESS_BLOCKS;
        double y = origin.y() + (pieceType == PieceType.FLOOR ? 1.0D : 0.0D) + GRID_FACE_OFFSET_BLOCKS;
        ArrayList<DisplayBox> boxes = new ArrayList<>(columns + rows + 2);
        for (int column = 0; column <= columns; column++) {
            double x = origin.x() + (double) column * tile / columns - line * 0.5D;
            boxes.add(new DisplayBox(x, y, origin.z(), 0.0F, 0.0F, 0.0F, line, line, tile));
        }
        for (int row = 0; row <= rows; row++) {
            double z = origin.z() + (double) row * tile / rows - line * 0.5D;
            boxes.add(new DisplayBox(origin.x(), y, z, 0.0F, 0.0F, 0.0F, tile, line, line));
        }
        return List.copyOf(boxes);
    }

    private static List<PreviewBox> previewBoxes(BuildSlot slot) {
        return boxesForOffsets(slot, previewFootprintOffsets(slot));
    }

    private static List<PreviewBox> visiblePreviewBoxes(ClientLevel level, List<PreviewBox> footprintBoxes) {
        ArrayList<PreviewBox> visible = new ArrayList<>(footprintBoxes.size());
        for (PreviewBox box : footprintBoxes) {
            if (!isBlockedByWorld(level, box.origin())) {
                visible.add(box);
            }
        }
        return List.copyOf(visible);
    }

    private static boolean isBlockedByWorld(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.canBeReplaced() && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static List<PreviewBox> editBoxes(BuildSlot slot) {
        return boxesForOffsets(slot, previewFootprintOffsets(slot));
    }

    private static List<PreviewBox> boxesForOffsets(BuildSlot slot, List<BlockOffset> offsets) {
        BlockOffset origin = SNAP_GRID.blockOrigin(slot.gridPos());
        ArrayList<PreviewBox> boxes = new ArrayList<>(offsets.size());
        for (BlockOffset local : offsets) {
            boxes.add(new PreviewBox(
                    new BlockPos(origin.x() + local.x(), origin.y() + local.y(), origin.z() + local.z()),
                    1,
                    1,
                    1
            ));
        }
        boxes.sort(Comparator
                .comparingInt((PreviewBox box) -> box.origin().getY())
                .thenComparingInt(box -> box.origin().getZ())
                .thenComparingInt(box -> box.origin().getX()));
        return List.copyOf(boxes);
    }

    private static List<BlockOffset> previewFootprintOffsets(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return FOOTPRINTS.project(slot).localBlocks();
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

    private record DisplayBox(
            double x,
            double y,
            double z,
            float translateX,
            float translateY,
            float translateZ,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
    }

    private record FacePlane(Direction direction, int plane) {
        private FacePlane {
            Objects.requireNonNull(direction, "direction");
        }
    }

    private record FaceCell(int u, int v) {
    }

    private record FaceRect(int u, int v, int width, int height) {
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

    private record EditPreview(
            boolean active,
            String dimension,
            PieceType pieceType,
            MaterialType material,
            BuildSlot slot,
            int selectedMask
    ) {
        private static EditPreview inactive() {
            return new EditPreview(false, "", null, null, null, 0);
        }

        private static EditPreview from(EditModePayload payload) {
            if (!payload.active()) {
                return inactive();
            }
            BuildSlot slot = new BuildSlot(
                    new BuildGridPos(payload.dimension(), payload.gridX(), payload.gridY(), payload.gridZ()),
                    payload.pieceType(),
                    payload.orientation()
            );
            return new EditPreview(true, payload.dimension(), payload.pieceType(), payload.material(), slot, payload.selectedMask());
        }

        private boolean matches(String currentDimension) {
            return active && Objects.equals(dimension, currentDimension);
        }
    }

    public record Snapshot(
            boolean active,
            PieceType pieceType,
            MaterialType material,
            BuildSlot slot,
            boolean valid,
            List<PreviewBox> boxes,
            boolean editing
    ) {
        private static Snapshot inactive() {
            return new Snapshot(false, null, null, null, false, List.of(), false);
        }

        private static Snapshot active(
                PieceType pieceType,
                MaterialType material,
                BuildSlot slot,
                boolean valid,
                List<PreviewBox> boxes
        ) {
            return active(pieceType, material, slot, valid, boxes, false);
        }

        private static Snapshot active(
                PieceType pieceType,
                MaterialType material,
                BuildSlot slot,
                boolean valid,
                List<PreviewBox> boxes,
                boolean editing
        ) {
            Objects.requireNonNull(pieceType, "pieceType");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(slot, "slot");
            return new Snapshot(true, pieceType, material, slot, valid, List.copyOf(boxes), editing);
        }

        private boolean sameVisual(Snapshot other) {
            return active == other.active
                    && valid == other.valid
                    && editing == other.editing
                    && Objects.equals(material, other.material)
                    && Objects.equals(slot, other.slot)
                    && Objects.equals(boxes, other.boxes);
        }
    }
}
