package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerAimStates;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPieceHealthDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.Optional;

public final class WeaponItem extends Item {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final String RELOAD_COMPLETE_TICK_KEY = "reload_complete_tick";
    private static final String SHOT_HEAT_KEY = "shot_heat";
    private static final String LAST_SHOT_TICK_KEY = "last_shot_tick";
    private static final double FORTNITE_TO_MINECRAFT_DAMAGE = 0.2D;
    private static final double ENTITY_HITBOX_INFLATE_BLOCKS = 0.3D;
    private static final double BULLET_TRACE_STEP_BLOCKS = 1.25D;
    private static final double MIN_HITSCAN_RANGE_BLOCKS = 512.0D;
    private static final float DEFAULT_ADS_FOV_MULTIPLIER = 0.70F;
    private static final float SCOPED_ADS_FOV_MULTIPLIER = 0.35F;
    private static final int MAX_TRACE_PARTICLES = 64;
    private static final SpreadProfile ASSAULT_RIFLE_SPREAD = new SpreadProfile(0.15D, 0.60D, 0.55D, 0.80D, 1.0D, 1.50D, 1.25D, 0.06D, 0.75D, 0.045D);
    private static final SpreadProfile SHOTGUN_SPREAD = new SpreadProfile(0.90909094D, 0.80D, 0.92D, 0.85D, 1.0D, 1.10D, 1.05D, 0.12D, 1.20D, 0.05D);
    private static final SpreadProfile SMG_SPREAD = new SpreadProfile(0.20D, 0.75D, 0.75D, 0.95D, 1.0D, 1.50D, 1.25D, 0.08D, 0.80D, 0.06D);
    private static final SpreadProfile PISTOL_SPREAD = new SpreadProfile(0.20D, 0.65D, 0.70D, 0.85D, 1.0D, 1.35D, 1.20D, 0.06D, 0.65D, 0.055D);
    private static final SpreadProfile DEFAULT_SPREAD = ASSAULT_RIFLE_SPREAD;
    private static BuildWorldState buildWorldState;
    private static WorldBuildMaterializer buildMaterializer;

    static final ClipContext.Block BULLET_BLOCK_MODE = ClipContext.Block.COLLIDER;

    private final WeaponDefinition definition;

