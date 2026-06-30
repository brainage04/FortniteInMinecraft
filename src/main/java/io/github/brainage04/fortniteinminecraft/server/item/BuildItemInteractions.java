package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BuildItemInteractions {
    static final long TURBO_PLACEMENT_INTERVAL_TICKS = 1L;
    static final long TURBO_INPUT_GRACE_TICKS = 8L;
    private static final double REPAIR_HEALTH_FRACTION = 0.2D;
    private static BuildWorldState registeredState;
    private static WorldBuildMaterializer registeredMaterializer;
    private static final Set<UUID> AUTOMATIC_PREVIEW_SUPPRESSED_PLAYERS = new HashSet<>();

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

    public static void suppressAutomaticPreview(ServerPlayer player, boolean suppressed) {
        Objects.requireNonNull(player, "player");
        if (suppressed) {
            AUTOMATIC_PREVIEW_SUPPRESSED_PLAYERS.add(player.getUUID());
        } else {
            AUTOMATIC_PREVIEW_SUPPRESSED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void clearAutomaticPreviewSuppressions() {
        AUTOMATIC_PREVIEW_SUPPRESSED_PLAYERS.clear();
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
        if (AUTOMATIC_PREVIEW_SUPPRESSED_PLAYERS.contains(player.getUUID())) {
            return;
        }
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session != null && session.buildModeActive()) {
            updatePreview(player, rules, session, session.selectedPiece());
            return;
        }

        BuildPieceItem item = ModItems.asBuildPiece(player.getMainHandItem());
        if (item == null) {
            if (session != null) {
                session.clearPreview();
                session.stopTurboPlacement();
            }
            sendInactivePreview(player);
            return;
        }

        PlayerBuildSession heldItemSession = sessions.getOrCreate(player.getUUID());
        heldItemSession.selectPiece(item.pieceType());
        updatePreview(player, rules, heldItemSession, item.pieceType());
    }

    public static boolean hasActiveBuildMode(ServerPlayer player, BuildSessionManager sessions) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        PlayerBuildSession session = sessions.get(player.getUUID());
        return session != null && session.buildModeActive();
    }

    public static InteractionResult selectPiece(
            ServerPlayer player,
            PieceType pieceType,
            BuildSessionManager sessions,
            BuildRules rules
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pieceType, "pieceType");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(rules, "rules");

        PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
        session.activateBuildMode(pieceType);
        updatePreview(player, rules, session, pieceType);
        player.sendSystemMessage(Component.literal("Selected " + label(pieceType) + " build piece."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult deactivateBuildMode(ServerPlayer player, BuildSessionManager sessions) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session == null || !session.buildModeActive()) {
            return InteractionResult.PASS;
        }
        session.deactivateBuildMode();
        sendInactivePreview(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult rotateSelectedPiece(
            ServerPlayer player,
            BuildSessionManager sessions,
            BuildRules rules
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(rules, "rules");
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session == null || !session.buildModeActive()) {
            return InteractionResult.PASS;
        }
        session.rotatePlacement();
        updatePreview(player, rules, session, session.selectedPiece());
        player.sendSystemMessage(Component.literal("Rotated " + label(session.selectedPiece()) + " placement."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult repairTargetedPiece(
            ServerPlayer player,
            BuildWorldState state,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(materializer, "materializer");

        ServerLevel level = player.level();
        HitResult hit = player.pick(PlacementTargeting.TARGET_RANGE_BLOCKS, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.sendSystemMessage(Component.literal("Look at a build piece to repair."), true);
            return InteractionResult.PASS;
        }

        String dimension = level.dimension().identifier().toString();
        BuildSlot slot = materializer.topOwnerAt(dimension, blockHit.getBlockPos());
        if (slot == null) {
            player.sendSystemMessage(Component.literal("Look at a tracked build piece to repair."), true);
            return InteractionResult.PASS;
        }

        BuildPieceState current = state.get(slot);
        if (current == null) {
            player.sendSystemMessage(Component.literal("Repair failed: tracked world block has no build state."), false);
            return InteractionResult.FAIL;
        }
        if (!player.isCreative() && !player.getUUID().equals(current.owner())) {
            player.sendSystemMessage(Component.literal("Cannot repair another player's build."), true);
            return InteractionResult.FAIL;
        }

        long tick = level.getGameTime();
        BuildPieceState progressed = current.progressedTo(tick);
        if (progressed.currentHealth() >= progressed.maxHealth()) {
            if (!progressed.equals(current)) {
                state.replace(progressed);
                materializer.refresh(level, progressed);
            }
            player.sendSystemMessage(Component.literal(label(slot.pieceType()) + " is already fully repaired."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        PlayerBuildContext context = playerContext(player);
        int cost = BuildConstants.DEFAULT_RESOURCE_COST;
        if (!context.creative() && !context.resources().spend(current.material(), cost)) {
            player.sendSystemMessage(Component.literal("Not enough " + label(current.material()) + " to repair."), true);
            return InteractionResult.FAIL;
        }

        BuildWorldState.RepairResult result = state.repair(slot, repairAmount(current), tick);
        if (!result.repaired()) {
            refundRepairCost(context, current.material(), cost);
            player.sendSystemMessage(Component.literal("Repair failed: build piece changed."), false);
            return InteractionResult.FAIL;
        }

        WorldBuildWriteResult refreshResult = materializer.refresh(level, result.after());
        if (!refreshResult.success()) {
            state.replace(result.before());
            refundRepairCost(context, current.material(), cost);
            player.sendSystemMessage(Component.literal("Repair rolled back: " + refreshResult.message() + "."), false);
            return InteractionResult.FAIL;
        }

        PlayerResourceStateSync.send(player);
        player.sendSystemMessage(Component.literal(
                "Repaired " + label(slot.pieceType()) + " to "
                        + result.after().currentHealth() + "/" + result.after().maxHealth() + "."
        ), true);
        return InteractionResult.SUCCESS_SERVER;
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

    public static InteractionResult handlePrimaryInput(
            ServerPlayer player,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        return placeSelected(player, sessions, state, rules, materializer);
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
        return cycleMaterial(player, ModItems.asBuildPiece(player.getItemInHand(hand)), sessions);
    }

    public static InteractionResult handleSecondaryInput(
            ServerPlayer player,
            BuildSessionManager sessions
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessions, "sessions");
        return cycleMaterial(player, null, sessions);
    }

    private static InteractionResult cycleMaterial(
            ServerPlayer player,
            BuildPieceItem heldBuildItem,
            BuildSessionManager sessions
    ) {
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session == null || !session.buildModeActive()) {
            if (heldBuildItem == null) {
                return InteractionResult.PASS;
            }
            session = sessions.getOrCreate(player.getUUID());
            session.selectPiece(heldBuildItem.pieceType());
        }

        long tick = player.level().getGameTime();
        if (!session.markMaterialCycle(tick)) {
            return InteractionResult.SUCCESS_SERVER;
        }
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
        PlacementCandidate candidate = targetCandidate(level, rules, session, item.pieceType(), player);
        session.rememberPreview(candidate);

        long tick = level.getGameTime();
        session.markBuildUse(tick);
        session.extendTurboPlacement(tick, TURBO_INPUT_GRACE_TICKS);
        if (candidate.slot().equals(session.lastPlacedSlot())) {
            return InteractionResult.SUCCESS_SERVER;
        }

        return placeCandidate(player, level, session, candidate, state, rules, materializer, tick, true, true);
    }

    private static InteractionResult placeSelected(
            ServerPlayer player,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        PlayerBuildSession session = sessions.get(player.getUUID());
        if (session == null || !session.buildModeActive()) {
            return InteractionResult.PASS;
        }

        ServerLevel level = player.level();
        PlacementCandidate candidate = targetCandidate(level, rules, session, session.selectedPiece(), player);
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

    private static void refundRepairCost(PlayerBuildContext player, MaterialType material, int cost) {
        if (!player.creative() && !player.resources().infinite()) {
            player.resources().add(material, cost);
        }
    }

    private static int repairAmount(BuildPieceState piece) {
        return Math.max(1, (int) Math.ceil(piece.maxHealth() * REPAIR_HEALTH_FRACTION));
    }

    private static PlacementCandidate targetCandidate(
            ServerLevel level,
            BuildRules rules,
            PlayerBuildSession session,
            PieceType pieceType,
            ServerPlayer player
    ) {
        HitResult hit = player.pick(PlacementTargeting.TARGET_RANGE_BLOCKS, 0.0F, false);
        BuildGridPos gridPos = PlacementTargeting.destinationGrid(
                level,
                rules,
                pieceType,
                player.getDirection(),
                hit.getLocation()
        );
        return session.candidateAt(gridPos, PlayerFacingOrientation.horizontal(player));
    }

    private static void updatePreview(ServerPlayer player, BuildRules rules, PlayerBuildSession session, PieceType pieceType) {
        PlacementCandidate candidate = targetCandidate(player.level(), rules, session, pieceType, player);
        session.rememberPreview(candidate);
        sendPreview(player, rules, candidate);
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

    private static void sendInactivePreview(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
            ServerPlayNetworking.send(player, BuildPreviewPayload.inactive());
        }
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
