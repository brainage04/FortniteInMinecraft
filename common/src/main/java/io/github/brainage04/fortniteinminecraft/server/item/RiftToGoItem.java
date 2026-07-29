package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;


public final class RiftToGoItem extends Item {
    static final long DEFAULT_RIFT_PORTAL_ACTIVE_DURATION_TICKS = 200L; // Default.Rift.Item.ActiveDuration = 10s.

    private final Definition definition;
    private final Item clientItem;

    public RiftToGoItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings,
                Component.literal("Mobility / " + definition.rarity().label()),
                Component.literal("Teleports upward by " + format(definition.verticalTeleportBlocks()) + " blocks."),
                Component.literal("Launches into redeploy for " + definition.redeployTicks() + " ticks."),
                Component.literal("Leaves a 10s rift portal each player can enter once."),
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        Vec3 origin = serverPlayer.position();
        MobilityItemInteractions.openRiftPortal(
                serverLevel,
                origin,
                DEFAULT_RIFT_PORTAL_ACTIVE_DURATION_TICKS,
                definition.redeployTicks(),
                definition.verticalTeleportBlocks(),
                definition.horizontalLaunchSpeed(),
                definition.verticalLaunchSpeed()
        );
        MobilityItemInteractions.teleportThroughRift(
                serverPlayer,
                definition.redeployTicks(),
                definition.verticalTeleportBlocks(),
                definition.horizontalLaunchSpeed(),
                definition.verticalLaunchSpeed()
        );
        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }

    public record Definition(
            String path,
            String displayName,
            FortniteRarity rarity,
            String sourceItemId,
            int cooldownTicks,
            long redeployTicks,
            double verticalTeleportBlocks,
            double horizontalLaunchSpeed,
            double verticalLaunchSpeed
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
            if (verticalTeleportBlocks <= 0.0D) {
                throw new IllegalArgumentException("verticalTeleportBlocks must be positive");
            }
            if (horizontalLaunchSpeed < 0.0D || verticalLaunchSpeed < 0.0D) {
                throw new IllegalArgumentException("launch speed cannot be negative");
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

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-4D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
