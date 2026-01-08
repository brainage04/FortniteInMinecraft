package com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.core;

import com.github.brainage04.fortnite_in_minecraft.entity.ModEntities;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.BoogieBombEntity;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ProjectileItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class BoogieBombItem extends SimplePolymerItem implements ProjectileItem {
    private static final boolean DECREMENT_AFTER_USE = false;

    public BoogieBombItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.PASS;

        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        BoogieBombEntity entity = ProjectileEntity.spawnWithVelocity(BoogieBombEntity::new, (ServerWorld) world, itemStack, user, 0.0F, 1.5F, 1.0F);
        entity.setOwner(user);

        user.incrementStat(Stats.USED.getOrCreateStat(this));

        if (DECREMENT_AFTER_USE && !user.isCreative()) user.getStackInHand(hand).decrement(1);

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

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        return new BoogieBombEntity(ModEntities.IMPULSE_GRENADE, world, pos);
    }
}
