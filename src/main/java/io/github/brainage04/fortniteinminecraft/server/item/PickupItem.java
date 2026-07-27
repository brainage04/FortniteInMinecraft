package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;


import java.util.Objects;


public final class PickupItem extends Item {
    private final String displayName;
    private final PickupPayload payload;
    private final Item clientItem;

    public PickupItem(String displayName, PickupPayload payload, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings, Component.literal("Pickup: " + payload.label())));
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public PickupPayload payload() {
        return payload;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
    }

    

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        PlayerResourceState resources = PlayerResourceStates.stateFor(serverPlayer);
        PickupPayload.PickupResult result = payload.applyTo(resources);
        if (!result.granted()) {
            serverPlayer.sendSystemMessage(Component.literal(result.label() + " is full."), true);
            return InteractionResult.FAIL;
        }
        serverPlayer.sendSystemMessage(Component.literal("Picked up " + result.accepted() + " " + result.label() + "."), true);
        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }
        PlayerResourceStateSync.send(serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }
}
