package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerAimStates;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;
import java.util.Objects;

import java.util.Optional;

public final class ProjectileWeaponItem extends Item {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final String RELOAD_COMPLETE_TICK_KEY = "reload_complete_tick";


    private final WeaponDefinition definition;
    private final float projectileSpeed;
    private final float inaccuracy;

    public ProjectileWeaponItem(
            WeaponDefinition definition,
            Item.Properties settings,
            Item clientItem,
            float projectileSpeed,
            float inaccuracy
    ) {
        super(ItemTooltips.withLore(settings,
                Component.literal(definition.category().label() + " / " + definition.rarity().label()),
                Component.literal("Projectile damage: " + format(definition.stats().damage())
                        + "; speed: " + format(projectileSpeed)),
                Component.literal("Reload/cooldown: " + format(definition.stats().reloadSeconds()) + "s; crit: "
                        + format(definition.stats().criticalMultiplier()) + "x"),
                Component.literal("Hold right-click before firing to steady the scope."),
                Component.literal("Source: " + definition.sourceStatRow())
        ));
        this.definition = Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(clientItem, "clientItem");
        if (projectileSpeed <= 0.0F) {
            throw new IllegalArgumentException("projectileSpeed must be positive");
        }
        if (inaccuracy < 0.0F) {
            throw new IllegalArgumentException("inaccuracy cannot be negative");
        }
        this.projectileSpeed = projectileSpeed;
        this.inaccuracy = inaccuracy;
    }

    public WeaponDefinition definition() {
        return definition;
    }

    public float projectileSpeed() {
        return projectileSpeed;
    }

    public float adsFovMultiplier() {
        return WeaponItem.adsFovMultiplier(definition);
    }

