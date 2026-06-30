package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Consumer;

public final class LaunchPadItem extends Item {
    private static final BlockState PLACED_STATE = Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState();
    private final String displayName;
    private final int cooldownTicks;
    private final long redeployTicks;

    public LaunchPadItem(String displayName, int cooldownTicks, long redeployTicks, Item.Properties settings, Item clientItem) {
        super(settings);
        this.displayName = requireText(displayName, "displayName");
        if (cooldownTicks < 0 || redeployTicks < 0L) {
            throw new IllegalArgumentException("cooldown/redeploy ticks cannot be negative");
        }
        this.cooldownTicks = cooldownTicks;
        this.redeployTicks = redeployTicks;
        Objects.requireNonNull(clientItem, "clientItem");
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Places a floor launch pad that fires on contact."));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        BlockPos pos = context.getClickedPos().above();
        if (!canPlaceLaunchPad(serverLevel, pos)) {
            serverPlayer.sendSystemMessage(Component.literal("Launch Pad needs an empty floor."), true);
            return InteractionResult.FAIL;
        }

        if (!serverLevel.setBlock(pos, PLACED_STATE, Block.UPDATE_ALL)) {
            serverPlayer.sendSystemMessage(Component.literal("Launch Pad placement failed."), true);
            return InteractionResult.FAIL;
        }
        MobilityItemInteractions.registerLaunchPad(serverLevel, pos, redeployTicks);
        serverPlayer.getCooldowns().addCooldown(stack, cooldownTicks);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, serverPlayer);
        MobilityItemInteractions.activateLaunchPad(serverPlayer, redeployTicks, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canPlaceLaunchPad(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos)
                && level.getBlockState(pos).canBeReplaced()
                && PLACED_STATE.canSurvive(level, pos);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
