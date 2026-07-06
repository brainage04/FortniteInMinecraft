package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;

import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.BuildResourceHarvest;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceHarvest;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public final class PickaxeItem extends Item {
    public static final int DEFAULT_STRUCTURE_DAMAGE = 50;
    public static final double DEFAULT_RANGE_BLOCKS = 5.0D;
    public static final int HARVEST_SWING_INTERVAL_TICKS = 15;
    public static final float DEFAULT_ENTITY_DAMAGE = 4.0F;
    static final double ENTITY_HITBOX_INFLATE_BLOCKS = 0.3D;
    private static final Map<UUID, Long> LAST_HARVEST_TICKS = new HashMap<>();

    private static BuildWorldState buildWorld;
    private static WorldBuildMaterializer materializer;

    private final int structureDamage;

    public PickaxeItem(Item.Properties settings, Item clientItem) {
        this(settings, clientItem, DEFAULT_STRUCTURE_DAMAGE);
    }

    public PickaxeItem(Item.Properties settings, Item clientItem, int structureDamage) {
        super(settings);
        Objects.requireNonNull(clientItem, "clientItem");
        if (structureDamage < 0) {
            throw new IllegalArgumentException("structureDamage cannot be negative");
        }
        this.structureDamage = structureDamage;
    }

    public static void configureHarvesting(BuildWorldState state, WorldBuildMaterializer worldMaterializer) {
        buildWorld = Objects.requireNonNull(state, "state");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");
    }

    public static void clearHarvestCooldown(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        LAST_HARVEST_TICKS.remove(player.getUUID());
    }

    public static void clearAllHarvestCooldowns() {
        LAST_HARVEST_TICKS.clear();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Harvesting Tool");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (buildWorld == null || materializer == null) {
            serverPlayer.sendSystemMessage(Component.literal("Harvesting is not configured."), true);
            return InteractionResult.PASS;
        }

        long tick = serverLevel.getGameTime();
        if (!canHarvest(serverPlayer, tick)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        HitResult hit = player.pick(DEFAULT_RANGE_BLOCKS, 0.0F, false);
        ItemStack stack = serverPlayer.getItemInHand(hand);
        LivingEntity entityTarget = pickEntityTarget(serverLevel, serverPlayer, hit);
        if (entityTarget != null) {
            return damageEntityTarget(serverLevel, serverPlayer, hand, stack, tick, entityTarget);
        }
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResult.PASS;
        }
        return damageBlockHit(serverLevel, serverPlayer, hand, stack, tick, blockHit);
    }

    InteractionResult damageBlockHit(
            ServerLevel serverLevel,
            ServerPlayer serverPlayer,
            InteractionHand hand,
            ItemStack stack,
            long tick,
            BlockHitResult blockHit
    ) {
        String dimension = serverLevel.dimension().identifier().toString();
        BlockPos hitPos = blockHit.getBlockPos();
        BuildSlot slot = materializer.topOwnerAt(dimension, hitPos);
        if (slot == null) {
            BlockPos supportPos = DeployableTriggerBlocks.supportPosFor(serverLevel.getBlockState(hitPos), hitPos);
            if (!supportPos.equals(hitPos)) {
                slot = materializer.topOwnerAt(dimension, supportPos);
            }
        }
        if (slot == null) {
            return harvestTerrainResource(serverLevel, serverPlayer, hand, stack, tick, blockHit);
        }

        BuildWeakPoints.Damage weakPointDamage = BuildWeakPoints.damageForHit(serverLevel, slot, blockHit.getLocation(), structureDamage);
        BuildResourceHarvest.HarvestResult result = BuildResourceHarvest.harvest(
                buildWorld,
                slot,
                weakPointDamage.amount(),
                tick
        );
        if (!result.hit()) {
            return InteractionResult.PASS;
        }
        recordHarvest(serverLevel, serverPlayer, hand, stack, tick, blockHit.getLocation());
        if (result.destroyed()) {
            WorldBuildWriteResult clearResult = materializer.clear(serverLevel, result.after());
            if (clearResult.success()) {
                buildWorld.remove(slot);
                BuildCollapseScheduler.scheduleAfterSupportRemoved(serverLevel, slot, tick);
                BuildWeakPoints.clear(slot);
            } else {
                FortniteInMinecraft.LOGGER.warn("Build clear failed after pickaxe damage at {}: {}", hitPos, clearResult.message());
            }
        } else {
            WorldBuildWriteResult refreshResult = materializer.refresh(serverLevel, result.after());
            if (!refreshResult.success()) {
                FortniteInMinecraft.LOGGER.warn("Build refresh failed after pickaxe damage at {}: {}", hitPos, refreshResult.message());
            }
        }
        serverPlayer.sendSystemMessage(Component.literal((result.destroyed() ? "Destroyed " : "Damaged ")
                + result.material().name().toLowerCase(Locale.ROOT) + " build piece."), true);
        PlayerResourceStateSync.send(serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult harvestTerrainResource(
            ServerLevel level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            long tick,
            BlockHitResult blockHit
    ) {
        BlockPos hitPos = blockHit.getBlockPos();
        BuildWeakPoints.Damage weakPointDamage = BuildWeakPoints.damageForTerrainHit(
                level,
                hitPos,
                blockHit.getLocation(),
                structureDamage
        );
        TerrainResourceHarvest.HarvestResult hit = TerrainResourceHarvest.hit(level, hitPos, weakPointDamage.amount());
        if (!hit.hit()) {
            return InteractionResult.PASS;
        }
        recordHarvest(level, player, hand, stack, tick, blockHit.getLocation());
        PlayerResourceState resources = PlayerResourceStates.stateFor(player);
        int accepted = resources.addMaterial(hit.material(), hit.resourceReward());
        if (hit.destroyed()) {
            BuildWeakPoints.clearTerrain(level, hitPos);
        }
        player.sendSystemMessage(Component.literal("Harvested " + accepted + " "
                + hit.material().name().toLowerCase(Locale.ROOT)
                + (hit.destroyed() ? " from terrain." : "; terrain health " + hit.remainingHealth() + ".")), true);
        PlayerResourceStateSync.send(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static LivingEntity pickEntityTarget(ServerLevel level, ServerPlayer player, HitResult blockingHit) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(DEFAULT_RANGE_BLOCKS));
        if (blockingHit.getType() != HitResult.Type.MISS) {
            end = blockingHit.getLocation();
        }
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity closest = null;
        double closestDistance = start.distanceToSqr(end);
        for (Entity entity : level.getEntities(player, searchBox, PickaxeItem::canHitEntity)) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(ENTITY_HITBOX_INFLATE_BLOCKS).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }

    private static boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
    }

    private static InteractionResult damageEntityTarget(
            ServerLevel level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            long tick,
            LivingEntity target
    ) {
        Vec3 hitLocation = target.getBoundingBox().getCenter();
        target.hurtServer(level, player.damageSources().playerAttack(player), DEFAULT_ENTITY_DAMAGE);
        recordHarvest(level, player, hand, stack, tick, hitLocation);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canHarvest(ServerPlayer player, long tick) {
        Long lastHarvestTick = LAST_HARVEST_TICKS.get(player.getUUID());
        return lastHarvestTick == null || canHarvestAt(lastHarvestTick, tick);
    }

    static boolean canHarvestAt(long lastHarvestTick, long tick) {
        return tick - lastHarvestTick >= HARVEST_SWING_INTERVAL_TICKS;
    }

    private static void recordHarvest(
            ServerLevel level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            long tick,
            Vec3 hitLocation
    ) {
        LAST_HARVEST_TICKS.put(player.getUUID(), tick);
        player.getCooldowns().addCooldown(stack, HARVEST_SWING_INTERVAL_TICKS);
        player.swing(hand, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.9F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, true, true, hitLocation.x(), hitLocation.y(), hitLocation.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
