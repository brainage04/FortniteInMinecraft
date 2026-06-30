package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.GrapplerProjectiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Consumer;

public final class GrapplerItem extends Item {
    private static final double UPWARD_LAUNCH_SCALE = 0.45D;

    private final Definition definition;

    public GrapplerItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(settings);
        this.definition = Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(clientItem, "clientItem");
    }

    public Definition definition() {
        return definition;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Left-click to fire a grappling hook."));
        tooltip.accept(Component.literal("Pulls you toward the first block in sight."));
        tooltip.accept(Component.literal("Source: " + definition.sourceItemId()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult fireFromHeldItem(ServerLevel level, ServerPlayer serverPlayer, InteractionHand hand) {
        if (level == null || serverPlayer == null) {
            return InteractionResult.PASS;
        }
        if (MobilityItemInteractions.isGliding(serverPlayer)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!GrapplerProjectiles.fire(serverPlayer, definition)) {
            return InteractionResult.FAIL;
        }
        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        return InteractionResult.SUCCESS_SERVER;
    }

    public static Vec3 pullVelocity(Vec3 from, Vec3 target, double pullSpeed, double upwardBoost) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(target, "target");
        if (pullSpeed < 0.0D || upwardBoost < 0.0D) {
            throw new IllegalArgumentException("pullSpeed/upwardBoost cannot be negative");
        }
        double effectiveUpwardBoost = upwardBoost * UPWARD_LAUNCH_SCALE;
        Vec3 delta = target.subtract(from);
        if (delta.lengthSqr() <= 1.0E-9D) {
            return new Vec3(0.0D, effectiveUpwardBoost, 0.0D);
        }
        Vec3 pull = delta.normalize().scale(pullSpeed);
        return new Vec3(pull.x(), Math.max(effectiveUpwardBoost, pull.y() + effectiveUpwardBoost), pull.z());
    }

    public record Definition(
            String path,
            String displayName,
            double rangeBlocks,
            double pullSpeed,
            double upwardBoost,
            int cooldownTicks,
            String sourceItemId
    ) {
        public Definition {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            if (rangeBlocks <= 0.0D) {
                throw new IllegalArgumentException("rangeBlocks must be positive");
            }
            if (pullSpeed < 0.0D || upwardBoost < 0.0D) {
                throw new IllegalArgumentException("pullSpeed/upwardBoost cannot be negative");
            }
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks cannot be negative");
            }
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return value;
        }
    }
}
