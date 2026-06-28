package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.placement.BuildSupportCascade;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPieceHealthDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class WeaponItem extends SimplePolymerItem {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final String RELOAD_COMPLETE_TICK_KEY = "reload_complete_tick";
    private static final double FORTNITE_TO_MINECRAFT_DAMAGE = 0.2D;
    private static final double ENTITY_HITBOX_INFLATE_BLOCKS = 0.3D;
    private static final double BULLET_TRACE_STEP_BLOCKS = 1.25D;
    private static final int MAX_TRACE_PARTICLES = 64;
    private static BuildWorldState buildWorldState;
    private static WorldBuildMaterializer buildMaterializer;
    private static BuildSupportCascade supportCascade;

    static final ClipContext.Block BULLET_BLOCK_MODE = ClipContext.Block.COLLIDER;

    private final WeaponDefinition definition;
    private final Item clientItem;

    public WeaponItem(WeaponDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public static void configureBuildDamage(BuildWorldState state, WorldBuildMaterializer materializer, BuildRules rules) {
        buildWorldState = Objects.requireNonNull(state, "state");
        buildMaterializer = Objects.requireNonNull(materializer, "materializer");
        supportCascade = new BuildSupportCascade(Objects.requireNonNull(rules, "rules"));
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
        out.set(DataComponents.ITEM_NAME, displayNameComponent());
        out.set(DataComponents.USE_COOLDOWN, cooldownComponent(definition));
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
        int magazine = magazine(stack);
        FireAttempt attempt = fireAttempt(magazine, tick < nextFireTick || player.getCooldowns().isOnCooldown(stack));
        if (attempt == FireAttempt.COOLDOWN) {
            resyncCooldownOverlay(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (attempt == FireAttempt.EMPTY_MAGAZINE) {
            player.sendSystemMessage(Component.literal("Empty " + definition.displayName() + ". Left-click to reload."), true);
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
        } else if (!damageBuild(level, player, trace, magazine)) {
            player.sendSystemMessage(Component.literal("Fired " + definition.displayName()
                    + " (" + magazine + "/" + definition.stats().magazineSize() + ")."), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static InteractionResult handleManualReloadOnSwing(ServerPlayer player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WeaponItem item)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = player.level();
        long tick = level.getGameTime();
        if (player.getCooldowns().isOnCooldown(stack)
                && item.customData(stack).getLongOr(RELOAD_COMPLETE_TICK_KEY, 0L) <= tick) {
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
        int remainingTicks = item.remainingCooldownTicks(stack, player.level().getGameTime());
        if (remainingTicks > 0 && !player.getCooldowns().isOnCooldown(stack)) {
            player.getCooldowns().addCooldown(stack, remainingTicks);
        }
    }

    public static void showHeldStatus(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (showHeldStatus(player, player.getItemInHand(InteractionHand.MAIN_HAND))) {
            return;
        }
        showHeldStatus(player, player.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static boolean showHeldStatus(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem item)) {
            return false;
        }
        player.sendSystemMessage(Component.literal(item.statusText(stack)), true);
        return true;
    }

    String statusText(ItemStack stack) {
        return statusText(magazine(stack));
    }

    String statusText(int magazine) {
        WeaponStats stats = definition.stats();
        return definition.displayName() + ": " + magazine + "/" + stats.magazineSize();
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

    private boolean damageBuild(ServerLevel level, ServerPlayer shooter, ShotTrace trace, int magazine) {
        if (buildWorldState == null || buildMaterializer == null || trace.blockHitPos() == null) {
            return false;
        }
        String dimension = level.dimension().identifier().toString();
        BuildSlot slot = buildMaterializer.topOwnerAt(dimension, trace.blockHitPos());
        if (slot == null) {
            return false;
        }
        int damage = buildDamage();
        BuildWorldState.DamageResult result = buildWorldState.damage(slot, damage, level.getGameTime());
        if (!result.hit()) {
            return false;
        }
        playBuildHitEffects(level, trace.hitLocation());
        if (result.destroyed()) {
            WorldBuildWriteResult clearResult = buildMaterializer.clear(level, result.after());
            if (clearResult.success()) {
                buildWorldState.remove(slot);
                int collapsed = clearUnsupportedBuilds(level);
                shooter.sendSystemMessage(Component.literal("Destroyed " + result.before().material().name().toLowerCase(Locale.ROOT)
                        + " " + result.before().slot().pieceType().name().toLowerCase(Locale.ROOT)
                        + " (" + magazine + "/" + definition.stats().magazineSize() + ")"
                        + collapseMessage(collapsed)), true);
            } else {
                shooter.sendSystemMessage(Component.literal("Build destroyed but world clear failed: " + clearResult.message()), false);
            }
        } else {
            WorldBuildWriteResult refreshResult = buildMaterializer.refresh(level, result.after());
            if (!refreshResult.success()) {
                shooter.sendSystemMessage(Component.literal("Build damaged but world refresh failed: " + refreshResult.message()), false);
            }
            shooter.sendSystemMessage(Component.literal(BuildPieceHealthDisplays.healthText(result.after())
                    + " (" + magazine + "/" + definition.stats().magazineSize() + ")."), true);
        }
        return true;
    }

    private static int clearUnsupportedBuilds(ServerLevel level) {
        if (supportCascade == null) {
            return 0;
        }
        String dimension = level.dimension().identifier().toString();
        WorldObstruction staticWorld = (candidateDimension, x, y, z) -> dimension.equals(candidateDimension)
                && !buildMaterializer.isTrackedBlock(candidateDimension, x, y, z)
                && level.getBlockState(new BlockPos(x, y, z)).isSolid();
        int collapsed = 0;
        for (BuildPieceState unsupported : supportCascade.unsupportedPieces(buildWorldState, dimension, staticWorld)) {
            WorldBuildWriteResult clearResult = buildMaterializer.clear(level, unsupported);
            if (clearResult.success()) {
                buildWorldState.remove(unsupported.slot());
                collapsed++;
            }
        }
        return collapsed;
    }

    private static String collapseMessage(int collapsed) {
        if (collapsed <= 0) {
            return ".";
        }
        return "; collapsed " + collapsed + " unsupported " + (collapsed == 1 ? "piece" : "pieces") + ".";
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
        return (float) Math.max(1.0D, definition.stats().totalDamagePerShot() * FORTNITE_TO_MINECRAFT_DAMAGE);
    }

    int buildDamage() {
        return Math.max(1, (int) Math.round(definition.stats().totalDamagePerShot()));
    }

    private int magazine(ItemStack stack) {
        return customData(stack).getIntOr(MAGAZINE_KEY, definition.stats().magazineSize());
    }

    private void startReload(ItemStack stack, long tick) {
        int reloadTicks = reloadTicks();
        setGunState(stack, definition.stats().magazineSize(), tick + reloadTicks);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(RELOAD_COMPLETE_TICK_KEY, tick + reloadTicks));
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

    private static boolean canShoot(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
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

    private static void playHitEffects(ServerLevel level, ServerPlayer shooter, Vec3 hitLocation, float damage) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7F, 1.35F);
        int damageParticles = Math.max(1, Math.min(12, Math.round(damage)));
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, true, true, hitLocation.x(), hitLocation.y(), hitLocation.z(), damageParticles, 0.2D, 0.3D, 0.2D, 0.05D);
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

    record DamageReport(boolean damaged, float damage) {
    }

    private record ShotTrace(Vec3 start, Vec3 end, LivingEntity target, Vec3 hitLocation, BlockPos blockHitPos) {
    }
}
