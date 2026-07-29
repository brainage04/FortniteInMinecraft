package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

final class KitCommandRegistrations {
  private KitCommandRegistrations() {
  }

  static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
    root.then(Commands.literal("kit")
        .executes(context -> giveKit(context, ModItems.ALL_ITEMS, "all Fortnite items"))
        .then(Commands.literal("build")
            .executes(context -> giveKit(context, ModItems.BUILD_PIECES, "build pieces")))
        .then(Commands.literal("combat")
            .executes(context -> giveKit(context, ModItems.COMBAT_ITEMS, "combat items")))
        .then(Commands.literal("all")
            .executes(context -> giveKit(context, ModItems.ALL_ITEMS, "all Fortnite items"))));
  }

  private static int giveKit(
      CommandContext<CommandSourceStack> context,
      Collection<? extends Item> items,
      String description) throws CommandSyntaxException {
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
}
