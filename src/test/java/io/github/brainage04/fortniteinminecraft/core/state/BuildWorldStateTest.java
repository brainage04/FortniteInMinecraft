package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildWorldStateTest {
    @Test
    void storesQueriesAndRemovesPiecesBySlot() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 4, 0, 9, PieceType.WALL, Orientation.NORTH);
        BuildPieceState piece = BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 42);

        assertTrue(state.addIfAbsent(piece));
        assertFalse(state.addIfAbsent(piece));
        assertSame(piece, state.get(slot));
        assertEquals(1, state.size());
        assertSame(piece, state.remove(slot));
        assertEquals(0, state.size());
    }

    @Test
    void indexedFootprintConflictsRejectDifferentPieceTypesButPermitSameTypeSeams() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot floor = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot adjacentFloor = BuildSlot.of("overworld", 1, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot wall = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.SOUTH);
        List<BlockOffset> sharedBlock = List.of(new BlockOffset(0, 0, 0));

        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(floor, MaterialType.WOOD, UUID.randomUUID(), 1), sharedBlock));
        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(adjacentFloor, MaterialType.WOOD, UUID.randomUUID(), 2), sharedBlock));
        assertFalse(state.addIfNotConflicting(BuildPieceState.placed(wall, MaterialType.WOOD, UUID.randomUUID(), 3), sharedBlock));
        assertEquals(2, state.size());
    }

    @Test
    void constructionProgressRaisesPlacedHealthTowardFinalHealth() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(state.addIfAbsent(BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 0)));

        state.progressConstruction(40);
        assertEquals(120, state.get(slot).currentHealth());

        state.progressConstruction(80);
        assertEquals(150, state.get(slot).currentHealth());
    }

    @Test
    void buildDamageAppliesAfterConstructionProgressAndCanDestroyPiece() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(state.addIfAbsent(BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 0)));

        BuildWorldState.DamageResult hit = state.damage(slot, 36, 20);

        assertTrue(hit.hit());
        assertEquals(69, hit.after().currentHealth());
        assertFalse(hit.destroyed());

        BuildWorldState.DamageResult destroyed = state.damage(slot, 500, 21);

        assertTrue(destroyed.destroyed());
        assertEquals(0, destroyed.after().currentHealth());
    }
}
