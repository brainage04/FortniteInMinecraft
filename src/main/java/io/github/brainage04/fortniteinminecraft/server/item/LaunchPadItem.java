package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.server.player.LaunchPadImpulse;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

public final class LaunchPadItem extends SimplePolymerItem {
    private final String displayName;
    private final int cooldownTicks;
    private final long redeployTicks;
    private final Item clientItem;

    public LaunchPadItem(String displayName, int cooldownTicks, long redeployTicks, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.displayName = requireText(displayName, "displayName");
        if (cooldownTicks < 0 || redeployTicks < 0L) {
            throw new IllegalArgumentException("cooldown/redeploy ticks cannot be negative");
        }
        this.cooldownTicks = cooldownTicks;
        this.redeployTicks = redeployTicks;
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
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
        tooltip.add(Component.literal("Portable launch impulse with temporary glider redeploy."));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        Vec3 impulse = LaunchPadImpulse.defaultImpulse(serverPlayer.getLookAngle());
        serverPlayer.setDeltaMovement(impulse);
        serverPlayer.hurtMarked = true;
        serverPlayer.setOnGround(false);
        serverPlayer.resetFallDistance();
        MobilityItemInteractions.enableRedeploy(serverPlayer, redeployTicks);
        serverPlayer.getCooldowns().addCooldown(stack, cooldownTicks);
        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.85F);
        serverLevel.sendParticles(ParticleTypes.CLOUD, true, true, serverPlayer.getX(), serverPlayer.getY() + 0.15D, serverPlayer.getZ(), 16, 0.45D, 0.12D, 0.45D, 0.08D);
        serverPlayer.sendSystemMessage(Component.literal("Launched."), true);
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
