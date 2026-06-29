package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementPreview;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementResult;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.session.ResourceWallet;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.PlayerFacingOrientation;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerMovementTuning;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerPlacementRescue;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class BuildItemInteractions {
  static final long TURBO_PLACEMENT_INTERVAL_TICKS = 1L;
  static final long TURBO_INPUT_GRACE_TICKS = 8L;
  private static BuildSessionManager registeredSessions;
  private static BuildPreviewRenderers registeredPreviewRenderers;

  private BuildItemInteractions() {
  }

  public static void register(
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    Objects.requireNonNull(sessions, "sessions");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(rules, "rules");
    Objects.requireNonNull(materializer, "materializer");
    Objects.requireNonNull(previewRenderers, "previewRenderers");
    registeredSessions = sessions;
    registeredPreviewRenderers = previewRenderers;

    UseItemCallback.EVENT.register((player, level, hand) -> place(
        player,
        level,
        hand,
        sessions,
        state,
        rules,
        materializer,
        previewRenderers));
    UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> place(
        player,
        level,
        hand,
        sessions,
        state,
        rules,
        materializer,
        previewRenderers));
    AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> cycleMaterial(
        player,
        level,
        hand,
        sessions,
        previewRenderers,
        false));
  }

  public static void handleBuildItemSwing(ServerPlayer player, InteractionHand hand) {
    if (registeredSessions == null || registeredPreviewRenderers == null) {
      return;
    }
    cycleMaterial(player, player.level(), hand, registeredSessions, registeredPreviewRenderers, true);
  }

  public static void updatePreviewFromHeldItem(
      ServerPlayer player,
      BuildSessionManager sessions,
      BuildRules rules,
      BuildPreviewRenderers previewRenderers) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(sessions, "sessions");
    Objects.requireNonNull(rules, "rules");
    Objects.requireNonNull(previewRenderers, "previewRenderers");

    BuildPieceItem item = ModItems.asBuildPiece(player.getMainHandItem());
    if (item == null) {
      PlayerMovementTuning.clear(player);
      PlayerBuildSession session = sessions.get(player.getUUID());
      if (session != null) {
        session.clearPreview();
        session.stopTurboPlacement();
      }
      previewRenderers.clear(player);
      return;
    }

    PlayerMovementTuning.apply(player);

    PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
    session.selectPiece(item.pieceType());
    session.rememberPreview(targetCandidate(player.level(), rules, session, item, player));
  }

  public static void tickTurboPlacement(
      ServerPlayer player,
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(sessions, "sessions");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(rules, "rules");
    Objects.requireNonNull(materializer, "materializer");
    Objects.requireNonNull(previewRenderers, "previewRenderers");

    PlayerBuildSession session = sessions.get(player.getUUID());
    if (session == null) {
      return;
    }

    ServerLevel level = player.level();
    long tick = level.getGameTime();
    if (!session.turboPlacementActive(tick)) {
      return;
    }

    PlacementCandidate candidate = session.previewCandidate();
    if (candidate == null || !session.canTurboPlace(candidate.slot(), tick)) {
      return;
    }

    placeCandidate(player, level, session, candidate, state, rules, materializer, previewRenderers, tick, false, false);
  }

  private static InteractionResult cycleMaterial(
      Player player,
      Level level,
      InteractionHand hand,
      BuildSessionManager sessions,
      BuildPreviewRenderers previewRenderers,
      boolean fromSwing) {
    if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.PASS;
    }

    BuildPieceItem item = ModItems.asBuildPiece(player.getItemInHand(hand));
    if (item == null) {
      return InteractionResult.PASS;
    }

    PlayerBuildSession session = sessions.getOrCreate(serverPlayer.getUUID());
    long tick = serverLevel.getGameTime();
    if (fromSwing && session.shouldIgnoreMaterialSwing(tick)) {
      return InteractionResult.SUCCESS_SERVER;
    }
    if (!session.markMaterialCycle(tick)) {
      return InteractionResult.SUCCESS_SERVER;
    }
    session.selectPiece(item.pieceType());
    MaterialType material = session.cycleMaterial();
    previewRenderers.clear(serverPlayer);
    ModItems.refreshBuildItemAppearances(serverPlayer);
    serverPlayer.sendSystemMessage(Component.literal("Selected " + label(material) + " build material."), true);
    return InteractionResult.SUCCESS_SERVER;
  }

  private static InteractionResult place(
      Player player,
      Level level,
      InteractionHand hand,
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.PASS;
    }

    BuildPieceItem item = ModItems.asBuildPiece(player.getItemInHand(hand));
    if (item == null) {
      return InteractionResult.PASS;
    }

    PlayerBuildSession session = sessions.getOrCreate(serverPlayer.getUUID());
    session.selectPiece(item.pieceType());
    PlacementCandidate candidate = targetCandidate(serverLevel, rules, session, item, serverPlayer);
    session.rememberPreview(candidate);

    long tick = serverLevel.getGameTime();
    session.markBuildUse(tick);
    session.extendTurboPlacement(tick, TURBO_INPUT_GRACE_TICKS);
    if (candidate.slot().equals(session.lastPlacedSlot())) {
      return InteractionResult.SUCCESS_SERVER;
    }

    return placeCandidate(
        serverPlayer,
        serverLevel,
        session,
        candidate,
        state,
        rules,
        materializer,
        previewRenderers,
        tick,
        true,
        true);
  }

  private static InteractionResult placeCandidate(
      ServerPlayer player,
      ServerLevel level,
      PlayerBuildSession session,
      PlacementCandidate candidate,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers,
      long tick,
      boolean reportFailures,
      boolean reportSuccess) {
    PlacementService placementService = new PlacementService(state, rules, obstructionFor(level, materializer));
    PlayerBuildContext buildContext = playerContext(player, session);
    PlacementPreview preview = placementService.preview(candidate, buildContext);
    if (!preview.valid()) {
      previewRenderers.show(session.previewMode(), level, player, preview.footprint(), false);
      if (reportFailures) {
        player.sendSystemMessage(Component.literal("Placement rejected: " + preview.message() + "."), true);
      }
      return InteractionResult.FAIL;
    }

    PlacementResult result = placementService.place(candidate, buildContext, tick);
    if (!result.placed()) {
      previewRenderers.show(session.previewMode(), level, player, preview.footprint(), false);
      if (reportFailures) {
        player.sendSystemMessage(Component.literal("Placement rejected: " + result.message() + "."), true);
      }
      return InteractionResult.FAIL;
    }

    WorldBuildWriteResult writeResult = materializer.place(level, result.piece(), result.footprint());
    if (!writeResult.success()) {
      state.remove(candidate.slot());
      refundIfNeeded(buildContext, candidate);
      session.stopTurboPlacement();
      if (reportFailures) {
        player.sendSystemMessage(Component.literal("Placement rolled back: " + writeResult.message() + "."), true);
      }
      return InteractionResult.FAIL;
    }

    PlayerPlacementRescue.rescueAfterPlacement(player, level, rules, materializer, result.footprint());

    previewRenderers.clear(player);
    session.rememberPlacement(candidate.slot(), tick, tick + TURBO_PLACEMENT_INTERVAL_TICKS);
    if (reportSuccess) {
      player.sendSystemMessage(
          Component.literal("Placed " + describe(candidate.slot()) + " using " + label(candidate.material()) + "."),
          true);
    }
    return InteractionResult.SUCCESS_SERVER;
  }

  private static PlayerBuildContext playerContext(ServerPlayer player, PlayerBuildSession session) {
    UUID playerId = player.getUUID();
    if (player.isCreative()) {
      return PlayerBuildContext.creative(playerId);
    }
    return PlayerBuildContext.survival(playerId, ResourceWallet.with(session.selectedMaterial(), 0));
  }

  private static WorldObstruction obstructionFor(ServerLevel level, WorldBuildMaterializer materializer) {
    return (dimension, blockX, blockY,
        blockZ) -> level.getBlockState(new BlockPos(blockX, blockY, blockZ)).blocksMotion()
            && !materializer.isTrackedBlock(dimension, blockX, blockY, blockZ);
  }

  private static void refundIfNeeded(PlayerBuildContext player, PlacementCandidate candidate) {
    if (!player.creative()) {
      player.resources().add(candidate.material(), candidate.material().placementCost());
    }
  }

  private static PlacementCandidate targetCandidate(
      ServerLevel level,
      BuildRules rules,
      PlayerBuildSession session,
      BuildPieceItem item,
      ServerPlayer player) {
    HitResult hit = player.pick(PlacementTargeting.TARGET_RANGE_BLOCKS, 0.0F, false);
    BuildGridPos gridPos = PlacementTargeting.destinationGrid(
        level,
        rules,
        item.pieceType(),
        player.getDirection(),
        hit.getLocation());
    return session.candidateAt(gridPos, PlayerFacingOrientation.horizontal(player));
  }

  private static String describe(BuildSlot slot) {
    BuildGridPos gridPos = slot.gridPos();
    return label(slot.pieceType()) + " " + label(slot.orientation()) + " at "
        + gridPos.dimension() + "[" + gridPos.x() + ", " + gridPos.y() + ", " + gridPos.z() + "]";
  }

  private static String label(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
