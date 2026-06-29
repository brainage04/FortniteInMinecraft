package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class CommandRegistrationHelpers {
  private static final DynamicCommandExceptionType INVALID_PIECE = new DynamicCommandExceptionType(
      input -> Component.literal("Unknown build piece: " + input));
  private static final DynamicCommandExceptionType INVALID_MATERIAL = new DynamicCommandExceptionType(
      input -> Component.literal("Unknown build material: " + input));
  private static final DynamicCommandExceptionType INVALID_PREVIEW_MODE = new DynamicCommandExceptionType(
      input -> Component.literal("Unknown preview mode: " + input));

  private CommandRegistrationHelpers() {
  }

  static PieceType pieceArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return enumArgument(context, "piece", PieceType.class, INVALID_PIECE);
  }

  static MaterialType materialArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return enumArgument(context, "material", MaterialType.class, INVALID_MATERIAL);
  }

  static PreviewMode previewModeArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return enumArgument(context, "mode", PreviewMode.class, INVALID_PREVIEW_MODE);
  }

  static CompletableFuture<Suggestions> suggestEnum(Enum<?>[] values, SuggestionsBuilder builder) {
    for (Enum<?> value : values) {
      builder.suggest(label(value));
    }
    return builder.buildFuture();
  }

  static String describeSelection(PlayerBuildSession session) {
    return label(session.selectedPiece()) + " / " + label(session.selectedMaterial());
  }

  static String describe(BuildSlot slot) {
    BuildGridPos gridPos = slot.gridPos();
    return label(slot.pieceType()) + " " + label(slot.orientation()) + " at "
        + gridPos.dimension() + "[" + gridPos.x() + ", " + gridPos.y() + ", " + gridPos.z() + "]";
  }

  static String plural(int count, String singular) {
    return count == 1 ? singular : singular + "s";
  }

  static String formatScale(float value) {
    return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
  }

  static String label(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }

  private static <E extends Enum<E>> E enumArgument(
      CommandContext<CommandSourceStack> context,
      String name,
      Class<E> enumType,
      DynamicCommandExceptionType failure) throws CommandSyntaxException {
    String raw = StringArgumentType.getString(context, name);
    try {
      return Enum.valueOf(enumType, raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      throw failure.create(raw);
    }
  }
}
