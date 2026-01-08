package com.github.brainage04.fortnite_in_minecraft.item.misc.grenade;

import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.ShockwaveGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.core.AbstractGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.core.AbstractGrenadeItem;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

public class ShockwaveGrenadeItem extends AbstractGrenadeItem {
    public ShockwaveGrenadeItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    protected ProjectileEntity.ProjectileCreator<AbstractGrenadeEntity> getEntityFactory() {
        return ShockwaveGrenadeEntity::new;
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        ShockwaveGrenadeEntity entity = new ShockwaveGrenadeEntity(world, pos);
        entity.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return entity;
    }
}
