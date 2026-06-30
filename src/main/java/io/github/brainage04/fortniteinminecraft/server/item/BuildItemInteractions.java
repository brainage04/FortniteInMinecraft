package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementPreview;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementResult;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.server.PlayerFacingOrientation;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerMovementTuning;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerPlacementRescue;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.WorldObstructions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class BuildItemInteractions {
    static final long TURBO_PLACEMENT_INTERVAL_TICKS = 1L;
    static final long TURBO_INPUT_GRACE_TICKS = 8L;
    private static BuildWorldState registeredState;
    private static WorldBuildMaterializer registeredMaterializer;

    private BuildItemInteractions() {
    }

    public static void register(
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(materializer, "materializer");
        registeredState = state;
        registeredMaterializer = materializer;
    }

    public static void updateHeldItemState(
            ServerPlayer player,
            BuildSessionManager sessions,
            BuildRules rules
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(rules, "rules");

        PlayerMovementTuning.apply(player);
        BuildPieceItem item = ModItems.asBuildPiece(player.getMainHandItem());
        if (item == null) {
            PlayerBuildSession session = sessions.get(player.getUUID());
            if (session != null) {
                session.clearPreview();
                session.stopTurboPlacement();
            }
            return;
        }

        PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
        session.selectPiece(item.pieceType());
        PlacementCandidate candidate = targetCandidate(player.level(), rules, session, item, player);
        session.rememberPreview(candidate);
        sendPreview(player, rules, candidate);
    }

    public static void tickTurboPlacement(
            ServerPlayer player,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(materializer, "materializer");

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

        placeCandidate(player, level, session, candidate, state, rules, materializer, tick, false, false);
    }

    public static InteractionResult handlePrimaryInput(
            ServerPlayer player,
            InteractionHand hand,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        return place(player, hand, sessions, state, rules, materializer);
    }

    public static void stopPrimaryInput(ServerPlayer player, BuildSessionManager sessions) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session != null) {
            session.stopTurboPlacement();
        }
    }

    public static InteractionResult handleSecondaryInput(
            ServerPlayer player,
            InteractionHand hand,
            BuildSessionManager sessions
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(sessions, "sessions");
        return cycleMaterial(player, hand, sessions);
    }

    private static InteractionResult cycleMaterial(
            ServerPlayer player,
            InteractionHand hand,
            BuildSessionManager sessions
    ) {
        BuildPieceItem item = ModItems.asBuildPiece(player.getItemInHand(hand));
        if (item == null) {
            return InteractionResult.PASS;
        }

        PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
        long tick = player.level().getGameTime();
        if (!session.markMaterialCycle(tick)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        session.selectPiece(item.pieceType());
        MaterialType material = session.cycleMaterial();
        ModItems.refreshBuildItemAppearances(player);
        player.sendSystemMessage(Component.literal("Selected " + label(material) + " build material."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult place(
            ServerPlayer player,
            InteractionHand hand,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        BuildPieceItem item = ModItems.asBuildPiece(player.getItemInHand(hand));
        if (item == null) {
            return InteractionResult.PASS;
        }

        ServerLevel level = player.level();
        PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
        session.selectPiece(item.pieceType());
        PlacementCandidate candidate = targetCandidate(level, rules, session, item, player);
        session.rememberPreview(candidate);

        long tick = level.getGameTime();
        session.markBuildUse(tick);
        session.extendTurboPlacement(tick, TURBO_INPUT_GRACE_TICKS);
        if (candidate.slot().equals(session.lastPlacedSlot())) {
            return InteractionResult.SUCCESS_SERVER;
        }

        return placeCandidate(player, level, session, candidate, state, rules, materializer, tick, true, true);
    }

    private static InteractionResult placeCandidate(
            ServerPlayer player,
            ServerLevel level,
            PlayerBuildSession session,
            PlacementCandidate candidate,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            long tick,
            boolean reportFailures,
            boolean reportSuccess
    ) {
        PlacementService placementService = new PlacementService(state, rules, WorldObstructions.trackedBuildAware(level, materializer));
        PlayerBuildContext buildContext = playerContext(player);
        PlacementPreview preview = placementService.preview(candidate, buildContext);
        if (!preview.valid()) {
            if (reportFailures) {
                player.sendSystemMessage(Component.literal("Placement rejected: " + preview.message() + "."), true);
            }
            return InteractionResult.FAIL;
        }

        PlacementResult result = placementService.place(candidate, buildContext, tick);
        if (!result.placed()) {
            if (reportFailures) {
                player.sendSystemMessage(Component.literal("Placement rejected: " + result.message() + "."), true);
            }
            return InteractionResult.FAIL;
        }

        WorldBuildWriteResult writeResult = materializer.place(level, result.piece(), result.footprint());
        if (!writeResult.success()) {
            state.remove(candidate.slot());
            refundIfNeeded(buildContext, candidate);
            PlayerResourceStateSync.send(player);
            session.stopTurboPlacement();
            if (reportFailures) {
                player.sendSystemMessage(Component.literal("Placement rolled back: " + writeResult.message() + "."), true);
            }
            return InteractionResult.FAIL;
        }

        PlayerPlacementRescue.rescueAfterPlacement(player, level, rules, materializer, result.footprint());
        PlayerResourceStateSync.send(player);

        session.rememberPlacement(candidate.slot(), tick, tick + TURBO_PLACEMENT_INTERVAL_TICKS);
        if (reportSuccess) {
            player.sendSystemMessage(
                    Component.literal("Placed " + describe(candidate.slot()) + " using " + label(candidate.material()) + "."),
                    true
            );
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static PlayerBuildContext playerContext(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (player.isCreative()) {
            return PlayerBuildContext.creative(playerId);
        }
        return PlayerBuildContext.survival(playerId, PlayerResourceStates.stateFor(player).materials());
    }

    private static void refundIfNeeded(PlayerBuildContext player, PlacementCandidate candidate) {
        if (!player.creative() && !player.resources().infinite()) {
            player.resources().add(candidate.material(), candidate.material().placementCost());
        }
    }

    private static PlacementCandidate targetCandidate(
            ServerLevel level,
            BuildRules rules,
            PlayerBuildSession session,
            BuildPieceItem item,
            ServerPlayer player
    ) {
        HitResult hit = player.pick(PlacementTargeting.TARGET_RANGE_BLOCKS, 0.0F, false);
        BuildGridPos gridPos = PlacementTargeting.destinationGrid(
                level,
                rules,
                item.pieceType(),
                player.getDirection(),
                hit.getLocation()
        );
        return session.candidateAt(gridPos, PlayerFacingOrientation.horizontal(player));
    }

    private static void sendPreview(ServerPlayer player, BuildRules rules, PlacementCandidate candidate) {
        if (registeredState == null || registeredMaterializer == null) {
            return;
        }
        if (!ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
            return;
        }

        PlacementService placementService = new PlacementService(
                registeredState,
                rules,
                WorldObstructions.trackedBuildAware(player.level(), registeredMaterializer)
        );
        PlacementPreview preview = placementService.preview(candidate, playerContext(player));
        ServerPlayNetworking.send(player, BuildPreviewPayload.active(
                candidate.slot(),
                candidate.material(),
                preview.valid()
        ));
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
