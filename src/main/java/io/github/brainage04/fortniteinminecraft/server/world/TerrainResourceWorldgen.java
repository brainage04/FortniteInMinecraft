package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class TerrainResourceWorldgen {
    static final int ATTEMPTS_PER_CHUNK = 3;
    static final int CHUNK_GENERATION_CHANCE = 2;
    private static final List<List<BlockOffset>> ROCK_SHAPES = List.of(
            List.of(new BlockOffset(0, 0, 0), new BlockOffset(1, 0, 0), new BlockOffset(0, 0, 1)),
            List.of(new BlockOffset(0, 0, 0), new BlockOffset(1, 0, 0), new BlockOffset(0, 0, 1), new BlockOffset(0, 1, 0)),
            List.of(
                    new BlockOffset(0, 0, 0),
                    new BlockOffset(1, 0, 0),
                    new BlockOffset(-1, 0, 0),
                    new BlockOffset(0, 0, 1),
                    new BlockOffset(0, 0, -1),
                    new BlockOffset(0, 1, 0)
            ),
            List.of(
                    new BlockOffset(0, 0, 0),
                    new BlockOffset(1, 0, 0),
                    new BlockOffset(0, 0, 1),
                    new BlockOffset(1, 0, 1),
                    new BlockOffset(0, 1, 0),
                    new BlockOffset(1, 1, 1)
            )
    );
    private static final List<BlockState> STONE_ROCK_STATES = List.of(
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
            Blocks.TUFF.defaultBlockState()
    );
    private static final List<BlockState> METAL_ROCK_STATES = List.of(
            Blocks.RAW_COPPER_BLOCK.defaultBlockState(),
            Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState()
    );
    private static boolean registered;

    private TerrainResourceWorldgen() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerChunkEvents.CHUNK_LOAD.register(TerrainResourceWorldgen::onChunkLoad);
        registered = true;
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean generated) {
        if (!generated) {
            return;
        }
        Random random = chunkRandom(level, chunk.getPos());
        if (random.nextInt(CHUNK_GENERATION_CHANCE) != 0) {
            return;
        }
        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            tryPlaceRock(level, chunk, random);
        }
    }

    private static Random chunkRandom(ServerLevel level, ChunkPos pos) {
        long seed = level.getSeed()
                ^ (long) pos.x() * 341873128712L
                ^ (long) pos.z() * 132897987541L
                ^ 0x4F1BBCDC79B65A2BL;
        return new Random(seed);
    }

    private static boolean tryPlaceRock(ServerLevel level, LevelChunk chunk, Random random) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(random, "random");
        MaterialType material = random.nextInt(4) == 0 ? MaterialType.METAL : MaterialType.STONE;
        List<BlockState> palette = material == MaterialType.METAL ? METAL_ROCK_STATES : STONE_ROCK_STATES;
        List<BlockOffset> shape = ROCK_SHAPES.get(random.nextInt(ROCK_SHAPES.size()));
        ChunkPos chunkPos = chunk.getPos();
        int localX = 2 + random.nextInt(12);
        int localZ = 2 + random.nextInt(12);
        int x = chunkPos.getBlockX(localX);
        int z = chunkPos.getBlockZ(localZ);
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
        BlockPos anchor = new BlockPos(x, y, z);
        List<BlockPos> positions = positions(anchor, shape);
        if (!canPlace(level, chunk, anchor, positions)) {
            return false;
        }
        for (BlockPos pos : positions) {
            chunk.setBlockState(pos, palette.get(random.nextInt(palette.size())), Block.UPDATE_ALL);
        }
        return true;
    }

    private static List<BlockPos> positions(BlockPos anchor, List<BlockOffset> footprint) {
        return footprint.stream()
                .map(offset -> anchor.offset(offset.x(), offset.y(), offset.z()))
                .map(BlockPos::immutable)
                .toList();
    }

    private static boolean canPlace(ServerLevel level, LevelChunk chunk, BlockPos anchor, List<BlockPos> positions) {
        BlockState support = chunk.getBlockState(anchor.below());
        if (support.isAir() || !support.getFluidState().isEmpty() || support.canBeReplaced()) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (!level.isInWorldBounds(pos) || !chunk.getPos().contains(pos)) {
                return false;
            }
            BlockState state = chunk.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }
}
