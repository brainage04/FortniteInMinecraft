package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GrapplerItem extends SimplePolymerItem {
    static final ClipContext.Block GRAPPLE_BLOCK_MODE = ClipContext.Block.COLLIDER;

    private final Definition definition;
    private final Item clientItem;

    public GrapplerItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public Definition definition() {
        return definition;
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
        out.set(DataComponents.ITEM_NAME, Component.literal(definition.displayName()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName());
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Pulls you toward the first block or player in sight."));
        tooltip.add(Component.literal("Source: " + definition.sourceItemId()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (MobilityItemInteractions.isGliding(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal("Cannot grapple while gliding."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        Optional<Vec3> target = grappleTarget(serverLevel, serverPlayer, definition.rangeBlocks());
        if (target.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.literal("No grapple target."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        Vec3 velocity = pullVelocity(serverPlayer.position(), target.get(), definition.pullSpeed(), definition.upwardBoost());
        serverPlayer.setDeltaMovement(velocity);
        serverPlayer.hurtMarked = true;
        serverPlayer.resetFallDistance();
        serverPlayer.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        playGrappleEffects(serverLevel, serverPlayer, target.get());
        serverPlayer.sendSystemMessage(Component.literal("Grappled."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    static Optional<Vec3> grappleTarget(ServerLevel level, ServerPlayer player, double rangeBlocks) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(rangeBlocks));
        HitResult blockHit = level.clip(new ClipContext(start, end, GRAPPLE_BLOCK_MODE, ClipContext.Fluid.NONE, player));
        double closestDistance = start.distanceToSqr(end);
        Vec3 closest = null;
        if (blockHit.getType() != HitResult.Type.MISS && blockHit instanceof BlockHitResult hit) {
            closest = hit.getLocation();
            closestDistance = start.distanceToSqr(closest);
            end = closest;
        }

        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        for (Entity entity : level.getEntities(player, searchBox, GrapplerItem::canGrapple)) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = hit.get();
            }
        }
        return Optional.ofNullable(closest);
    }

    static Vec3 pullVelocity(Vec3 from, Vec3 target, double pullSpeed, double upwardBoost) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(target, "target");
        if (pullSpeed < 0.0D || upwardBoost < 0.0D) {
            throw new IllegalArgumentException("pullSpeed/upwardBoost cannot be negative");
        }
        Vec3 delta = target.subtract(from);
        if (delta.lengthSqr() <= 1.0E-9D) {
            return new Vec3(0.0D, upwardBoost, 0.0D);
        }
        Vec3 pull = delta.normalize().scale(pullSpeed);
        return new Vec3(pull.x(), Math.max(upwardBoost, pull.y() + upwardBoost), pull.z());
    }

    private static boolean canGrapple(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
    }

    private static void playGrappleEffects(ServerLevel level, ServerPlayer player, Vec3 target) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.55F);
        level.playSound(null, target.x(), target.y(), target.z(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.5F);
        level.sendParticles(ParticleTypes.CRIT, true, true, target.x(), target.y(), target.z(), 8, 0.18D, 0.18D, 0.18D, 0.02D);
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
