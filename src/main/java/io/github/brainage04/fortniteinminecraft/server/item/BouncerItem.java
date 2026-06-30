package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class BouncerItem extends Item {
    private static final BlockState PLACED_STATE = Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState();

    private final Definition definition;
    private final Item clientItem;

    public BouncerItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(settings);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public Definition definition() {
        return definition;
    }

    Item clientItem() {
        return clientItem;
    }

    static UseCooldown cooldownComponent(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(definition.cooldownTicks() / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Trap / " + definition.rarity().label()));
        tooltip.accept(Component.literal("Places a floor bouncer using the launch-pad trigger."));
        if (definition.redeployTicks() > 0L) {
            tooltip.accept(Component.literal("Launches on contact and grants " + definition.redeployTicks() + " ticks of redeploy."));
        } else {
            tooltip.accept(Component.literal("Launches on contact without glider redeploy."));
        }
        tooltip.accept(Component.literal("Source: " + definition.sourceItemId()));
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
        if (!canPlaceBouncer(serverLevel, pos)) {
            serverPlayer.sendSystemMessage(Component.literal("Bouncer needs an empty floor."), true);
            return InteractionResult.FAIL;
        }
        if (!serverLevel.setBlock(pos, PLACED_STATE, Block.UPDATE_ALL)) {
            serverPlayer.sendSystemMessage(Component.literal("Bouncer placement failed."), true);
            return InteractionResult.FAIL;
        }
        MobilityItemInteractions.registerLaunchPad(serverLevel, pos, definition.redeployTicks());
        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, serverPlayer);
        MobilityItemInteractions.activateLaunchPad(serverPlayer, definition.redeployTicks(), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canPlaceBouncer(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos)
                && level.getBlockState(pos).canBeReplaced()
                && PLACED_STATE.canSurvive(level, pos);
    }

    public record Definition(
            String path,
            String displayName,
            FortniteRarity rarity,
            String sourceItemId,
            int cooldownTicks,
            long redeployTicks
    ) {
        public Definition {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(rarity, "rarity");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks cannot be negative");
            }
            if (redeployTicks < 0L) {
                throw new IllegalArgumentException("redeployTicks cannot be negative");
            }
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    private static ChatFormatting rarityColor(FortniteRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }
}
