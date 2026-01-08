package com.github.brainage04.fortnite_in_minecraft.item;

import com.github.brainage04.fortnite_in_minecraft.FortniteInMinecraft;
import com.github.brainage04.fortnite_in_minecraft.item.building.Material;
import com.github.brainage04.fortnite_in_minecraft.item.building.PencilItem;
import com.github.brainage04.fortnite_in_minecraft.item.building.PieceItem;
import com.github.brainage04.fortnite_in_minecraft.item.building.PieceType;
import com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.ImpulseGrenadeItem;
import com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.ShockwaveGrenadeItem;
import com.github.brainage04.fortnite_in_minecraft.item.misc.grenade.core.BoogieBombItem;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.AssaultRifle;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.Shotgun;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.SniperRifle;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.SubmachineGun;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item WOOD_WALL = register(
            "wood_wall",
            settings -> new PieceItem(settings, PieceType.WALL, Material.WOOD),
            new Item.Settings()
    );
    public static final Item WOOD_FLOOR = register(
            "wood_floor",
            settings -> new PieceItem(settings, PieceType.FLOOR, Material.WOOD),
            new Item.Settings()
    );
    public static final Item WOOD_STAIR = register(
            "wood_stair",
            settings -> new PieceItem(settings, PieceType.STAIR, Material.WOOD),
            new Item.Settings()
    );
    public static final Item WOOD_CONE = register(
            "wood_cone",
            settings -> new PieceItem(settings, PieceType.CONE, Material.WOOD),
            new Item.Settings()
    );

    public static final Item BRICK_WALL = register(
            "brick_wall",
            settings -> new PieceItem(settings, PieceType.WALL, Material.BRICK),
            new Item.Settings()
    );
    public static final Item BRICK_FLOOR = register(
            "brick_floor",
            settings -> new PieceItem(settings, PieceType.FLOOR, Material.BRICK),
            new Item.Settings()
    );
    public static final Item BRICK_STAIR = register(
            "brick_stair",
            settings -> new PieceItem(settings, PieceType.STAIR, Material.BRICK),
            new Item.Settings()
    );
    public static final Item BRICK_CONE = register(
            "brick_cone",
            settings -> new PieceItem(settings, PieceType.CONE, Material.BRICK),
            new Item.Settings()
    );

    public static final Item METAL_WALL = register(
            "metal_wall",
            settings -> new PieceItem(settings, PieceType.WALL, Material.METAL),
            new Item.Settings()
    );
    public static final Item METAL_FLOOR = register(
            "metal_floor",
            settings -> new PieceItem(settings, PieceType.FLOOR, Material.METAL),
            new Item.Settings()
    );
    public static final Item METAL_STAIR = register(
            "metal_stair",
            settings -> new PieceItem(settings, PieceType.STAIR, Material.METAL),
            new Item.Settings()
    );
    public static final Item METAL_CONE = register(
            "metal_cone",
            settings -> new PieceItem(settings, PieceType.CONE, Material.METAL),
            new Item.Settings()
    );

    public static final Item PENCIL = register(
            "pencil",
            PencilItem::new,
            new Item.Settings()
    );

    public static final Item ASSAULT_RIFLE = register(
            "assault_rifle",
            AssaultRifle::new,
            new Item.Settings()
    );

    public static final Item SUBMACHINE_GUN = register(
            "submachine_gun",
            SubmachineGun::new,
            new Item.Settings()
    );

    public static final Item SHOTGUN = register(
            "shotgun",
            Shotgun::new,
            new Item.Settings()
    );

    public static final Item SNIPER_RIFLE = register(
            "sniper_rifle",
            SniperRifle::new,
            new Item.Settings()
    );

    public static final Item IMPULSE_GRENADE = register(
            "impulse_grenade",
            ImpulseGrenadeItem::new,
            new Item.Settings()
    );

    public static final Item SHOCKWAVE_GRENADE = register(
            "shockwave_grenade",
            ShockwaveGrenadeItem::new,
            new Item.Settings()
    );

    public static final Item BOOGIE_BOMB = register(
            "boogie_bomb",
            BoogieBombItem::new,
            new Item.Settings()
    );

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(FortniteInMinecraft.MOD_ID, path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize() {
        // todo: make SniperRifle implement ProjectileItem
        //DispenserBlock.registerProjectileBehavior(SNIPER_RIFLE);

        DispenserBlock.registerProjectileBehavior(IMPULSE_GRENADE);
        DispenserBlock.registerProjectileBehavior(SHOCKWAVE_GRENADE);
        DispenserBlock.registerProjectileBehavior(BOOGIE_BOMB);
    }
}
