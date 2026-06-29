package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.brainage04.fortniteinminecraft.server.item.CombatSettings;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.formatScale;

final class GameplaySettingCommandRegistrations {
  private GameplaySettingCommandRegistrations() {
  }

  static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
    root
        .then(Commands.literal("prevent-bullet-knockback")
            .executes(GameplaySettingCommandRegistrations::describePreventBulletKnockback)
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(GameplaySettingCommandRegistrations::setPreventBulletKnockback)))
        .then(Commands.literal("hitmarker-scale")
            .executes(GameplaySettingCommandRegistrations::describeHitMarkerScale)
            .then(Commands.literal("reset")
                .executes(GameplaySettingCommandRegistrations::resetHitMarkerScale))
            .then(Commands.argument("near", FloatArgumentType.floatArg(
                HitMarkerDisplays.MIN_MARKER_SCALE,
                HitMarkerDisplays.MAX_MARKER_SCALE))
                .then(Commands.argument("far", FloatArgumentType.floatArg(
                    HitMarkerDisplays.MIN_MARKER_SCALE,
                    HitMarkerDisplays.MAX_MARKER_SCALE))
                    .executes(GameplaySettingCommandRegistrations::setHitMarkerScale))));
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

  private static int describeHitMarkerScale(CommandContext<CommandSourceStack> context) {
    HitMarkerDisplays.ScaleModel model = HitMarkerDisplays.scaleModel();
    context.getSource().sendSuccess(() -> Component.literal("Hitmarker scale: "
        + formatScale(model.nearScale()) + " at 0 blocks, "
        + formatScale(model.farScale()) + " at "
        + formatScale((float) HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS) + " blocks."), false);
    return 1;
  }

  private static int setHitMarkerScale(CommandContext<CommandSourceStack> context) {
    float nearScale = FloatArgumentType.getFloat(context, "near");
    float farScale = FloatArgumentType.getFloat(context, "far");
    HitMarkerDisplays.ScaleModel model = HitMarkerDisplays.setScaleModel(nearScale, farScale);
    context.getSource().sendSuccess(() -> Component.literal("Hitmarker scale: "
        + formatScale(model.nearScale()) + " at 0 blocks, "
        + formatScale(model.farScale()) + " at "
        + formatScale((float) HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS) + " blocks."), true);
    return 1;
  }

  private static int resetHitMarkerScale(CommandContext<CommandSourceStack> context) {
    HitMarkerDisplays.ScaleModel model = HitMarkerDisplays.resetScaleModel();
    context.getSource().sendSuccess(() -> Component.literal("Hitmarker scale reset to "
        + formatScale(model.nearScale()) + " at 0 blocks, "
        + formatScale(model.farScale()) + " at "
        + formatScale((float) HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS) + " blocks."), true);
    return 1;
  }
}
