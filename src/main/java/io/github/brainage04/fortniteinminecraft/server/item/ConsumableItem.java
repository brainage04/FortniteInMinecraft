package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Objects;
import java.util.function.Consumer;

public final class ConsumableItem extends Item {
    private static final double FORTNITE_TO_MINECRAFT_HEALTH = 0.2D;

    private final ConsumableDefinition definition;
    private final Item clientItem;

    public ConsumableItem(ConsumableDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings);
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
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Rarity: " + definition.rarity().label()));
        tooltip.accept(Component.literal("Use time: " + format(definition.castSeconds()) + "s"
                + (definition.movementLocked() ? "; movement locked" : "; mobile use")));
        if (definition.restoresEffectiveHealth()) {
            tooltip.accept(Component.literal("Effective health: +" + definition.effectiveRestore()));
        }
        if (definition.healthRestore() > 0) {
            tooltip.accept(Component.literal("Health: +" + definition.healthRestore()
                    + (definition.healthCap() > 0 ? " up to " + definition.healthCap() : "")));
        }
        if (definition.shieldRestore() > 0) {
            tooltip.accept(Component.literal("Shield: +" + definition.shieldRestore()
                    + (definition.shieldCap() > 0 ? " up to " + definition.shieldCap() : "")));
        }
        tooltip.accept(Component.literal("Source: " + definition.sourceItemId()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canBenefit(player)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(definition.displayName() + " would have no effect."), true);
            }
            return InteractionResult.FAIL;
        }
        if (definition.movementLocked() && player instanceof ServerPlayer serverPlayer
                && MobilityItemInteractions.isGliding(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal("Cannot use " + definition.displayName() + " while gliding."), true);
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
        return definition.shieldRestore() > 0 ? ItemUseAnimation.DRINK : ItemUseAnimation.EAT;
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
            applyRestores(player);
            player.sendSystemMessage(Component.literal("Used " + definition.displayName() + "."), true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    private void applyRestores(ServerPlayer player) {
        if (definition.restoresEffectiveHealth()) {
            applyEffectiveHealth(player);
            return;
        }
        if (definition.healthRestore() > 0) {
            player.setHealth(healthAfterUse(definition, player.getHealth(), player.getMaxHealth()));
        }
        if (definition.shieldRestore() > 0) {
            float shield = shieldAfterUse(definition, player.getAbsorptionAmount(), player.getMaxAbsorption());
            raiseMaxAbsorption(player, shield);
            player.setAbsorptionAmount(shield);
        }
    }

    private void applyEffectiveHealth(ServerPlayer player) {
        float amount = toMinecraftHealth(definition.effectiveRestore());
        float missingHealth = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
        float healthGain = Math.min(missingHealth, amount);
        if (healthGain > 0.0F) {
            player.setHealth(player.getHealth() + healthGain);
        }
        float shieldGain = amount - healthGain;
        if (shieldGain > 0.0F) {
            float shield = shieldAfterEffectiveUse(definition, player.getAbsorptionAmount(), player.getMaxAbsorption(), shieldGain);
            raiseMaxAbsorption(player, shield);
            player.setAbsorptionAmount(shield);
        }
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
        if (definition.restoresEffectiveHealth()) {
            return Math.min(maxHealth, currentHealth + toMinecraftHealth(definition.effectiveRestore()));
        }
        if (definition.healthRestore() <= 0) {
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
        if (definition.restoresEffectiveHealth()) {
            return shieldAfterEffectiveUse(definition, currentAbsorption, maxAbsorption, toMinecraftHealth(definition.effectiveRestore()));
        }
        if (definition.shieldRestore() <= 0) {
            return currentAbsorption;
        }
        float cap = shieldCap(definition, currentAbsorption, maxAbsorption);
        return Math.min(Math.max(currentAbsorption, cap), currentAbsorption + toMinecraftHealth(definition.shieldRestore()));
    }

    private static float shieldAfterEffectiveUse(
            ConsumableDefinition definition,
            float currentAbsorption,
            float maxAbsorption,
            float shieldGain
    ) {
        float cap = definition.shieldCap() > 0
                ? toMinecraftHealth(definition.shieldCap())
                : (maxAbsorption > 0.0F ? maxAbsorption : currentAbsorption + shieldGain);
        return Math.min(Math.max(currentAbsorption, cap), currentAbsorption + shieldGain);
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

    private static ChatFormatting rarityColor(FortniteRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
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
