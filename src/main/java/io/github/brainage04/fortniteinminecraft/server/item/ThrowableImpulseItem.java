package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ThrowableImpulseItem extends SimplePolymerItem {
    private static final float THROW_POWER = 1.5F;
    private static final float THROW_INACCURACY = 1.0F;
    private static final ArrayList<ActiveGrenade> ACTIVE_GRENADES = new ArrayList<>();
    private static boolean tickRegistered;

    private final Definition definition;
    private final Item clientItem;

    public ThrowableImpulseItem(Definition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
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

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Radius: " + format(definition.radius()) + " blocks"));
        tooltip.add(Component.literal("Impulse: " + format(definition.horizontalStrength())
                + " horizontal / " + format(definition.verticalStrength()) + " vertical"));
        tooltip.add(Component.literal("Fuse: " + format(definition.fuseTicks() / 20.0D) + "s; cooldown: "
                + format(definition.cooldownTicks() / 20.0D) + "s"));
        if (definition.resetsFallDistance()) {
            tooltip.add(Component.literal("Cancels fall distance on launch"));
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
        Snowball projectile = new Snowball(serverLevel, player, projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_POWER, THROW_INACCURACY);
        if (!serverLevel.addFreshEntity(projectile)) {
            return InteractionResult.FAIL;
        }

        ACTIVE_GRENADES.add(new ActiveGrenade(projectile, serverLevel.dimension(), serverLevel.getGameTime(), definition));
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
            if (projectile.isRemoved() || !projectile.isAlive()) {
                detonate(level, active.lastPosition(), active.definition(), projectile);
                iterator.remove();
                continue;
            }

            active.setLastPosition(projectile.position());
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
            if (definition.resetsFallDistance()) {
                target.resetFallDistance();
            }
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
        private Vec3 lastPosition;

        private ActiveGrenade(
                Snowball projectile,
                net.minecraft.resources.ResourceKey<Level> dimension,
                long spawnTick,
                Definition definition
        ) {
            this.projectile = Objects.requireNonNull(projectile, "projectile");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.spawnTick = spawnTick;
            this.definition = Objects.requireNonNull(definition, "definition");
            this.lastPosition = projectile.position();
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

        private Vec3 lastPosition() {
            return lastPosition;
        }

        private void setLastPosition(Vec3 lastPosition) {
            this.lastPosition = Objects.requireNonNull(lastPosition, "lastPosition");
        }
    }
}
