package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.Optional;

public final class ThrowableImpulseItem extends Item {
    private static final float THROW_POWER = 1.5F;
    private static final float THROW_INACCURACY = 1.0F;
    private static final long IMPACT_EXPLOSION_DELAY_TICKS = 10L;
    private static final ArrayList<ActiveGrenade> ACTIVE_GRENADES = new ArrayList<>();
    private static final EntityType<Display.ItemDisplay> ITEM_DISPLAY_TYPE = itemDisplayType();
    private static boolean tickRegistered;

    private final Definition definition;
    private final Item clientItem;

    public ThrowableImpulseItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(settings);
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
        return displayNameComponent();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Radius: " + format(definition.radius()) + " blocks"));
        tooltip.accept(Component.literal("Impulse: " + format(definition.horizontalStrength())
                + " horizontal / " + format(definition.verticalStrength()) + " vertical"));
        tooltip.accept(Component.literal("Fuse: " + format(definition.fuseTicks() / 20.0D) + "s; cooldown: "
                + format(definition.cooldownTicks() / 20.0D) + "s"));
        tooltip.accept(Component.literal("Sticks for 0.5s after impact before exploding"));
        if (definition.resetsFallDistance()) {
            tooltip.accept(Component.literal("Cancels fall distance on launch"));
        }
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
        TrackedImpulseSnowball projectile = new TrackedImpulseSnowball(serverLevel, player, projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, THROW_INACCURACY);
        if (!serverLevel.addFreshEntity(projectile)) {
            return InteractionResult.FAIL;
        }

