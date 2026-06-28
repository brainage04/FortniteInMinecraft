package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    void emptyMagazineFireDoesNotStartAutomaticReload() {
        assertEquals(WeaponItem.FireAttempt.EMPTY_MAGAZINE, WeaponItem.fireAttempt(0, false));
        assertEquals(WeaponItem.FireAttempt.FIRE, WeaponItem.fireAttempt(1, false));
        assertEquals(WeaponItem.FireAttempt.COOLDOWN, WeaponItem.fireAttempt(0, true));
    }

    @Test
    void buildDamageUsesFullFortniteWeaponDamage() {
        WeaponItem item = ModItems.WEAPONS.get(0);

        assertEquals((int) Math.round(item.definition().stats().damage() * item.definition().stats().pellets()), item.buildDamage());
    }

    @Test
    void manualReloadStartsPartialMagazineReloadWithoutShot() {
        assertEquals(WeaponItem.ManualReloadResult.STARTED,
                WeaponItem.manualReloadDecision(12, 30, 0L, 200L));
    }

    @Test
    void manualReloadIgnoresFullMagazine() {
        assertEquals(WeaponItem.ManualReloadResult.FULL_MAGAZINE,
                WeaponItem.manualReloadDecision(30, 30, 0L, 200L));
    }

    @Test
    void manualReloadDoesNotRestartAlreadyReloadingGun() {
        assertEquals(WeaponItem.ManualReloadResult.ALREADY_RELOADING,
                WeaponItem.manualReloadDecision(0, 30, 240L, 201L));
    }

    @Test
    void manualReloadStartsEmptyMagazineReload() {
        assertEquals(WeaponItem.ManualReloadResult.STARTED,
                WeaponItem.manualReloadDecision(0, 30, 0L, 200L));
    }

    @Test
    void manualReloadIgnoresNonGunStacks() {
        assertEquals(WeaponItem.ManualReloadResult.NOT_WEAPON, WeaponItem.tryStartManualReload(ItemStack.EMPTY, 200L));
    }

    @Test
    void shieldConsumablesRaiseZeroVanillaAbsorptionCap() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(10.0F, ConsumableItem.shieldCap(smallShield, 0.0F, 0.0F), 0.001F);
        assertEquals(5.0F, ConsumableItem.shieldAfterUse(smallShield, 0.0F, 0.0F), 0.001F);
    }

    @Test
    void shieldConsumablesRespectFortniteShieldCap() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(10.0F, ConsumableItem.shieldAfterUse(smallShield, 8.0F, 0.0F), 0.001F);
        assertEquals(12.0F, ConsumableItem.shieldAfterUse(smallShield, 12.0F, 20.0F), 0.001F);
    }

    @Test
    void consumableProgressTextUsesElapsedAndNeededSecondsFromTicks() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(41, ConsumableItem.useTicks(smallShield));
        assertEquals("0/2.05s", ConsumableItem.progressText(smallShield, 41));
        assertEquals("1/2.05s", ConsumableItem.progressText(smallShield, 21));
        assertEquals("2.05/2.05s", ConsumableItem.progressText(smallShield, 0));
        assertEquals("0/2.05s", ConsumableItem.progressText(smallShield, 99));
        assertEquals("2.05/2.05s", ConsumableItem.progressText(smallShield, -1));
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
