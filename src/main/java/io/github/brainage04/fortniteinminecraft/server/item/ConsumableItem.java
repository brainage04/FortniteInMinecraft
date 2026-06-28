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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    Item clientItem() {
        return clientItem;
    }

    public ConsumableDefinition definition() {
        return definition;
    }

    static int useTicks(ConsumableDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return Math.max(1, (int) Math.ceil(definition.castSeconds() * 20.0D));
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
        if (!canBenefit(player)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(definition.displayName() + " would have no effect."), true);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useTicks(definition);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return definition.restoresShield() ? ItemUseAnimation.DRINK : ItemUseAnimation.EAT;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal(progressText(definition, remainingUseDuration)), true);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            if (!canBenefit(player)) {
                player.sendSystemMessage(Component.literal(definition.displayName() + " would have no effect."), true);
                return stack;
            }
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
        player.setHealth(healthAfterUse(definition, player.getHealth(), player.getMaxHealth()));
    }

    private void applyShield(ServerPlayer player) {
        if (!definition.restoresShield()) {
            return;
        }
        float shield = shieldAfterUse(definition, player.getAbsorptionAmount(), player.getMaxAbsorption());
        raiseMaxAbsorption(player, shield);
        player.setAbsorptionAmount(shield);
    }


    private boolean canBenefit(Player player) {
        return canBenefit(
                definition,
                player.getHealth(),
                player.getMaxHealth(),
                player.getAbsorptionAmount(),
                player.getMaxAbsorption()
        );
    }

    static boolean canBenefit(
            ConsumableDefinition definition,
            float currentHealth,
            float maxHealth,
            float currentAbsorption,
            float maxAbsorption
    ) {
        Objects.requireNonNull(definition, "definition");
        return healthAfterUse(definition, currentHealth, maxHealth) > currentHealth + 1.0E-4F
                || shieldAfterUse(definition, currentAbsorption, maxAbsorption) > currentAbsorption + 1.0E-4F;
    }

    static float healthAfterUse(ConsumableDefinition definition, float currentHealth, float maxHealth) {
        Objects.requireNonNull(definition, "definition");
        if (!definition.restoresHealth()) {
            return currentHealth;
        }
        float amount = toMinecraftHealth(definition.healthRestore());
        float cap = definition.healthCap() > 0
                ? Math.min(maxHealth, toMinecraftHealth(definition.healthCap()))
                : maxHealth;
        return Math.min(cap, currentHealth + amount);
    }
    static float shieldAfterUse(ConsumableDefinition definition, float currentAbsorption, float maxAbsorption) {
        Objects.requireNonNull(definition, "definition");
        if (!definition.restoresShield()) {
            return currentAbsorption;
        }
        float cap = shieldCap(definition, currentAbsorption, maxAbsorption);
        return Math.min(Math.max(currentAbsorption, cap), currentAbsorption + toMinecraftHealth(definition.shieldRestore()));
    }

    static float shieldCap(ConsumableDefinition definition, float currentAbsorption, float maxAbsorption) {
        Objects.requireNonNull(definition, "definition");
        if (definition.shieldCap() > 0) {
            return toMinecraftHealth(definition.shieldCap());
        }
        float restoredAbsorption = currentAbsorption + toMinecraftHealth(definition.shieldRestore());
        return maxAbsorption > 0.0F ? maxAbsorption : restoredAbsorption;
    }

    static String progressText(ConsumableDefinition definition, int remainingUseTicks) {
        Objects.requireNonNull(definition, "definition");
        int totalTicks = useTicks(definition);
        int clampedRemainingTicks = Math.clamp(remainingUseTicks, 0, totalTicks);
        int usedTicks = totalTicks - clampedRemainingTicks;
        return formatTicksFixed(usedTicks) + "/" + formatTicksFixed(totalTicks) + "s";
    }

    private static void raiseMaxAbsorption(ServerPlayer player, float cap) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (attribute != null && attribute.getValue() < cap) {
            attribute.setBaseValue(cap);
        }
    }

    private static String formatTicksFixed(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.2f", ticks / 20.0D);
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
