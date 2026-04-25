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

public class ShockwaveGrenadeEntity extends AbstractGrenadeEntity {
    public ShockwaveGrenadeEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    @Override
    protected boolean shouldTakeFallDamage() {
        return true;
    }

    public ShockwaveGrenadeEntity(World world, Position pos) {
        super(ModEntities.SHOCKWAVE_GRENADE, world, pos);
    }

    public ShockwaveGrenadeEntity(ServerWorld serverWorld, LivingEntity livingEntity, ItemStack itemStack) {
        super(ModEntities.SHOCKWAVE_GRENADE, serverWorld, livingEntity, itemStack);
    }

    @Override
    protected float getKnockbackModifier() {
        return 4;
    }
}
