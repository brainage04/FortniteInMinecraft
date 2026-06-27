package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}
