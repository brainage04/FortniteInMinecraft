package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.EditModePayload;
import io.github.brainage04.fortniteinminecraft.core.edit.BuildEditGrids;
import io.github.brainage04.fortniteinminecraft.core.edit.EditGridCell;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.placement.BuildTargeting;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BuildEditInteractions {
    private static final double EDIT_TRACE_RANGE_BLOCKS = BuildTargeting.TARGET_RANGE_BLOCKS + 2.0D;
    private static final Map<UUID, EditSession> SESSIONS = new HashMap<>();

    private BuildEditInteractions() {
    }

    public static InteractionResult handleEditKey(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(materializer, "materializer");

        EditSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            if (player.isShiftKeyDown()) {
                return cancelEditing(player, state, rules, materializer);
            }
            return confirmEditing(player, state);
        }
        return beginTargetedPiece(player, state, rules, materializer);
    }

    public static boolean handlePrimaryInput(
            ServerPlayer player,
            boolean pressed,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        return handleCellInput(player, pressed, state, rules, materializer);
    }

    public static boolean handleSecondaryInput(
            ServerPlayer player,
            boolean pressed,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        return handleCellInput(player, pressed, state, rules, materializer);
    }

    public static boolean hasActiveEditSession(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return SESSIONS.containsKey(player.getUUID());
    }

    public static InteractionResult cancelEditing(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        EditSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            syncEditMode(player, false);
            return InteractionResult.PASS;
        }
        if (!applyPiece(player, state, rules, materializer, session, session.originalPiece())) {
            SESSIONS.put(player.getUUID(), session);
            syncEditMode(player, true);
            return InteractionResult.FAIL;
        }
        player.sendSystemMessage(Component.literal("Canceled edit; restored original " + label(session.slot().pieceType()) + "."), true);
        syncEditMode(player, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void syncEditMode(ServerPlayer player, boolean active) {
        ServerPlayNetworking.send(player, new EditModePayload(active));
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    private static InteractionResult beginTargetedPiece(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        ServerLevel level = player.level();
        HitResult hit = player.pick(BuildTargeting.TARGET_RANGE_BLOCKS, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.sendSystemMessage(Component.literal("Look at a build piece to edit."), true);
            syncEditMode(player, false);
            return InteractionResult.PASS;
        }

        String dimension = level.dimension().identifier().toString();
        BlockPos hitPos = blockHit.getBlockPos();
        BuildSlot slot = materializer.topOwnerAt(dimension, hitPos);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("Look at a tracked build piece to edit."), true);
            syncEditMode(player, false);
            return InteractionResult.PASS;
        }

        BuildPieceState current = state.get(slot);
        if (current == null) {
            player.sendSystemMessage(Component.literal("Build edit failed: tracked world block has no build state."), false);
            syncEditMode(player, false);
            return InteractionResult.FAIL;
        }
        if (!player.isCreative() && !player.getUUID().equals(current.owner())) {
            player.sendSystemMessage(Component.literal("Cannot edit another player's build."), true);
            syncEditMode(player, false);
            return InteractionResult.FAIL;
        }

        int selectedMask = BuildEditGrids.maskForVariant(slot.pieceType(), current.editVariant()).orElse(0);
        EditSession session = new EditSession(current, selectedMask);
        SESSIONS.put(player.getUUID(), session);
        syncEditMode(player, true);
        player.sendSystemMessage(Component.literal(
                "Editing " + label(slot.pieceType()) + ": attack/use-drag cells, sneak+use reset, edit confirm, sneak+edit cancel."
        ), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean handleCellInput(
            ServerPlayer player,
            boolean pressed,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        Objects.requireNonNull(player, "player");
        EditSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        if (!pressed) {
            session.clearDrag();
            return true;
        }
        if (player.isShiftKeyDown()) {
            session.clearDrag();
            resetSelection(player, state, rules, materializer, session);
            return true;
        }

        Optional<EditGridCell> cell = targetedCell(player, session, rules);
        if (cell.isEmpty()) {
            player.sendSystemMessage(Component.literal("Aim at the edit grid for this " + label(session.slot().pieceType()) + "."), true);
            return true;
        }
        if (cell.get().equals(session.lastToggledCell())) {
            return true;
        }

        int nextMask = BuildEditGrids.toggle(session.slot().pieceType(), session.selectedMask(), cell.get());
        if (!BuildEditGrids.isConfirmableMask(session.slot().pieceType(), nextMask)) {
            player.sendSystemMessage(Component.literal("Edit rejected: selection would remove the entire piece."), true);
            session.rememberCell(cell.get());
            return true;
        }
        if (applyMask(player, state, rules, materializer, session, nextMask)) {
            session.rememberCell(cell.get());
            player.sendSystemMessage(Component.literal(
                    "Selected " + selectedCellSummary(session) + " -> "
                            + BuildEditGrids.label(session.slot().pieceType(), session.selectedMask())
            ), true);
        }
        return true;
    }

    private static void resetSelection(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            EditSession session
    ) {
        if (applyMask(player, state, rules, materializer, session, 0)) {
            player.sendSystemMessage(Component.literal("Edit reset to base shape; press edit to confirm or sneak+edit to cancel."), true);
        }
    }

    private static InteractionResult confirmEditing(ServerPlayer player, BuildWorldState state) {
        EditSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            syncEditMode(player, false);
            return InteractionResult.PASS;
        }
        BuildPieceState current = state.get(session.slot());
        if (current == null || !current.id().equals(session.originalPiece().id())) {
            player.sendSystemMessage(Component.literal("Build edit confirm failed: edited piece changed."), false);
            syncEditMode(player, false);
            return InteractionResult.FAIL;
        }
        player.sendSystemMessage(Component.literal(
                "Confirmed " + label(session.slot().pieceType()) + " edit: "
                        + BuildEditGrids.label(session.slot().pieceType(), session.selectedMask()) + "."
        ), true);
        syncEditMode(player, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean applyMask(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            EditSession session,
            int selectedMask
    ) {
        BuildPieceState replacement = session.originalPiece().withEditVariant(
                BuildEditGrids.variantFor(session.slot().pieceType(), selectedMask)
        );
        if (!applyPiece(player, state, rules, materializer, session, replacement)) {
            return false;
        }
        session.selectedMask(selectedMask);
        return true;
    }

    private static boolean applyPiece(
            ServerPlayer player,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            EditSession session,
            BuildPieceState replacement
    ) {
        ServerLevel level = player.level();
        BuildPieceState previous = state.get(session.slot());
        if (previous == null || !previous.id().equals(session.originalPiece().id())) {
            player.sendSystemMessage(Component.literal("Build edit failed: edited piece changed."), false);
            return false;
        }
        if (previous.editVariant().equals(replacement.editVariant())) {
            return true;
        }

        FootprintProjector projector = new FootprintProjector(rules);
        SnapGrid snapGrid = new SnapGrid(rules);
        PieceFootprint oldFootprint = projector.project(previous);
        PieceFootprint newFootprint = projector.project(replacement);

        WorldBuildWriteResult clearResult = materializer.clear(level, previous);
        if (!clearResult.success()) {
            FortniteInMinecraft.LOGGER.warn("Build edit preview clear failed for {}: {}", session.slot(), clearResult.message());
            return false;
        }
        state.remove(session.slot());
        BuildWeakPoints.clear(session.slot());

        List<BlockOffset> newBlocks = newFootprint.absoluteBlocks(snapGrid.blockOrigin(replacement.slot().gridPos()));
        if (!state.addIfNotConflicting(replacement, newBlocks)) {
            restore(level, state, materializer, previous, oldFootprint, snapGrid);
            player.sendSystemMessage(Component.literal("Build edit rejected: edited shape would overlap another piece."), true);
            return false;
        }

        WorldBuildWriteResult placeResult = materializer.place(level, replacement, newFootprint);
        if (!placeResult.success()) {
            state.remove(session.slot());
            restore(level, state, materializer, previous, oldFootprint, snapGrid);
            FortniteInMinecraft.LOGGER.warn("Build edit preview rollback for {}: {}", session.slot(), placeResult.message());
            return false;
        }

        BuildCollapseScheduler.scheduleAfterSupportRemoved(level, session.slot(), level.getGameTime());
        return true;
    }

    private static Optional<EditGridCell> targetedCell(ServerPlayer player, EditSession session, BuildRules rules) {
        SnapGrid snapGrid = new SnapGrid(rules);
        BlockOffset origin = snapGrid.blockOrigin(session.slot().gridPos());
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int tile = rules.footprintSizeBlocks();
        int max = tile - 1;

        return switch (session.slot().pieceType()) {
            case WALL -> targetedWallCell(session, rules, origin, eye, look, tile, max);
            case FLOOR, ROOF, STAIR -> intersection(origin.y(), eye.y, look.y)
                    .flatMap(t -> {
                        Vec3 hit = eye.add(look.scale(t));
                        return BuildEditGrids.cellAtLocal(
                                session.slot().pieceType(),
                                hit.x - origin.x(),
                                0.0D,
                                hit.z - origin.z(),
                                tile,
                                rules.wallHeightBlocks()
                        );
                    });
        };
    }

    private static Optional<EditGridCell> targetedWallCell(
            EditSession session,
            BuildRules rules,
            BlockOffset origin,
            Vec3 eye,
            Vec3 look,
            int tile,
            int max
    ) {
        return switch (session.slot().orientation()) {
            case NORTH -> intersection(origin.z(), eye.z, look.z).flatMap(t -> {
                Vec3 hit = eye.add(look.scale(t));
                return wallCell(session, rules, hit.x - origin.x(), hit.y - origin.y(), tile);
            });
            case SOUTH -> intersection(origin.z() + max, eye.z, look.z).flatMap(t -> {
                Vec3 hit = eye.add(look.scale(t));
                return wallCell(session, rules, max - (hit.x - origin.x()), hit.y - origin.y(), tile);
            });
            case EAST -> intersection(origin.x() + max, eye.x, look.x).flatMap(t -> {
                Vec3 hit = eye.add(look.scale(t));
                return wallCell(session, rules, hit.z - origin.z(), hit.y - origin.y(), tile);
            });
            case WEST -> intersection(origin.x(), eye.x, look.x).flatMap(t -> {
                Vec3 hit = eye.add(look.scale(t));
                return wallCell(session, rules, max - (hit.z - origin.z()), hit.y - origin.y(), tile);
            });
        };
    }

    private static Optional<EditGridCell> wallCell(EditSession session, BuildRules rules, double localX, double localY, int tile) {
        return BuildEditGrids.cellAtLocal(
                session.slot().pieceType(),
                localX,
                localY,
                0.0D,
                tile,
                rules.wallHeightBlocks()
        );
    }

    private static Optional<Double> intersection(double plane, double origin, double direction) {
        if (Math.abs(direction) < 1.0E-5D) {
            return Optional.empty();
        }
        double t = (plane - origin) / direction;
        if (t < 0.0D || t > EDIT_TRACE_RANGE_BLOCKS) {
            return Optional.empty();
        }
        return Optional.of(t);
    }

    private static void restore(
            ServerLevel level,
            BuildWorldState state,
            WorldBuildMaterializer materializer,
            BuildPieceState piece,
            PieceFootprint footprint,
            SnapGrid snapGrid
    ) {
        List<BlockOffset> occupiedBlocks = footprint.absoluteBlocks(snapGrid.blockOrigin(piece.slot().gridPos()));
        state.addIfNotConflicting(piece, occupiedBlocks);
        materializer.place(level, piece, footprint);
    }

    private static String selectedCellSummary(EditSession session) {
        return Integer.bitCount(session.selectedMask()) + "/"
                + BuildEditGrids.cellCount(session.slot().pieceType()) + " cells";
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static final class EditSession {
        private final BuildPieceState originalPiece;
        private int selectedMask;
        private EditGridCell lastToggledCell;

        private EditSession(BuildPieceState originalPiece, int selectedMask) {
            this.originalPiece = Objects.requireNonNull(originalPiece, "originalPiece");
            this.selectedMask = selectedMask;
        }

        private BuildSlot slot() {
            return originalPiece.slot();
        }

        private BuildPieceState originalPiece() {
            return originalPiece;
        }

        private int selectedMask() {
            return selectedMask;
        }

        private void selectedMask(int selectedMask) {
            this.selectedMask = selectedMask;
        }

        private EditGridCell lastToggledCell() {
            return lastToggledCell;
        }

        private void rememberCell(EditGridCell cell) {
            lastToggledCell = Objects.requireNonNull(cell, "cell");
        }

        private void clearDrag() {
            lastToggledCell = null;
        }
    }
}
