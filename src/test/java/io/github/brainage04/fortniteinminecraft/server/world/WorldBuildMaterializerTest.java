package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldBuildMaterializerTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void defaultsMapMaterialsToVisibleBlocks() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());

        assertSame(Blocks.OAK_PLANKS, materializer.blockStateFor(MaterialType.WOOD).getBlock());
        assertSame(Blocks.STONE_BRICKS, materializer.blockStateFor(MaterialType.STONE).getBlock());
        assertSame(Blocks.COPPER_BLOCK.waxed().unaffected(), materializer.blockStateFor(MaterialType.METAL).getBlock());
    }

    @Test
    void projectsFootprintToWorldBlockPositions() {
        BuildRules rules = BuildRules.defaults();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(rules);
        PieceFootprint footprint = footprint(BuildSlot.of("overworld", 2, 1, -1, PieceType.FLOOR, Orientation.NORTH));

        List<BlockPos> positions = materializer.blockPositions(footprint);

        assertEquals(25, positions.size());
        assertEquals(new BlockPos(7, 3, -5), positions.getFirst());
        assertEquals(new BlockPos(11, 3, -1), positions.getLast());
    }

    @Test
    void placeTracksBlocksAndClearRemovesOnlyTrackedBlocks() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        BuildPieceState piece = piece(slot, MaterialType.WOOD, MaterialType.WOOD.finalHealth());
        PieceFootprint footprint = footprint(slot);
        InMemoryBlocks blocks = new InMemoryBlocks();

        WorldBuildWriteResult placed = materializer.place(piece, footprint, blocks);

        assertTrue(placed.success(), placed.message());
        assertEquals(25, placed.blockCount());
        assertEquals(25, materializer.trackedBlockCount(slot));
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), blocks.stateAt(pos));
        }

        WorldBuildWriteResult cleared = materializer.clear(piece, blocks);

        assertTrue(cleared.success(), cleared.message());
        assertEquals(25, cleared.blockCount());
        assertEquals(0, materializer.trackedBlockCount(slot));
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            assertEquals(Blocks.AIR.defaultBlockState(), blocks.stateAt(pos));
        }
        assertEquals(25, blocks.blocks.size());
    }

    @Test
    void overlappingPiecesKeepSharedBlocksUntilLastOwnerClears() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot firstSlot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot secondSlot = BuildSlot.of("overworld", 1, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildPieceState firstPiece = piece(firstSlot, MaterialType.WOOD, MaterialType.WOOD.finalHealth());
        BuildPieceState secondPiece = piece(secondSlot, MaterialType.STONE, MaterialType.STONE.finalHealth());
        PieceFootprint firstFootprint = footprint(firstSlot);
        PieceFootprint secondFootprint = footprint(secondSlot);
        BlockPos shared = new BlockPos(3, -1, -1);
        InMemoryBlocks blocks = new InMemoryBlocks();

        WorldBuildWriteResult firstPlaced = materializer.place(firstPiece, firstFootprint, blocks);
        WorldBuildWriteResult secondPlaced = materializer.place(secondPiece, secondFootprint, blocks);

        assertTrue(firstPlaced.success(), firstPlaced.message());
        assertTrue(secondPlaced.success(), secondPlaced.message());
        assertEquals(25, firstPlaced.blockCount());
        assertEquals(25, secondPlaced.blockCount());
        assertEquals(2, materializer.ownedBlockCount("overworld", shared));
        assertEquals(secondSlot, materializer.topOwnerAt("overworld", shared));
        assertTrue(materializer.isTrackedBlock("overworld", shared.getX(), shared.getY(), shared.getZ()));
        assertEquals(Blocks.STONE_BRICKS.defaultBlockState(), blocks.stateAt(shared));

        WorldBuildWriteResult firstCleared = materializer.clear(firstPiece, blocks);

        assertTrue(firstCleared.success(), firstCleared.message());
        assertEquals(20, firstCleared.blockCount());
        assertEquals(0, materializer.trackedBlockCount(firstSlot));
        assertEquals(25, materializer.trackedBlockCount(secondSlot));
        assertEquals(1, materializer.ownedBlockCount("overworld", shared));
        assertEquals(secondSlot, materializer.topOwnerAt("overworld", shared));
        assertEquals(Blocks.STONE_BRICKS.defaultBlockState(), blocks.stateAt(shared));

        WorldBuildWriteResult secondCleared = materializer.clear(secondPiece, blocks);

        assertTrue(secondCleared.success(), secondCleared.message());
        assertEquals(25, secondCleared.blockCount());
        assertEquals(0, materializer.ownedBlockCount("overworld", shared));
        assertEquals(null, materializer.topOwnerAt("overworld", shared));
        assertEquals(Blocks.AIR.defaultBlockState(), blocks.stateAt(shared));
    }

    @Test
    void placeRollsBackAlreadyWrittenBlocksOnFailure() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildPieceState piece = piece(slot, MaterialType.STONE, MaterialType.STONE.finalHealth());
        PieceFootprint footprint = footprint(slot);
        InMemoryBlocks blocks = new InMemoryBlocks();
        blocks.failOnCall = 3;

        WorldBuildWriteResult result = materializer.place(piece, footprint, blocks);

        assertFalse(result.success());
        assertEquals(3, result.blockCount());
        assertEquals(0, materializer.trackedBlockCount(slot));
        List<BlockPos> positions = materializer.blockPositions(footprint);
        assertEquals(Blocks.AIR.defaultBlockState(), blocks.stateAt(positions.get(0)));
        assertEquals(Blocks.AIR.defaultBlockState(), blocks.stateAt(positions.get(1)));
        assertEquals(Blocks.AIR.defaultBlockState(), blocks.stateAt(positions.get(2)));
    }

    @Test
    void clearRollsBackWhenWorldWriteFails() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildPieceState piece = piece(slot, MaterialType.METAL, MaterialType.METAL.finalHealth());
        PieceFootprint footprint = footprint(slot);
        InMemoryBlocks blocks = new InMemoryBlocks();
        WorldBuildWriteResult placed = materializer.place(piece, footprint, blocks);
        assertTrue(placed.success(), placed.message());
        blocks.failOnCall = blocks.calls + 2;

        WorldBuildWriteResult result = materializer.clear(piece, blocks);

        assertFalse(result.success());
        assertEquals(2, result.blockCount());
        assertEquals(25, materializer.trackedBlockCount(slot));
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            assertEquals(Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState(), blocks.stateAt(pos));
        }
    }


    @Test
    void damagePalettesUseRequestedBlockFamilies() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        BlockPos pos = new BlockPos(0, 0, 0);

        assertSame(Blocks.OAK_PLANKS, materializer.blockStateFor(piece(slot, MaterialType.WOOD, MaterialType.WOOD.finalHealth()), pos).getBlock());
        assertSame(Blocks.DARK_OAK_PLANKS, materializer.blockStateFor(piece(slot, MaterialType.WOOD, 0), pos).getBlock());
        assertSame(Blocks.STONE_BRICKS, materializer.blockStateFor(piece(slot, MaterialType.STONE, MaterialType.STONE.finalHealth()), pos).getBlock());
        assertSame(Blocks.MOSSY_COBBLESTONE, materializer.blockStateFor(piece(slot, MaterialType.STONE, 0), pos).getBlock());
        assertSame(Blocks.COPPER_BLOCK.waxed().unaffected(), materializer.blockStateFor(piece(slot, MaterialType.METAL, MaterialType.METAL.finalHealth()), pos).getBlock());
        assertSame(Blocks.COPPER_BLOCK.waxed().oxidized(), materializer.blockStateFor(piece(slot, MaterialType.METAL, 0), pos).getBlock());
    }

    @Test
    void refreshRepaintsVisibleBlocksForDamagedPiece() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        PieceFootprint footprint = footprint(slot);
        InMemoryBlocks blocks = new InMemoryBlocks();

        assertTrue(materializer.place(piece(slot, MaterialType.WOOD, MaterialType.WOOD.finalHealth()), footprint, blocks).success());
        WorldBuildWriteResult refreshed = materializer.refresh(piece(slot, MaterialType.WOOD, 0), blocks);

        assertTrue(refreshed.success(), refreshed.message());
        assertEquals(25, refreshed.blockCount());
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            assertEquals(Blocks.DARK_OAK_PLANKS.defaultBlockState(), blocks.stateAt(pos));
        }
    }

    @Test
    void resourceNodeRegistryDamagesTrackedCustomResourceBlocks() {
        ResourceNodeRegistry.clearAll();
        BlockPos first = new BlockPos(1, 64, 1);
        BlockPos second = new BlockPos(2, 64, 1);

        ResourceNodeRegistry.register("overworld", List.of(first, second), MaterialType.STONE, 100, 12);

        ResourceNodeRegistry.ResourceHit chipped = ResourceNodeRegistry.hit("overworld", first, 40);
        assertTrue(chipped.hit());
        assertFalse(chipped.destroyed());
        assertEquals(MaterialType.STONE, chipped.material());
        assertEquals(12, chipped.resourceReward());
        assertEquals(60, chipped.remainingHealth());

        ResourceNodeRegistry.ResourceHit destroyed = ResourceNodeRegistry.hit("overworld", first, 60);
        assertTrue(destroyed.destroyed());
        assertFalse(ResourceNodeRegistry.tracked("overworld", first));
        assertTrue(ResourceNodeRegistry.tracked("overworld", second));
        ResourceNodeRegistry.clearAll();
    }

    private static BuildPieceState piece(BuildSlot slot, MaterialType material, int health) {
        return new BuildPieceState(
                UUID.randomUUID(),
                PLAYER,
                slot,
                material,
                health,
                material.finalHealth(),
                1,
                1,
                BuildPieceState.BASE_VARIANT
        );
    }
    private static PieceFootprint footprint(BuildSlot slot) {
        return new FootprintProjector(BuildRules.defaults()).project(slot);
    }

    private static final class InMemoryBlocks implements WorldBuildMaterializer.BlockWriter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private int calls;
        private int failOnCall = -1;

        @Override
        public boolean setBlock(BlockPos pos, BlockState state) {
            int call = calls++;
            if (call == failOnCall) {
                return false;
            }
            blocks.put(pos, state);
            return true;
        }

        private BlockState stateAt(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
