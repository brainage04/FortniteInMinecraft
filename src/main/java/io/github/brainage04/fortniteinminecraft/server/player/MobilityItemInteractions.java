package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.server.item.DeployableTriggerBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.TrapTriggerBlock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class MobilityItemInteractions {
    public static final long DEFAULT_IMPULSE_LAUNCH_TICKS = 100L;
    private static final int GLIDER_EFFECT_DURATION_TICKS = 8;
    private static final int GLIDER_EFFECT_REFRESH_THRESHOLD_TICKS = 3;
    private static final double GLIDER_LATERAL_FRICTION_PER_SECOND = 0.25D;
    private static final long GLIDER_INDICATOR_INTERVAL_TICKS = 4L;
    private static final double GLIDER_MAX_HORIZONTAL_SPEED_BLOCKS_PER_TICK = 0.9D;
    private static final double GLIDER_HORIZONTAL_ACCELERATION_BLOCKS_PER_TICK = 0.0512D;
    private static final long LAUNCH_PAD_RETRIGGER_TICKS = 8L;
    private static final double SLIDE_MIN_START_SPEED_BLOCKS_PER_TICK = 0.13D;
    private static final double SLIDE_MIN_CONTINUE_SPEED_BLOCKS_PER_TICK = 0.06D;
    private static final double SLIDE_FLAT_ACCELERATION_BLOCKS_PER_TICK = 0.025D;
    private static final double SLIDE_DOWNHILL_ACCELERATION_BLOCKS_PER_TICK = 0.07D;
    private static final double SLIDE_HORIZONTAL_FRICTION_PER_TICK = 0.94D;
    private static final double SLIDE_MAX_HORIZONTAL_SPEED_BLOCKS_PER_TICK = 0.72D;
    private static final long SLIDE_MAX_DURATION_TICKS = 45L;
    private static final long SLIDE_PARTICLE_INTERVAL_TICKS = 4L;
    private static final double MIN_PRESERVED_HORIZONTAL_SPEED = 1.0E-5D;
    private static final double LAUNCH_AIR_DRAG_DIRECTION_DOT_MIN = 0.99D;
    private static final double LAUNCH_PAD_FOOT_EPSILON = 0.05D;
    private static final double WALL_BOUNCER_HORIZONTAL_VELOCITY_BLOCKS_PER_TICK = 1.35D;
    private static final double WALL_BOUNCER_UPWARD_VELOCITY_BLOCKS_PER_TICK = 0.55D;
    private static final double RIFT_PORTAL_TRIGGER_RADIUS_BLOCKS = 1.25D;
    private static final double RIFT_PORTAL_TRIGGER_HEIGHT_BLOCKS = 2.5D;
    private static final long RIFT_PORTAL_PARTICLE_INTERVAL_TICKS = 5L;
    private static final Map<UUID, GliderState> GLIDERS = new HashMap<>();
    private static final Map<UUID, SlideState> SLIDES = new HashMap<>();
    private static final Map<UUID, ImpulseLaunchState> IMPULSE_LAUNCHES = new HashMap<>();
    private static final Map<LaunchPadKey, TrackedLaunchPad> LAUNCH_PADS = new HashMap<>();
    private static final Map<UUID, Long> LAST_LAUNCH_PAD_ACTIVATIONS = new HashMap<>();
    private static final List<ActiveRiftPortal> RIFT_PORTALS = new ArrayList<>();
    private static boolean registered;

    private MobilityItemInteractions() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(MobilityItemInteractions::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GLIDERS.remove(handler.player.getUUID());
            IMPULSE_LAUNCHES.remove(handler.player.getUUID());
            SLIDES.remove(handler.player.getUUID());
            LAST_LAUNCH_PAD_ACTIVATIONS.remove(handler.player.getUUID());
        });
        registered = true;
    }

    public static void enableRedeploy(ServerPlayer player, long durationTicks) {
        Objects.requireNonNull(player, "player");
        GLIDERS.computeIfAbsent(player.getUUID(), uuid -> new GliderState())
                .enableRedeploy(player.level().getGameTime(), durationTicks);
    }

    public static void enableImpulseLaunch(ServerPlayer player, boolean cancelFallDamage) {
        enableImpulseLaunch(player, DEFAULT_IMPULSE_LAUNCH_TICKS, cancelFallDamage);
    }

    public static void enableImpulseLaunch(ServerPlayer player, long durationTicks, boolean cancelFallDamage) {
        enableLaunchAirFrictionSuppression(player, durationTicks, cancelFallDamage);
    }

    public static void enableLaunchAirFrictionSuppression(ServerPlayer player, boolean cancelFallDamage) {
        enableLaunchAirFrictionSuppression(player, DEFAULT_IMPULSE_LAUNCH_TICKS, cancelFallDamage);
    }

    public static void enableLaunchAirFrictionSuppression(ServerPlayer player, long durationTicks, boolean cancelFallDamage) {
        Objects.requireNonNull(player, "player");
        if (durationTicks <= 0L) {
            return;
        }
        if (cancelFallDamage) {
            player.resetFallDistance();
        }
        long expiresAtTick = player.level().getGameTime() + durationTicks;
        IMPULSE_LAUNCHES.put(player.getUUID(), new ImpulseLaunchState(cancelFallDamage, player.getDeltaMovement(), expiresAtTick));
    }

    public static void registerLaunchPad(ServerLevel level, BlockPos pos, long redeployTicks) {
        registerLaunchPad(level, pos, redeployTicks, DeployableTriggerBlocks.TRAP_TRIGGER);
    }

    public static void registerLaunchPad(ServerLevel level, BlockPos pos, long redeployTicks, Block triggerBlock) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(triggerBlock, "triggerBlock");
        LAUNCH_PADS.put(new LaunchPadKey(level.dimension(), pos), new TrackedLaunchPad(redeployTicks, triggerBlock));
    }

    public static void registerLaunchPadFootprint(ServerLevel level, Iterable<BlockPos> positions, long redeployTicks, Block triggerBlock) {
        Objects.requireNonNull(positions, "positions");
        for (BlockPos pos : positions) {
            registerLaunchPad(level, pos, redeployTicks, triggerBlock);
        }
    }

    public static boolean activateLaunchPad(ServerPlayer player, long redeployTicks) {
        return activateLaunchPad(player, redeployTicks, false);
    }

    public static boolean activateLaunchPad(ServerPlayer player, long redeployTicks, boolean force) {
        return activateLaunchPad(player, redeployTicks, force, Direction.UP);
    }

    public static boolean activateLaunchPad(ServerPlayer player, long redeployTicks, boolean force, Direction surfaceNormal) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(surfaceNormal, "surfaceNormal");
        if (redeployTicks < 0L) {
            throw new IllegalArgumentException("redeployTicks cannot be negative");
        }
        long tick = player.level().getGameTime();
        UUID playerId = player.getUUID();
        Long previousTick = LAST_LAUNCH_PAD_ACTIVATIONS.get(playerId);
        if (!force && previousTick != null && tick - previousTick < LAUNCH_PAD_RETRIGGER_TICKS) {
            return false;
        }

        Vec3 impulse = launchImpulse(player.getLookAngle(), surfaceNormal);
        player.setDeltaMovement(impulse);
        player.hurtMarked = true;
        player.setOnGround(false);
        player.resetFallDistance();
        enableLaunchAirFrictionSuppression(player, true);
        enableRedeploy(player, redeployTicks);
        LAST_LAUNCH_PAD_ACTIVATIONS.put(playerId, tick);

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.85F);
        level.sendParticles(ParticleTypes.CLOUD, true, true, player.getX(), player.getY() + 0.15D, player.getZ(), 16, 0.45D, 0.12D, 0.45D, 0.08D);
        return true;
    }

    public static void openRiftPortal(
            ServerLevel level,
            Vec3 origin,
            long durationTicks,
            long redeployTicks,
            double verticalTeleportBlocks,
            double horizontalLaunchSpeed,
            double verticalLaunchSpeed
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");
        if (durationTicks < 0L) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
        if (durationTicks == 0L) {
            return;
        }
        RIFT_PORTALS.add(new ActiveRiftPortal(
                level.dimension(),
                origin,
                riftPortalExpireTick(level.getGameTime(), durationTicks),
                redeployTicks,
                verticalTeleportBlocks,
                horizontalLaunchSpeed,
                verticalLaunchSpeed
        ));
        emitRiftPortalParticles(level, origin, 48);
    }

    public static void teleportThroughRift(
            ServerPlayer player,
            long redeployTicks,
            double verticalTeleportBlocks,
            double horizontalLaunchSpeed,
            double verticalLaunchSpeed
    ) {
        Objects.requireNonNull(player, "player");
        if (verticalTeleportBlocks <= 0.0D) {
            throw new IllegalArgumentException("verticalTeleportBlocks must be positive");
        }
        if (horizontalLaunchSpeed < 0.0D || verticalLaunchSpeed < 0.0D) {
            throw new IllegalArgumentException("launch speed cannot be negative");
        }

        ServerLevel level = player.level();
        Vec3 before = player.position();
        emitRiftTravelParticles(level, before, 48);
        level.playSound(null, before.x(), before.y(), before.z(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.15F);

        player.teleportTo(player.getX(), player.getY() + verticalTeleportBlocks, player.getZ());
        Vec3 horizontal = horizontalRiftLaunch(player.getLookAngle(), horizontalLaunchSpeed);
        player.setDeltaMovement(horizontal.x(), verticalLaunchSpeed, horizontal.z());
        player.hurtMarked = true;
        player.resetFallDistance();
        enableRedeploy(player, redeployTicks);
        enableLaunchAirFrictionSuppression(player, true);

        Vec3 after = player.position();
        emitRiftTravelParticles(level, after, 72);
        level.playSound(null, after.x(), after.y(), after.z(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.85F);
    }

    public static boolean toggleGlider(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        GliderState state = GLIDERS.computeIfAbsent(player.getUUID(), uuid -> new GliderState());
        if (player.onGround()) {
            state.land();
            GLIDERS.remove(player.getUUID());
            return false;
        }
        boolean changed = state.toggle(player.level().getGameTime(), groundDistanceBlocks(player));
        if (changed && state.isDeployed()) {
            player.resetFallDistance();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        return changed;
    }

    public static boolean isGliding(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        GliderState state = GLIDERS.get(player.getUUID());
        return state != null && !player.onGround() && state.isDeployed();
    }

    public static void clearAll() {
        GLIDERS.clear();
        IMPULSE_LAUNCHES.clear();
        LAUNCH_PADS.clear();
        LAST_LAUNCH_PAD_ACTIVATIONS.clear();
        SLIDES.clear();
        RIFT_PORTALS.clear();
    }

    private static void tickLevel(ServerLevel level) {
        tickSlides(level);
        tickLaunchPads(level);
        tickRiftPortals(level);
        tickGliders(level);
        tickImpulseLaunches(level);
    }

    private static void tickSlides(ServerLevel level) {
        long tick = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            SlideState state = SLIDES.get(playerId);
            if (player.isSpectator() || !player.isAlive()) {
                SLIDES.remove(playerId);
                continue;
            }

            Vec3 velocity = player.getDeltaMovement();
            boolean wantsSlide = player.onGround() && player.isSprinting() && player.isShiftKeyDown();
            double horizontalSpeed = horizontalSpeed(velocity);
            if (state == null) {
                if (wantsSlide && horizontalSpeed >= SLIDE_MIN_START_SPEED_BLOCKS_PER_TICK) {
                    SLIDES.put(playerId, new SlideState(tick, player.getY()));
                }
                continue;
            }

            if (!wantsSlide
                    || tick - state.startedTick() > SLIDE_MAX_DURATION_TICKS
                    || horizontalSpeed < SLIDE_MIN_CONTINUE_SPEED_BLOCKS_PER_TICK) {
                SLIDES.remove(playerId);
                continue;
            }

            double verticalDelta = player.getY() - state.lastY();
            Vec3 nextVelocity = slideVelocity(velocity, player.getLookAngle(), verticalDelta);
            player.setDeltaMovement(nextVelocity.x(), velocity.y(), nextVelocity.z());
            player.hurtMarked = true;
            state.setLastY(player.getY());
            showSlideTrail(player);
        }
    }

    private static void tickLaunchPads(ServerLevel level) {
        if (LAUNCH_PADS.isEmpty()) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        Iterator<Map.Entry<LaunchPadKey, TrackedLaunchPad>> iterator = LAUNCH_PADS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LaunchPadKey, TrackedLaunchPad> entry = iterator.next();
            LaunchPadKey key = entry.getKey();
            if (!key.dimension().equals(dimension)) {
                continue;
            }
            if (!entry.getValue().isPresentAt(level, key.pos())) {
                iterator.remove();
            }
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            for (BlockPos pos : launchPadPositionsTouching(player)) {
                if (tryActivateLaunchPadAt(level, dimension, player, pos)) {
                    break;
                }
            }
        }
    }

    private static void tickGliders(ServerLevel level) {
        long tick = level.getGameTime();
        Iterator<Map.Entry<UUID, GliderState>> iterator = GLIDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GliderState> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.level() != level) {
                continue;
            }
            GliderState state = entry.getValue();
            if (player.onGround()) {
                state.land();
                iterator.remove();
                continue;
            }
            if (state.isDeployed()) {
                applyGlide(player);
            } else if (!state.canRedeploy(tick)) {
                iterator.remove();
            }
        }
    }

    private static void tickRiftPortals(ServerLevel level) {
        if (RIFT_PORTALS.isEmpty()) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        long tick = level.getGameTime();
        Iterator<ActiveRiftPortal> iterator = RIFT_PORTALS.iterator();
        while (iterator.hasNext()) {
            ActiveRiftPortal portal = iterator.next();
            if (!portal.dimension().equals(dimension)) {
                continue;
            }
            if (!portal.isActive(tick)) {
                iterator.remove();
                continue;
            }

            showRiftPortal(level, portal);
            AABB triggerBox = riftPortalTriggerBox(portal.origin());
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || !player.isAlive()) {
                    continue;
                }
                if (!player.getBoundingBox().intersects(triggerBox) || !portal.tryUse(player.getUUID())) {
                    continue;
                }
                teleportThroughRift(
                        player,
                        portal.redeployTicks(),
                        portal.verticalTeleportBlocks(),
                        portal.horizontalLaunchSpeed(),
                        portal.verticalLaunchSpeed()
                );
            }
        }
    }

    private static void tickImpulseLaunches(ServerLevel level) {
        long tick = level.getGameTime();
        Iterator<Map.Entry<UUID, ImpulseLaunchState>> iterator = IMPULSE_LAUNCHES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ImpulseLaunchState> entry = iterator.next();
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer player)) {
                iterator.remove();
                continue;
            }
            ImpulseLaunchState state = entry.getValue();
            if (!launchAirDragSuppressionActive(tick, state.expiresAtTick()) || player.onGround()) {
                iterator.remove();
                continue;
            }
            if (state.cancelFallDamage()) {
                player.resetFallDistance();
            }
            preserveLaunchedHorizontalVelocity(player, state, tick);
        }
    }

    private static void applyGlide(ServerPlayer player) {
        Vec3 velocity = glideVelocity(player.getDeltaMovement(), player.getLookAngle());
        player.setDeltaMovement(velocity.x(), velocity.y(), velocity.z());
        player.hurtMarked = true;
        refreshSlowFalling(player);
        showGliderIndicator(player);
        player.resetFallDistance();
    }

    static Vec3 glideVelocity(Vec3 velocity, Vec3 look) {
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(look, "look");
        double y = GliderState.fallSpeed(velocity.y(), GliderState.DEFAULT_MAX_FALL_SPEED);
        Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
        Vec3 horizontal = new Vec3(velocity.x(), 0.0D, velocity.z()).scale(1.0D - GLIDER_LATERAL_FRICTION_PER_SECOND / 20.0D);
        if (horizontalLook.lengthSqr() > 1.0E-9D) {
            horizontal = horizontal.add(horizontalLook.normalize().scale(GLIDER_HORIZONTAL_ACCELERATION_BLOCKS_PER_TICK));
        }
        if (horizontal.length() > GLIDER_MAX_HORIZONTAL_SPEED_BLOCKS_PER_TICK) {
            horizontal = horizontal.normalize().scale(GLIDER_MAX_HORIZONTAL_SPEED_BLOCKS_PER_TICK);
        }
        return new Vec3(horizontal.x(), y, horizontal.z());
    }

    static Vec3 slideVelocity(Vec3 velocity, Vec3 look, double verticalDelta) {
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(look, "look");
        Vec3 horizontal = new Vec3(velocity.x(), 0.0D, velocity.z());
        Vec3 direction = horizontal.lengthSqr() > 1.0E-9D ? horizontal.normalize() : horizontalLook(look);
        if (direction.lengthSqr() <= 1.0E-9D) {
            return velocity;
        }

        double downhill = Math.clamp(-verticalDelta, 0.0D, 0.6D);
        double uphill = Math.clamp(verticalDelta, 0.0D, 0.6D);
        double acceleration = SLIDE_FLAT_ACCELERATION_BLOCKS_PER_TICK
                + downhill * SLIDE_DOWNHILL_ACCELERATION_BLOCKS_PER_TICK
                - uphill * SLIDE_FLAT_ACCELERATION_BLOCKS_PER_TICK;
        Vec3 nextHorizontal = horizontal.scale(SLIDE_HORIZONTAL_FRICTION_PER_TICK).add(direction.scale(acceleration));
        double speedCap = SLIDE_MAX_HORIZONTAL_SPEED_BLOCKS_PER_TICK
                + downhill * SLIDE_DOWNHILL_ACCELERATION_BLOCKS_PER_TICK;
        if (nextHorizontal.length() > speedCap) {
            nextHorizontal = nextHorizontal.normalize().scale(speedCap);
        }
        return new Vec3(nextHorizontal.x(), velocity.y(), nextHorizontal.z());
    }

    private static Vec3 horizontalLook(Vec3 look) {
        Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
        return horizontalLook.lengthSqr() <= 1.0E-9D ? Vec3.ZERO : horizontalLook.normalize();
    }


    private static void refreshSlowFalling(ServerPlayer player) {
        MobEffectInstance existing = player.getEffect(MobEffects.SLOW_FALLING);
        if (existing == null || existing.getDuration() <= GLIDER_EFFECT_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, GLIDER_EFFECT_DURATION_TICKS, 0, false, false, false));
        }
    }

    private static void showGliderIndicator(ServerPlayer player) {
        if (player.level().getGameTime() % GLIDER_INDICATOR_INTERVAL_TICKS != 0L) {
            return;
        }
        player.level().sendParticles(ParticleTypes.END_ROD, true, true,
                player.getX(), player.getY() + player.getBbHeight() + 0.35D, player.getZ(),
                3, 0.55D, 0.06D, 0.55D, 0.01D);
    }

    private static void showSlideTrail(ServerPlayer player) {
        if (player.level().getGameTime() % SLIDE_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }
        player.level().sendParticles(ParticleTypes.CLOUD, true, true,
                player.getX(), player.getY() + 0.08D, player.getZ(),
                4, 0.35D, 0.03D, 0.35D, 0.01D);
    }

    private static void showRiftPortal(ServerLevel level, ActiveRiftPortal portal) {
        if (level.getGameTime() % RIFT_PORTAL_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }
        emitRiftPortalParticles(level, portal.origin(), 24);
    }

    private static void emitRiftPortalParticles(ServerLevel level, Vec3 origin, int count) {
        level.sendParticles(ParticleTypes.PORTAL, true, true,
                origin.x(), origin.y() + 1.0D, origin.z(),
                count, 0.6D, 1.0D, 0.6D, 0.08D);
    }

    private static void emitRiftTravelParticles(ServerLevel level, Vec3 origin, int count) {
        level.sendParticles(ParticleTypes.PORTAL, true, true,
                origin.x(), origin.y() + 1.0D, origin.z(),
                count, 0.7D, 1.1D, 0.7D, 0.18D);
    }

    private static AABB riftPortalTriggerBox(Vec3 origin) {
        return new AABB(
                origin.x() - RIFT_PORTAL_TRIGGER_RADIUS_BLOCKS,
                origin.y() - 0.25D,
                origin.z() - RIFT_PORTAL_TRIGGER_RADIUS_BLOCKS,
                origin.x() + RIFT_PORTAL_TRIGGER_RADIUS_BLOCKS,
                origin.y() + RIFT_PORTAL_TRIGGER_HEIGHT_BLOCKS,
                origin.z() + RIFT_PORTAL_TRIGGER_RADIUS_BLOCKS
        );
    }

    private static Vec3 horizontalRiftLaunch(Vec3 look, double horizontalLaunchSpeed) {
        Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
        return horizontalLook.lengthSqr() <= 1.0E-8D
                ? Vec3.ZERO
                : horizontalLook.normalize().scale(horizontalLaunchSpeed);
    }

    static long riftPortalExpireTick(long currentTick, long durationTicks) {
        if (durationTicks < 0L) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
        return currentTick + durationTicks;
    }

    static boolean riftPortalActive(long tick, long expireTick) {
        return tick < expireTick;
    }

    static double groundDistanceBlocks(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Vec3 start = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
        Vec3 end = start.add(0.0D, -GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS, 0.0D);
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS;
        }
        return start.distanceTo(hit.getLocation());
    }

    private static Iterable<BlockPos> launchPadPositionsTouching(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(LAUNCH_PAD_FOOT_EPSILON);
        return BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ)
        );
    }

    private static boolean tryActivateLaunchPadAt(ServerLevel level, ResourceKey<Level> dimension, ServerPlayer player, BlockPos pos) {
        TrackedLaunchPad pad = LAUNCH_PADS.get(new LaunchPadKey(dimension, pos));
        if (pad == null || !pad.isPresentAt(level, pos)) {
            return false;
        }
        activateLaunchPad(player, pad.redeployTicks(), false, pad.surfaceNormalAt(level, pos));
        return true;
    }

    static Vec3 launchImpulse(Vec3 lookAngle, Direction surfaceNormal) {
        if (surfaceNormal == Direction.UP) {
            return LaunchPadImpulse.defaultImpulse(lookAngle);
        }
        if (surfaceNormal.getAxis().isHorizontal()) {
            return new Vec3(
                    surfaceNormal.getStepX() * WALL_BOUNCER_HORIZONTAL_VELOCITY_BLOCKS_PER_TICK,
                    WALL_BOUNCER_UPWARD_VELOCITY_BLOCKS_PER_TICK,
                    surfaceNormal.getStepZ() * WALL_BOUNCER_HORIZONTAL_VELOCITY_BLOCKS_PER_TICK
            );
        }
        return LaunchPadImpulse.defaultImpulse(lookAngle);
    }

    private static void preserveLaunchedHorizontalVelocity(ServerPlayer player, ImpulseLaunchState state, long tick) {
        Vec3 velocity = player.getDeltaMovement();
        Vec3 corrected = launchAirDragSuppressedVelocity(velocity, state.horizontalVelocity(), tick, state.expiresAtTick());
        if (corrected == velocity) {
            state.setHorizontalVelocity(velocity);
            return;
        }
        player.setDeltaMovement(corrected.x(), corrected.y(), corrected.z());
        player.hurtMarked = true;
    }

    static Vec3 launchAirDragSuppressedVelocity(Vec3 velocity, Vec3 preservedHorizontalVelocity, long currentTick, long expiresAtTick) {
        if (!launchAirDragSuppressionActive(currentTick, expiresAtTick)) {
            return velocity;
        }
        return withoutLaunchAirDrag(velocity, preservedHorizontalVelocity);
    }

    static boolean launchAirDragSuppressionActive(long currentTick, long expiresAtTick) {
        return currentTick < expiresAtTick;
    }

    static Vec3 withoutLaunchAirDrag(Vec3 velocity, Vec3 preservedHorizontalVelocity) {
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(preservedHorizontalVelocity, "preservedHorizontalVelocity");
        double preservedSpeedSqr = horizontalSpeedSqr(preservedHorizontalVelocity);
        double minSpeedSqr = MIN_PRESERVED_HORIZONTAL_SPEED * MIN_PRESERVED_HORIZONTAL_SPEED;
        if (preservedSpeedSqr <= minSpeedSqr) {
            return velocity;
        }
        double currentSpeedSqr = horizontalSpeedSqr(velocity);
        if (currentSpeedSqr >= preservedSpeedSqr || currentSpeedSqr <= minSpeedSqr) {
            return velocity;
        }
        double dot = velocity.x() * preservedHorizontalVelocity.x() + velocity.z() * preservedHorizontalVelocity.z();
        double minAlignedDotSqr = currentSpeedSqr * preservedSpeedSqr
                * LAUNCH_AIR_DRAG_DIRECTION_DOT_MIN * LAUNCH_AIR_DRAG_DIRECTION_DOT_MIN;
        if (dot <= 0.0D || dot * dot < minAlignedDotSqr) {
            return velocity;
        }
        return new Vec3(preservedHorizontalVelocity.x(), velocity.y(), preservedHorizontalVelocity.z());
    }

    private static double horizontalSpeed(Vec3 velocity) {
        return Math.sqrt(horizontalSpeedSqr(velocity));
    }

    private static double horizontalSpeedSqr(Vec3 velocity) {
        return velocity.x() * velocity.x() + velocity.z() * velocity.z();
    }

    private record LaunchPadKey(ResourceKey<Level> dimension, BlockPos pos) {
        private LaunchPadKey {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(pos, "pos");
            pos = pos.immutable();
        }
    }

    private record TrackedLaunchPad(long redeployTicks, Block triggerBlock) {
        private TrackedLaunchPad {
            if (redeployTicks < 0L) {
                throw new IllegalArgumentException("redeployTicks cannot be negative");
            }
            Objects.requireNonNull(triggerBlock, "triggerBlock");
        }

        private boolean isPresentAt(ServerLevel level, BlockPos pos) {
            return level.getBlockState(pos).is(triggerBlock);
        }

        private Direction surfaceNormalAt(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(TrapTriggerBlock.FACING)) {
                return state.getValue(TrapTriggerBlock.FACING);
            }
            return Direction.UP;
        }
    }

    static final class ActiveRiftPortal {
        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final long expireTick;
        private final long redeployTicks;
        private final double verticalTeleportBlocks;
        private final double horizontalLaunchSpeed;
        private final double verticalLaunchSpeed;
        private final Set<UUID> usedPlayers = new HashSet<>();

        ActiveRiftPortal(
                ResourceKey<Level> dimension,
                Vec3 origin,
                long expireTick,
                long redeployTicks,
                double verticalTeleportBlocks,
                double horizontalLaunchSpeed,
                double verticalLaunchSpeed
        ) {
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.origin = Objects.requireNonNull(origin, "origin");
            this.expireTick = expireTick;
            if (redeployTicks < 0L) {
                throw new IllegalArgumentException("redeployTicks cannot be negative");
            }
            if (verticalTeleportBlocks <= 0.0D) {
                throw new IllegalArgumentException("verticalTeleportBlocks must be positive");
            }
            if (horizontalLaunchSpeed < 0.0D || verticalLaunchSpeed < 0.0D) {
                throw new IllegalArgumentException("launch speed cannot be negative");
            }
            this.redeployTicks = redeployTicks;
            this.verticalTeleportBlocks = verticalTeleportBlocks;
            this.horizontalLaunchSpeed = horizontalLaunchSpeed;
            this.verticalLaunchSpeed = verticalLaunchSpeed;
        }

        ResourceKey<Level> dimension() {
            return dimension;
        }

        Vec3 origin() {
            return origin;
        }

        long expireTick() {
            return expireTick;
        }

        boolean isActive(long tick) {
            return riftPortalActive(tick, expireTick);
        }

        long redeployTicks() {
            return redeployTicks;
        }

        double verticalTeleportBlocks() {
            return verticalTeleportBlocks;
        }

        double horizontalLaunchSpeed() {
            return horizontalLaunchSpeed;
        }

        double verticalLaunchSpeed() {
            return verticalLaunchSpeed;
        }

        boolean hasUsed(UUID playerId) {
            return usedPlayers.contains(playerId);
        }

        boolean tryUse(UUID playerId) {
            return usedPlayers.add(Objects.requireNonNull(playerId, "playerId"));
        }
    }

    private static final class SlideState {
        private final long startedTick;
        private double lastY;

        private SlideState(long startedTick, double lastY) {
            this.startedTick = startedTick;
            this.lastY = lastY;
        }

        private long startedTick() {
            return startedTick;
        }

        private double lastY() {
            return lastY;
        }

        private void setLastY(double lastY) {
            this.lastY = lastY;
        }
    }

    private static final class ImpulseLaunchState {
        private final boolean cancelFallDamage;
        private final long expiresAtTick;
        private Vec3 horizontalVelocity;

        private ImpulseLaunchState(boolean cancelFallDamage, Vec3 launchVelocity, long expiresAtTick) {
            this.cancelFallDamage = cancelFallDamage;
            this.expiresAtTick = expiresAtTick;
            setHorizontalVelocity(launchVelocity);
        }

        private boolean cancelFallDamage() {
            return cancelFallDamage;
        }

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private Vec3 horizontalVelocity() {
            return horizontalVelocity;
        }

        private void setHorizontalVelocity(Vec3 velocity) {
            horizontalVelocity = new Vec3(velocity.x(), 0.0D, velocity.z());
        }
    }
}
