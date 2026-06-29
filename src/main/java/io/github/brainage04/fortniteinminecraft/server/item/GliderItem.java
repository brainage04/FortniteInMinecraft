package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

public final class GliderItem extends SimplePolymerItem {
    private final String displayName;
    private final long redeployTicks;
    private final Item clientItem;

    public GliderItem(String displayName, long redeployTicks, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.displayName = requireText(displayName, "displayName");
        if (redeployTicks < 0L) {
            throw new IllegalArgumentException("redeployTicks cannot be negative");
        }
        this.redeployTicks = redeployTicks;
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public long redeployTicks() {
        return redeployTicks;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public void modifyBasePolymerItemStack(
            ItemStack out,
            ItemStack stack,
            PacketContext context,
            HolderLookup.Provider registries
    ) {
        out.set(DataComponents.ITEM_NAME, Component.literal(displayName));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Redeploys into a slow fall while airborne."));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.onGround()) {
            serverPlayer.sendSystemMessage(Component.literal("Jump or fall before deploying the glider."), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        MobilityItemInteractions.enableRedeploy(serverPlayer, redeployTicks);
        serverPlayer.resetFallDistance();
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.7F, 1.2F);
        serverPlayer.sendSystemMessage(Component.literal("Glider deployed."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
