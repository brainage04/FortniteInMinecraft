package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class DeployableGameTestHooks {
    private DeployableGameTestHooks() {
    }

    public static boolean damageBuild(
            ServerLevel level,
            ServerPlayer player,
            BlockPos hitPos,
            Vec3 hitLocation,
            int damage,
            String source
    ) {
        return WeaponItem.damageBuild(level, player, hitPos, hitLocation, damage, source);
    }

    public static InteractionResult damageWithPickaxe(
            ServerLevel level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            long tick,
            BlockHitResult hit
    ) {
        return ModItems.PICKAXE.damageBlockHit(level, player, hand, stack, tick, hit);
    }
}
