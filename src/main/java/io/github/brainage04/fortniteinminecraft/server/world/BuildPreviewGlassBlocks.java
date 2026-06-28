package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import io.github.brainage04.fortniteinminecraft.mixin.BlockDisplayAccessor;
import io.github.brainage04.fortniteinminecraft.mixin.DisplayAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

public final class BuildPreviewGlassBlocks implements BuildPreviewRenderer {
    static final float PREVIEW_OUTSET_BLOCKS = 0.01F;
    private static final int CHUNK_SIZE_BLOCKS = 16;

    private static final BlockState VALID = Blocks.STAINED_GLASS.lightBlue().defaultBlockState();
    private static final BlockState INVALID = Blocks.STAINED_GLASS.red().defaultBlockState();
    private static final EntityType<Display.BlockDisplay> BLOCK_DISPLAY_TYPE = blockDisplayType();

    private final WorldBuildMaterializer materializer;
    private final Map<UUID, ActivePreview> activePreviews = new HashMap<>();

    public BuildPreviewGlassBlocks(WorldBuildMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    @Override
    public PreviewMode mode() {
        return PreviewMode.GLASS;
    }

    @Override
    public int show(ServerLevel level, ServerPlayer player, PieceFootprint footprint, boolean valid) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(footprint, "footprint");

        String dimension = dimensionOf(level);
        BlockState state = previewState(valid);
        List<PreviewVolume> volumes = List.copyOf(previewVolumes(footprint, materializer));
        ActivePreview previous = activePreviews.get(player.getUUID());
        ArrayList<Display.BlockDisplay> displays = new ArrayList<>(volumes.size());
        List<Display.BlockDisplay> reusable = List.of();

        if (previous != null && previous.dimension().equals(dimension)) {
            reusable = previous.displays();
        } else if (previous != null) {
            previous.displays().forEach(Display.BlockDisplay::discard);
        }

        for (int i = 0; i < volumes.size(); i++) {
            PreviewVolume volume = volumes.get(i);
            Display.BlockDisplay display = i < reusable.size() ? reusable.get(i) : null;
            if (display == null || display.isRemoved()) {
                display = spawnDisplay(level, volume, state);
            } else {
                configureDisplay(display, volume, state);
            }
            if (display != null) {
                displays.add(display);
            }
        }

        for (int i = volumes.size(); i < reusable.size(); i++) {
            reusable.get(i).discard();
        }

        activePreviews.put(player.getUUID(), new ActivePreview(dimension, displays));
        return displays.size();
    }

