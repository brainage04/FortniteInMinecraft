package com.github.brainage04.fortnite_in_minecraft.item.weapon.core;

import com.github.brainage04.fortnite_in_minecraft.util.ParticleUtils;
import com.github.brainage04.fortnite_in_minecraft.util.ProjectileUtils;
import com.github.brainage04.fortnite_in_minecraft.util.SoundUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

public abstract class GunItem<T extends GunStats> extends SimplePolymerItem {
    public final T stats;
    public final SoundEvent sound;
    private static final int MAX_DISTANCE = 500;

    public GunItem(Settings settings, T stats) {
        super(settings);
        this.stats = stats;
        this.sound = SoundEvents.ENTITY_IRON_GOLEM_ATTACK;
    }

    public GunItem(Settings settings, T stats, SoundEvent sound) {
        super(settings);
        this.stats = stats;
        this.sound = sound;
    }

    @Override
    public abstract Item getPolymerItem(ItemStack itemStack, PacketContext context);

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        //MinecraftClient.getInstance().itemUseCooldown = stats.cooldown;
        stats.currentCapacity--;

        player.getItemCooldownManager().set(
                player.getStackInHand(hand),
                stats.currentCapacity <= 0 ? stats.ticksToReload : stats.cooldown
        );

        ServerWorld serverWorld = (ServerWorld) world;

        world.playSound(
                null,
                player.getBlockPos(),
                stats.currentCapacity <= 0 ? SoundEvents.BLOCK_ANVIL_LAND : sound,
                SoundCategory.PLAYERS,
                1,
                stats.currentCapacity <= 0 ? 1 : 2
        );

        if (stats.currentCapacity <= 0) stats.currentCapacity = stats.maxCapacity;

        // todo: 1.5x damage on headshots (enchanted hit for normal, crit for headshot)
        if (stats instanceof ProjectileGunStats projectileGunStats) {
            ArrowEntity arrow = ProjectileUtils.spawnProjectile(
                    (world1, shooter, stack) -> new ArrowEntity(world1, shooter, stack, null),
                    projectileGunStats.velocity,
                    world,
                    player,
                    hand
            );

            arrow.setDamage((double) projectileGunStats.damage / projectileGunStats.velocity);
        } else if (stats instanceof HitScanGunStats hitScanGunStats) {
            HitResult hit = player.raycast(MAX_DISTANCE, 1, false);

            ParticleUtils.createParticles(
                    serverWorld,
                    serverWorld.getPlayers(),
                    ParticleTypes.ENCHANTED_HIT,
                    hit.getPos(),
                    stats.damage * 10,
                    stats.damage * 0.05
            );

            SoundUtils.playShotSound(world, player);

            if (hit.getType() != HitResult.Type.ENTITY) return super.use(world, player, hand);

            Registry<DamageType> damageTypeRegistry = world.getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).orElse(null);
            if (damageTypeRegistry == null) return super.use(world, player, hand);

            DamageType damageType = damageTypeRegistry.get(DamageTypes.PLAYER_ATTACK);
            RegistryEntry<DamageType> damageTypeRegistryEntry = damageTypeRegistry.getEntry(damageType);

            DamageSource source = new DamageSource(
                    damageTypeRegistryEntry,
                    player,
                    player
            );

            EntityHitResult entityHit = (EntityHitResult) hit;
            Entity entity = entityHit.getEntity();

            entity.damage(
                    (ServerWorld) world,
                    source,
                    hitScanGunStats.damage
            );

            if (entity instanceof LivingEntity livingEntity) {
                world.getServer().execute(() -> {
                    livingEntity.hurtTime = 1;
                    livingEntity.maxHurtTime = 1;
                });
            }
        }

        return super.use(world, player, hand);
    }
}
