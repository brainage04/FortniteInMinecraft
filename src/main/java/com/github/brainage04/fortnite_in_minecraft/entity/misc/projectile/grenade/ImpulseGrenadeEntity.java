package com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade;

import com.github.brainage04.fortnite_in_minecraft.entity.ModEntities;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.core.AbstractGrenadeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

public class ImpulseGrenadeEntity extends AbstractGrenadeEntity {
    public ImpulseGrenadeEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    @Override
    protected boolean shouldTakeFallDamage() {
        return true;
    }

    public ImpulseGrenadeEntity(World world, Position pos) {
        super(ModEntities.IMPULSE_GRENADE, world, pos);
    }

    public ImpulseGrenadeEntity(ServerWorld serverWorld, LivingEntity livingEntity, ItemStack itemStack) {
        super(ModEntities.IMPULSE_GRENADE, serverWorld, livingEntity, itemStack);
    }

    @Override
    protected float getKnockbackModifier() {
        return 2;
    }
}
