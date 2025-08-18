package com.github.brainage04.fortniteinminecraft.event;

import com.github.brainage04.fortniteinminecraft.item.ModItems;
import com.github.brainage04.fortniteinminecraft.item.building.PencilItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

public class ModEvents {
    public static void initialize() {
        // pencil (instant break on left click)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            //if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);

            if (stack.getItem() == ModItems.PENCIL) {
                MinecraftClient.getInstance().itemUseCooldown = PencilItem.COOLDOWN;

                BlockState state = world.getBlockState(pos);

                if (PencilItem.WHITELIST.contains(state.getBlock())) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    world.playSound(player, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.MASTER);

                    if (PencilItem.useMats) {
                        player.giveOrDropStack(new ItemStack(state.getBlock()));
                    }

                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }
}
