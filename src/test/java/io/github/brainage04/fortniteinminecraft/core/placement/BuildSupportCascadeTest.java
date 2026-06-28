package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildSupportCascadeTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BuildRules RULES = BuildRules.defaults();
    private static final BuildSupportCascade CASCADE = new BuildSupportCascade(RULES);

    @Test
    void groundedConnectedComponentSurvives() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot grounded = floorSlot(0, 0, 0);
        BuildSlot connected = floorSlot(1, 0, 0);
        state.addIfAbsent(BuildPieceState.placed(grounded, MaterialType.WOOD, PLAYER, 1));
        state.addIfAbsent(BuildPieceState.placed(connected, MaterialType.WOOD, PLAYER, 1));

        List<BuildPieceState> unsupported = CASCADE.unsupportedPieces(state, "overworld", solidGroundUnderOrigin());

        assertTrue(unsupported.isEmpty());
    }

    @Test
    void ungroundedComponentCollapsesImmediately() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot floating = floorSlot(0, 1, 0);
        state.addIfAbsent(BuildPieceState.placed(floating, MaterialType.WOOD, PLAYER, 1));

        List<BuildPieceState> unsupported = CASCADE.unsupportedPieces(state, "overworld", WorldObstruction.none());

        assertEquals(List.of(floating), unsupported.stream().map(BuildPieceState::slot).toList());
    }

    @Test
    void disconnectedFloatingComponentCollapsesWhileGroundedComponentSurvives() {
        BuildWorldState state = new BuildWorldState();
        BuildSlot grounded = floorSlot(0, 0, 0);
        BuildSlot floating = floorSlot(3, 0, 0);
        state.addIfAbsent(BuildPieceState.placed(grounded, MaterialType.WOOD, PLAYER, 1));
        state.addIfAbsent(BuildPieceState.placed(floating, MaterialType.WOOD, PLAYER, 1));

        List<BuildPieceState> unsupported = CASCADE.unsupportedPieces(state, "overworld", solidGroundUnderOrigin());

        assertEquals(List.of(floating), unsupported.stream().map(BuildPieceState::slot).toList());
    }

    private static BuildSlot floorSlot(int x, int y, int z) {
        return BuildSlot.of("overworld", x, y, z, PieceType.FLOOR, Orientation.NORTH);
    }

    private static WorldObstruction solidGroundUnderOrigin() {
        return (dimension, x, y, z) -> dimension.equals("overworld") && y == -2 && x == 0 && z == 0;
    }
}
