package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementPreview;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementResult;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.session.ResourceWallet;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.PlayerFacingOrientation;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.CombatSettings;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerPlacementRescue;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class BuildCommands {
    private static final DynamicCommandExceptionType INVALID_PIECE = new DynamicCommandExceptionType(
            input -> Component.literal("Unknown build piece: " + input)
    );
    private static final DynamicCommandExceptionType INVALID_MATERIAL = new DynamicCommandExceptionType(
            input -> Component.literal("Unknown build material: " + input)
    );
    private static final DynamicCommandExceptionType INVALID_PREVIEW_MODE = new DynamicCommandExceptionType(
            input -> Component.literal("Unknown preview mode: " + input)
    );


    private BuildCommands() {
    }

    public static void register(
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
    ) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher, sessions, state, rules, materializer, previewRenderers)
        );
    }

    static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
    ) {
        dispatcher.register(Commands.literal("fim")
                .then(Commands.literal("select")
                        .then(Commands.argument("piece", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEnum(PieceType.values(), builder))
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestEnum(MaterialType.values(), builder))
                                        .executes(context -> select(context, sessions)))))
                .then(Commands.literal("preview-mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEnum(PreviewMode.values(), builder))
                                .executes(context -> selectPreviewMode(context, sessions, previewRenderers))))
                .then(Commands.literal("preview")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> preview(context, sessions, state, rules, materializer, previewRenderers))))
                .then(Commands.literal("place")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> place(context, sessions, state, rules, materializer, previewRenderers))))
                .then(Commands.literal("session")
                        .executes(context -> describeSession(context, sessions)))
                .then(Commands.literal("kit")
                        .executes(context -> giveKit(context, ModItems.ALL_ITEMS, "all Fortnite items"))
                        .then(Commands.literal("build")
                                .executes(context -> giveKit(context, ModItems.BUILD_PIECES, "build pieces")))
                        .then(Commands.literal("combat")
                                .executes(context -> giveKit(context, ModItems.COMBAT_ITEMS, "combat items")))
                        .then(Commands.literal("all")
                                .executes(context -> giveKit(context, ModItems.ALL_ITEMS, "all Fortnite items"))))
                .then(Commands.literal("prevent-bullet-knockback")
                        .executes(BuildCommands::describePreventBulletKnockback)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(BuildCommands::setPreventBulletKnockback)))
                .then(Commands.literal("clear")
                        .executes(context -> clearSession(context, sessions, state, materializer, previewRenderers))));
    }

    private static int select(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions
    ) throws CommandSyntaxException {
        PlayerBuildSession session = sessionFor(context, sessions);
        session.selectPiece(pieceArgument(context));
        session.selectMaterial(materialArgument(context));
        ModItems.refreshBuildItemAppearances(context.getSource().getPlayerOrException());
        context.getSource().sendSuccess(() -> Component.literal("Selected " + describeSelection(session) + "."), false);
        return 1;
    }

    private static int selectPreviewMode(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildPreviewRenderers previewRenderers
    ) throws CommandSyntaxException {
        PlayerBuildSession session = sessionFor(context, sessions);
        PreviewMode mode = previewModeArgument(context);
        session.selectPreviewMode(mode);
        previewRenderers.clear(context.getSource().getPlayerOrException());
        context.getSource().sendSuccess(() -> Component.literal("Preview mode: " + label(mode) + "."), false);
        return 1;
    }

    private static int preview(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        CommandCandidate commandCandidate = commandCandidate(context, sessions, rules);
        PlacementCandidate candidate = commandCandidate.candidate();
        PlacementService placementService = new PlacementService(state, rules, obstructionFor(level, materializer));
        PlacementPreview preview = placementService.preview(candidate, playerContext(player, commandCandidate.session()));
        int rendered = previewRenderers.show(commandCandidate.session().previewMode(), level, player, preview.footprint(), preview.valid());

        if (!preview.valid()) {
            source.sendFailure(Component.literal("Preview rejected: " + preview.message() + " (" + rendered + " "
                    + previewRenderers.renderedUnit(commandCandidate.session().previewMode(), false) + ")."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Preview valid for " + describe(candidate.slot())
                + " (" + rendered + " " + previewRenderers.renderedUnit(commandCandidate.session().previewMode(), true) + ")."), false);
        return 1;
    }

    private static int place(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        CommandCandidate commandCandidate = commandCandidate(context, sessions, rules);
        PlacementCandidate candidate = commandCandidate.candidate();

        PlacementService placementService = new PlacementService(state, rules, obstructionFor(level, materializer));
        PlayerBuildContext buildContext = playerContext(player, commandCandidate.session());
        PlacementPreview preview = placementService.preview(candidate, buildContext);
        if (!preview.valid()) {
            previewRenderers.show(commandCandidate.session().previewMode(), level, player, preview.footprint(), false);
            source.sendFailure(Component.literal("Placement rejected: " + preview.message() + "."));
            return 0;
        }

        long tick = level.getGameTime();
        PlacementResult result = placementService.place(candidate, buildContext, tick);
        if (!result.placed()) {
            previewRenderers.show(commandCandidate.session().previewMode(), level, player, preview.footprint(), false);
            source.sendFailure(Component.literal("Placement rejected: " + result.message() + "."));
            return 0;
        }

        WorldBuildWriteResult writeResult = materializer.place(level, result.piece(), result.footprint());
        if (!writeResult.success()) {
            state.remove(candidate.slot());
            refundIfNeeded(buildContext, candidate);
            source.sendFailure(Component.literal("Placement rolled back: " + writeResult.message() + "."));
            return 0;
        }

        PlayerPlacementRescue.rescueAfterPlacement(player, level, rules, materializer, result.footprint());

        previewRenderers.clear(player);
        commandCandidate.session().rememberPlacement(candidate.slot(), tick, tick);
        source.sendSuccess(() -> Component.literal("Placed " + describe(candidate.slot()) + " using "
                + label(candidate.material()) + " (" + writeResult.blockCount() + " world blocks changed)."), false);
        return 1;
    }

    private static int describeSession(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions
    ) throws CommandSyntaxException {
        PlayerBuildSession session = sessionFor(context, sessions);
        BuildSlot lastSlot = session.lastPlacedSlot();
        String lastPlacement = lastSlot == null ? "none" : describe(lastSlot) + " at tick " + session.lastPlacementTick();
        context.getSource().sendSuccess(() -> Component.literal(
                "Selection: " + describeSelection(session) + "; preview mode: " + label(session.previewMode())
                        + "; last placement: " + lastPlacement + "."
        ), false);
        return 1;
    }

    private static int giveKit(
            CommandContext<CommandSourceStack> context,
            Collection<? extends Item> items,
            String description
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int added = 0;
        for (Item item : items) {
            if (player.addItem(new ItemStack(item))) {
                added++;
            }
        }
        int finalAdded = added;
        context.getSource().sendSuccess(() -> Component.literal("Added " + finalAdded + " " + description + "."), false);
        return added;
    }

    private static int describePreventBulletKnockback(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Prevent bullet knockback: "
                + CombatSettings.preventBulletKnockback() + "."), false);
        return CombatSettings.preventBulletKnockback() ? 1 : 0;
    }

    private static int setPreventBulletKnockback(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        CombatSettings.setPreventBulletKnockback(enabled);
        context.getSource().sendSuccess(() -> Component.literal("Prevent bullet knockback: " + enabled + "."), true);
        return enabled ? 1 : 0;
    }

    private static int clearSession(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions,
            BuildWorldState state,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
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
            clearedBlocks += writeResult.blockCount();
        }

        previewRenderers.clear(player);
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


    private static PlayerBuildSession sessionFor(
            CommandContext<CommandSourceStack> context,
            BuildSessionManager sessions
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return sessions.getOrCreate(player.getUUID());
    }

    private static PlayerBuildContext playerContext(ServerPlayer player, PlayerBuildSession session) {
        UUID playerId = player.getUUID();
        if (player.isCreative()) {
            return PlayerBuildContext.creative(playerId);
        }
        return PlayerBuildContext.survival(playerId, ResourceWallet.with(session.selectedMaterial(), 0));
    }

    private static WorldObstruction obstructionFor(ServerLevel level, WorldBuildMaterializer materializer) {
        return (dimension, blockX, blockY, blockZ) -> level.getBlockState(new BlockPos(blockX, blockY, blockZ)).blocksMotion()
                && !materializer.isTrackedBlock(dimension, blockX, blockY, blockZ);
    }

    private static void refundIfNeeded(PlayerBuildContext player, PlacementCandidate candidate) {
        if (!player.creative()) {
            player.resources().add(candidate.material(), candidate.material().placementCost());
        }
    }

    private static PieceType pieceArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return enumArgument(context, "piece", PieceType.class, INVALID_PIECE);
    }

    private static MaterialType materialArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return enumArgument(context, "material", MaterialType.class, INVALID_MATERIAL);
    }

    private static PreviewMode previewModeArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return enumArgument(context, "mode", PreviewMode.class, INVALID_PREVIEW_MODE);
    }

    private static <E extends Enum<E>> E enumArgument(
            CommandContext<CommandSourceStack> context,
            String name,
            Class<E> enumType,
            DynamicCommandExceptionType failure
    ) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, name);
        try {
            return Enum.valueOf(enumType, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw failure.create(raw);
        }
    }

    private static CompletableFuture<Suggestions> suggestEnum(Enum<?>[] values, SuggestionsBuilder builder) {
        for (Enum<?> value : values) {
            builder.suggest(label(value));
        }
        return builder.buildFuture();
    }

    private static String describeSelection(PlayerBuildSession session) {
        return label(session.selectedPiece()) + " / " + label(session.selectedMaterial());
    }

    private static String describe(BuildSlot slot) {
        BuildGridPos gridPos = slot.gridPos();
        return label(slot.pieceType()) + " " + label(slot.orientation()) + " at "
                + gridPos.dimension() + "[" + gridPos.x() + ", " + gridPos.y() + ", " + gridPos.z() + "]";
    }

    private static String plural(int count, String singular) {
        return count == 1 ? singular : singular + "s";
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private record CommandCandidate(PlayerBuildSession session, PlacementCandidate candidate) {
    }
}
