package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class WeaponItem extends SimplePolymerItem {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final double FORTNITE_TO_MINECRAFT_DAMAGE = 0.2D;
    private static final double ENTITY_HITBOX_INFLATE_BLOCKS = 0.3D;
    private static final double BULLET_TRACE_STEP_BLOCKS = 1.25D;
    private static final int MAX_TRACE_PARTICLES = 64;
    static final ClipContext.Block BULLET_BLOCK_MODE = ClipContext.Block.COLLIDER;

    private final WeaponDefinition definition;
    private final Item clientItem;

    public WeaponItem(WeaponDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public WeaponDefinition definition() {
        return definition;
    }

    static UseCooldown cooldownComponent(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(fireDelayTicks(definition) / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
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
        out.set(DataComponents.USE_COOLDOWN, cooldownComponent(definition));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.rarity().label() + " " + definition.displayName());
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        WeaponStats stats = definition.stats();
        tooltip.add(Component.literal(definition.category().label() + " / " + definition.rarity().label()));
        tooltip.add(Component.literal("Damage: " + format(stats.damage())
                + (stats.pellets() > 1 ? " x" + stats.pellets() : "")));
        tooltip.add(Component.literal("Fire rate: " + format(stats.fireRatePerSecond()) + "/s; magazine: " + stats.magazineSize()));
        tooltip.add(Component.literal("Reload: " + format(stats.reloadSeconds()) + "s; crit: " + format(stats.criticalMultiplier()) + "x"));
        tooltip.add(Component.literal("Source: " + definition.sourceStatRow()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        WeaponAutoFire.rememberInput(serverPlayer, hand, this, serverLevel.getGameTime());
        return fireFromHeldItem(serverLevel, serverPlayer, hand);
    }

    InteractionResult fireFromHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != this) {
            return InteractionResult.PASS;
        }

        long tick = level.getGameTime();
        long nextFireTick = customData(stack).getLongOr(NEXT_FIRE_TICK_KEY, 0L);
        if (tick < nextFireTick || player.getCooldowns().isOnCooldown(stack)) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }

        int magazine = magazine(stack);
        if (magazine <= 0) {
            int reloadTicks = reloadTicks();
            reload(stack, tick, reloadTicks);
            player.getCooldowns().addCooldown(stack, reloadTicks);
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + definition.displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        magazine--;
        int fireDelayTicks = fireDelayTicks();
        setGunState(stack, magazine, tick + fireDelayTicks);
        player.getCooldowns().addCooldown(stack, fireDelayTicks);

        ShotTrace trace = traceShot(level, player, definition.stats().rangeBlocks());
        playShotEffects(level, player, trace);
        if (trace.target() != null) {
            DamageReport report = damageTarget(level, player, trace.target());
            playHitEffects(level, player, trace.hitLocation(), report.damage());
            HitMarkerDisplays.show(level, player, trace.target(), report.damage());
        } else {
            player.sendSystemMessage(Component.literal("Fired " + definition.displayName()
                    + " (" + magazine + "/" + definition.stats().magazineSize() + ")."), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void resyncCooldownOverlay(ServerPlayer player, ItemStack stack) {
        Objects.requireNonNull(player, "player");
        if (!(stack.getItem() instanceof WeaponItem item)) {
            return;
        }
        int remainingTicks = item.remainingCooldownTicks(stack, player.level().getGameTime());
        if (remainingTicks > 0) {
            player.getCooldowns().addCooldown(stack, remainingTicks);
        }
    }

    private DamageReport damageTarget(ServerLevel level, ServerPlayer shooter, LivingEntity target) {
        float before = target.getHealth() + target.getAbsorptionAmount();
        Vec3 velocityBeforeHit = target.getDeltaMovement();
        prepareBulletHit(target);
        boolean damaged = target.hurtServer(level, shooter.damageSources().playerAttack(shooter), minecraftDamage());
        if (damaged) {
            reduceBulletInvulnerability(target);
            if (CombatSettings.preventBulletKnockback() && !(target instanceof ServerPlayer)) {
                target.setDeltaMovement(velocityBeforeHit);
                target.hurtMarked = false;
            }
        }
        float after = Math.max(0.0F, target.getHealth()) + target.getAbsorptionAmount();
        return new DamageReport(damaged, Math.max(0.0F, before - after));
    }

    static void prepareBulletHit(LivingEntity target) {
        Objects.requireNonNull(target, "target");
        if (target instanceof ServerPlayer) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurtTime = 0;
    }

    static void reduceBulletInvulnerability(LivingEntity target) {
        Objects.requireNonNull(target, "target");
        if (target instanceof ServerPlayer) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurtTime = Math.min(target.hurtTime, 1);
        target.hurtDuration = Math.min(target.hurtDuration, 1);
    }

    private float minecraftDamage() {
        return (float) Math.max(1.0D, definition.stats().damage() * definition.stats().pellets() * FORTNITE_TO_MINECRAFT_DAMAGE);
    }

    private int magazine(ItemStack stack) {
        return customData(stack).getIntOr(MAGAZINE_KEY, definition.stats().magazineSize());
    }

    private void reload(ItemStack stack, long tick, int reloadTicks) {
        setGunState(stack, definition.stats().magazineSize(), tick + reloadTicks);
    }

    private void setGunState(ItemStack stack, int magazine, long nextFireTick) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(MAGAZINE_KEY, magazine);
            tag.putLong(NEXT_FIRE_TICK_KEY, nextFireTick);
        });
    }

    private CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    int fireDelayTicks() {
        return fireDelayTicks(definition);
    }

    static int fireDelayTicks(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return Math.max(1, (int) Math.round(20.0D / definition.stats().fireRatePerSecond()));
    }

    int reloadTicks() {
        return Math.max(1, (int) Math.round(definition.stats().reloadSeconds() * 20.0D));
    }

    int remainingCooldownTicks(ItemStack stack, long tick) {
        long nextFireTick = customData(stack).getLongOr(NEXT_FIRE_TICK_KEY, 0L);
        if (nextFireTick <= tick) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, nextFireTick - tick);
    }

    private static ShotTrace traceShot(ServerLevel level, ServerPlayer player, double rangeBlocks) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 rayEnd = start.add(look.scale(rangeBlocks));
        HitResult blockHit = level.clip(new ClipContext(start, rayEnd, BULLET_BLOCK_MODE, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            rayEnd = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(rayEnd.subtract(start)).inflate(1.0D);
        LivingEntity closest = null;
        Vec3 closestHit = rayEnd;
        double closestDistance = start.distanceToSqr(rayEnd);
        for (Entity entity : level.getEntities(player, searchBox, WeaponItem::canShoot)) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(ENTITY_HITBOX_INFLATE_BLOCKS).clip(start, rayEnd);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = (LivingEntity) entity;
                closestHit = hit.get();
            }
        }
        return new ShotTrace(start, closest == null ? rayEnd : closestHit, closest, closestHit);
    }

    private static boolean canShoot(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
    }

    private static void playShotEffects(ServerLevel level, ServerPlayer shooter, ShotTrace trace) {
        Vec3 muzzle = muzzlePosition(shooter, trace.start());
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 0.65F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, muzzle.x(), muzzle.y(), muzzle.z(), 3, 0.05D, 0.05D, 0.05D, 0.0D);
        spawnBulletTrace(level, trace.start(), trace.end());
    }

    static Vec3 muzzlePosition(ServerPlayer shooter, Vec3 start) {
        return muzzlePosition(start, shooter.getLookAngle());
    }

    static Vec3 muzzlePosition(Vec3 start, Vec3 look) {
        Vec3 right = look.cross(Vec3.Y_AXIS);
        if (right.lengthSqr() < 1.0E-6D) {
            right = Vec3.X_AXIS;
        } else {
            right = right.normalize();
        }
        return start.add(look.normalize().scale(0.75D)).add(right.scale(0.28D)).add(0.0D, -0.22D, 0.0D);
    }

    private static void playReloadSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.8F, 0.75F);
    }

    private static void playHitEffects(ServerLevel level, ServerPlayer shooter, Vec3 hitLocation, float damage) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7F, 1.35F);
        int damageParticles = Math.max(1, Math.min(12, Math.round(damage)));
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, true, true, hitLocation.x(), hitLocation.y(), hitLocation.z(), damageParticles, 0.2D, 0.3D, 0.2D, 0.05D);
    }

    private static void spawnBulletTrace(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.0D) {
            return;
        }
        int samples = Math.max(2, Math.min(MAX_TRACE_PARTICLES, (int) Math.ceil(length / BULLET_TRACE_STEP_BLOCKS)));
        Vec3 step = delta.scale(1.0D / samples);
        for (int i = 1; i <= samples; i++) {
            Vec3 pos = start.add(step.scale(i));
            level.sendParticles(ParticleTypes.CRIT, true, true, pos.x(), pos.y(), pos.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }


    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    record DamageReport(boolean damaged, float damage) {
    }

    private record ShotTrace(Vec3 start, Vec3 end, LivingEntity target, Vec3 hitLocation) {
    }
}
