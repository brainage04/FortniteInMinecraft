package com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile;

import com.github.brainage04.fortnite_in_minecraft.effect.ModEffects;
import com.github.brainage04.fortnite_in_minecraft.entity.ModEntities;
import com.github.brainage04.fortnite_in_minecraft.util.ParticleUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class BoogieBombEntity extends SnowballEntity implements PolymerEntity {
    public BoogieBombEntity(EntityType<? extends Entity> type, World world) {
        super((EntityType<? extends SnowballEntity>) type, world);
    }

    public BoogieBombEntity(EntityType<? extends Entity> type, World world, Position pos) {
        super((EntityType<? extends SnowballEntity>) type, world);
        setPos(
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    public BoogieBombEntity(ServerWorld serverWorld, LivingEntity livingEntity, ItemStack itemStack) {
        super(ModEntities.BOOGIE_BOMB, serverWorld);
        setPosition(livingEntity.getEyePos());
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.SNOWBALL;
    }

    private void boogieNearbyPlayers(World world, Vec3d origin) {
        // Get all players within 5 blocks
        List<PlayerEntity> players = world.getEntitiesByClass(
                PlayerEntity.class,
                new Box(
                        origin.x - 5, origin.y - 5, origin.z - 5,
                        origin.x + 5, origin.y + 5, origin.z + 5
                ),
                player -> true
        );

        for (PlayerEntity player : players) {
            player.addStatusEffect(new StatusEffectInstance(ModEffects.BOOGIE, 100));
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (getWorld() instanceof ServerWorld serverWorld) {
            ParticleUtils.createParticles(
                    serverWorld,
                    serverWorld.getPlayers(),
                    ParticleTypes.EXPLOSION,
                    hitResult.getPos(),
                    20,
                    2
            );

            boogieNearbyPlayers(getWorld(), getPos());
        }
    }
}
