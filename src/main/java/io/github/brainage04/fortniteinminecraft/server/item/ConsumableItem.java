package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

public final class ConsumableItem extends SimplePolymerItem {
    private static final double FORTNITE_TO_MINECRAFT_HEALTH = 0.2D;

    private final ConsumableDefinition definition;
    private final Item clientItem;

    public ConsumableItem(ConsumableDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public ConsumableDefinition definition() {
        return definition;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName());
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Use time: " + format(definition.castSeconds()) + "s"
                + (definition.movementLocked() ? "; movement locked" : "; mobile use")));
        if (definition.restoresHealth()) {
            tooltip.add(Component.literal("Health: +" + definition.healthRestore()
                    + (definition.healthCap() > 0 ? " up to " + definition.healthCap() : "")));
        }
        if (definition.restoresShield()) {
            tooltip.add(Component.literal("Shield: +" + definition.shieldRestore()
                    + (definition.shieldCap() > 0 ? " up to " + definition.shieldCap() : "")));
        }
        tooltip.add(Component.literal("Source: " + definition.sourceItemId()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Math.max(1, (int) Math.ceil(definition.castSeconds() * 20.0D));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return definition.restoresShield() ? ItemUseAnimation.DRINK : ItemUseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            applyHealth(player);
            applyShield(player);
            player.sendSystemMessage(Component.literal("Used " + definition.displayName() + "."), true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    private void applyHealth(ServerPlayer player) {
        if (!definition.restoresHealth()) {
            return;
        }
        float amount = toMinecraftHealth(definition.healthRestore());
        float cap = definition.healthCap() > 0
                ? Math.min(player.getMaxHealth(), toMinecraftHealth(definition.healthCap()))
                : player.getMaxHealth();
        player.setHealth(Math.min(cap, player.getHealth() + amount));
    }

    private void applyShield(ServerPlayer player) {
        if (!definition.restoresShield()) {
            return;
        }
        float amount = toMinecraftHealth(definition.shieldRestore());
        float cap = definition.shieldCap() > 0 ? toMinecraftHealth(definition.shieldCap()) : player.getMaxAbsorption();
        player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + amount));
    }

    private static float toMinecraftHealth(int fortniteHealth) {
        return (float) (fortniteHealth * FORTNITE_TO_MINECRAFT_HEALTH);
    }

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
