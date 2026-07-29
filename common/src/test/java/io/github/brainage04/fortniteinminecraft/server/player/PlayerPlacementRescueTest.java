package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPlacementRescueTest {
    @Test
    void detectsBothBottomAndTopHalfIntersections() {
        AABB playerBox = new AABB(0.2D, 64.0D, 0.2D, 0.8D, 65.8D, 0.8D);

        assertTrue(PlayerPlacementRescue.intersectsBody(playerBox, List.of(new BlockPos(0, 64, 0))));
        assertTrue(PlayerPlacementRescue.intersectsBody(playerBox, List.of(new BlockPos(0, 65, 0))));
    }

    @Test
    void ignoresPlacedBlocksOutsidePlayerBody() {
        AABB playerBox = new AABB(0.2D, 64.0D, 0.2D, 0.8D, 65.8D, 0.8D);

        assertFalse(PlayerPlacementRescue.intersectsBody(playerBox, List.of(new BlockPos(2, 64, 0))));
    }

    @Test
    void liftsAboveHighestIntersectingPlacedBlock() {
        AABB playerBox = new AABB(0.2D, 64.0D, 0.2D, 0.8D, 65.8D, 0.8D);

        double offset = PlayerPlacementRescue.upwardOffset(playerBox, List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(0, 65, 0)
        ));

        assertEquals(2.000001D, offset, 1.0E-9D);
    }

    @Test
    void rescueCapAllowsOnlyTwoBlocksOfTeleportDisplacement() {
        assertTrue(PlayerPlacementRescue.withinRescueCap(2.000001D));
        assertEquals(2.0D, PlayerPlacementRescue.cappedRescueOffset(2.000001D), 1.0E-9D);
        assertFalse(PlayerPlacementRescue.withinRescueCap(2.25D));
    }
}
