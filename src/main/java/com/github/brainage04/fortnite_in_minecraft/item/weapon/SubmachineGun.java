package com.github.brainage04.fortnite_in_minecraft.item.weapon;

import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.GunItem;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.HitScanGunStats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class SubmachineGun extends GunItem<HitScanGunStats> {
    public SubmachineGun(Settings settings) {
        super(settings, new HitScanGunStats(3, 2, 20, 30));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.GOLDEN_HOE;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Registries.ITEM.getId(getPolymerItem(stack, context));
    }
}
