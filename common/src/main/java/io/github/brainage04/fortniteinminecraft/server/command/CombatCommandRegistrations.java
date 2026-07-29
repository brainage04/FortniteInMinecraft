package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.fortniteinminecraft.server.item.ExplosiveProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.ProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

final class CombatCommandRegistrations {
    private CombatCommandRegistrations() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload")
                .executes(CombatCommandRegistrations::reloadHeldWeapon));
    }

    private static int reloadHeldWeapon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        InteractionResult result = WeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND);
        if (result == InteractionResult.PASS) {
            result = ProjectileWeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND);
        }
        if (result == InteractionResult.PASS) {
            result = ExplosiveProjectileWeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND);
        }
        if (result == InteractionResult.PASS) {
            context.getSource().sendFailure(Component.literal("Hold a reloadable Fortnite weapon with a non-full magazine."));
            return 0;
        }
        return 1;
    }
}
