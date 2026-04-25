package com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.core;

import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.core.AbstractGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.util.ProjectileUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ProjectileItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public abstract class AbstractGrenadeItem extends SimplePolymerItem implements ProjectileItem {
    private static final boolean DECREMENT_AFTER_USE = false;

    public AbstractGrenadeItem(Item.Settings settings) {
        super(settings);
    }

    protected abstract ProjectileEntity.ProjectileCreator<AbstractGrenadeEntity> getEntityFactory();

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.PASS;

        ProjectileUtils.spawnProjectile(getEntityFactory(), 1, world, user, hand);

        if (DECREMENT_AFTER_USE && !user.isCreative()) {
            user.getStackInHand(hand).decrement(1);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.ARROW;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Registries.ITEM.getId(Items.SNOWBALL);
    }
}