    static UseCooldown cooldownComponent(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(cooldownTicks(definition) / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayNameComponent();
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

    public InteractionResult fireFromHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand, boolean scoped) {
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
        int magazine = magazine(stack);
        boolean infiniteAmmo = WeaponItem.hasInfiniteAmmo(player);
        int magazineSize = definition.stats().magazineSize();
        if (infiniteAmmo && magazine < magazineSize) {
            setGunState(stack, magazineSize, tick);
            magazine = magazineSize;
        }
        WeaponItem.FireAttempt attempt = WeaponItem.fireAttempt(magazine, tick < nextFireTick || player.getCooldowns().isOnCooldown(stack));
        if (attempt == WeaponItem.FireAttempt.COOLDOWN) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (attempt == WeaponItem.FireAttempt.EMPTY_MAGAZINE) {
            if (startReloadAndNotify(level, player, stack, tick)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            player.sendSystemMessage(Component.literal("Empty " + definition.displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        int magazineAfterShot = infiniteAmmo ? magazine : magazine - 1;
        int fireDelayTicks = WeaponItem.fireDelayTicks(definition);
        setGunState(stack, magazineAfterShot, tick + fireDelayTicks);
        player.getCooldowns().addCooldown(stack, fireDelayTicks);

        fireArrow(level, player, stack, scoped, magazineAfterShot);
        boolean autoReloadStarted = WeaponItem.shouldAutoReload(magazineAfterShot, infiniteAmmo)
                && startReloadAndNotify(level, player, stack, tick);
        player.sendSystemMessage(Component.literal(autoReloadStarted
                ? statusText(stack, scoped)
                : statusText(magazineAfterShot, scoped)), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void fireArrow(ServerLevel level, ServerPlayer shooter, ItemStack weaponStack, boolean scoped, int magazineAfterShot) {
        FeedbackArrow arrow = new FeedbackArrow(level, shooter, weaponStack, definition, statusSuffix(magazineAfterShot));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        Vec3 look = shooter.getLookAngle();
        arrow.shoot(look.x(), look.y(), look.z(), projectileSpeed, scopedInaccuracy(inaccuracy, scoped));
        level.addFreshEntity(arrow);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
        Vec3 muzzle = WeaponItem.muzzlePosition(shooter, shooter.getEyePosition());
        level.sendParticles(ParticleTypes.CRIT, true, true, muzzle.x(), muzzle.y(), muzzle.z(), 4, 0.08D, 0.08D, 0.08D, 0.0D);
    }

    public static InteractionResult handleManualReload(ServerPlayer player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ProjectileWeaponItem item)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = player.level();
        long tick = level.getGameTime();
        item.completeReloadIfReady(stack, tick);
        if (WeaponItem.hasInfiniteAmmo(player)) {
            item.setGunState(stack, item.definition.stats().magazineSize(), tick);
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
            player.sendSystemMessage(Component.literal("Reloading " + item.definition.displayName() + "."), true);
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
        if (!(stack.getItem() instanceof ProjectileWeaponItem item)) {
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
            if (stack.getItem() instanceof ProjectileWeaponItem item) {
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
        if (!(stack.getItem() instanceof ProjectileWeaponItem item)) {
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
            return definition.displayName() + (aiming ? " ADS" : "") + ": " + magazine + "/" + definition.stats().magazineSize() + " (reloading)";
        }
        return statusText(magazine, aiming);
    }

    String statusText(int magazine, boolean aiming) {
        return definition.displayName() + (aiming ? " ADS" : "") + ": " + magazine + "/" + definition.stats().magazineSize();
    }

    private String statusSuffix(int magazine) {
        return " (" + magazine + "/" + definition.stats().magazineSize() + ")";
    }

    private static DamageReport damageTarget(ServerLevel level, ServerPlayer shooter, LivingEntity target, WeaponDefinition definition, boolean headshot) {
        float shieldBeforeHit = target.getAbsorptionAmount();
        float before = target.getHealth() + shieldBeforeHit;
        Vec3 velocityBeforeHit = target.getDeltaMovement();
        WeaponItem.prepareBulletHit(target);
        boolean damaged = target.hurtServer(level, shooter.damageSources().playerAttack(shooter), WeaponItem.minecraftDamage(definition, headshot));
        if (damaged) {
            WeaponItem.reduceBulletInvulnerability(target);
            if (CombatSettings.preventBulletKnockback() && !(target instanceof ServerPlayer)) {
                target.setDeltaMovement(velocityBeforeHit);
                target.hurtMarked = false;
            }
        }
        float after = Math.max(0.0F, target.getHealth()) + target.getAbsorptionAmount();
        return new DamageReport(Math.max(0.0F, before - after), shieldBeforeHit > 0.0F);
    }

    private static final class FeedbackArrow extends Arrow {
        private final WeaponDefinition definition;
        private final String statusSuffix;

        private FeedbackArrow(ServerLevel level, ServerPlayer shooter, ItemStack weaponStack, WeaponDefinition definition, String statusSuffix) {
            super(level, shooter, new ItemStack(Items.ARROW), weaponStack);
            this.definition = Objects.requireNonNull(definition, "definition");
            this.statusSuffix = Objects.requireNonNull(statusSuffix, "statusSuffix");
        }

        @Override
        protected boolean canHitEntity(Entity entity) {
            return entity instanceof LivingEntity living
                    && living.isAlive()
                    && living.isPickable()
                    && !living.isSpectator()
                    && super.canHitEntity(entity);
        }

        @Override
        protected void onHitEntity(EntityHitResult result) {
            Entity hit = result.getEntity();
            if (!(level() instanceof ServerLevel level)
                    || !(getOwner() instanceof ServerPlayer shooter)
                    || !(hit instanceof LivingEntity target)) {
                super.onHitEntity(result);
                return;
            }

            boolean headshot = WeaponItem.isHeadshot(target, result.getLocation());
            DamageReport report = damageTarget(level, shooter, target, definition, headshot);
            WeaponItem.playHitEffects(level, shooter, result.getLocation(), report.damage(), headshot);
            HitMarkerDisplays.show(level, shooter, target, report.damage(), headshot, report.shielded());
            discard();
        }

        @Override
        protected void onHitBlock(BlockHitResult result) {
            super.onHitBlock(result);
            if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer shooter) {
                WeaponItem.damageBuild(
                        level,
                        shooter,
                        result.getBlockPos(),
                        result.getLocation(),
                        WeaponItem.buildDamage(definition),
                        statusSuffix
                );
            }
        }
    }

    private record DamageReport(float damage, boolean shielded) {
    }

    private int magazine(ItemStack stack) {
        return customData(stack).getIntOr(MAGAZINE_KEY, definition.stats().magazineSize());
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
            setGunState(stack, definition.stats().magazineSize(), tick);
            return false;
        }
        WeaponItem.ManualReloadResult result = tryStartManualReloadOnGun(stack, tick);
        if (result == WeaponItem.ManualReloadResult.STARTED) {
            player.getCooldowns().addCooldown(stack, remainingCooldownTicks(stack, tick));
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + definition.displayName() + "."), true);
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
        setGunState(stack, definition.stats().magazineSize(), tick);
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
                definition.stats().magazineSize(),
                customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L),
                tick
        );
        if (result == WeaponItem.ManualReloadResult.STARTED) {
            startReload(stack, tick);
        }
        return result;
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
        return Math.max(1, (int) Math.round(definition.stats().reloadSeconds() * 20.0D));
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

    static int cooldownTicks(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        int fireTicks = WeaponItem.fireDelayTicks(definition);
        int reloadTicks = Math.max(1, (int) Math.round(definition.stats().reloadSeconds() * 20.0D));
        return Math.max(fireTicks, reloadTicks);
    }

    static float minecraftDamage(WeaponDefinition definition) {
        return minecraftDamage(definition, false);
    }

    static float minecraftDamage(WeaponDefinition definition, boolean headshot) {
        return WeaponItem.minecraftDamage(definition, headshot);
    }

    static float scopedInaccuracy(float baseInaccuracy, boolean scoped) {
        if (baseInaccuracy < 0.0F) {
            throw new IllegalArgumentException("baseInaccuracy cannot be negative");
        }
        return scoped ? baseInaccuracy * 0.25F : baseInaccuracy;
    }

    private Component displayNameComponent() {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
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
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
