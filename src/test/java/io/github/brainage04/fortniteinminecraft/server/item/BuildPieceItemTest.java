package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                Items.CUT_COPPER_STAIRS.waxed().unaffected()
        );

        assertSame(Items.OAK_STAIRS, item.clientItemFor(MaterialType.WOOD));
        assertSame(Items.COBBLESTONE_STAIRS, item.clientItemFor(MaterialType.STONE));
        assertSame(Items.CUT_COPPER_STAIRS.waxed().unaffected(), item.clientItemFor(MaterialType.METAL));
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
    void weaponNamesUseFortniteRarityColorWithoutRarityPrefix() {
        WeaponItem item = ModItems.WEAPONS.stream()
                .filter(weapon -> weapon.definition().path().equals("weapon_assault_rifle_legendary"))
                .findFirst()
                .orElseThrow();

        assertEquals("Assault Rifle", item.getName(ItemStack.EMPTY).getString());
        assertEquals(TextColor.GOLD, item.getName(ItemStack.EMPTY).getStyle().getColor());
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

        assertEquals((int) Math.round(item.definition().stats().totalDamagePerShot()), item.buildDamage());
    }

    @Test
    void weaponCatalogIncludesSourceBackedPumpShotgunSlice() {
        WeaponItem item = ModItems.WEAPONS.stream()
                .filter(weapon -> weapon.definition().path().equals("weapon_pump_shotgun_legendary"))
                .findFirst()
                .orElseThrow();

        assertEquals("Pump Shotgun", item.definition().displayName());
        assertEquals("Shotgun_Standard_Athena_SR_Ore_T03", item.definition().sourceStatRow());
        assertEquals(12.8D, item.definition().stats().damage(), 0.001D);
        assertEquals(10, item.definition().stats().pellets());
        assertEquals(128.0D, item.definition().stats().totalDamagePerShot(), 0.001D);
    }

    @Test
    void weaponCatalogIncludesExpandedSourceBackedHitscanFamilies() {
        assertTrue(ModItems.WEAPONS.size() >= 57);
        assertFalse(ModItems.WEAPONS.stream()
                .anyMatch(weapon -> weapon.definition().path().contains("bolt_action_sniper")));

        WeaponItem warforged = weapon("weapon_warforged_assault_rifle_legendary");
        assertEquals("Warforged Assault Rifle", warforged.definition().displayName());
        assertEquals(WeaponCategory.ASSAULT_RIFLE, warforged.definition().category());
        assertEquals(FortniteRarity.LEGENDARY, warforged.definition().rarity());
        assertEquals("WID_Assault_SunRose_HS_Athena_SR", warforged.definition().sourceItemId());
        assertEquals("Assault_Sunrose_Athena_SR_Ore_T03", warforged.definition().sourceStatRow());
        assertEquals(26.0D, warforged.definition().stats().damage(), 0.001D);

        WeaponItem tactical = weapon("weapon_tactical_shotgun_common");
        assertEquals(WeaponCategory.SHOTGUN, tactical.definition().category());
        assertEquals(10, tactical.definition().stats().pellets());
        assertEquals(120.0D, tactical.definition().stats().maxDamagePerShot(), 0.001D);
    }

    @Test
    void weaponStatsCapTotalDamagePerShotWhenSourceProvidesCap() {
        WeaponStats capped = new WeaponStats(50.0D, 2.0D, 5, 1.0D, 2.0D, 5, 20.0D, 120.0D);

        assertEquals(120.0D, capped.totalDamagePerShot(), 0.001D);
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
    void consumablesCannotStartWhenTheyWouldHaveNoEffect() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", 2.03D, 0, 0, 25, 50, true, "small_shield"
        );
        ConsumableDefinition bandage = new ConsumableDefinition(
                "test_bandage", "Bandage", 3.53D, 15, 75, 0, 0, true, "bandage"
        );

        assertTrue(ConsumableItem.canBenefit(smallShield, 20.0F, 20.0F, 8.0F, 20.0F));
        assertFalse(ConsumableItem.canBenefit(smallShield, 20.0F, 20.0F, 10.0F, 20.0F));
        assertTrue(ConsumableItem.canBenefit(bandage, 10.0F, 20.0F, 0.0F, 0.0F));
        assertFalse(ConsumableItem.canBenefit(bandage, 15.0F, 20.0F, 0.0F, 0.0F));
    }

    @Test
    void consumableProgressTextUsesElapsedAndNeededSecondsFromTicks() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(41, ConsumableItem.useTicks(smallShield));
        assertEquals("0.00/2.05s", ConsumableItem.progressText(smallShield, 41));
        assertEquals("1.00/2.05s", ConsumableItem.progressText(smallShield, 21));
        assertEquals("2.05/2.05s", ConsumableItem.progressText(smallShield, 0));
        assertEquals("0.00/2.05s", ConsumableItem.progressText(smallShield, 99));
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
    void consumableCatalogIncludesAdditionalSupportedSourceBackedFood() {
        assertTrue(ModItems.CONSUMABLES.size() >= 7);

        ConsumableItem apple = consumable("consumable_apple");
        assertEquals("Apple", apple.definition().displayName());
        assertEquals("WID_Athena_Apple", apple.definition().sourceItemId());
        assertEquals(5, apple.definition().healthRestore());
        assertFalse(apple.definition().movementLocked());

        ConsumableItem banana = consumable("consumable_banana");
        assertEquals("Banana", banana.definition().displayName());
        assertEquals("WID_Athena_Banana", banana.definition().sourceItemId());
        assertEquals(5, banana.definition().healthRestore());
    }

    @Test
    void projectileWeaponCatalogSeparatesBallisticSnipersFromHitscan() {
        assertEquals(9, ModItems.PROJECTILE_WEAPONS.size());

        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");
        ProjectileWeaponItem huntingRifle = projectileWeapon("weapon_hunting_rifle_legendary");

        assertEquals("Bolt-Action Sniper Rifle", bolt.definition().displayName());
        assertEquals(121.0D, bolt.definition().stats().damage(), 0.001D);
        assertEquals("Hunting Rifle", huntingRifle.definition().displayName());
        assertEquals("Sniper_NoScope_Athena_SR_Ore_T03", huntingRifle.definition().sourceStatRow());
        assertEquals(TextColor.GOLD, bolt.getName(ItemStack.EMPTY).getStyle().getColor());
        assertTrue(ModItems.COMBAT_ITEMS.contains(bolt));
        assertTrue(ModItems.ALL_ITEMS.contains(huntingRifle));
    }

    @Test
    void deferredWeaponCatalogKeepsExplosiveFamiliesAsPlaceholders() {
        assertEquals(5, ModItems.DEFERRED_WEAPONS.size());
        Item rocket = named(ModItems.DEFERRED_WEAPONS, "Rocket Launcher");
        Item proximity = named(ModItems.DEFERRED_WEAPONS, "Proximity Grenade Launcher");

        assertFalse(rocket instanceof WeaponItem);
        assertFalse(proximity instanceof ProjectileWeaponItem);
        assertTrue(ModItems.COMBAT_ITEMS.contains(rocket));
        assertTrue(ModItems.ALL_ITEMS.contains(proximity));
    }

    @Test
    void pickupCatalogIncludesAmmoMaterialsAndGold() {
        assertEquals(9, ModItems.PICKUPS.size());

        PickupItem wood = pickup("Wood");
        assertEquals(MaterialType.WOOD, wood.payload().material());
        assertEquals(30, wood.payload().amount());

        PickupItem mediumAmmo = pickup("Medium Ammo");
        assertEquals(AmmoType.MEDIUM, mediumAmmo.payload().ammoType());
        assertEquals(18, mediumAmmo.payload().amount());

        PickupItem gold = pickup("Gold");
        assertEquals(100, gold.payload().goldAmount());
        assertTrue(ModItems.ALL_ITEMS.contains(gold));
    }

    @Test
    void allItemsCatalogCombinesBuildCombatUtilityAndPickupCategories() {
        assertEquals(ModItems.WEAPONS.size() + ModItems.PROJECTILE_WEAPONS.size() + ModItems.DEFERRED_WEAPONS.size()
                        + ModItems.THROWABLES.size() + ModItems.UTILITY_ITEMS.size() + ModItems.CONSUMABLES.size(),
                ModItems.COMBAT_ITEMS.size());
        assertEquals(ModItems.BUILD_PIECES.size() + ModItems.COMBAT_ITEMS.size() + ModItems.PICKUPS.size()
                        + ModItems.RESOURCE_NODE_ITEMS.size(),
                ModItems.ALL_ITEMS.size());
        assertTrue(ModItems.ALL_ITEMS.containsAll(ModItems.BUILD_PIECES));
        assertTrue(ModItems.ALL_ITEMS.containsAll(ModItems.COMBAT_ITEMS));
        assertTrue(ModItems.ALL_ITEMS.containsAll(ModItems.PICKUPS));
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

    @Test
    void utilityCatalogIncludesMobilityToolsAndResourceNodes() {
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.PICKAXE));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.GRAPPLER));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.LAUNCH_PAD));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.GLIDER));
        assertEquals(3, ModItems.RESOURCE_NODE_ITEMS.size());
        assertTrue(ModItems.ALL_ITEMS.containsAll(ModItems.RESOURCE_NODE_ITEMS));
    }

    @Test
    void projectileWeaponCooldownUsesReloadForSingleShotSnipers() {
        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");

        assertEquals(47, ProjectileWeaponItem.cooldownTicks(bolt.definition()));
        assertEquals(24.2F, ProjectileWeaponItem.minecraftDamage(bolt.definition()), 0.001F);
    }

    @Test
    void sniperScopeSteadiesProjectileSpreadWithoutChangingBaseConfig() {
        assertEquals(0.4F, ProjectileWeaponItem.scopedInaccuracy(0.4F, false), 0.001F);
        assertEquals(0.1F, ProjectileWeaponItem.scopedInaccuracy(0.4F, true), 0.001F);
    }

    @Test
    void grapplerPullVelocityAimsAtTargetAndAddsLift() {
        Vec3 velocity = GrapplerItem.pullVelocity(new Vec3(0.0D, 64.0D, 0.0D), new Vec3(4.0D, 64.0D, 0.0D), 1.6D, 0.25D);

        assertEquals(1.6D, velocity.x(), 1.0E-9D);
        assertEquals(0.25D, velocity.y(), 1.0E-9D);
        assertEquals(0.0D, velocity.z(), 1.0E-9D);
    }

    @Test
    void resourceNodeFootprintsOffsetFromPlacementAnchor() {
        List<BlockPos> positions = ResourceNodeItem.positions(
                new BlockPos(10, 64, -4),
                List.of(new BlockOffset(0, 0, 0), new BlockOffset(1, 2, -1))
        );

        assertEquals(List.of(new BlockPos(10, 64, -4), new BlockPos(11, 66, -5)), positions);
    }



    private static WeaponItem weapon(String path) {
        return ModItems.WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ProjectileWeaponItem projectileWeapon(String path) {
        return ModItems.PROJECTILE_WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ConsumableItem consumable(String path) {
        return ModItems.CONSUMABLES.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static PickupItem pickup(String displayName) {
        return ModItems.PICKUPS.stream()
                .filter(item -> item.getName(ItemStack.EMPTY).getString().equals(displayName))
                .findFirst()
                .orElseThrow();
    }

    private static Item named(List<? extends Item> items, String displayName) {
        return items.stream()
                .filter(item -> item.getName(ItemStack.EMPTY).getString().equals(displayName))
                .findFirst()
                .orElseThrow();
    }
    private static Item.Properties properties(String path) {
        ResourceKey<Item> key = ResourceKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath("fortniteinminecraft_test", path)
        );
        return new Item.Properties().setId(key).stacksTo(1);
    }
}
