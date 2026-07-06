package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeployableFootprintsTest {
    @Test
    void launchPadFootprintCoversThreeByThreeCenteredAboveClickedFloor() {
        List<BlockPos> footprint = DeployableFootprints.centeredFloorSquare(new BlockPos(10, 65, 10), DeployableFootprints.LAUNCH_PAD_SIZE_BLOCKS);

        assertFootprintBounds(footprint, 9, 9, 11, 65, 65, 9, 11);
        assertTrue(footprint.contains(new BlockPos(10, 65, 10)));
        assertTrue(footprint.contains(new BlockPos(9, 65, 9)));
        assertTrue(footprint.contains(new BlockPos(11, 65, 11)));
    }

    @Test
    void bouncerFootprintMatchesCurrentFortniteFloorSize() {
        List<BlockPos> footprint = DeployableFootprints.centeredFloorSquare(new BlockPos(10, 65, 10), DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS);
        int radius = BuildConstants.PIECE_FOOTPRINT_BLOCKS / 2;

        assertFootprintBounds(
                footprint,
                BuildConstants.PIECE_FOOTPRINT_BLOCKS * BuildConstants.PIECE_FOOTPRINT_BLOCKS,
                10 - radius,
                10 + radius,
                65,
                65,
                10 - radius,
                10 + radius
        );
        assertTrue(footprint.contains(new BlockPos(10 - radius, 65, 10 - radius)));
        assertTrue(footprint.contains(new BlockPos(10 + radius, 65, 10 + radius)));
    }

    @Test
    void wallTrapFootprintUsesFiveWideByFiveHighPlane() {
        List<BlockPos> northWall = DeployableFootprints.centeredSurfaceSquare(
                new BlockPos(10, 65, 10),
                Direction.NORTH,
                DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS
        );
        int radius = BuildConstants.PIECE_FOOTPRINT_BLOCKS / 2;

        assertFootprintBounds(
                northWall,
                BuildConstants.PIECE_FOOTPRINT_BLOCKS * BuildConstants.PIECE_FOOTPRINT_BLOCKS,
                10 - radius,
                10 + radius,
                65 - radius,
                65 + radius,
                10,
                10
        );
        assertTrue(northWall.contains(new BlockPos(10 - radius, 65 - radius, 10)));
        assertTrue(northWall.contains(new BlockPos(10 + radius, 65 + radius, 10)));
    }

    @Test
    void eastWallTrapFootprintRotatesToConstantXPlane() {
        List<BlockPos> eastWall = DeployableFootprints.centeredSurfaceSquare(
                new BlockPos(10, 65, 10),
                Direction.EAST,
                DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS
        );
        int radius = BuildConstants.PIECE_FOOTPRINT_BLOCKS / 2;

        assertFootprintBounds(
                eastWall,
                BuildConstants.PIECE_FOOTPRINT_BLOCKS * BuildConstants.PIECE_FOOTPRINT_BLOCKS,
                10,
                10,
                65 - radius,
                65 + radius,
                10 - radius,
                10 + radius
        );
        assertTrue(eastWall.contains(new BlockPos(10, 65 - radius, 10 - radius)));
        assertTrue(eastWall.contains(new BlockPos(10, 65 + radius, 10 + radius)));
    }

    private static void assertFootprintBounds(
            List<BlockPos> footprint,
            int expectedSize,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        assertEquals(expectedSize, footprint.size());
        assertEquals(minX, footprint.stream().mapToInt(BlockPos::getX).min().orElseThrow());
        assertEquals(maxX, footprint.stream().mapToInt(BlockPos::getX).max().orElseThrow());
        assertEquals(minY, footprint.stream().mapToInt(BlockPos::getY).min().orElseThrow());
        assertEquals(maxY, footprint.stream().mapToInt(BlockPos::getY).max().orElseThrow());
        assertEquals(minZ, footprint.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
        assertEquals(maxZ, footprint.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
    }
}
