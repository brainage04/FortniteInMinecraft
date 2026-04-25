package com.github.brainage04.fortnite_in_minecraft.item.weapon;

import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.GunItem;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.ProjectileGunStats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class SniperRifle extends GunItem<ProjectileGunStats> {
    public SniperRifle(Settings settings) {
        super(settings, new ProjectileGunStats(12, 30, 60, 5, 6, 2));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.DIAMOND_HOE;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Registries.ITEM.getId(getPolymerItem(stack, context));
    }
}