    public WeaponItem(WeaponDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings);
        this.definition = Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(clientItem, "clientItem");
    }

    public static void configureBuildDamage(BuildWorldState state, WorldBuildMaterializer materializer, BuildRules rules) {
        buildWorldState = Objects.requireNonNull(state, "state");
        buildMaterializer = Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(rules, "rules");
    }

    public WeaponDefinition definition() {
        return definition;
    }

    public float adsFovMultiplier() {
        return adsFovMultiplier(definition);
    }

    static float adsFovMultiplier(WeaponDefinition definition) {
        return usesScopedAdsZoom(definition) ? SCOPED_ADS_FOV_MULTIPLIER : DEFAULT_ADS_FOV_MULTIPLIER;
    }

    static boolean usesScopedAdsZoom(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String path = definition.path().toLowerCase(Locale.ROOT);
        String displayName = definition.displayName().toLowerCase(Locale.ROOT);
        return definition.category() == WeaponCategory.SNIPER
                || path.contains("scoped")
                || displayName.contains("scoped")
                || displayName.contains("sniper");
    }

    static UseCooldown cooldownComponent(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(fireDelayTicks(definition) / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayNameComponent();
    }

    private Component displayNameComponent() {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    private static ChatFormatting rarityColor(io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        WeaponStats stats = definition.stats();
        tooltip.accept(Component.literal(definition.category().label() + " / " + definition.rarity().label()));
        tooltip.accept(Component.literal("Damage: " + format(stats.damage())
                + (stats.pellets() > 1 ? " x" + stats.pellets() : "")));
        tooltip.accept(Component.literal("Fire rate: " + format(stats.fireRatePerSecond()) + "/s; magazine: " + stats.magazineSize()));
        tooltip.accept(Component.literal("Reload: " + format(stats.reloadSeconds()) + "s; crit: " + format(stats.criticalMultiplier()) + "x"));
        tooltip.accept(Component.literal("Source: " + definition.sourceStatRow()));
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
        boolean infiniteAmmo = hasInfiniteAmmo(player);
        int magazineSize = definition.stats().magazineSize();
        if (infiniteAmmo && magazine < magazineSize) {
            setGunState(stack, magazineSize, tick);
            magazine = magazineSize;
        }
        FireAttempt attempt = fireAttempt(magazine, tick < nextFireTick || player.getCooldowns().isOnCooldown(stack));
        if (attempt == FireAttempt.COOLDOWN) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (attempt == FireAttempt.EMPTY_MAGAZINE) {
            if (startReloadAndNotify(level, player, stack, tick)) {
                return InteractionResult.SUCCESS_SERVER;
            }
            player.sendSystemMessage(Component.literal("Empty " + definition.displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        int magazineAfterShot = infiniteAmmo ? magazine : magazine - 1;
        int fireDelayTicks = fireDelayTicks();
        setGunState(stack, magazineAfterShot, tick + fireDelayTicks);
        player.getCooldowns().addCooldown(stack, fireDelayTicks);

        double heat = shotHeat(stack, tick);
        ShotTrace trace = traceShot(level, player, definition, heat);
        recordShotHeat(stack, tick, heat);
        playShotEffects(level, player, trace);
        boolean autoReloadStarted = shouldAutoReload(magazineAfterShot, infiniteAmmo)
                && startReloadAndNotify(level, player, stack, tick);
        if (trace.target() != null) {
            boolean headshot = isHeadshot(trace.target(), trace.hitLocation());
            DamageReport report = damageTarget(level, player, trace.target(), headshot);
            playHitEffects(level, player, trace.hitLocation(), report.damage(), headshot);
            HitMarkerDisplays.show(level, player, trace.target(), report.damage(), headshot, report.shielded());
        } else if (!damageBuild(level, player, trace, magazineAfterShot)) {
            player.sendSystemMessage(Component.literal(autoReloadStarted
                    ? statusText(stack, PlayerAimStates.isAiming(player))
                    : statusText(magazineAfterShot, PlayerAimStates.isAiming(player))), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult handleManualReload(ServerPlayer player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WeaponItem item)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = player.level();
        long tick = level.getGameTime();
        item.completeReloadIfReady(stack, tick);
        if (hasInfiniteAmmo(player)) {
            item.setGunState(stack, item.definition.stats().magazineSize(), tick);
            player.sendSystemMessage(Component.literal(item.statusText(stack, PlayerAimStates.isAiming(player))), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player.getCooldowns().isOnCooldown(stack) && item.isReloading(stack, tick)) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        ManualReloadResult result = item.tryStartManualReloadOnGun(stack, tick);
        if (result == ManualReloadResult.STARTED) {
            player.getCooldowns().addCooldown(stack, item.remainingCooldownTicks(stack, tick));
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + item.definition.displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (result == ManualReloadResult.ALREADY_RELOADING) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    static ManualReloadResult tryStartManualReload(ItemStack stack, long tick) {
        Objects.requireNonNull(stack, "stack");
        if (!(stack.getItem() instanceof WeaponItem item)) {
            return ManualReloadResult.NOT_WEAPON;
        }
        return item.tryStartManualReloadOnGun(stack, tick);
    }

    public static void resyncCooldownOverlay(ServerPlayer player, ItemStack stack) {
        Objects.requireNonNull(player, "player");
        if (!(stack.getItem() instanceof WeaponItem item)) {
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
            if (stack.getItem() instanceof WeaponItem item) {
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
        if (!(stack.getItem() instanceof WeaponItem item)) {
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

    String statusText(int magazine) {
        return statusText(magazine, false);
    }

    String statusText(int magazine, boolean aiming) {
        WeaponStats stats = definition.stats();
        return definition.displayName() + (aiming ? " ADS" : "") + ": " + magazine + "/" + stats.magazineSize();
    }

    private DamageReport damageTarget(ServerLevel level, ServerPlayer shooter, LivingEntity target, boolean headshot) {
        float shieldBeforeHit = target.getAbsorptionAmount();
        float before = target.getHealth() + shieldBeforeHit;
        Vec3 velocityBeforeHit = target.getDeltaMovement();
        prepareBulletHit(target);
        boolean damaged = target.hurtServer(level, shooter.damageSources().playerAttack(shooter), minecraftDamage(headshot));
        if (damaged) {
            reduceBulletInvulnerability(target);
            if (CombatSettings.preventBulletKnockback() && !(target instanceof ServerPlayer)) {
                target.setDeltaMovement(velocityBeforeHit);
                target.hurtMarked = false;
            }
        }
        float after = Math.max(0.0F, target.getHealth()) + target.getAbsorptionAmount();
        return new DamageReport(Math.max(0.0F, before - after), shieldBeforeHit > 0.0F);
    }

    private boolean damageBuild(ServerLevel level, ServerPlayer shooter, ShotTrace trace, int magazine) {
        if (trace.blockHitPos() == null) {
            return false;
        }
        String statusSuffix = " (" + magazine + "/" + definition.stats().magazineSize() + ")";
        return damageBuild(level, shooter, trace.blockHitPos(), trace.hitLocation(), buildDamage(), statusSuffix);
    }

    static boolean damageBuild(
            ServerLevel level,
            ServerPlayer shooter,
            BlockPos hitPos,
            Vec3 hitLocation,
            int damage,
            String statusSuffix
    ) {
        if (buildWorldState == null || buildMaterializer == null) {
            return false;
        }
        String dimension = level.dimension().identifier().toString();
        BuildSlot slot = buildMaterializer.topOwnerAt(dimension, hitPos);
        if (slot == null) {
            return false;
        }
        BuildWeakPoints.Damage weakPointDamage = BuildWeakPoints.damageForHit(level, slot, hitLocation, damage);
        BuildWorldState.DamageResult result = buildWorldState.damage(slot, weakPointDamage.amount(), level.getGameTime());
        if (!result.hit()) {
            return false;
        }
        playBuildHitEffects(level, hitLocation);
        if (result.destroyed()) {
            WorldBuildWriteResult clearResult = buildMaterializer.clear(level, result.after());
            if (clearResult.success()) {
                buildWorldState.remove(slot);
                BuildWeakPoints.clear(slot);
                int collapsed = BuildCollapseScheduler.scheduleAfterSupportRemoved(level, slot, level.getGameTime());
                shooter.sendSystemMessage(Component.literal("Destroyed " + result.before().material().name().toLowerCase(Locale.ROOT)
                        + " " + result.before().slot().pieceType().name().toLowerCase(Locale.ROOT)
                        + statusSuffix
                        + collapseMessage(collapsed)), true);
            } else {
                FortniteInMinecraft.LOGGER.warn("Build clear failed after weapon damage at {}: {}", hitPos, clearResult.message());
            }
        } else {
            WorldBuildWriteResult refreshResult = buildMaterializer.refresh(level, result.after());
            if (!refreshResult.success()) {
                FortniteInMinecraft.LOGGER.warn("Build refresh failed after weapon damage at {}: {}", hitPos, refreshResult.message());
            }
            shooter.sendSystemMessage(Component.literal(BuildPieceHealthDisplays.healthText(result.after())
                    + statusSuffix + "."), true);
        }
        return true;
    }


    private static String collapseMessage(int collapsed) {
        if (collapsed <= 0) {
            return ".";
        }
        return "; destabilized " + collapsed + " unsupported " + (collapsed == 1 ? "piece" : "pieces") + ".";
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

    private float minecraftDamage(boolean headshot) {
        return minecraftDamage(definition, headshot);
    }

    static float minecraftDamage(WeaponDefinition definition) {
        return minecraftDamage(definition, false);
    }

    static float minecraftDamage(WeaponDefinition definition, boolean headshot) {
        Objects.requireNonNull(definition, "definition");
        WeaponStats stats = definition.stats();
        double headshotMultiplier = headshot ? stats.criticalMultiplier() : 1.0D;
        return (float) Math.max(1.0D, stats.totalDamagePerShot() * headshotMultiplier * FORTNITE_TO_MINECRAFT_DAMAGE);
    }

    int buildDamage() {
        return buildDamage(definition);
    }

    static int buildDamage(WeaponDefinition definition) {
        return Math.max(1, (int) Math.round(definition.stats().totalDamagePerShot()));
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
        if (hasInfiniteAmmo(player)) {
            setGunState(stack, definition.stats().magazineSize(), tick);
            return false;
        }
        ManualReloadResult result = tryStartManualReloadOnGun(stack, tick);
        if (result == ManualReloadResult.STARTED) {
            player.getCooldowns().addCooldown(stack, remainingCooldownTicks(stack, tick));
            playReloadSound(level, player);
            player.sendSystemMessage(Component.literal("Reloading " + definition.displayName() + "."), true);
            return true;
        }
        if (result == ManualReloadResult.ALREADY_RELOADING) {
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

    private ManualReloadResult tryStartManualReloadOnGun(ItemStack stack, long tick) {
        ManualReloadResult result = manualReloadDecision(
                magazine(stack),
                definition.stats().magazineSize(),
                customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L),
                tick
        );
        if (result == ManualReloadResult.STARTED) {
            startReload(stack, tick);
        }
        return result;
    }

    static ManualReloadResult manualReloadDecision(int magazine, int magazineSize, long reloadCompleteTick, long tick) {
        if (reloadCompleteTick > tick) {
            return ManualReloadResult.ALREADY_RELOADING;
        }
        if (magazine >= magazineSize) {
            return ManualReloadResult.FULL_MAGAZINE;
        }
        return ManualReloadResult.STARTED;
    }

    static FireAttempt fireAttempt(int magazine, boolean coolingDown) {
        if (coolingDown) {
            return FireAttempt.COOLDOWN;
        }
        return magazine <= 0 ? FireAttempt.EMPTY_MAGAZINE : FireAttempt.FIRE;
    }

    static boolean shouldAutoReload(int magazine, boolean infiniteAmmo) {
        return !infiniteAmmo && magazine <= 0;
    }

    public static boolean hasInfiniteAmmo(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return PlayerResourceStates.stateFor(player).infiniteAmmo();
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

    private double shotHeat(ItemStack stack, long tick) {
        SpreadProfile profile = spreadProfile(definition);
        CompoundTag data = customData(stack);
        double heat = data.getDoubleOr(SHOT_HEAT_KEY, 0.0D);
        long lastShotTick = data.getLongOr(LAST_SHOT_TICK_KEY, tick);
        long elapsed = Math.max(0L, tick - lastShotTick);
        return Math.max(0.0D, heat - elapsed * profile.coolPerTick());
    }

    private void recordShotHeat(ItemStack stack, long tick, double previousHeat) {
        SpreadProfile profile = spreadProfile(definition);
        double heat = Math.min(profile.maxHeat(), previousHeat + profile.heatPerShot());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble(SHOT_HEAT_KEY, heat);
            tag.putLong(LAST_SHOT_TICK_KEY, tick);
        });
    }

    static double effectiveHitscanRange(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return Math.max(MIN_HITSCAN_RANGE_BLOCKS, definition.stats().rangeBlocks());
    }

    private static ShotTrace traceShot(ServerLevel level, ServerPlayer player, WeaponDefinition definition, double heat) {
        Vec3 start = player.getEyePosition();
        Vec3 look = applySpread(player, player.getLookAngle(), spreadDegrees(definition, spreadState(player, heat)));
        double rangeBlocks = effectiveHitscanRange(definition);
        Vec3 rayEnd = start.add(look.scale(rangeBlocks));
        BlockHitResult blockHit = null;
        HitResult hitResult = level.clip(new ClipContext(start, rayEnd, BULLET_BLOCK_MODE, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() != HitResult.Type.MISS && hitResult instanceof BlockHitResult hitBlock) {
            blockHit = hitBlock;
            rayEnd = hitBlock.getLocation();
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
        return new ShotTrace(
                start,
                closest == null ? rayEnd : closestHit,
                closest,
                closest == null ? rayEnd : closestHit,
                blockHit == null ? null : blockHit.getBlockPos()
        );
    }

    private static SpreadState spreadState(ServerPlayer player, double heat) {
        Vec3 movement = player.getDeltaMovement();
        boolean moving = movement.x() * movement.x() + movement.z() * movement.z() > 0.0036D;
        return new SpreadState(
                PlayerAimStates.isAiming(player),
                player.isCrouching(),
                player.isSprinting(),
                !player.onGround(),
                moving,
                heat
        );
    }

    static double spreadDegrees(WeaponDefinition definition, SpreadState state) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(state, "state");
        SpreadProfile profile = spreadProfile(definition);
        double spread = profile.baseSpread();
        if (state.aiming()) {
            spread *= profile.adsMultiplier();
            if (usesScopedAdsZoom(definition)) {
                spread *= 0.35D;
            }
        }
        if (state.airborne()) {
            spread *= profile.airborneMultiplier();
        } else if (state.sprinting()) {
            spread *= profile.sprintingMultiplier();
        } else if (state.crouching()) {
            spread *= profile.crouchingMultiplier();
        } else if (state.moving()) {
            spread *= profile.movingMultiplier();
        } else {
            spread *= profile.standingStillMultiplier();
        }
        return Math.max(0.0D, spread + Math.max(0.0D, state.heat()));
    }

    private static SpreadProfile spreadProfile(WeaponDefinition definition) {
        return switch (definition.category()) {
            case ASSAULT_RIFLE -> ASSAULT_RIFLE_SPREAD;
            case SHOTGUN -> SHOTGUN_SPREAD;
            case SMG -> SMG_SPREAD;
            case PISTOL -> PISTOL_SPREAD;
            default -> DEFAULT_SPREAD;
        };
    }

    private static Vec3 applySpread(ServerPlayer player, Vec3 look, double spreadDegrees) {
        Vec3 forward = look.normalize();
        if (spreadDegrees <= 1.0E-6D) {
            return forward;
        }
        RandomSource random = player.getRandom();
        double cone = Math.tan(Math.toRadians(spreadDegrees));
        Vec3 right = forward.cross(Vec3.Y_AXIS);
        if (right.lengthSqr() < 1.0E-6D) {
            right = Vec3.X_AXIS;
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        double horizontal = (random.nextDouble() * 2.0D - 1.0D) * cone;
        double vertical = (random.nextDouble() * 2.0D - 1.0D) * cone;
        return forward.add(right.scale(horizontal)).add(up.scale(vertical)).normalize();
    }

    private static boolean canShoot(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
    }

    static boolean isHeadshot(LivingEntity target, Vec3 hitLocation) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hitLocation, "hitLocation");
        return isHeadshot(hitLocation.y(), target.getEyeY());
    }

    static boolean isHeadshot(double hitY, double eyeY) {
        return hitY >= eyeY - 0.15D;
    }

    private static void playShotEffects(ServerLevel level, ServerPlayer shooter, ShotTrace trace) {
        Vec3 muzzle = muzzlePosition(shooter, trace.start());
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9F, 1.35F);
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

    static void playHitEffects(ServerLevel level, ServerPlayer shooter, Vec3 hitLocation, float damage) {
        playHitEffects(level, shooter, hitLocation, damage, false);
    }

    static void playHitEffects(ServerLevel level, ServerPlayer shooter, Vec3 hitLocation, float damage, boolean headshot) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), hitSound(headshot), SoundSource.PLAYERS, 0.7F, hitSoundPitch(headshot));
        int damageParticles = Math.max(1, Math.min(12, Math.round(damage)));
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, true, true, hitLocation.x(), hitLocation.y(), hitLocation.z(), damageParticles, 0.2D, 0.3D, 0.2D, 0.05D);
    }

    static SoundEvent hitSound(boolean headshot) {
        return headshot ? SoundEvents.PLAYER_LEVELUP : SoundEvents.PLAYER_ATTACK_CRIT;
    }

    static float hitSoundPitch(boolean headshot) {
        return headshot ? 1.65F : 1.35F;
    }

    private static void playBuildHitEffects(ServerLevel level, Vec3 hitLocation) {
        level.playSound(null, hitLocation.x(), hitLocation.y(), hitLocation.z(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 0.9F, 1.0F);
        level.sendParticles(ParticleTypes.CRIT, true, true, hitLocation.x(), hitLocation.y(), hitLocation.z(), 6, 0.18D, 0.18D, 0.18D, 0.03D);
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

    enum ManualReloadResult {
        STARTED,
        FULL_MAGAZINE,
        ALREADY_RELOADING,
        NOT_WEAPON
    }

    enum FireAttempt {
        FIRE,
        EMPTY_MAGAZINE,
        COOLDOWN
    }

    record SpreadState(boolean aiming, boolean crouching, boolean sprinting, boolean airborne, boolean moving, double heat) {
    }

    private record SpreadProfile(
            double baseSpread,
            double adsMultiplier,
            double standingStillMultiplier,
            double crouchingMultiplier,
            double movingMultiplier,
            double sprintingMultiplier,
            double airborneMultiplier,
            double heatPerShot,
            double maxHeat,
            double coolPerTick
    ) {
    }

    record DamageReport(float damage, boolean shielded) {
    }

    private record ShotTrace(Vec3 start, Vec3 end, LivingEntity target, Vec3 hitLocation, BlockPos blockHitPos) {
    }
}
