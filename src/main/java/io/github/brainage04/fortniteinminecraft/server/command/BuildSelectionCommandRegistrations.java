package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.describe;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.describeSelection;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.label;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.materialArgument;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.pieceArgument;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.previewModeArgument;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.suggestEnum;

final class BuildSelectionCommandRegistrations {
  private BuildSelectionCommandRegistrations() {
  }

  static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandRegistrar registrar) {
    root
        .then(selectCommand(registrar.sessions()))
        .then(previewModeCommand(registrar.sessions(), registrar.previewRenderers()))
        .then(sessionCommand(registrar.sessions()));
  }

  private static LiteralArgumentBuilder<CommandSourceStack> selectCommand(BuildSessionManager sessions) {
    return Commands.literal("select")
        .then(Commands.argument("piece", StringArgumentType.word())
            .suggests((context, builder) -> suggestEnum(PieceType.values(), builder))
            .then(Commands.argument("material", StringArgumentType.word())
                .suggests((context, builder) -> suggestEnum(MaterialType.values(), builder))
                .executes(context -> select(context, sessions))));
  }

  private static LiteralArgumentBuilder<CommandSourceStack> previewModeCommand(
      BuildSessionManager sessions,
      BuildPreviewRenderers previewRenderers) {
    return Commands.literal("preview-mode")
        .then(Commands.argument("mode", StringArgumentType.word())
            .suggests((context, builder) -> suggestEnum(PreviewMode.values(), builder))
            .executes(context -> selectPreviewMode(context, sessions, previewRenderers)));
  }

  private static LiteralArgumentBuilder<CommandSourceStack> sessionCommand(BuildSessionManager sessions) {
    return Commands.literal("session")
        .executes(context -> describeSession(context, sessions));
  }

  private static int select(
      CommandContext<CommandSourceStack> context,
      BuildSessionManager sessions) throws CommandSyntaxException {
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
      BuildPreviewRenderers previewRenderers) throws CommandSyntaxException {
    PlayerBuildSession session = sessionFor(context, sessions);
    PreviewMode mode = previewModeArgument(context);
    session.selectPreviewMode(mode);
    previewRenderers.clear(context.getSource().getPlayerOrException());
    context.getSource().sendSuccess(() -> Component.literal("Preview mode: " + label(mode) + "."), false);
    return 1;
  }

  private static int describeSession(
      CommandContext<CommandSourceStack> context,
      BuildSessionManager sessions) throws CommandSyntaxException {
    PlayerBuildSession session = sessionFor(context, sessions);
    BuildSlot lastSlot = session.lastPlacedSlot();
    String lastPlacement = lastSlot == null ? "none" : describe(lastSlot) + " at tick " + session.lastPlacementTick();
    context.getSource().sendSuccess(() -> Component.literal(
        "Selection: " + describeSelection(session) + "; preview mode: " + label(session.previewMode())
            + "; last placement: " + lastPlacement + "."),
        false);
    return 1;
  }

  private static PlayerBuildSession sessionFor(
      CommandContext<CommandSourceStack> context,
      BuildSessionManager sessions) throws CommandSyntaxException {
    return sessions.getOrCreate(context.getSource().getPlayerOrException().getUUID());
  }
}