        ActiveGrenade active = new ActiveGrenade(projectile, serverLevel.dimension(), serverLevel.getGameTime(), definition, clientItem);
        projectile.setActiveGrenade(active);
        ACTIVE_GRENADES.add(active);
        player.getCooldowns().addCooldown(stack, definition.cooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void registerTicker() {
        if (tickRegistered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(ThrowableImpulseItem::tickLevel);
        tickRegistered = true;
    }

    private static void tickLevel(ServerLevel level) {
        if (ACTIVE_GRENADES.isEmpty()) {
            return;
        }

        Iterator<ActiveGrenade> iterator = ACTIVE_GRENADES.iterator();
        while (iterator.hasNext()) {
            ActiveGrenade active = iterator.next();
            if (!active.dimension().equals(level.dimension())) {
                continue;
            }

            Snowball projectile = active.projectile();
            if (active.stuck()) {
                if (level.getGameTime() >= active.impactTick()) {
                    active.discardStuckDisplay();
                    detonate(level, active.impactPosition(), active.definition(), projectile);
                    iterator.remove();
                } else {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true,
                            active.impactPosition().x(), active.impactPosition().y(), active.impactPosition().z(),
                            1, 0.03D, 0.03D, 0.03D, 0.0D);
                }
                continue;
            }
            if (projectile.isRemoved() || !projectile.isAlive()) {
                active.stick(level, level.getGameTime(), projectile.position());
                continue;
            }

            if (level.getGameTime() - active.spawnTick() >= active.definition().fuseTicks()) {
                Vec3 detonation = projectile.position();
                projectile.discard();
                detonate(level, detonation, active.definition(), projectile);
                iterator.remove();
            }
        }
    }

    private static void detonate(ServerLevel level, Vec3 origin, Definition definition, Entity source) {
        level.playSound(null, origin.x(), origin.y(), origin.z(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.9F, definition.explosionPitch());
        level.sendParticles(ParticleTypes.EXPLOSION, true, true, origin.x(), origin.y(), origin.z(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, origin.x(), origin.y(), origin.z(),
                28, definition.radius() * 0.35D, definition.radius() * 0.2D, definition.radius() * 0.35D, 0.08D);

        AABB area = new AABB(
                origin.x() - definition.radius(),
                origin.y() - definition.radius(),
                origin.z() - definition.radius(),
                origin.x() + definition.radius(),
                origin.y() + definition.radius(),
                origin.z() + definition.radius()
        );
        for (Entity target : level.getEntities(source, area, target -> target.isAlive() && target.isPushable())) {
            Vec3 impulse = ImpulsePhysics.radialImpulse(
                    origin,
                    target.getBoundingBox().getCenter(),
                    definition.radius(),
                    definition.horizontalStrength(),
                    definition.verticalStrength()
            );
            if (impulse.lengthSqr() <= 1.0E-8D) {
                continue;
            }
            target.addDeltaMovement(impulse);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer player) {
                MobilityItemInteractions.enableImpulseLaunch(player, definition.resetsFallDistance());
            } else if (definition.resetsFallDistance()) {
                target.resetFallDistance();
            }
        }
    }

    private static final class TrackedImpulseSnowball extends Snowball {
        private ActiveGrenade activeGrenade;

        private TrackedImpulseSnowball(Level level, Player owner, ItemStack stack) {
            super(level, owner, stack);
        }

        private void setActiveGrenade(ActiveGrenade activeGrenade) {
            this.activeGrenade = Objects.requireNonNull(activeGrenade, "activeGrenade");
        }

        @Override
        protected void onHit(HitResult result) {
            if (!level().isClientSide()
                    && level() instanceof ServerLevel serverLevel
                    && activeGrenade != null
                    && !activeGrenade.stuck()) {
                activeGrenade.stick(serverLevel, serverLevel.getGameTime(), result.getLocation());
            }
            super.onHit(result);
        }
    }

    private Component displayNameComponent() {
        return Component.literal(definition.displayName()).withStyle(definition.textColor());
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-4D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.ItemDisplay> itemDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:item_display entity type");
        }
        return (EntityType<Display.ItemDisplay>) type;
    }

    public record Definition(
            String path,
            String displayName,
            double radius,
            double horizontalStrength,
            double verticalStrength,
            int fuseTicks,
            int cooldownTicks,
            boolean resetsFallDistance,
            float explosionPitch,
            ChatFormatting textColor
    ) {
        public Definition {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(textColor, "textColor");
            if (path.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            if (displayName.isBlank()) {
                throw new IllegalArgumentException("displayName must not be blank");
            }
            if (radius <= 0.0D) {
                throw new IllegalArgumentException("radius must be positive");
            }
            if (horizontalStrength < 0.0D) {
                throw new IllegalArgumentException("horizontalStrength must be non-negative");
            }
            if (verticalStrength < 0.0D) {
                throw new IllegalArgumentException("verticalStrength must be non-negative");
            }
            if (fuseTicks <= 0) {
                throw new IllegalArgumentException("fuseTicks must be positive");
            }
            if (cooldownTicks < 0) {
                throw new IllegalArgumentException("cooldownTicks must be non-negative");
            }
            if (explosionPitch <= 0.0F) {
                throw new IllegalArgumentException("explosionPitch must be positive");
            }
        }
    }

    private static final class ActiveGrenade {
        private final Snowball projectile;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final long spawnTick;
        private final Definition definition;
        private final Item stuckItem;
        private Vec3 impactPosition;
        private long impactTick = -1L;
        private Display.ItemDisplay stuckDisplay;

        private ActiveGrenade(
                Snowball projectile,
                net.minecraft.resources.ResourceKey<Level> dimension,
                long spawnTick,
                Definition definition,
                Item stuckItem
        ) {
            this.projectile = Objects.requireNonNull(projectile, "projectile");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.spawnTick = spawnTick;
            this.definition = Objects.requireNonNull(definition, "definition");
            this.stuckItem = Objects.requireNonNull(stuckItem, "stuckItem");
        }

        private Snowball projectile() {
            return projectile;
        }

        private net.minecraft.resources.ResourceKey<Level> dimension() {
            return dimension;
        }

        private long spawnTick() {
            return spawnTick;
        }

        private Definition definition() {
            return definition;
        }

        private boolean stuck() {
            return impactPosition != null;
        }

        private Vec3 impactPosition() {
            return impactPosition;
        }

        private long impactTick() {
            return impactTick;
        }

        private void stick(ServerLevel level, long tick, Vec3 position) {
            impactPosition = Objects.requireNonNull(position, "position");
            impactTick = tick + IMPACT_EXPLOSION_DELAY_TICKS;
            stuckDisplay = new Display.ItemDisplay(ITEM_DISPLAY_TYPE, level);
            stuckDisplay.setNoGravity(true);
            stuckDisplay.setPos(position.x(), position.y(), position.z());
            stuckDisplay.getSlot(0).set(new ItemStack(stuckItem));
            if (!level.addFreshEntity(stuckDisplay)) {
                stuckDisplay.discard();
                stuckDisplay = null;
            }
        }

        private void discardStuckDisplay() {
            if (stuckDisplay != null) {
                stuckDisplay.discard();
                stuckDisplay = null;
            }
        }
    }
}
