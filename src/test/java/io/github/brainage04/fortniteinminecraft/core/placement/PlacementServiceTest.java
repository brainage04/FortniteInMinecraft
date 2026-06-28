package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.ResourceWallet;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void survivalPlacementSpendsSelectedMaterialAndCommitsState() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.WOOD, 25);
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.WOOD), PlayerBuildContext.survival(PLAYER, wallet), 10);

        assertTrue(result.placed());
        assertNotNull(state.get(slot));
        assertEquals(15, wallet.get(MaterialType.WOOD));
        assertEquals(150, result.piece().maxHealth());
    }

    @Test
    void duplicatePlacementRejectsWithoutDoubleSpend() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.STONE, 30);
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 1, 0, 1, PieceType.FLOOR, Orientation.WEST);
        PlayerBuildContext player = PlayerBuildContext.survival(PLAYER, wallet);

        service.place(new PlacementCandidate(slot, MaterialType.STONE), player, 1);
        PlacementResult duplicate = service.place(new PlacementCandidate(slot, MaterialType.STONE), player, 2);

        assertEquals(PlacementFailure.OCCUPIED, duplicate.failure());
        assertEquals(20, wallet.get(MaterialType.STONE));
        assertEquals(1, state.size());
    }

    @Test
    void differentPieceTypeAtSameGridRejectsWithoutDoubleSpend() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.WOOD, 50);
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot floor = BuildSlot.of("overworld", 1, 0, 1, PieceType.FLOOR, Orientation.NORTH);
        BuildSlot stair = BuildSlot.of("overworld", 1, 0, 1, PieceType.STAIR, Orientation.EAST);
        PlayerBuildContext player = PlayerBuildContext.survival(PLAYER, wallet);

        PlacementResult first = service.place(new PlacementCandidate(floor, MaterialType.WOOD), player, 1);
        PlacementResult duplicateCell = service.place(new PlacementCandidate(stair, MaterialType.WOOD), player, 2);

        assertTrue(first.placed(), first.message());
        assertEquals(PlacementFailure.OCCUPIED, duplicateCell.failure());
        assertEquals(40, wallet.get(MaterialType.WOOD));
        assertEquals(1, state.size());
    }

    @Test
    void differentStairOrientationAtSameGridRejects() {
        BuildWorldState state = new BuildWorldState();
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot north = BuildSlot.of("overworld", 1, 0, 1, PieceType.STAIR, Orientation.NORTH);
        BuildSlot east = BuildSlot.of("overworld", 1, 0, 1, PieceType.STAIR, Orientation.EAST);

        PlacementResult first = service.place(new PlacementCandidate(north, MaterialType.WOOD), PlayerBuildContext.creative(PLAYER), 1);
        PlacementResult duplicateCell = service.place(new PlacementCandidate(east, MaterialType.WOOD), PlayerBuildContext.creative(PLAYER), 2);

        assertTrue(first.placed(), first.message());
        assertEquals(PlacementFailure.OCCUPIED, duplicateCell.failure());
        assertEquals(1, state.size());
    }

    @Test
    void canonicalWallSlotsRejectApproachSideDuplicates() {
        BuildWorldState state = new BuildWorldState();
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot eastFace = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.EAST);
        BuildSlot samePlaneFromEastSide = BuildSlot.of("overworld", 1, 0, 0, PieceType.WALL, Orientation.WEST);

        PlacementResult first = service.place(new PlacementCandidate(eastFace, MaterialType.WOOD), PlayerBuildContext.creative(PLAYER), 1);
        PlacementResult duplicate = service.place(new PlacementCandidate(samePlaneFromEastSide, MaterialType.WOOD), PlayerBuildContext.creative(PLAYER), 2);

        assertEquals(eastFace, samePlaneFromEastSide);
        assertTrue(first.placed());
        assertEquals(PlacementFailure.OCCUPIED, duplicate.failure());
        assertEquals(1, state.size());
    }

    @Test
    void insufficientResourcesRejectsAtomically() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.METAL, 9);
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.ROOF, Orientation.SOUTH);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.METAL), PlayerBuildContext.survival(PLAYER, wallet), 1);

        assertEquals(PlacementFailure.INSUFFICIENT_RESOURCES, result.failure());
        assertEquals(9, wallet.get(MaterialType.METAL));
        assertEquals(0, state.size());
    }

    @Test
    void creativePlacementBypassesResourceSpend() {
        BuildWorldState state = new BuildWorldState();
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.STAIR, Orientation.EAST);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.METAL), PlayerBuildContext.creative(PLAYER), 1);

        assertTrue(result.placed());
        assertEquals(1, state.size());
    }

    @Test
    void creativePlacementCanFloatForCommandSmokeTests() {
        BuildWorldState state = new BuildWorldState();
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 0, 1, 0, PieceType.FLOOR, Orientation.NORTH);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.WOOD), PlayerBuildContext.creative(PLAYER), 1);

        assertTrue(result.placed(), result.message());
        assertEquals(1, state.size());
    }

    @Test
    void unsupportedPlacementAboveGroundRejectsWithoutMutation() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.WOOD, 50);
        PlacementService service = new PlacementService(state, BuildRules.defaults(), WorldObstruction.none());
        BuildSlot slot = BuildSlot.of("overworld", 0, 1, 0, PieceType.FLOOR, Orientation.NORTH);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.WOOD), PlayerBuildContext.survival(PLAYER, wallet), 1);

        assertEquals(PlacementFailure.UNSUPPORTED, result.failure());
        assertEquals(50, wallet.get(MaterialType.WOOD));
        assertEquals(0, state.size());
    }

    @Test
    void anySolidWorldBlockObstructsPlacement() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.WOOD, 50);
        WorldObstruction obstruction = (dimension, x, y, z) -> x == -1 && y == -1 && z == -1;
        PlacementService service = new PlacementService(state, BuildRules.defaults(), obstruction);
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);

        PlacementResult result = service.place(new PlacementCandidate(slot, MaterialType.WOOD), PlayerBuildContext.survival(PLAYER, wallet), 1);

        assertEquals(PlacementFailure.OBSTRUCTED, result.failure());
        assertEquals(50, wallet.get(MaterialType.WOOD));
        assertEquals(0, state.size());
    }

    @Test
    void majoritySolidWorldBlocksObstructPlacement() {
        BuildWorldState state = new BuildWorldState();
        ResourceWallet wallet = ResourceWallet.with(MaterialType.WOOD, 50);
        WorldObstruction obstruction = (dimension, x, y, z) -> y == -1 && x <= 1;
        PlacementService service = new PlacementService(state, BuildRules.defaults(), obstruction);
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH);
        PlacementCandidate candidate = new PlacementCandidate(slot, MaterialType.WOOD);

        PlacementPreview preview = service.preview(candidate, PlayerBuildContext.creative(PLAYER));
        PlacementResult result = service.place(candidate, PlayerBuildContext.survival(PLAYER, wallet), 1);

        assertEquals(PlacementFailure.OBSTRUCTED, preview.failure());
        assertEquals(25, preview.footprint().localBlocks().size());
        assertEquals(PlacementFailure.OBSTRUCTED, result.failure());
        assertEquals(50, wallet.get(MaterialType.WOOD));
        assertEquals(0, state.size());
    }
}
