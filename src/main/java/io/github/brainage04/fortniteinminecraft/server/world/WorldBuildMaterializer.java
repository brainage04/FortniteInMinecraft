package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorldBuildMaterializer {
    private static final int BLOCK_UPDATE_FLAGS = Block.UPDATE_ALL;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final SnapGrid snapGrid;
    private final EnumMap<MaterialType, BlockState> solidBlockStatesByMaterial;
    private final EnumMap<MaterialType, BlockState> hologramBlockStatesByMaterial;
    private final Map<BuildSlot, List<BlockPos>> placedBlocksBySlot = new HashMap<>();
    private final Map<BuildSlot, BuildPieceState> piecesBySlot = new HashMap<>();
    private final Map<WorldBlockKey, LinkedHashSet<BuildSlot>> ownersByBlock = new HashMap<>();
    private final Map<WorldBlockKey, BlockState> originalStatesByBlock = new HashMap<>();

    public WorldBuildMaterializer(BuildRules rules, Map<MaterialType, List<BlockState>> blockPalettesByMaterial) {
        this.snapGrid = new SnapGrid(Objects.requireNonNull(rules, "rules"));
        Objects.requireNonNull(blockPalettesByMaterial, "blockPalettesByMaterial");
        this.solidBlockStatesByMaterial = new EnumMap<>(MaterialType.class);
        this.hologramBlockStatesByMaterial = new EnumMap<>(MaterialType.class);
        for (MaterialType material : MaterialType.values()) {
            List<BlockState> palette = blockPalettesByMaterial.get(material);
            if (palette == null || palette.isEmpty()) {
                throw new IllegalArgumentException("missing block palette for " + material);
            }
            solidBlockStatesByMaterial.put(material, palette.getFirst());
            hologramBlockStatesByMaterial.put(material, BuildVisualBlocks.hologramState(material));
        }
    }

    public static WorldBuildMaterializer defaults(BuildRules rules) {
        EnumMap<MaterialType, List<BlockState>> palettes = new EnumMap<>(MaterialType.class);
        palettes.put(MaterialType.WOOD, List.of(Blocks.OAK_PLANKS.defaultBlockState()));
        palettes.put(MaterialType.STONE, List.of(Blocks.STONE_BRICKS.defaultBlockState()));
        palettes.put(MaterialType.METAL, List.of(Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState()));
        return new WorldBuildMaterializer(rules, palettes);
    }

    public WorldBuildWriteResult place(ServerLevel level, BuildPieceState piece, PieceFootprint footprint) {
        Objects.requireNonNull(level, "level");
        return place(piece, footprint, new ServerLevelBlockWriter(level));
    }

    public WorldBuildWriteResult clear(ServerLevel level, BuildPieceState piece) {
        Objects.requireNonNull(level, "level");
        return clear(piece, new ServerLevelBlockWriter(level));
    }

    public List<BlockPos> blockPositions(PieceFootprint footprint) {
        Objects.requireNonNull(footprint, "footprint");
        BuildGridPos gridPos = footprint.slot().gridPos();
        BlockOffset origin = snapGrid.blockOrigin(gridPos);
        ArrayList<BlockPos> positions = new ArrayList<>(footprint.localBlocks().size());
        for (BlockOffset local : footprint.localBlocks()) {
            positions.add(new BlockPos(origin.x() + local.x(), origin.y() + local.y(), origin.z() + local.z()));
        }
        return List.copyOf(positions);
    }

    public BlockState blockStateFor(MaterialType material) {
        return solidBlockStatesByMaterial.get(Objects.requireNonNull(material, "material"));
    }

    BlockState blockStateFor(BuildPieceState piece, BlockPos pos) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(pos, "pos");
        return hologramVisible(piece, pos)
                ? hologramBlockStatesByMaterial.get(piece.material())
                : blockStateFor(piece.material());
    }

    public int trackedBlockCount(BuildSlot slot) {
        List<BlockPos> positions = placedBlocksBySlot.get(Objects.requireNonNull(slot, "slot"));
        return positions == null ? 0 : positions.size();
    }

    public List<BlockPos> trackedBlockPositions(BuildSlot slot) {
        List<BlockPos> positions = placedBlocksBySlot.get(Objects.requireNonNull(slot, "slot"));
        return positions == null ? List.of() : List.copyOf(positions);
    }

    public int ownedBlockCount(String dimension, BlockPos pos) {
        Set<BuildSlot> owners = ownersByBlock.get(new WorldBlockKey(dimension, pos));
        return owners == null ? 0 : owners.size();
    }

    public boolean isTrackedBlock(String dimension, int x, int y, int z) {
        return ownersByBlock.containsKey(new WorldBlockKey(dimension, new BlockPos(x, y, z)));
    }

    public BlockState originalBlockState(String dimension, int x, int y, int z) {
        return originalStatesByBlock.get(new WorldBlockKey(
                Objects.requireNonNull(dimension, "dimension"),
                new BlockPos(x, y, z)
        ));
    }

    public BuildSlot topOwnerAt(String dimension, BlockPos pos) {
        LinkedHashSet<BuildSlot> owners = ownersByBlock.get(new WorldBlockKey(
                Objects.requireNonNull(dimension, "dimension"),
                Objects.requireNonNull(pos, "pos")
        ));
        return owners == null || owners.isEmpty() ? null : lastOwner(owners);
    }

    WorldBuildWriteResult place(BuildPieceState piece, PieceFootprint footprint, BlockWriter writer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(writer, "writer");
        requireMatchingSlot(piece.slot(), footprint.slot());
        if (placedBlocksBySlot.containsKey(piece.slot())) {
            return WorldBuildWriteResult.failure(0, "build slot already has tracked world blocks");
        }

        BuildSlot slot = piece.slot();
        String dimension = slot.gridPos().dimension();
        List<BlockPos> positions = blockPositions(footprint);
        ArrayList<BlockRestore> rewrites = new ArrayList<>(positions.size());
        ArrayList<BlockSnapshot> blockSnapshots = new ArrayList<>(positions.size());
        piecesBySlot.put(slot, piece);

        for (BlockPos pos : positions) {
            WorldBlockKey key = new WorldBlockKey(dimension, pos);
            BlockSnapshot snapshot = snapshot(key);
            blockSnapshots.add(snapshot);
            BlockState previousState;
            if (snapshot.owners().isEmpty()) {
                previousState = writer.blockState(pos);
                originalStatesByBlock.put(key, previousState);
            } else {
                previousState = visibleState(snapshot.owners(), snapshot.originalState(), pos);
            }
            BlockState blockState = blockStateFor(piece, pos);
            if (!previousState.equals(blockState)) {
                if (!writer.setBlock(pos, blockState)) {
                    rollback(rewrites, writer);
                    restoreSnapshots(blockSnapshots);
                    piecesBySlot.remove(slot);
                    return WorldBuildWriteResult.failure(rewrites.size(), "world write failed at " + describe(pos));
                }
                rewrites.add(new BlockRestore(pos, previousState));
            }
            ownersByBlock.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(slot);
        }
        placedBlocksBySlot.put(slot, List.copyOf(positions));
        return WorldBuildWriteResult.success(rewrites.size(), "world blocks placed");
    }

    WorldBuildWriteResult clear(BuildPieceState piece, BlockWriter writer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(writer, "writer");
        BuildSlot slot = piece.slot();
        List<BlockPos> positions = placedBlocksBySlot.get(slot);
        if (positions == null) {
            return WorldBuildWriteResult.failure(0, "build slot has no tracked world blocks");
        }

        String dimension = slot.gridPos().dimension();
        ArrayList<BlockRestore> rewrites = new ArrayList<>(positions.size());
        ArrayList<BlockSnapshot> blockSnapshots = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            WorldBlockKey key = new WorldBlockKey(dimension, pos);
            BlockSnapshot snapshot = snapshot(key);
            LinkedHashSet<BuildSlot> owners = ownersByBlock.get(key);
            if (owners == null || !owners.contains(slot)) {
                rollback(rewrites, writer);
                restoreSnapshots(blockSnapshots);
                return WorldBuildWriteResult.failure(rewrites.size(), "tracked ownership missing at " + describe(pos));
            }

            blockSnapshots.add(snapshot);
            boolean visibleOwner = slot.equals(lastOwner(owners));
            owners.remove(slot);
            if (owners.isEmpty()) {
                ownersByBlock.remove(key);
            }

            if (visibleOwner) {
                BlockState nextState;
                if (owners.isEmpty()) {
                    nextState = originalStatesByBlock.getOrDefault(key, AIR);
                    originalStatesByBlock.remove(key);
                } else {
                    nextState = blockStateForOwner(lastOwner(owners), pos);
                }
                BlockState previousState = blockStateFor(piece, pos);
                if (!writer.setBlock(pos, nextState)) {
                    rollback(rewrites, writer);
                    restoreSnapshots(blockSnapshots);
                    return WorldBuildWriteResult.failure(rewrites.size(), "world clear failed at " + describe(pos));
                }
                rewrites.add(new BlockRestore(pos, previousState));
            }
        }

        placedBlocksBySlot.remove(slot);
        piecesBySlot.remove(slot);
        return WorldBuildWriteResult.success(rewrites.size(), "world blocks cleared");
    }

    public WorldBuildWriteResult refresh(ServerLevel level, BuildPieceState piece) {
        Objects.requireNonNull(level, "level");
        return refresh(piece, new ServerLevelBlockWriter(level));
    }

    WorldBuildWriteResult refresh(BuildPieceState piece, BlockWriter writer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(writer, "writer");
        BuildSlot slot = piece.slot();
        List<BlockPos> positions = placedBlocksBySlot.get(slot);
        BuildPieceState previousPiece = piecesBySlot.get(slot);
        if (positions == null || previousPiece == null) {
            return WorldBuildWriteResult.failure(0, "build slot has no tracked world blocks");
        }

        String dimension = slot.gridPos().dimension();
        ArrayList<BlockRestore> rewrites = new ArrayList<>(positions.size());
        piecesBySlot.put(slot, piece);
        for (BlockPos pos : positions) {
            LinkedHashSet<BuildSlot> owners = ownersByBlock.get(new WorldBlockKey(dimension, pos));
            if (owners == null || owners.isEmpty() || !slot.equals(lastOwner(owners))) {
                continue;
            }
            BlockState previousState = blockStateFor(previousPiece, pos);
            BlockState nextState = blockStateFor(piece, pos);
            if (previousState.equals(nextState)) {
                continue;
            }
            if (!writer.setBlock(pos, nextState)) {
                rollback(rewrites, writer);
                piecesBySlot.put(slot, previousPiece);
                return WorldBuildWriteResult.failure(rewrites.size(), "world refresh failed at " + describe(pos));
            }
            rewrites.add(new BlockRestore(pos, previousState));
        }
        return WorldBuildWriteResult.success(rewrites.size(), "world blocks refreshed");
    }

    private BlockSnapshot snapshot(WorldBlockKey key) {
        LinkedHashSet<BuildSlot> owners = ownersByBlock.get(key);
        return new BlockSnapshot(
                key,
                owners == null ? new LinkedHashSet<>() : new LinkedHashSet<>(owners),
                originalStatesByBlock.get(key)
        );
    }

    private void restoreSnapshots(List<BlockSnapshot> snapshots) {
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            BlockSnapshot snapshot = snapshots.get(i);
            if (snapshot.owners().isEmpty()) {
                ownersByBlock.remove(snapshot.key());
            } else {
                ownersByBlock.put(snapshot.key(), new LinkedHashSet<>(snapshot.owners()));
            }
            if (snapshot.originalState() == null) {
                originalStatesByBlock.remove(snapshot.key());
            } else {
                originalStatesByBlock.put(snapshot.key(), snapshot.originalState());
            }
        }
    }

    private BlockState visibleState(LinkedHashSet<BuildSlot> owners, BlockState originalState, BlockPos pos) {
        return owners.isEmpty() ? Objects.requireNonNullElse(originalState, AIR) : blockStateForOwner(lastOwner(owners), pos);
    }

    private BlockState blockStateForOwner(BuildSlot slot, BlockPos pos) {
        BuildPieceState piece = piecesBySlot.get(Objects.requireNonNull(slot, "slot"));
        if (piece == null) {
            throw new IllegalStateException("missing piece for tracked build slot");
        }
        return blockStateFor(piece, pos);
    }


    static boolean hologramVisible(BuildPieceState piece, BlockPos pos) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(pos, "pos");
        double solidRatio = Math.clamp(piece.healthRatio(), 0.0D, 1.0D);
        return solidRatio < 1.0D && damageDither(pos) >= solidRatio;
    }

    private static double damageDither(BlockPos pos) {
        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) pos.getX() * 0xBF58476D1CE4E5B9L;
        hash ^= (long) pos.getY() * 0x94D049BB133111EBL;
        hash ^= (long) pos.getZ() * 0xD6E8FEB86659FD93L;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (double) (hash & 0xFFFFL) / 65536.0D;
    }
    private static BuildSlot lastOwner(LinkedHashSet<BuildSlot> owners) {
        BuildSlot last = null;
        for (BuildSlot owner : owners) {
            last = owner;
        }
        return Objects.requireNonNull(last, "owners cannot be empty");
    }

    private static void rollback(List<BlockRestore> rewrites, BlockWriter writer) {
        for (int i = rewrites.size() - 1; i >= 0; i--) {
            BlockRestore restore = rewrites.get(i);
            writer.setBlock(restore.pos(), restore.previousState());
        }
    }

    private static void requireMatchingSlot(BuildSlot pieceSlot, BuildSlot footprintSlot) {
        if (!pieceSlot.equals(footprintSlot)) {
            throw new IllegalArgumentException("piece and footprint slots must match");
        }
    }

    private static String describe(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    interface BlockWriter {
        boolean setBlock(BlockPos pos, BlockState state);

        BlockState blockState(BlockPos pos);
    }

    private record WorldBlockKey(String dimension, BlockPos pos) {
        private WorldBlockKey {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(pos, "pos");
        }
    }

    private record ServerLevelBlockWriter(ServerLevel level) implements BlockWriter {
        private ServerLevelBlockWriter {
            Objects.requireNonNull(level, "level");
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state) {
            return level.setBlock(pos, state, BLOCK_UPDATE_FLAGS);
        }

        @Override
        public BlockState blockState(BlockPos pos) {
            return level.getBlockState(pos);
        }
    }

    private record BlockSnapshot(WorldBlockKey key, LinkedHashSet<BuildSlot> owners, BlockState originalState) {
    }

    private record BlockRestore(BlockPos pos, BlockState previousState) {
    }
}
