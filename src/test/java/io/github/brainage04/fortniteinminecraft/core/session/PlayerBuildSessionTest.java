package io.github.brainage04.fortniteinminecraft.core.session;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementResult;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBuildSessionTest {
  private static final UUID PLAYER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PLAYER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void candidateUsesSelectedPieceMaterialAndSuppliedOrientation() {
    PlayerBuildSession session = new PlayerBuildSession();

    PlacementCandidate candidate = session.candidateAt(new BuildGridPos("overworld", 1, 0, 2), Orientation.EAST);

    assertEquals(PieceType.WALL, session.selectedPiece());
    assertEquals(MaterialType.WOOD, session.selectedMaterial());
    assertEquals(BuildSlot.of("overworld", 1, 0, 2, PieceType.WALL, Orientation.EAST), candidate.slot());
    assertEquals(MaterialType.WOOD, candidate.material());
  }

  @Test
  void managerKeepsPlayerSelectionsIndependent() {
    BuildSessionManager manager = new BuildSessionManager();
    PlayerBuildSession first = manager.getOrCreate(PLAYER_ONE);
    PlayerBuildSession second = manager.getOrCreate(PLAYER_TWO);
    assertSame(first, manager.get(PLAYER_ONE));
    assertSame(second, manager.get(PLAYER_TWO));
    assertNull(manager.get(UUID.fromString("00000000-0000-0000-0000-000000000003")));

    first.selectPiece(PieceType.STAIR);
    first.selectMaterial(MaterialType.METAL);

    assertSame(first, manager.getOrCreate(PLAYER_ONE));
    assertSame(second, manager.getOrCreate(PLAYER_TWO));
    assertEquals(PieceType.STAIR, first.selectedPiece());
    assertEquals(MaterialType.METAL, first.selectedMaterial());
    assertEquals(PieceType.WALL, second.selectedPiece());
    assertEquals(MaterialType.WOOD, second.selectedMaterial());
    assertEquals(2, manager.size());
  }

  @Test
  void selectionChangeClearsPreviewAndTurboState() {
    PlayerBuildSession session = new PlayerBuildSession();
    BuildGridPos gridPos = new BuildGridPos("overworld", 0, 0, 0);
    PlacementCandidate preview = session.candidateAt(gridPos, Orientation.NORTH);
    session.rememberPreview(preview);
    session.rememberPlacement(preview.slot(), 10, 14);

    session.selectMaterial(MaterialType.STONE);

    assertNull(session.previewCandidate());
    assertNull(session.lastPlacedSlot());
    assertEquals(PlayerBuildSession.NO_TURBO_PLACEMENT_TICK, session.lastPlacementTick());
    assertEquals(PlayerBuildSession.NO_TURBO_PLACEMENT_TICK, session.nextTurboPlacementTick());
    assertEquals(PlayerBuildSession.NO_TURBO_PLACEMENT_TICK, session.turboPlacementUntilTick());

    session.rememberPreview(session.candidateAt(gridPos, Orientation.NORTH));
    session.selectPiece(PieceType.ROOF);

    assertNull(session.previewCandidate());
    assertEquals(PieceType.ROOF, session.selectedPiece());
  }

  @Test
  void cycleMaterialWrapsInFortniteOrderAndClearsPreview() {
    PlayerBuildSession session = new PlayerBuildSession();
    BuildGridPos gridPos = new BuildGridPos("overworld", 0, 0, 0);
    session.rememberPreview(session.candidateAt(gridPos, Orientation.NORTH));

    assertEquals(MaterialType.STONE, session.cycleMaterial());
    assertEquals(MaterialType.METAL, session.cycleMaterial());
    assertEquals(MaterialType.WOOD, session.cycleMaterial());
    assertNull(session.previewCandidate());
  }

  @Test
  void materialCycleMarkerDebouncesDuplicateSwingAndAttackPackets() {
    PlayerBuildSession session = new PlayerBuildSession();

    assertTrue(session.markMaterialCycle(42));
    assertFalse(session.markMaterialCycle(42));
    assertTrue(session.markMaterialCycle(43));
    assertEquals(43, session.lastMaterialCycleTick());
  }

  @Test
  void materialSwingAfterBuildUseIsSuppressedBriefly() {
    PlayerBuildSession session = new PlayerBuildSession();

    session.markBuildUse(10);

    assertTrue(session.shouldIgnoreMaterialSwing(10));
    assertTrue(session.shouldIgnoreMaterialSwing(14));
    assertFalse(session.shouldIgnoreMaterialSwing(15));
  }

  @Test
  void turboPlacementWindowSkipsRepeatedSlotAndHonorsCadence() {
    PlayerBuildSession session = new PlayerBuildSession();
    BuildSlot first = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
    BuildSlot second = BuildSlot.of("overworld", 1, 0, 0, PieceType.WALL, Orientation.NORTH);

    session.extendTurboPlacement(10, 8);
    session.rememberPlacement(first, 10, 12);

    assertFalse(session.canTurboPlace(first, 12));
    assertFalse(session.canTurboPlace(second, 11));
    assertTrue(session.canTurboPlace(second, 12));
    assertTrue(session.turboPlacementActive(18));
    assertFalse(session.turboPlacementActive(19));
  }


  @Test
  void resetAndRemoveBoundManagerStateByPlayer() {
    BuildSessionManager manager = new BuildSessionManager();
    PlayerBuildSession original = manager.getOrCreate(PLAYER_ONE);
    original.selectMaterial(MaterialType.METAL);

    PlayerBuildSession reset = manager.reset(PLAYER_ONE);

    assertNotSame(original, reset);
    assertEquals(MaterialType.WOOD, reset.selectedMaterial());
    assertEquals(1, manager.size());
    assertTrue(manager.remove(PLAYER_ONE));
    assertFalse(manager.remove(PLAYER_ONE));
    assertEquals(0, manager.size());
  }

  @Test
  void sessionCandidateFeedsPlacementServiceAtomically() {
    PlayerBuildSession session = new PlayerBuildSession();
    session.selectPiece(PieceType.FLOOR);
    session.selectMaterial(MaterialType.STONE);
    PlacementCandidate candidate = session.candidateAt(new BuildGridPos("overworld", 0, 0, 0), Orientation.SOUTH);
    BuildWorldState state = new BuildWorldState();
    PlacementService service = new PlacementService(state, BuildRules.defaults(),
        (dimension, x, y, z) -> y == -2 && z == -1 && x >= -1 && x <= 3);
    ResourceWallet wallet = ResourceWallet.with(MaterialType.STONE, 50);

    PlacementResult result = service.place(candidate, PlayerBuildContext.survival(PLAYER_ONE, wallet), 20);

    assertTrue(result.placed(), result.message());
    assertEquals(BuildSlot.of("overworld", 0, 0, 0, PieceType.FLOOR, Orientation.NORTH), result.piece().slot());
    assertEquals(40, wallet.get(MaterialType.STONE));
    assertTrue(state.contains(candidate.slot()));
  }
}
