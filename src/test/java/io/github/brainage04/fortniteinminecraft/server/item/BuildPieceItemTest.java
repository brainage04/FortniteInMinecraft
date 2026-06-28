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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
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
    void weaponItemsExposeAccurateCooldownComponentForClientOverlay() {
        WeaponItem item = ModItems.WEAPONS.get(0);
        UseCooldown cooldown = WeaponItem.cooldownComponent(item.definition());

        assertNotNull(cooldown);
        assertTrue(cooldown.cooldownGroup().isPresent());
        assertEquals(item.fireDelayTicks(), cooldown.ticks());
    }

    @Test
    void weaponStatusTextShowsHeldAmmoAndMagazineSize() {
        WeaponItem item = ModItems.WEAPONS.get(0);

        assertEquals("Assault Rifle: 30/30", item.statusText(30));
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
    void consumableClientItemsAvoidVanillaRightClickUseOverrides() {
        assertSame(Items.PAPER, ModItems.CONSUMABLES.get(0).clientItem());
        assertSame(Items.IRON_INGOT, ModItems.CONSUMABLES.get(1).clientItem());
        assertSame(Items.AMETHYST_SHARD, ModItems.CONSUMABLES.get(2).clientItem());
        assertSame(Items.PRISMARINE_SHARD, ModItems.CONSUMABLES.get(3).clientItem());
        assertSame(Items.ECHO_SHARD, ModItems.CONSUMABLES.get(4).clientItem());
    }

    @Test
    void bulletRaycastUsesColliderShapesSoGrassDoesNotBlockShots() {
        assertSame(ClipContext.Block.COLLIDER, WeaponItem.BULLET_BLOCK_MODE);
    }

    @Test
    void heldWeaponInputGraceCanOutrunVanillaRightClickDelay() {
        assertTrue(WeaponAutoFire.HELD_USE_GRACE_TICKS > 4L);
    }

    @Test
    void muzzleParticleOriginMovesDownAndRightFromView() {
        Vec3 start = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 muzzle = WeaponItem.muzzlePosition(start, new Vec3(0.0D, 0.0D, -1.0D));

        assertTrue(muzzle.x() > start.x());
        assertTrue(muzzle.y() < start.y());
        assertTrue(muzzle.z() < start.z());
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
