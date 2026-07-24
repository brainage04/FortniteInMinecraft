package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.server.item.GrapplerItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class GrapplerProjectiles {
    public static final double PROJECTILE_SPEED_BLOCKS_PER_TICK = 4.0D;
    public static final double ROPE_PARTICLE_STEP_BLOCKS = 1.0D;
    private static final double PLAYER_PULL_SPEED_MULTIPLIER = 1.25D;
    private static final Map<UUID, ActiveGrapple> ACTIVE_GRAPPLES = new HashMap<>();
    private static boolean registered;

    private GrapplerProjectiles() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        FortniteInMinecraft.platform().registerEndLevelTick(GrapplerProjectiles::tickLevel);
        FortniteInMinecraft.platform().registerPlayerDisconnect(player -> ACTIVE_GRAPPLES.remove(player.getUUID()));
        registered = true;
    }

    public static boolean fire(ServerPlayer player, GrapplerItem.Definition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-9D) {
            return false;
        }
        ACTIVE_GRAPPLES.put(player.getUUID(), new ActiveGrapple(
                player.level().dimension(),
                player.getEyePosition(),
                direction.normalize(),
                definition.rangeBlocks(),
                definition
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.65F);
        return true;
    }

    public static void clearAll() {
        ACTIVE_GRAPPLES.clear();
    }

    private static void tickLevel(ServerLevel level) {
        if (ACTIVE_GRAPPLES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, ActiveGrapple>> iterator = ACTIVE_GRAPPLES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveGrapple> entry = iterator.next();
            ActiveGrapple active = entry.getValue();
            if (!active.dimension().equals(level.dimension())) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                iterator.remove();
                continue;
            }
            TickResult result = active.tick(level, player);
            drawRope(level, player.getEyePosition(), active.tip());
            if (result == TickResult.IMPACT) {
                launchPlayer(level, player, active.tip(), active.definition());
                iterator.remove();
            } else if (result == TickResult.DONE) {
                iterator.remove();
            }
        }
    }

    private static void launchPlayer(ServerLevel level, ServerPlayer player, Vec3 target, GrapplerItem.Definition definition) {
        Vec3 velocity = GrapplerItem.pullVelocity(player.position(), target, definition.pullSpeed() * PLAYER_PULL_SPEED_MULTIPLIER, definition.upwardBoost());
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();
        MobilityItemInteractions.enableImpulseLaunch(player, 60L, true);
        level.playSound(null, target.x(), target.y(), target.z(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.5F);
        level.sendParticles(ParticleTypes.CRIT, true, true, target.x(), target.y(), target.z(), 8, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    private static void drawRope(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-6D) {
            return;
        }
        Vec3 step = delta.scale(ROPE_PARTICLE_STEP_BLOCKS / length);
        int particles = Math.max(1, (int) Math.ceil(length / ROPE_PARTICLE_STEP_BLOCKS));
        for (int i = 0; i <= particles; i++) {
            Vec3 point = start.add(step.scale(Math.min(i * ROPE_PARTICLE_STEP_BLOCKS, length)));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, point.x(), point.y(), point.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private enum TickResult {
        ACTIVE,
        IMPACT,
        DONE
    }

    private static final class ActiveGrapple {
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final Vec3 direction;
        private final double maxDistance;
        private final GrapplerItem.Definition definition;
        private Vec3 tip;
        private double distance;
        private boolean retracting;

        private ActiveGrapple(
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 origin,
                Vec3 direction,
                double maxDistance,
                GrapplerItem.Definition definition
        ) {
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.origin = Objects.requireNonNull(origin, "origin");
            this.direction = Objects.requireNonNull(direction, "direction");
            this.maxDistance = maxDistance;
            this.definition = Objects.requireNonNull(definition, "definition");
            this.tip = origin;
        }

        private TickResult tick(ServerLevel level, ServerPlayer player) {
            if (retracting) {
                distance = Math.max(0.0D, distance - PROJECTILE_SPEED_BLOCKS_PER_TICK);
                tip = origin.add(direction.scale(distance));
                return distance <= 0.0D ? TickResult.DONE : TickResult.ACTIVE;
            }

            Vec3 previous = tip;
            distance = Math.min(maxDistance, distance + PROJECTILE_SPEED_BLOCKS_PER_TICK);
            Vec3 next = origin.add(direction.scale(distance));
            HitResult hit = level.clip(new ClipContext(previous, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                tip = blockHit.getLocation();
                return TickResult.IMPACT;
            }
            tip = next;
            if (distance >= maxDistance) {
                retracting = true;
            }
            return TickResult.ACTIVE;
        }

        private net.minecraft.resources.ResourceKey<Level> dimension() {
            return dimension;
        }

        private Vec3 tip() {
            return tip;
        }

        private GrapplerItem.Definition definition() {
            return definition;
        }
    }
}
