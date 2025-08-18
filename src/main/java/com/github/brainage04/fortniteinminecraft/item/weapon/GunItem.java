package com.github.brainage04.fortniteinminecraft.item.weapon;

import com.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public class GunItem<T extends GunStats> extends Item {
    public final T stats;

    public static final double MAX_DISTANCE = 500;

    public GunItem(Settings settings, T stats) {
        super(settings);
        this.stats = stats;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        MinecraftClient.getInstance().itemUseCooldown = stats.cooldown;

        player.getItemCooldownManager().set(
                player.getStackInHand(hand),
                stats.cooldown
        );

        // sound
        world.playSound(player, player.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.MASTER, 1, 2);

        HitResult hit = player.raycast(MAX_DISTANCE, 1, false);

        // particle
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;

            for (ServerPlayerEntity serverPlayerEntity : serverWorld.getPlayers()) {
                if (!serverWorld.spawnParticles(
                        serverPlayerEntity,
                        ParticleTypes.CRIT,
                        true,
                        true,
                        hit.getPos().x,
                        hit.getPos().y,
                        hit.getPos().z,
                        stats.damage * 10,
                        0,
                        0,
                        0,
                        stats.damage * 0.05
                )) {
                    FortniteInMinecraft.LOGGER.error("Particles not sent - this shouldn't happen!");
                }
            }
        }

        if (stats instanceof ProjectileGunStats projectileGunStats) {
            // shoot projectile with drop (if applicable)
        } else if (stats instanceof HitScanGunStats hitScanGunStats) {
            if (world.isClient) return super.use(world, player, hand);

            // raycast straight forward (ignoring drop) and do damage instantly
            Vec3d start = player.getCameraPosVec(1);
            Vec3d rotation = player.getRotationVec(1);
            Vec3d end = start.add(rotation.multiply(hitScanGunStats.range));

            Box box = player.getBoundingBox().stretch(rotation.multiply(hitScanGunStats.range)).expand(1.0, 1.0, 1.0);
            EntityHitResult entityHitResult = ProjectileUtil.raycast(
                    player,
                    start,
                    end,
                    box,
                    entity -> !entity.isSpectator() && entity.canHit(),
                    hitScanGunStats.range * hitScanGunStats.range
            );
            if (entityHitResult == null) return super.use(world, player, hand);

            Optional<Registry<DamageType>> optionalDamageTypeRegistry = world.getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE);
            if (optionalDamageTypeRegistry.isEmpty()) return super.use(world, player, hand);

            Registry<DamageType> damageTypeRegistry = optionalDamageTypeRegistry.get();
            DamageType damageType = damageTypeRegistry.get(DamageTypes.PLAYER_ATTACK);
            RegistryEntry<DamageType> damageTypeRegistryEntry = damageTypeRegistry.getEntry(damageType);

            DamageSource source = new DamageSource(
                    damageTypeRegistryEntry,
                    player,
                    player
            );

            Entity entity = entityHitResult.getEntity();

            entity.damage(
                    (ServerWorld) world,
                    source,
                    hitScanGunStats.damage
            );

            if (entity instanceof LivingEntity livingEntity) livingEntity.maxHurtTime = 1;
        }

        return super.use(world, player, hand);
    }
}
