package com.github.brainage04.fortniteinminecraft.item.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class PencilItem extends Item {
    public static final int COOLDOWN = 1;
    public static boolean useMats = true;

    public static final List<Block> WHITELIST = List.of(
            Blocks.OAK_PLANKS,
            Blocks.BRICKS,
            Blocks.COPPER_BLOCK
    );

    public PencilItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        MinecraftClient.getInstance().itemUseCooldown = COOLDOWN;

        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        MinecraftClient.getInstance().itemUseCooldown = COOLDOWN;

        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (player == null) return super.useOnBlock(context);

        BlockPos clicked = context.getBlockPos();
        BlockState clickedState = world.getBlockState(clicked);
        Block clickedBlock = clickedState.getBlock();
        BlockPos target = clicked.offset(context.getSide(), 1);

        boolean shouldPlace = false;

        if (!WHITELIST.contains(clickedBlock)) {
            playErrorSound(world, player, clicked);
            return super.useOnBlock(context);
        }

        if (useMats) {
            DefaultedList<ItemStack> main = player.getInventory().getMainStacks();
            Item item = clickedBlock.asItem();
            int minStackSize = item.getMaxCount() + 1;
            int minStackSizeSlot = -1;

            for (int i = 0; i < main.size(); i++) {
                ItemStack stack = main.get(i);

                if (stack.getItem() == item) {
                    if (stack.getCount() < minStackSize) {
                        minStackSize = stack.getCount();
                        minStackSizeSlot = i;
                    }
                }
            }

            if (minStackSizeSlot != -1) {
                shouldPlace = true;
                ItemStack stack = main.get(minStackSizeSlot);
                stack.decrement(1);
                stack.setBobbingAnimationTime(5);
            } else {
                playErrorSound(world, player, clicked);
            }
        } else shouldPlace = true;

        if (shouldPlace) {
            world.setBlockState(target, clickedState);
            world.playSound(player, clicked, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.MASTER);
        }

        return ActionResult.SUCCESS;
    }

    private static void playErrorSound(World world, PlayerEntity player, BlockPos clicked) {
        world.playSound(player, clicked, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1, 1);
    }
}
