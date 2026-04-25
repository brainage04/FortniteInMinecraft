package com.github.brainage04.fortnite_in_minecraft.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ProjectileUtils {
    public static <T extends PersistentProjectileEntity> T spawnProjectile(ProjectileEntity.ProjectileCreator<T> factory, float speed, World world, PlayerEntity user, Hand hand) {
        ServerWorld serverWorld = (ServerWorld) world;
        ItemStack stack = user.getStackInHand(hand);

        T projectile = ProjectileEntity.spawn(
                factory.create(serverWorld, user, stack),
                serverWorld,
                stack,
                entity -> entity.setVelocity(user, user.getPitch(), user.getYaw(), 0, speed, 0)
        );
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        SoundUtils.playThrownSound(world, user);
        user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));

        return projectile;
    }
}
