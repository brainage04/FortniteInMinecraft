package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.edit.BuildEditGrids;
import io.github.brainage04.fortniteinminecraft.core.edit.EditGridCell;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
    void stairUsesFiveWideDiagonalSurfaceWithinFiveBlockWallHeight() {
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
    void roofUsesFiveByFiveConePerimeterShape() {
        PieceFootprint footprint = projector.project(BuildSlot.of("overworld", 0, 0, 0, PieceType.ROOF, Orientation.NORTH));

        assertEquals(25, footprint.localBlocks().size());
        assertTrue(footprint.localBlocks().contains(new BlockOffset(0, 1, 0)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(4, 1, 4)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(1, 2, 1)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(3, 2, 3)));
        assertTrue(footprint.localBlocks().contains(new BlockOffset(2, 3, 2)));
        assertFalse(footprint.localBlocks().contains(new BlockOffset(2, 1, 2)));
    }

    @Test
    void wallEditSelectionsProjectWindowAndDoorFootprints() {
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.SOUTH);
        BuildPieceState base = BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 1);
        int windowMask = BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 1));
        int doorMask = windowMask | BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 2));
        BuildPieceState window = base.withEditVariant(BuildEditGrids.variantFor(PieceType.WALL, windowMask));
        BuildPieceState door = base.withEditVariant(BuildEditGrids.variantFor(PieceType.WALL, doorMask));

        PieceFootprint baseFootprint = projector.project(base);
        PieceFootprint windowFootprint = projector.project(window);
        PieceFootprint doorFootprint = projector.project(door);

        assertEquals(25, baseFootprint.localBlocks().size());
        assertTrue(baseFootprint.localBlocks().contains(new BlockOffset(2, 2, 4)));
        assertFalse(windowFootprint.localBlocks().contains(new BlockOffset(2, 2, 4)));
        assertTrue(windowFootprint.localBlocks().contains(new BlockOffset(2, 0, 4)));
        assertFalse(doorFootprint.localBlocks().contains(new BlockOffset(2, 0, 4)));
        assertTrue(doorFootprint.localBlocks().size() < windowFootprint.localBlocks().size());
        assertEquals(slot, doorFootprint.slot());
    }

    @Test
    void floorEditSelectionsProjectCornerAndHalfFootprints() {
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildPieceState base = BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 1);
        int cornerMask = BuildEditGrids.bit(PieceType.FLOOR, new EditGridCell(0, 0));
        int halfMask = cornerMask | BuildEditGrids.bit(PieceType.FLOOR, new EditGridCell(1, 0));
        BuildPieceState corner = base.withEditVariant(BuildEditGrids.variantFor(PieceType.FLOOR, cornerMask));
        BuildPieceState half = base.withEditVariant(BuildEditGrids.variantFor(PieceType.FLOOR, halfMask));

        PieceFootprint cornerFootprint = projector.project(corner);
        PieceFootprint halfFootprint = projector.project(half);

        assertEquals(16, cornerFootprint.localBlocks().size());
        assertEquals(10, halfFootprint.localBlocks().size());
        assertFalse(cornerFootprint.localBlocks().contains(new BlockOffset(0, 0, 0)));
        assertTrue(cornerFootprint.localBlocks().contains(new BlockOffset(4, 0, 4)));
        assertFalse(halfFootprint.localBlocks().contains(new BlockOffset(4, 0, 0)));
    }
}
