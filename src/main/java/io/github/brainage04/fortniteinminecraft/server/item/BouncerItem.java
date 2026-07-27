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


import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class BouncerItem extends Item {
    private static BlockState placedState(Direction surfaceNormal) {
        return DeployableFootprints.triggerState(surfaceNormal);
    }

    private final Definition definition;
    private final Item clientItem;

    public BouncerItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings,
                Component.literal("Trap / " + definition.rarity().label()),
                Component.literal("Places a floor- or wall-sized bouncer using a deployable trigger."),
                Component.literal(definition.redeployTicks() > 0L
                        ? "Launches on contact and grants " + definition.redeployTicks() + " ticks of redeploy."
                        : "Launches on contact without glider redeploy."),
                Component.literal("Source: " + definition.sourceItemId())
        ));
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
    public InteractionResult useOn(UseOnContext context) {
        Direction surfaceNormal = context.getClickedFace();
        if (surfaceNormal == Direction.DOWN) {
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
        BlockState placedState = placedState(surfaceNormal);

        List<BlockPos> footprint = bouncerFootprint(context.getClickedPos(), surfaceNormal);
        if (!canPlaceBouncer(serverLevel, footprint, placedState)) {
            serverPlayer.sendSystemMessage(Component.literal("Bouncer needs a clear supported floor- or wall-sized surface."), true);
            return InteractionResult.FAIL;
        }
        if (!DeployableFootprints.placeAll(serverLevel, footprint, placedState)) {
            serverPlayer.sendSystemMessage(Component.literal("Bouncer placement failed."), true);
            return InteractionResult.FAIL;
        }
        MobilityItemInteractions.registerLaunchPadFootprint(serverLevel, footprint, definition.redeployTicks(), placedState.getBlock());
        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, serverPlayer);
        MobilityItemInteractions.activateLaunchPad(serverPlayer, definition.redeployTicks(), true, surfaceNormal);
        return InteractionResult.SUCCESS_SERVER;
    }

    static List<BlockPos> bouncerFootprint(BlockPos clickedPos, Direction surfaceNormal) {
        return DeployableFootprints.centeredSurfaceSquare(clickedPos.relative(surfaceNormal), surfaceNormal, DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS);
    }

    private static boolean canPlaceBouncer(ServerLevel level, List<BlockPos> footprint, BlockState placedState) {
        return DeployableFootprints.canPlaceAll(level, footprint, placedState);
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
