package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementPreview;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementResult;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.PlayerFacingOrientation;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerPlacementRescue;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.WorldObstructions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.describe;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.label;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.plural;

final class BuildPlacementCommandRegistrations {
    private BuildPlacementCommandRegistrations() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandRegistrar registrar) {
        root
                .then(previewCommand(registrar))
                .then(placeCommand(registrar))
                .then(clearCommand(registrar));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> previewCommand(CommandRegistrar registrar) {
        return Commands.literal("preview")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> preview(
                                context,
                                registrar.sessions(),
                                registrar.state(),
                                registrar.rules(),
                                registrar.materializer())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> placeCommand(CommandRegistrar registrar) {
        return Commands.literal("place")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> place(
                                context,
                                registrar.sessions(),
                                registrar.state(),
                                registrar.rules(),
                                registrar.materializer())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clearCommand(CommandRegistrar registrar) {
        return Commands.literal("clear")
                .executes(context -> clearSession(
                        context,
                        registrar.sessions(),
                        registrar.state(),
                        registrar.materializer()));
    }

    private static int preview(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        CommandCandidate commandCandidate = commandCandidate(context, sessions, rules);
        PlacementCandidate candidate = commandCandidate.candidate();
        PlacementService placementService = new PlacementService(state, rules, WorldObstructions.trackedBuildAware(level, materializer));
        PlacementPreview preview = placementService.preview(candidate, playerContext(player));

        if (!preview.valid()) {
            source.sendFailure(Component.literal("Preview rejected: " + preview.message() + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Preview valid for " + describe(candidate.slot()) + "."), false);
        return 1;
    }

    private static int place(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        CommandCandidate commandCandidate = commandCandidate(context, sessions, rules);
        PlacementCandidate candidate = commandCandidate.candidate();

        PlacementService placementService = new PlacementService(state, rules, WorldObstructions.trackedBuildAware(level, materializer));
        PlayerBuildContext buildContext = playerContext(player);
        PlacementPreview preview = placementService.preview(candidate, buildContext);
        if (!preview.valid()) {
            source.sendFailure(Component.literal("Placement rejected: " + preview.message() + "."));
            return 0;
        }

        long tick = level.getGameTime();
        PlacementResult result = placementService.place(candidate, buildContext, tick);
        if (!result.placed()) {
            source.sendFailure(Component.literal("Placement rejected: " + result.message() + "."));
            return 0;
        }

        WorldBuildWriteResult writeResult = materializer.place(level, result.piece(), result.footprint());
        if (!writeResult.success()) {
            state.remove(candidate.slot());
            refundIfNeeded(buildContext, candidate);
            PlayerResourceStateSync.send(player);
            source.sendFailure(Component.literal("Placement rolled back: " + writeResult.message() + "."));
            return 0;
        }

        PlayerPlacementRescue.rescueAfterPlacement(player, level, rules, materializer, result.footprint());
        PlayerResourceStateSync.send(player);

        commandCandidate.session().rememberPlacement(candidate.slot(), tick, tick);
        source.sendSuccess(() -> Component.literal("Placed " + describe(candidate.slot()) + " using "
                + label(candidate.material()) + " (" + writeResult.blockCount() + " world blocks changed)."), false);
        return 1;
    }

    private static int clearSession(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            WorldBuildMaterializer materializer
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        UUID playerId = player.getUUID();
        String dimension = level.dimension().identifier().toString();
        List<BuildPieceState> ownedPieces = state.pieces().stream()
                .filter(piece -> playerId.equals(piece.owner()))
                .filter(piece -> dimension.equals(piece.slot().gridPos().dimension()))
                .toList();

        int clearedBlocks = 0;
        for (BuildPieceState piece : ownedPieces) {
            WorldBuildWriteResult writeResult = materializer.clear(level, piece);
            if (!writeResult.success()) {
                source.sendFailure(Component.literal("Clear aborted: " + writeResult.message() + "."));
                return 0;
            }
            state.remove(piece.slot());
            BuildWeakPoints.clear(piece.slot());
            clearedBlocks += writeResult.blockCount();
        }

        sessions.reset(playerId);
        int clearedPieces = ownedPieces.size();
        int finalClearedBlocks = clearedBlocks;
        source.sendSuccess(() -> Component.literal("Build session reset; cleared "
                + clearedPieces + " " + plural(clearedPieces, "piece") + " and "
                + finalClearedBlocks + " " + plural(finalClearedBlocks, "world block") + " in this dimension."), false);
        return 1;
    }

    private static CommandCandidate commandCandidate(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildRules rules
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = context.getSource().getLevel();
        PlayerBuildSession session = sessions.getOrCreate(player.getUUID());
        Orientation orientation = PlayerFacingOrientation.horizontal(player);

        BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
        String dimension = level.dimension().identifier().toString();
        BuildGridPos gridPos = new SnapGrid(rules).snap(dimension, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        PlacementCandidate candidate = session.candidateAt(gridPos, orientation);
        session.rememberPreview(candidate);
        return new CommandCandidate(session, candidate);
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

    private record CommandCandidate(PlayerBuildSession session, PlacementCandidate candidate) {
    }
}
