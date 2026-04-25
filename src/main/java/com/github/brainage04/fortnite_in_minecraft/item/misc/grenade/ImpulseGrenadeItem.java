package com.github.brainage04.fortnite_in_minecraft.item.misc.grenade;

import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.ImpulseGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.core.AbstractGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.core.AbstractGrenadeItem;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

public class ImpulseGrenadeItem extends AbstractGrenadeItem {
    public ImpulseGrenadeItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    protected ProjectileEntity.ProjectileCreator<AbstractGrenadeEntity> getEntityFactory() {
        return ImpulseGrenadeEntity::new;
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        ImpulseGrenadeEntity entity = new ImpulseGrenadeEntity(world, pos);
        entity.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return entity;
    }
}
