package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootprintProjectorTest {
    private final FootprintProjector projector = new FootprintProjector(BuildRules.defaults());

    @Test
    void wallUsesFiveWideByFiveHighPlane() {
        PieceFootprint footprint = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.SOUTH));

        assertEquals(25, footprint.localBlocks().size());
        assertTrue(footprint.localBlocks().contains(new BlockOffset(0, 0, 4)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(4, 4, 4)));
    }

    @Test
    void wallOrientationRotatesPlaneToCanonicalCellEdge() {
        PieceFootprint footprint = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.EAST));

        assertTrue(footprint.localBlocks().contains(new BlockOffset(4, 0, 0)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(4, 4, 4)));
    }

    @Test
    void floorIgnoresOrientationAndCoversFullFiveByFiveSurface() {
        PieceFootprint footprint = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.SOUTH));

        assertEquals(Orientation.NORTH, footprint.slot().orientation());
        assertEquals(25, footprint.localBlocks().size());
    }

    @Test
    void stairUsesOldModFiveWideDiagonalSurfaceOnly() {
        PieceFootprint north = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.STAIR, Orientation.NORTH));
        PieceFootprint south = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.STAIR, Orientation.SOUTH));

        assertEquals(25, north.localBlocks().size());
        assertTrue(north.localBlocks().contains(new BlockOffset(0, 4, 0)));
        assertTrue(north.localBlocks().contains(new BlockOffset(4, 0, 4)));
        assertFalse(north.localBlocks().contains(new BlockOffset(0, 0, 0)));
        assertTrue(south.localBlocks().contains(new BlockOffset(0, 4, 4)));
        assertTrue(south.localBlocks().contains(new BlockOffset(4, 0, 0)));
    }

    @Test
    void roofUsesOldModConePerimeterShape() {
        PieceFootprint footprint = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.ROOF, Orientation.NORTH));

        assertEquals(25, footprint.localBlocks().size());
        assertTrue(footprint.localBlocks().contains(new BlockOffset(0, 1, 0)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(4, 1, 4)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(1, 2, 1)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(2, 3, 2)));
        assertFalse(footprint.localBlocks().contains(new BlockOffset(2, 1, 2)));
    }
}
