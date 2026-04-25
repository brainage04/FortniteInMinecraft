package com.github.brainage04.fortnite_in_minecraft.item.weapon;

import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.GunItem;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.HitScanGunStats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class Shotgun extends GunItem<HitScanGunStats> {
    public Shotgun(Settings settings) {
        super(settings, new HitScanGunStats(10, 15, 60, 8), SoundEvents.ENTITY_GENERIC_EXPLODE.value());
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.FLINT_AND_STEEL;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Registries.ITEM.getId(getPolymerItem(stack, context));
    }
}
