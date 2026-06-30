package io.github.brainage04.fortniteinminecraft.core.edit;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.EditVariantId;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildEditGridsTest {
    @Test
    void wallSelectionMasksRoundTripThroughVariantIds() {
        int center = BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 1));
        int door = center | BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 2));

        EditVariantId windowVariant = BuildEditGrids.variantFor(PieceType.WALL, center);
        EditVariantId doorVariant = BuildEditGrids.variantFor(PieceType.WALL, door);

        assertEquals(center, BuildEditGrids.maskForVariant(PieceType.WALL, windowVariant).orElseThrow());
        assertEquals(door, BuildEditGrids.maskForVariant(PieceType.WALL, doorVariant).orElseThrow());
        assertEquals("center window", BuildEditGrids.label(PieceType.WALL, center));
        assertEquals("center door", BuildEditGrids.label(PieceType.WALL, door));
    }

    @Test
    void draggingTogglesCellsAndRejectsWholePieceRemoval() {
        int first = BuildEditGrids.toggle(PieceType.FLOOR, 0, new EditGridCell(0, 0));
        int second = BuildEditGrids.toggle(PieceType.FLOOR, first, new EditGridCell(1, 0));
        int all = BuildEditGrids.validMask(PieceType.FLOOR);

        assertEquals("half floor", BuildEditGrids.label(PieceType.FLOOR, second));
        assertTrue(BuildEditGrids.isConfirmableMask(PieceType.FLOOR, second));
        assertFalse(BuildEditGrids.isConfirmableMask(PieceType.FLOOR, all));
    }

    @Test
    void selectedCellsRemoveOnlyTheirProjectedBlocks() {
        int center = BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 1));
        EditVariantId variant = BuildEditGrids.variantFor(PieceType.WALL, center);

        assertFalse(BuildEditGrids.keepsBlock(PieceType.WALL, variant, new BlockOffset(2, 2, 0), 5, 5));
        assertTrue(BuildEditGrids.keepsBlock(PieceType.WALL, variant, new BlockOffset(2, 0, 0), 5, 5));
    }

    @Test
    void localHitPositionsMapToFortniteEditGridCells() {
        assertEquals(new EditGridCell(1, 1), BuildEditGrids.cellAtLocal(PieceType.WALL, 2.5D, 2.5D, 0.0D, 5, 5).orElseThrow());
        assertEquals(new EditGridCell(1, 0), BuildEditGrids.cellAtLocal(PieceType.FLOOR, 4.5D, 0.0D, 1.0D, 5, 5).orElseThrow());
    }
}
