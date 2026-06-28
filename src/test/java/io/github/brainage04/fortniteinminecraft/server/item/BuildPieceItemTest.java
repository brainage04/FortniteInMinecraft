package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.UseCooldown;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPieceItemTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void clientItemAppearanceFollowsSharedMaterialPalette() {
        BuildPieceItem item = new BuildPieceItem(
                PieceType.STAIR,
                properties("test_build_stair"),
                Items.OAK_STAIRS,
                Items.COBBLESTONE_STAIRS,
                Items.CUT_COPPER_STAIRS.weathering().unaffected()
        );

        assertSame(Items.OAK_STAIRS, item.clientItemFor(MaterialType.WOOD));
        assertSame(Items.COBBLESTONE_STAIRS, item.clientItemFor(MaterialType.STONE));
        assertSame(Items.CUT_COPPER_STAIRS.weathering().unaffected(), item.clientItemFor(MaterialType.METAL));
    }

    @Test
    void weaponItemsExposeCooldownComponentForClientOverlay() {
        WeaponItem item = ModItems.WEAPONS.get(0);
        UseCooldown cooldown = WeaponItem.cooldownComponent(item.definition());

        assertNotNull(cooldown);
        assertTrue(cooldown.cooldownGroup().isPresent());
    }

    @Test
    void consumableItemsExposeFoodUseProgressComponents() {
        ConsumableItem item = ModItems.CONSUMABLES.get(0);
        Consumable consumable = ConsumableItem.consumableComponent(item.definition());

        assertNotNull(ConsumableItem.foodProperties());
        assertNotNull(consumable);
        assertEquals((int) Math.ceil(item.definition().castSeconds() * 20.0D), consumable.consumeTicks());
    }

    @Test
    void bulletKnockbackPreventionDefaultsOnButCanBeChanged() {
        assertTrue(CombatSettings.preventBulletKnockback());

        CombatSettings.setPreventBulletKnockback(false);
        assertFalse(CombatSettings.preventBulletKnockback());

        CombatSettings.setPreventBulletKnockback(true);
    }


    private static Item.Properties properties(String path) {
        ResourceKey<Item> key = ResourceKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath("fortniteinminecraft_test", path)
        );
        return new Item.Properties().setId(key).stacksTo(1);
    }
}
