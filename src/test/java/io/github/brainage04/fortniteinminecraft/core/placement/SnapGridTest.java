package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapGridTest {
    private final SnapGrid grid = new SnapGrid(BuildRules.defaults());

    @Test
    void snapsPositiveBlocksToNearestOldFourStrideCell() {
        assertEquals(new BuildGridPos("overworld", 2, 2, 3), grid.snap("overworld", 7, 8, 11));
    }

    @Test
    void snapsNegativeBlocksLikeOldRoundBasedAlignment() {
        assertEquals(new BuildGridPos("overworld", 0, 0, 0), grid.snap("overworld", -1, -1, -1));
    }

    @Test
    void mapsGridCellBackToFiveBlockFootprintOrigin() {
        assertEquals(new BlockOffset(7, 7, -5), grid.blockOrigin(new BuildGridPos("overworld", 2, 2, -1)));
    }
}
