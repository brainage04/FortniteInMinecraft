package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerAimStates;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;


import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


public final class ExplosiveProjectileWeaponItem extends Item {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final String RELOAD_COMPLETE_TICK_KEY = "reload_complete_tick";
    private static final ArrayList<ActiveProjectile> ACTIVE_PROJECTILES = new ArrayList<>();
    private static boolean tickRegistered;

    private final Definition definition;
    private final Item clientItem;

    public ExplosiveProjectileWeaponItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings, lore(definition)));
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
        registerTicker();
    }

    public Definition explosiveDefinition() {
        return definition;
    }

    public WeaponDefinition definition() {
        return definition.weapon();
    }

    public float adsFovMultiplier() {
        return WeaponItem.adsFovMultiplier(definition.weapon());
    }

    Item clientItem() {
        return clientItem;
    }

    static UseCooldown cooldownComponent(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(cooldownTicks(definition) / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.weapon().path()
        )));
    }

    static int cooldownTicks(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        WeaponStats stats = definition.weapon().stats();
        int fireTicks = Math.max(1, (int) Math.round(20.0D / stats.fireRatePerSecond()));
        int reloadTicks = Math.max(1, (int) Math.round(stats.reloadSeconds() * 20.0D));
        return stats.magazineSize() <= 1 ? Math.max(fireTicks, reloadTicks) : fireTicks;
    }

    static float minecraftDamage(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return WeaponItem.minecraftDamage(definition.weapon());
    }

    static float fortniteUnitsPerSecondToBlocksPerTick(double unitsPerSecond) {
        if (unitsPerSecond <= 0.0D) {
            throw new IllegalArgumentException("unitsPerSecond must be positive");
        }
        return (float) (unitsPerSecond / 100.0D / 20.0D);
    }

    private static List<Component> lore(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        WeaponDefinition weapon = definition.weapon();
        WeaponStats stats = weapon.stats();
        List<Component> lines = new ArrayList<>(5);
        lines.add(Component.literal(weapon.category().label() + " / " + weapon.rarity().label()));
        if (definition.hasImpulseOnly()) {
            lines.add(Component.literal("Shockwave radius: " + format(definition.explosionRadiusBlocks())
                    + " blocks; impulse: " + format(definition.impulseHorizontalStrength())
                    + " horizontal / " + format(definition.impulseVerticalStrength()) + " vertical"));
        } else {
            lines.add(Component.literal("Explosive damage: " + format(stats.damage())
                    + "; radius: " + format(definition.explosionRadiusBlocks()) + " blocks"));
        }
        lines.add(Component.literal("Projectile speed: " + format(definition.projectileSpeed())
                + " blocks/tick; cooldown: " + format(cooldownTicks(definition) / 20.0D) + "s"));
        if (!definition.evidenceNote().isBlank()) {
            lines.add(Component.literal(definition.evidenceNote()));
        }
        lines.add(Component.literal("Source: " + weapon.sourceStatRow()));
        return lines;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.weapon().displayName()).withStyle(rarityColor(definition.weapon().rarity()));
    }

    

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level == null || player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != this) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerAimStates.setAiming(serverPlayer, true);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            PlayerAimStates.setAiming(player, false);
        }
        return false;
    }

    public InteractionResult fireFromHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        return fireFromHeldItem(level, player, hand, false);
    }

    InteractionResult fireBurstShotFromHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        return fireFromHeldItem(level, player, hand, true);
    }

    private InteractionResult fireFromHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand, boolean burstShot) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != this) {
            return InteractionResult.PASS;
        }
        if (MobilityItemInteractions.isGliding(player)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        long tick = level.getGameTime();
        completeReloadIfReady(stack, tick);
        long nextFireTick = customData(stack).getLongOr(NEXT_FIRE_TICK_KEY, 0L);
        long reloadCompleteTick = customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L);
        int magazine = magazine(stack);
        boolean infiniteAmmo = WeaponItem.hasInfiniteAmmo(player);
        int magazineSize = definition.weapon().stats().magazineSize();
        if (infiniteAmmo && magazine < magazineSize) {
            setGunState(stack, magazineSize, tick);
            magazine = magazineSize;
        }
        boolean blockedByCooldown = reloadCompleteTick > tick
                || (!burstShot && (tick < nextFireTick || player.getCooldowns().isOnCooldown(stack)));
        WeaponItem.FireAttempt attempt = WeaponItem.fireAttempt(magazine, blockedByCooldown);
        if (attempt == WeaponItem.FireAttempt.COOLDOWN) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (attempt == WeaponItem.FireAttempt.EMPTY_MAGAZINE) {
            if (startReloadAndNotify(level, player, stack, tick)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            player.sendSystemMessage(Component.literal("Empty " + definition.weapon().displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack projectileStack = new ItemStack(clientItem);
        TrackedExplosiveSnowball projectile = new TrackedExplosiveSnowball(level, player, projectileStack);
        projectile.setNoGravity(definition.gravityFreeProjectile());
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, definition.projectileSpeed(), definition.inaccuracy());
        if (!level.addFreshEntity(projectile)) {
            return InteractionResult.FAIL;
        }

        int magazineAfterShot = infiniteAmmo ? magazine : magazine - 1;
        int fireDelayTicks = WeaponItem.fireDelayTicks(definition.weapon());
        setGunState(stack, magazineAfterShot, burstShot ? nextFireTick : tick + fireDelayTicks);
        if (!burstShot) {
            player.getCooldowns().addCooldown(stack, fireDelayTicks);
            int remainingBurstShots = remainingBurstShots(magazineAfterShot, infiniteAmmo);
            if (remainingBurstShots > 0) {
                int burstIntervalTicks = WeaponItem.burstIntervalTicks(definition.weapon());
                WeaponAutoFire.scheduleBurstShots(
                        player,
                        hand,
                        this,
                        tick + burstIntervalTicks,
                        remainingBurstShots,
                        burstIntervalTicks
                );
            }
        }

        ActiveProjectile active = new ActiveProjectile(projectile, level.dimension(), tick, definition, player.getUUID());
        projectile.setActiveProjectile(active);
        ACTIVE_PROJECTILES.add(active);
        boolean autoReloadStarted = WeaponItem.shouldAutoReload(magazineAfterShot, infiniteAmmo)
                && startReloadAndNotify(level, player, stack, tick);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.sendSystemMessage(Component.literal(autoReloadStarted
                ? statusText(stack, PlayerAimStates.isAiming(player))
                : statusText(magazineAfterShot, PlayerAimStates.isAiming(player))), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult handleManualReload(ServerPlayer player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ExplosiveProjectileWeaponItem item)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = player.level();
        long tick = level.getGameTime();
        item.completeReloadIfReady(stack, tick);
        if (WeaponItem.hasInfiniteAmmo(player)) {
            item.setGunState(stack, item.definition.weapon().stats().magazineSize(), tick);
            player.sendSystemMessage(Component.literal(item.statusText(stack, PlayerAimStates.isAiming(player))), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player.getCooldowns().isOnCooldown(stack) && item.isReloading(stack, tick)) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        WeaponItem.ManualReloadResult result = item.tryStartManualReloadOnGun(stack, tick);
        if (result == WeaponItem.ManualReloadResult.STARTED) {
            player.getCooldowns().addCooldown(stack, item.remainingCooldownTicks(stack, tick));
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + item.definition.weapon().displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (result == WeaponItem.ManualReloadResult.ALREADY_RELOADING) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    public static void resyncCooldownOverlay(ServerPlayer player, ItemStack stack) {
        Objects.requireNonNull(player, "player");
        if (!(stack.getItem() instanceof ExplosiveProjectileWeaponItem item)) {
            return;
        }
        long tick = player.level().getGameTime();
        item.completeReloadIfReady(stack, tick);
        int remainingTicks = item.remainingCooldownTicks(stack, tick);
        if (remainingTicks > 0 && !player.getCooldowns().isOnCooldown(stack)) {
            player.getCooldowns().addCooldown(stack, remainingTicks);
        }
    }

    public static void cancelInactiveReloads(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long tick = player.level().getGameTime();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == mainHand || stack == offHand) {
                continue;
            }
            if (stack.getItem() instanceof ExplosiveProjectileWeaponItem item) {
                item.cancelReload(stack, tick);
            }
        }
    }

    public static boolean showHeldStatus(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (showHeldStatus(player, player.getItemInHand(InteractionHand.MAIN_HAND))) {
            return true;
        }
        return showHeldStatus(player, player.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static boolean showHeldStatus(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ExplosiveProjectileWeaponItem item)) {
            return false;
        }
        long tick = player.level().getGameTime();
        item.completeReloadIfReady(stack, tick);
        player.sendSystemMessage(Component.literal(item.statusText(stack, PlayerAimStates.isAiming(player))), true);
        return true;
    }

    String statusText(ItemStack stack, boolean aiming) {
        int magazine = magazine(stack);
        if (customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L) > 0L) {
            return definition.weapon().displayName() + (aiming ? " ADS" : "") + ": " + magazine + "/" + definition.weapon().stats().magazineSize() + " (reloading)";
        }
        return statusText(magazine, aiming);
    }

    String statusText(int magazine, boolean aiming) {
        return definition.weapon().displayName() + (aiming ? " ADS" : "") + ": " + magazine + "/" + definition.weapon().stats().magazineSize();
    }

    private int magazine(ItemStack stack) {
        return customData(stack).getIntOr(MAGAZINE_KEY, definition.weapon().stats().magazineSize());
    }

    private void startReload(ItemStack stack, long tick) {
        int reloadTicks = reloadTicks();
        int magazine = magazine(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(MAGAZINE_KEY, magazine);
            tag.putLong(NEXT_FIRE_TICK_KEY, tick + reloadTicks);
            tag.putLong(RELOAD_COMPLETE_TICK_KEY, tick + reloadTicks);
        });
    }

    private boolean startReloadAndNotify(ServerLevel level, ServerPlayer player, ItemStack stack, long tick) {
        if (WeaponItem.hasInfiniteAmmo(player)) {
            setGunState(stack, definition.weapon().stats().magazineSize(), tick);
            return false;
        }
        WeaponItem.ManualReloadResult result = tryStartManualReloadOnGun(stack, tick);
        if (result == WeaponItem.ManualReloadResult.STARTED) {
            player.getCooldowns().addCooldown(stack, remainingCooldownTicks(stack, tick));
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + definition.weapon().displayName() + "."), true);
            return true;
        }
        if (result == WeaponItem.ManualReloadResult.ALREADY_RELOADING) {
            resyncCooldownOverlay(player, stack);
            return true;
        }
        return false;
    }

    private boolean completeReloadIfReady(ItemStack stack, long tick) {
        long reloadCompleteTick = customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L);
        if (reloadCompleteTick <= 0L || reloadCompleteTick > tick) {
            return false;
        }
        setGunState(stack, definition.weapon().stats().magazineSize(), tick);
        return true;
    }

    private boolean isReloading(ItemStack stack, long tick) {
        return customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L) > tick;
    }

    private void cancelReload(ItemStack stack, long tick) {
        if (!isReloading(stack, tick)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(NEXT_FIRE_TICK_KEY, tick);
            tag.remove(RELOAD_COMPLETE_TICK_KEY);
        });
    }

    private WeaponItem.ManualReloadResult tryStartManualReloadOnGun(ItemStack stack, long tick) {
        WeaponItem.ManualReloadResult result = WeaponItem.manualReloadDecision(
                magazine(stack),
                definition.weapon().stats().magazineSize(),
                customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L),
                tick
        );
        if (result == WeaponItem.ManualReloadResult.STARTED) {
            startReload(stack, tick);
        }
        return result;
    }

    private int remainingBurstShots(int magazineAfterShot, boolean infiniteAmmo) {
        int remainingBurstShots = definition.weapon().stats().cartridgePerFire() - 1;
        if (remainingBurstShots <= 0 || (!infiniteAmmo && magazineAfterShot <= 0)) {
            return 0;
        }
        return infiniteAmmo ? remainingBurstShots : Math.min(remainingBurstShots, magazineAfterShot);
    }

    boolean canContinueScheduledBurst(ServerPlayer player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != this) {
            return false;
        }
        long tick = player.level().getGameTime();
        return !isReloading(stack, tick) && (WeaponItem.hasInfiniteAmmo(player) || magazine(stack) > 0);
    }

    void setGunState(ItemStack stack, int magazine, long nextFireTick) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(MAGAZINE_KEY, magazine);
            tag.putLong(NEXT_FIRE_TICK_KEY, nextFireTick);
            tag.remove(RELOAD_COMPLETE_TICK_KEY);
        });
    }

    private CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    int reloadTicks() {
        return Math.max(1, (int) Math.round(definition.weapon().stats().reloadSeconds() * 20.0D));
    }

    int remainingCooldownTicks(ItemStack stack, long tick) {
        CompoundTag data = customData(stack);
        long reloadCompleteTick = data.getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L);
        if (reloadCompleteTick > 0L && reloadCompleteTick <= tick) {
            return 0;
        }
        long nextFireTick = data.getLongOr(NEXT_FIRE_TICK_KEY, 0L);
        if (nextFireTick <= tick) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, nextFireTick - tick);
    }

    private static void playReloadSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.8F, 0.75F);
    }

    private static void registerTicker() {
        if (tickRegistered) {
            return;
        }
        FortniteInMinecraft.platform().registerEndLevelTick(ExplosiveProjectileWeaponItem::tickLevel);
        tickRegistered = true;
    }

    private static void tickLevel(ServerLevel level) {
        if (ACTIVE_PROJECTILES.isEmpty()) {
            return;
        }
        Iterator<ActiveProjectile> iterator = ACTIVE_PROJECTILES.iterator();
        while (iterator.hasNext()) {
            ActiveProjectile active = iterator.next();
            if (!active.dimension().equals(level.dimension())) {
                continue;
            }
            if (active.exploded()) {
                iterator.remove();
                continue;
            }

            Snowball projectile = active.projectile();
            long tick = level.getGameTime();
            Vec3 position = active.position();
            if (active.shouldDetonateAfterImpact(tick)
                    || active.shouldProximityDetonate(level, tick, position)
                    || tick - active.spawnTick() >= active.definition().fuseTicks()) {
                active.explode(level, position, null, projectile);
                iterator.remove();
                continue;
            }

            if (active.stuck()) {
                level.sendParticles(ParticleTypes.SMOKE, true, true,
                        position.x(), position.y(), position.z(), 1, 0.02D, 0.02D, 0.02D, 0.0D);
                continue;
            }
            if (projectile.isRemoved() || !projectile.isAlive()) {
                active.stick(tick, projectile.position(), null);
            }
        }
    }

    private static void detonate(ServerLevel level, Vec3 origin, BlockPos hitBlockPos, Definition definition, Entity source, UUID ownerId) {
        Player owner = level.getPlayerByUUID(ownerId);
        ServerPlayer shooter = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        level.playSound(null, origin.x(), origin.y(), origin.z(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F,
                definition.hasImpulseOnly() ? 1.15F : 0.85F);
        level.sendParticles(ParticleTypes.EXPLOSION, true, true, origin.x(), origin.y(), origin.z(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(definition.hasImpulseOnly() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME,
                true, true, origin.x(), origin.y(), origin.z(), 24,
                definition.explosionRadiusBlocks() * 0.25D,
                definition.explosionRadiusBlocks() * 0.18D,
                definition.explosionRadiusBlocks() * 0.25D,
                0.08D);

        if (hitBlockPos != null && shooter != null && definition.environmentDamage() > 0) {
            WeaponItem.damageBuild(level, shooter, hitBlockPos, origin, definition.environmentDamage(), "");
        }

        AABB area = new AABB(
                origin.x() - definition.explosionRadiusBlocks(),
                origin.y() - definition.explosionRadiusBlocks(),
                origin.z() - definition.explosionRadiusBlocks(),
                origin.x() + definition.explosionRadiusBlocks(),
                origin.y() + definition.explosionRadiusBlocks(),
                origin.z() + definition.explosionRadiusBlocks()
        );
        for (Entity entity : level.getEntities(source, area, target -> target instanceof LivingEntity living
                && living.isAlive()
                && living.isPickable()
                && !living.isSpectator())) {
            LivingEntity target = (LivingEntity) entity;
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            if (targetCenter.distanceTo(origin) > definition.explosionRadiusBlocks()) {
                continue;
            }

            if (!definition.hasImpulseOnly() && shooter != null) {
                float damage = minecraftDamage(definition);
                float shieldBeforeHit = target.getAbsorptionAmount();
                float before = target.getHealth() + shieldBeforeHit;
                Vec3 velocityBeforeHit = target.getDeltaMovement();
                WeaponItem.prepareBulletHit(target);
                boolean damaged = target.hurtServer(level, shooter.damageSources().playerAttack(shooter), damage);
                if (damaged) {
                    WeaponItem.reduceBulletInvulnerability(target);
                    if (CombatSettings.preventBulletKnockback() && !(target instanceof ServerPlayer)) {
                        target.setDeltaMovement(velocityBeforeHit);
                        target.hurtMarked = false;
                    }
                    float after = Math.max(0.0F, target.getHealth()) + target.getAbsorptionAmount();
                    float dealt = Math.max(0.0F, before - after);
                    WeaponItem.playHitEffects(level, shooter, targetCenter, dealt);
                    HitMarkerDisplays.show(level, shooter, target, dealt, false, shieldBeforeHit > 0.0F);
                }
            }

            if (definition.impulseHorizontalStrength() > 0.0D || definition.impulseVerticalStrength() > 0.0D) {
                Vec3 impulse = ImpulsePhysics.radialImpulse(
                        origin,
                        targetCenter,
                        definition.explosionRadiusBlocks(),
                        definition.impulseHorizontalStrength(),
                        definition.impulseVerticalStrength()
                );
                if (impulse.lengthSqr() > 1.0E-8D) {
                    target.addDeltaMovement(impulse);
                    target.hurtMarked = true;
                    if (target instanceof ServerPlayer launched) {
                        MobilityItemInteractions.enableImpulseLaunch(launched, definition.resetsFallDistance());
                    } else if (definition.resetsFallDistance()) {
                        target.resetFallDistance();
                    }
                }
            }
        }
    }

    private static final class TrackedExplosiveSnowball extends Snowball {
        private ActiveProjectile activeProjectile;

        private TrackedExplosiveSnowball(Level level, Player owner, ItemStack stack) {
            super(level, owner, stack);
        }

        private void setActiveProjectile(ActiveProjectile activeProjectile) {
            this.activeProjectile = Objects.requireNonNull(activeProjectile, "activeProjectile");
        }

        @Override
        protected void onHit(HitResult result) {
            if (!level().isClientSide()
                    && level() instanceof ServerLevel serverLevel
                    && activeProjectile != null
                    && !activeProjectile.exploded()) {
                BlockPos hitBlock = result instanceof BlockHitResult blockHit && result.getType() != HitResult.Type.MISS
                        ? blockHit.getBlockPos()
                        : null;
                if (activeProjectile.definition().impactExplosionDelayTicks() > 0L) {
                    activeProjectile.stick(serverLevel.getGameTime(), result.getLocation(), hitBlock);
                } else if (activeProjectile.definition().explodeOnImpact() || result.getType() == HitResult.Type.ENTITY) {
                    activeProjectile.explode(serverLevel, result.getLocation(), hitBlock, this);
                } else if (!activeProjectile.stuck()) {
                    activeProjectile.stick(serverLevel.getGameTime(), result.getLocation(), hitBlock);
                }
            }
            super.onHit(result);
        }
    }

    private static final class ActiveProjectile {
        private final Snowball projectile;
        private final ResourceKey<Level> dimension;
        private final long spawnTick;
        private final Definition definition;
        private final UUID ownerId;
        private Vec3 stuckPosition;
        private BlockPos stuckBlock;
        private long stuckTick = -1L;
        private boolean exploded;

        private ActiveProjectile(Snowball projectile, ResourceKey<Level> dimension, long spawnTick, Definition definition, UUID ownerId) {
            this.projectile = Objects.requireNonNull(projectile, "projectile");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.spawnTick = spawnTick;
            this.definition = Objects.requireNonNull(definition, "definition");
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        }

        private Snowball projectile() {
            return projectile;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private long spawnTick() {
            return spawnTick;
        }

        private Definition definition() {
            return definition;
        }

        private boolean stuck() {
            return stuckPosition != null;
        }

        private Vec3 position() {
            return stuckPosition == null ? projectile.position() : stuckPosition;
        }

        private boolean exploded() {
            return exploded;
        }

        private void stick(long tick, Vec3 position, BlockPos block) {
            if (stuckPosition != null) {
                return;
            }
            stuckTick = tick;
            stuckPosition = Objects.requireNonNull(position, "position");
            stuckBlock = block == null ? null : block.immutable();
        }

        private boolean shouldDetonateAfterImpact(long tick) {
            return stuck()
                    && definition.impactExplosionDelayTicks() > 0L
                    && tick - stuckTick >= definition.impactExplosionDelayTicks();
        }

        private boolean shouldProximityDetonate(ServerLevel level, long tick, Vec3 position) {
            if (!definition.proximityTriggered() || tick - spawnTick < definition.armingDelayTicks()) {
                return false;
            }
            AABB area = new AABB(
                    position.x() - definition.proximityRadiusBlocks(),
                    position.y() - definition.proximityRadiusBlocks(),
                    position.z() - definition.proximityRadiusBlocks(),
                    position.x() + definition.proximityRadiusBlocks(),
                    position.y() + definition.proximityRadiusBlocks(),
                    position.z() + definition.proximityRadiusBlocks()
            );
            return !level.getEntities(projectile, area, target -> target instanceof LivingEntity living
                    && living.isAlive()
                    && living.isPickable()
                    && !living.isSpectator()).isEmpty();
        }

        private void explode(ServerLevel level, Vec3 position, BlockPos hitBlock, Entity source) {
            if (exploded) {
                return;
            }
            exploded = true;
            detonate(level, position, hitBlock == null ? stuckBlock : hitBlock, definition, source, ownerId);
            projectile.discard();
        }
    }

    public record Definition(
            WeaponDefinition weapon,
            int environmentDamage,
            double explosionRadiusBlocks,
            int fuseTicks,
            int armingDelayTicks,
            boolean explodeOnImpact,
            boolean proximityTriggered,
            double proximityRadiusBlocks,
            double impulseHorizontalStrength,
            double impulseVerticalStrength,
            boolean resetsFallDistance,
            float projectileSpeed,
            float inaccuracy,
            long impactExplosionDelayTicks,
            boolean gravityFreeProjectile,
            String evidenceNote
    ) {
        public Definition {
            if (impactExplosionDelayTicks < 0L) {
                throw new IllegalArgumentException("impactExplosionDelayTicks cannot be negative");
            }
            Objects.requireNonNull(weapon, "weapon");
            if (environmentDamage < 0) {
                throw new IllegalArgumentException("environmentDamage cannot be negative");
            }
            if (explosionRadiusBlocks <= 0.0D) {
                throw new IllegalArgumentException("explosionRadiusBlocks must be positive");
            }
            if (fuseTicks <= 0) {
                throw new IllegalArgumentException("fuseTicks must be positive");
            }
            if (armingDelayTicks < 0) {
                throw new IllegalArgumentException("armingDelayTicks cannot be negative");
            }
            if (proximityRadiusBlocks < 0.0D) {
                throw new IllegalArgumentException("proximityRadiusBlocks cannot be negative");
            }
            if (impulseHorizontalStrength < 0.0D || impulseVerticalStrength < 0.0D) {
                throw new IllegalArgumentException("impulse strengths cannot be negative");
            }
            if (projectileSpeed <= 0.0F) {
                throw new IllegalArgumentException("projectileSpeed must be positive");
            }
            if (inaccuracy < 0.0F) {
                throw new IllegalArgumentException("inaccuracy cannot be negative");
            }
            evidenceNote = Objects.requireNonNullElse(evidenceNote, "");
        }

        boolean hasImpulseOnly() {
            return weapon.stats().damage() <= 0.0D && (impulseHorizontalStrength > 0.0D || impulseVerticalStrength > 0.0D);
        }
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
