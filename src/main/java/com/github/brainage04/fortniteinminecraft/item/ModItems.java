package com.github.brainage04.fortniteinminecraft.item;

import com.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import com.github.brainage04.fortniteinminecraft.item.building.PencilItem;
import com.github.brainage04.fortniteinminecraft.item.building.PieceItem;
import com.github.brainage04.fortniteinminecraft.item.building.PieceType;
import com.github.brainage04.fortniteinminecraft.item.weapon.GunItem;
import com.github.brainage04.fortniteinminecraft.item.weapon.HitScanGunStats;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item WOOD_WALL = register(
            "wood_wall",
            settings -> new PieceItem(settings, PieceType.WALL, Blocks.OAK_PLANKS),
            new Item.Settings()
    );
    public static final Item WOOD_FLOOR = register(
            "wood_floor",
            settings -> new PieceItem(settings, PieceType.FLOOR, Blocks.OAK_PLANKS),
            new Item.Settings()
    );
    public static final Item WOOD_STAIR = register(
            "wood_stair",
            settings -> new PieceItem(settings, PieceType.STAIR, Blocks.OAK_PLANKS),
            new Item.Settings()
    );
    public static final Item WOOD_CONE = register(
            "wood_cone",
            settings -> new PieceItem(settings, PieceType.CONE, Blocks.OAK_PLANKS),
            new Item.Settings()
    );

    public static final Item PENCIL = register(
            "pencil",
            PencilItem::new,
            new Item.Settings()
    );

    public static final Item ASSAULT_RIFLE = register(
            "assault_rifle",
            settings -> new GunItem<>(settings, new HitScanGunStats(6, 4, 100, 30)),
            new Item.Settings()
    );

    public static final Item SUBMACHINE_GUN = register(
            "submachine_gun",
            settings -> new GunItem<>(settings, new HitScanGunStats(3, 2, 50, 30)),
            new Item.Settings()
    );

    public static final Item SHOTGUN = register(
            "shotgun",
            settings -> new GunItem<>(settings, new HitScanGunStats(10, 15, 20 ,8)),
            new Item.Settings()
    );

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(FortniteInMinecraft.MOD_ID, path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize() {

    }
}