    @Override
    public void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ActivePreview previous = activePreviews.remove(player.getUUID());
        if (previous == null) {
            return;
        }
        previous.displays().forEach(Display.BlockDisplay::discard);
    }

    @Override
    public void clearAll() {
        activePreviews.values().forEach(preview -> preview.displays().forEach(Display.BlockDisplay::discard));
        activePreviews.clear();
    }

    int activePreviewCount() {
        return activePreviews.size();
    }

    @Override
    public String renderedUnit(boolean valid) {
        return valid ? "light blue glass holograms" : "red glass holograms";
    }

    static BlockState previewState(boolean valid) {
        return valid ? VALID : INVALID;
    }

    static LinkedHashSet<PreviewVolume> previewVolumes(PieceFootprint footprint, WorldBuildMaterializer materializer) {
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(materializer, "materializer");
        List<BlockPos> positions = materializer.blockPositions(footprint);
        PieceType pieceType = footprint.slot().pieceType();
        LinkedHashSet<PreviewVolume> volumes = switch (pieceType) {
            case WALL, FLOOR -> singleVolume(positions);
            case STAIR -> stairRowVolumes(positions);
            case ROOF -> unitVolumes(positions);
        };
        return splitAtChunkBorders(volumes);
    }

    private static LinkedHashSet<PreviewVolume> singleVolume(Collection<BlockPos> positions) {
        LinkedHashSet<PreviewVolume> volumes = new LinkedHashSet<>(1);
        if (!positions.isEmpty()) {
            volumes.add(volumeAround(positions));
        }
        return volumes;
    }

    private static LinkedHashSet<PreviewVolume> stairRowVolumes(List<BlockPos> positions) {
        TreeMap<Integer, ArrayList<BlockPos>> rowsByY = new TreeMap<>();
        for (BlockPos pos : positions) {
            rowsByY.computeIfAbsent(pos.getY(), ignored -> new ArrayList<>()).add(pos);
        }

        LinkedHashSet<PreviewVolume> volumes = new LinkedHashSet<>(rowsByY.size());
        for (ArrayList<BlockPos> row : rowsByY.values()) {
            volumes.add(volumeAround(row));
        }
        return volumes;
    }

    private static LinkedHashSet<PreviewVolume> unitVolumes(List<BlockPos> positions) {
        LinkedHashSet<PreviewVolume> volumes = new LinkedHashSet<>(positions.size());
        for (BlockPos pos : positions) {
            volumes.add(new PreviewVolume(pos, 1, 1, 1));
        }
        return volumes;
    }

    private static PreviewVolume volumeAround(Collection<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new PreviewVolume(new BlockPos(minX, minY, minZ), maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    static LinkedHashSet<PreviewVolume> splitAtChunkBorders(Collection<PreviewVolume> volumes) {
        Objects.requireNonNull(volumes, "volumes");
        LinkedHashSet<PreviewVolume> split = new LinkedHashSet<>();
        for (PreviewVolume volume : volumes) {
            split.addAll(splitAtChunkBorders(volume));
        }
        return split;
    }

    static LinkedHashSet<PreviewVolume> splitAtChunkBorders(PreviewVolume volume) {
        Objects.requireNonNull(volume, "volume");
        LinkedHashSet<PreviewVolume> split = new LinkedHashSet<>();
        BlockPos origin = volume.origin();
        List<AxisRange> xRanges = chunkRanges(origin.getX(), volume.sizeX());
        List<AxisRange> zRanges = chunkRanges(origin.getZ(), volume.sizeZ());
        for (AxisRange xRange : xRanges) {
            for (AxisRange zRange : zRanges) {
                split.add(new PreviewVolume(
                        new BlockPos(xRange.start(), origin.getY(), zRange.start()),
                        xRange.size(),
                        volume.sizeY(),
                        zRange.size()
                ));
            }
        }
        return split;
    }

    private static List<AxisRange> chunkRanges(int start, int size) {
        ArrayList<AxisRange> ranges = new ArrayList<>();
        int current = start;
        int end = start + size;
        while (current < end) {
            int chunkEnd = Math.floorDiv(current, CHUNK_SIZE_BLOCKS) * CHUNK_SIZE_BLOCKS + CHUNK_SIZE_BLOCKS;
            int next = Math.min(end, chunkEnd);
            ranges.add(new AxisRange(current, next - current));
            current = next;
        }
        return ranges;
    }

    private static Display.BlockDisplay spawnDisplay(ServerLevel level, PreviewVolume volume, BlockState state) {
        Display.BlockDisplay display = new Display.BlockDisplay(BLOCK_DISPLAY_TYPE, level);
        display.setNoGravity(true);
        configureDisplay(display, volume, state);
        if (!level.addFreshEntity(display)) {
            display.discard();
            return null;
        }
        return display;
    }

    private static void configureDisplay(Display.BlockDisplay display, PreviewVolume volume, BlockState state) {
        BlockPos origin = volume.origin();
        display.setPos(origin.getX(), origin.getY(), origin.getZ());
        setBlockState(display, state);
        setTransformation(display, volume);
    }

    private static void setBlockState(Display.BlockDisplay display, BlockState state) {
        ((BlockDisplayAccessor) display).fortniteinminecraft$setBlockState(state);
    }

    private static void setTransformation(Display.BlockDisplay display, PreviewVolume volume) {
        float outset = PREVIEW_OUTSET_BLOCKS;
        Vector3f translation = new Vector3f(-outset, -outset, -outset);
        Vector3f scale = new Vector3f(
                expandedScale(volume.sizeX(), outset),
                expandedScale(volume.sizeY(), outset),
                expandedScale(volume.sizeZ(), outset)
        );
        ((DisplayAccessor) display).fortniteinminecraft$setTransformation(
                new Transformation(translation, new Quaternionf(), scale, new Quaternionf())
        );
    }

    private static float expandedScale(int blocks, float outset) {
        return blocks + outset * 2.0F;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.BlockDisplay> blockDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("block_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:block_display entity type");
        }
        return (EntityType<Display.BlockDisplay>) type;
    }

    private static String dimensionOf(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    record PreviewVolume(BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        PreviewVolume {
            Objects.requireNonNull(origin, "origin");
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                throw new IllegalArgumentException("preview volume dimensions must be positive");
            }
        }
    }

    private record AxisRange(int start, int size) {
    }

    private record ActivePreview(String dimension, List<Display.BlockDisplay> displays) {
        private ActivePreview {
            Objects.requireNonNull(dimension, "dimension");
            displays = List.copyOf(displays);
        }
    }
}
