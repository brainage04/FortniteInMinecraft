package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.placement.BuildSupportCascade;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.EditVariantId;
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
    void indexedFootprintConflictsAllowSharedBuildTypeSeamsAndCrossTypeIntersections() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot floor = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot adjacentFloor = BuildSlot.of("overworld", 1, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot stair = BuildSlot.of("overworld", 0, 0, 0, PieceType.STAIR, Orientation.NORTH);
        BuildSlot wall = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.SOUTH);
        List<BlockOffset> sharedBlock = List.of(new BlockOffset(0, 0, 0));

        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(floor, MaterialType.WOOD, UUID.randomUUID(), 1), sharedBlock));
        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(adjacentFloor, MaterialType.WOOD, UUID.randomUUID(), 2), sharedBlock));
        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(stair, MaterialType.WOOD, UUID.randomUUID(), 3), sharedBlock));
        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(wall, MaterialType.WOOD, UUID.randomUUID(), 4), sharedBlock));
        assertEquals(4, state.size());
    }

    @Test
    void roofConeFootprintCanShareFloorWallGridBlock() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot floor = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot roof = BuildSlot.of("overworld", 0, 0, 0, PieceType.ROOF, Orientation.NORTH);
        List<BlockOffset> sharedBlock = List.of(new BlockOffset(0, 0, 0));

        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(floor, MaterialType.WOOD, UUID.randomUUID(), 1), sharedBlock));
        assertTrue(state.addIfNotConflicting(BuildPieceState.placed(roof, MaterialType.WOOD, UUID.randomUUID(), 2), sharedBlock));
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
    void constructionProgressCanBeScopedToDimension() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot overworld = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        BuildSlot nether = BuildSlot.of("the_nether", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(state.addIfAbsent(BuildPieceState.placed(overworld, MaterialType.WOOD, UUID.randomUUID(), 0)));
        assertTrue(state.addIfAbsent(BuildPieceState.placed(nether, MaterialType.WOOD, UUID.randomUUID(), 0)));

        List<BuildPieceState> changed = state.progressConstruction("the_nether", 40);

        assertEquals(List.of(nether), changed.stream().map(BuildPieceState::slot).toList());
        assertEquals(90, state.get(overworld).currentHealth());
        assertEquals(120, state.get(nether).currentHealth());
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

    @Test
    void buildRepairProgressesConstructionAndCapsAtFinalHealth() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(state.addIfAbsent(BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 0)));

        BuildWorldState.DamageResult hit = state.damage(slot, 60, 20);
        assertEquals(45, hit.after().currentHealth());

        BuildWorldState.RepairResult repair = state.repair(slot, 500, 21);

        assertTrue(repair.repaired());
        assertEquals(MaterialType.WOOD.finalHealth(), repair.after().currentHealth());
    }

    @Test
    void placedPiecesUseTypedBaseEditVariant() {
        BuildPieceState piece = BuildPieceState.placed(
                BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH),
                MaterialType.WOOD,
                UUID.randomUUID(),
                0
        );

        assertSame(EditVariantId.BASE, piece.editVariant());
    }

    @Test
    void replaceIfCurrentProtectsEditConfirmAgainstStaleTargets() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        BuildPieceState original = BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 0);
        BuildPieceState edited = new BuildPieceState(
                original.id(),
                original.owner(),
                original.slot(),
                original.material(),
                original.currentHealth(),
                original.maxHealth(),
                original.placedAtTick(),
                original.lastHealthUpdateTick(),
                new EditVariantId("wall-window")
        );
        assertTrue(state.addIfAbsent(original));

        assertFalse(state.replaceIfCurrent(slot, UUID.randomUUID(), edited));
        assertSame(original, state.get(slot));
        assertTrue(state.replaceIfCurrent(slot, original.id(), edited));
        assertSame(edited, state.get(slot));
    }

    @Test
    void scheduledCollapsesDrainByDueTickAndSkipRemovedPieces() {
        BuildWorldState state = new BuildWorldState();
        BuildPieceState near = BuildPieceState.placed(
                BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH),
                MaterialType.WOOD,
                UUID.randomUUID(),
                0
        );
        BuildPieceState far = BuildPieceState.placed(
                BuildSlot.of("overworld", 1, 0, 0, PieceType.FLOOR, Orientation.NORTH),
                MaterialType.WOOD,
                UUID.randomUUID(),
                0
        );
        state.addIfAbsent(near);
        state.addIfAbsent(far);

        int scheduled = state.scheduleCollapse(List.of(
                new BuildSupportCascade.CollapseStep(near, 0, 4),
                new BuildSupportCascade.CollapseStep(far, 1, 7)
        ), 10);

        assertEquals(2, scheduled);
        assertTrue(state.drainDueCollapses("overworld", 13).isEmpty());
        assertEquals(List.of(near.slot()), state.drainDueCollapses("overworld", 14).stream().map(BuildPieceState::slot).toList());
        assertEquals(1, state.scheduledCollapseCount());

        state.remove(far.slot());

        assertTrue(state.drainDueCollapses("overworld", 17).isEmpty());
        assertEquals(0, state.scheduledCollapseCount());
    }

    @Test
    void revalidatedDrainDropsDueCollapsesThatRegainedSupport() {
        BuildWorldState state = new BuildWorldState();
        BuildPieceState piece = BuildPieceState.placed(
                BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH),
                MaterialType.WOOD,
                UUID.randomUUID(),
                0
        );
        state.addIfAbsent(piece);
        assertEquals(1, state.scheduleCollapse(List.of(
                new BuildSupportCascade.CollapseStep(piece, 0, 4)
        ), 10));

        assertTrue(state.drainDueCollapses("overworld", 14, List.of()).isEmpty());
        assertEquals(0, state.scheduledCollapseCount());
    }
}
