package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


public final class ExplosiveThrowableItem extends Item {
    private static final ArrayList<ActiveThrowable> ACTIVE_THROWABLES = new ArrayList<>();
    private static final EntityType<Display.ItemDisplay> ITEM_DISPLAY_TYPE = itemDisplayType();
    private static boolean tickRegistered;

    private final Definition definition;
    private final Item clientItem;

    public ExplosiveThrowableItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(ItemTooltips.withLore(settings,
                Component.literal("Throwable / " + definition.rarity().label()),
                Component.literal("Sticky explosive damage: " + format(definition.damage())
                        + "; radius: " + format(definition.explosionRadiusBlocks()) + " blocks"),
                Component.literal("Sticks to the first impact, then detonates after "
                        + format(definition.stickDelayTicks() / 20.0D) + "s"),
                Component.literal("Source: " + definition.sourceItemId())
        ));
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
        registerTicker();
    }

    public Definition definition() {
        return definition;
    }

    static UseCooldown cooldownComponent(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(definition.cooldownTicks() / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack projectileStack = new ItemStack(clientItem);
        TrackedExplosiveThrowable projectile = new TrackedExplosiveThrowable(serverLevel, player, projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, definition.throwPower(), definition.throwInaccuracy());
        if (!serverLevel.addFreshEntity(projectile)) {
            return InteractionResult.FAIL;
        }

        ActiveThrowable active = new ActiveThrowable(projectile, serverLevel.dimension(), serverLevel.getGameTime(), definition,
                player.getUUID(), clientItem);
        projectile.setActiveThrowable(active);
        ACTIVE_THROWABLES.add(active);
        player.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void registerTicker() {
        if (tickRegistered) {
            return;
        }
        FortniteInMinecraft.platform().registerEndLevelTick(ExplosiveThrowableItem::tickLevel);
        tickRegistered = true;
    }

    private static void tickLevel(ServerLevel level) {
        if (ACTIVE_THROWABLES.isEmpty()) {
            return;
        }
        Iterator<ActiveThrowable> iterator = ACTIVE_THROWABLES.iterator();
        while (iterator.hasNext()) {
            ActiveThrowable active = iterator.next();
            if (!active.dimension().equals(level.dimension())) {
                continue;
            }
            if (active.exploded()) {
                iterator.remove();
                continue;
            }

            Snowball projectile = active.projectile();
            if (active.stuck()) {
                if (level.getGameTime() >= active.detonationTick()) {
                    active.explode(level, active.position(), projectile);
                    iterator.remove();
                } else {
                    level.sendParticles(ParticleTypes.SMOKE, true, true,
                            active.position().x(), active.position().y(), active.position().z(),
                            1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
                continue;
            }
            if (projectile.isRemoved() || !projectile.isAlive()
                    || level.getGameTime() - active.spawnTick() >= active.definition().fuseTicks()) {
                active.stick(level, level.getGameTime(), projectile.position(), null);
            }
        }
    }

    private static void detonate(ServerLevel level, Vec3 origin, BlockPos hitBlockPos, Definition definition, Entity source, UUID ownerId) {
        Player owner = level.getPlayerByUUID(ownerId);
        ServerPlayer thrower = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        level.playSound(null, origin.x(), origin.y(), origin.z(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, true, true, origin.x(), origin.y(), origin.z(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, true, true, origin.x(), origin.y(), origin.z(),
                18, definition.explosionRadiusBlocks() * 0.25D, definition.explosionRadiusBlocks() * 0.16D,
                definition.explosionRadiusBlocks() * 0.25D, 0.06D);

        if (hitBlockPos != null && thrower != null && definition.environmentDamage() > 0) {
            WeaponItem.damageBuild(level, thrower, hitBlockPos, origin, definition.environmentDamage(), "");
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
            if (thrower == null) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;
            Vec3 center = target.getBoundingBox().getCenter();
            if (center.distanceTo(origin) > definition.explosionRadiusBlocks()) {
                continue;
            }
            float shieldBeforeHit = target.getAbsorptionAmount();
            float before = target.getHealth() + shieldBeforeHit;
            Vec3 velocityBeforeHit = target.getDeltaMovement();
            WeaponItem.prepareBulletHit(target);
            boolean damaged = target.hurtServer(level, thrower.damageSources().playerAttack(thrower), minecraftDamage(definition.damage()));
            if (damaged) {
                WeaponItem.reduceBulletInvulnerability(target);
                if (CombatSettings.preventBulletKnockback() && !(target instanceof ServerPlayer)) {
                    target.setDeltaMovement(velocityBeforeHit);
                    target.hurtMarked = false;
                }
                float after = Math.max(0.0F, target.getHealth()) + target.getAbsorptionAmount();
                float dealt = Math.max(0.0F, before - after);
                WeaponItem.playHitEffects(level, thrower, center, dealt);
                HitMarkerDisplays.show(level, thrower, target, dealt, false, shieldBeforeHit > 0.0F);
            }
        }
    }

    private static float minecraftDamage(double fortniteDamage) {
        return (float) Math.max(1.0D, fortniteDamage * 0.2D);
    }

    private static final class TrackedExplosiveThrowable extends Snowball {
        private ActiveThrowable activeThrowable;

        private TrackedExplosiveThrowable(Level level, Player owner, ItemStack stack) {
            super(level, owner, stack);
        }

        private void setActiveThrowable(ActiveThrowable activeThrowable) {
            this.activeThrowable = Objects.requireNonNull(activeThrowable, "activeThrowable");
        }

        @Override
        protected void onHit(HitResult result) {
            if (!level().isClientSide()
                    && level() instanceof ServerLevel serverLevel
                    && activeThrowable != null
                    && !activeThrowable.stuck()) {
                BlockPos block = result instanceof BlockHitResult blockHit && result.getType() != HitResult.Type.MISS
                        ? blockHit.getBlockPos()
                        : null;
                activeThrowable.stick(serverLevel, serverLevel.getGameTime(), result.getLocation(), block);
            }
            super.onHit(result);
        }
    }

    private static final class ActiveThrowable {
        private final Snowball projectile;
        private final ResourceKey<Level> dimension;
        private final long spawnTick;
        private final Definition definition;
        private final UUID ownerId;
        private final Item stuckItem;
        private Vec3 position;
        private BlockPos hitBlock;
        private long detonationTick = -1L;
        private Display.ItemDisplay stuckDisplay;
        private boolean exploded;

        private ActiveThrowable(Snowball projectile, ResourceKey<Level> dimension, long spawnTick, Definition definition, UUID ownerId, Item stuckItem) {
            this.projectile = Objects.requireNonNull(projectile, "projectile");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.spawnTick = spawnTick;
            this.definition = Objects.requireNonNull(definition, "definition");
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
            this.stuckItem = Objects.requireNonNull(stuckItem, "stuckItem");
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
            return position != null;
        }

        private Vec3 position() {
            return position;
        }

        private long detonationTick() {
            return detonationTick;
        }

        private boolean exploded() {
            return exploded;
        }

        private void stick(ServerLevel level, long tick, Vec3 impact, BlockPos block) {
            if (position != null) {
                return;
            }
            position = Objects.requireNonNull(impact, "impact");
            hitBlock = block == null ? null : block.immutable();
            detonationTick = tick + definition.stickDelayTicks();
            stuckDisplay = new Display.ItemDisplay(ITEM_DISPLAY_TYPE, level);
            stuckDisplay.setNoGravity(true);
            stuckDisplay.setPos(position.x(), position.y(), position.z());
            stuckDisplay.getSlot(0).set(new ItemStack(stuckItem));
            if (!level.addFreshEntity(stuckDisplay)) {
                stuckDisplay.discard();
                stuckDisplay = null;
            }
        }

        private void explode(ServerLevel level, Vec3 origin, Entity source) {
            if (exploded) {
                return;
            }
            exploded = true;
            if (stuckDisplay != null) {
                stuckDisplay.discard();
                stuckDisplay = null;
            }
            detonate(level, origin, hitBlock, definition, source, ownerId);
            projectile.discard();
        }
    }

    public record Definition(
            String path,
            String displayName,
            FortniteRarity rarity,
            String sourceItemId,
            double damage,
            int environmentDamage,
            double explosionRadiusBlocks,
            int fuseTicks,
            int stickDelayTicks,
            int cooldownTicks,
            float throwPower,
            float throwInaccuracy
    ) {
        public Definition {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(rarity, "rarity");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            if (damage < 0.0D) {
                throw new IllegalArgumentException("damage cannot be negative");
            }
            if (environmentDamage < 0) {
                throw new IllegalArgumentException("environmentDamage cannot be negative");
            }
            if (explosionRadiusBlocks <= 0.0D) {
                throw new IllegalArgumentException("explosionRadiusBlocks must be positive");
            }
            if (fuseTicks <= 0 || stickDelayTicks <= 0) {
                throw new IllegalArgumentException("fuse and stick delay must be positive");
            }
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks cannot be negative");
            }
            if (throwPower <= 0.0F) {
                throw new IllegalArgumentException("throwPower must be positive");
            }
            if (throwInaccuracy < 0.0F) {
                throw new IllegalArgumentException("throwInaccuracy cannot be negative");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.ItemDisplay> itemDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:item_display entity type");
        }
        return (EntityType<Display.ItemDisplay>) type;
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

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-4D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
