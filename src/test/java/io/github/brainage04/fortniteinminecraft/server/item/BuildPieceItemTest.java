package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.server.player.GrapplerProjectiles;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void weaponRightClickUseDoesNotStartCooldownWithoutClientInput() {
        WeaponItem weapon = ModItems.WEAPONS.get(0);
        ProjectileWeaponItem sniper = ModItems.PROJECTILE_WEAPONS.get(0);

        assertEquals(InteractionResult.PASS, weapon.use(null, null, InteractionHand.MAIN_HAND));
        assertEquals(InteractionResult.PASS, sniper.use(null, null, InteractionHand.MAIN_HAND));
    }

    @Test
    void gunAdsUsesSpyglassAnimationWithoutChangingReloadInput() {
        WeaponItem weapon = ModItems.WEAPONS.get(0);
        ProjectileWeaponItem sniper = ModItems.PROJECTILE_WEAPONS.get(0);

        assertEquals(net.minecraft.world.item.ItemUseAnimation.SPYGLASS, weapon.getUseAnimation(ItemStack.EMPTY));
        assertEquals(net.minecraft.world.item.ItemUseAnimation.SPYGLASS, sniper.getUseAnimation(ItemStack.EMPTY));
        assertEquals("Assault Rifle ADS: 30/30", weapon.statusText(30, true));
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
    void thermalScopedAssaultRifleIsAnAssaultRifle() {
        WeaponItem thermal = weapon("weapon_thermal_scoped_assault_rifle_legendary");

        assertEquals(WeaponCategory.ASSAULT_RIFLE, thermal.definition().category());
    }

    @Test
    void emptyMagazineStartsAutomaticReloadUnlessInfiniteAmmoIsEnabled() {
        assertEquals(WeaponItem.FireAttempt.EMPTY_MAGAZINE, WeaponItem.fireAttempt(0, false));
        assertTrue(WeaponItem.shouldAutoReload(0, false));
        assertFalse(WeaponItem.shouldAutoReload(1, false));
        assertFalse(WeaponItem.shouldAutoReload(0, true));
        assertEquals(WeaponItem.FireAttempt.FIRE, WeaponItem.fireAttempt(1, false));
        assertEquals(WeaponItem.FireAttempt.COOLDOWN, WeaponItem.fireAttempt(0, true));
    }

    @Test
    void hitscanRangeAndBloomFollowFortniteSpreadShape() {
        WeaponItem assault = weapon("weapon_assault_rifle_common");
        WeaponItem scoped = weapon("weapon_scoped_assault_rifle_legendary");
        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");

        assertEquals(512.0D, WeaponItem.effectiveHitscanRange(assault.definition()), 0.001D);
        assertTrue(scoped.adsFovMultiplier() < assault.adsFovMultiplier());
        assertTrue(bolt.adsFovMultiplier() < assault.adsFovMultiplier());

        WeaponItem.SpreadState idle = new WeaponItem.SpreadState(false, false, false, false, false, 0.0D);
        WeaponItem.SpreadState moving = new WeaponItem.SpreadState(false, false, false, false, true, 0.0D);
        WeaponItem.SpreadState crouching = new WeaponItem.SpreadState(false, true, false, false, false, 0.0D);
        WeaponItem.SpreadState ads = new WeaponItem.SpreadState(true, false, false, false, false, 0.0D);
        WeaponItem.SpreadState repeated = new WeaponItem.SpreadState(false, false, false, false, false, 0.35D);

        double idleSpread = WeaponItem.spreadDegrees(assault.definition(), idle);
        assertTrue(WeaponItem.spreadDegrees(assault.definition(), moving) > idleSpread);
        assertTrue(WeaponItem.spreadDegrees(assault.definition(), crouching) < WeaponItem.spreadDegrees(assault.definition(), moving));
        assertTrue(WeaponItem.spreadDegrees(assault.definition(), crouching) > 0.0D);
        assertTrue(WeaponItem.spreadDegrees(assault.definition(), ads) < idleSpread);
        assertTrue(WeaponItem.spreadDegrees(assault.definition(), repeated) > idleSpread);
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
        assertTrue(ModItems.WEAPONS.size() >= 223);
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
                "test_small_shield", "Small Shield", FortniteRarity.UNCOMMON, 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(10.0F, ConsumableItem.shieldCap(smallShield, 0.0F, 0.0F), 0.001F);
        assertEquals(5.0F, ConsumableItem.shieldAfterUse(smallShield, 0.0F, 0.0F), 0.001F);
    }

    @Test
    void shieldConsumablesRespectFortniteShieldCap() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", FortniteRarity.UNCOMMON, 2.03D, 0, 0, 25, 50, true, "small_shield"
        );

        assertEquals(10.0F, ConsumableItem.shieldAfterUse(smallShield, 8.0F, 0.0F), 0.001F);
        assertEquals(12.0F, ConsumableItem.shieldAfterUse(smallShield, 12.0F, 20.0F), 0.001F);
    }

    @Test
    void consumablesCannotStartWhenTheyWouldHaveNoEffect() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", FortniteRarity.UNCOMMON, 2.03D, 0, 0, 25, 50, true, "small_shield"
        );
        ConsumableDefinition bandage = new ConsumableDefinition(
                "test_bandage", "Bandage", FortniteRarity.COMMON, 3.53D, 15, 75, 0, 0, true, "bandage"
        );

        assertTrue(ConsumableItem.canBenefit(smallShield, 20.0F, 20.0F, 8.0F, 20.0F));
        assertFalse(ConsumableItem.canBenefit(smallShield, 20.0F, 20.0F, 10.0F, 20.0F));
        assertTrue(ConsumableItem.canBenefit(bandage, 10.0F, 20.0F, 0.0F, 0.0F));
        assertFalse(ConsumableItem.canBenefit(bandage, 15.0F, 20.0F, 0.0F, 0.0F));
    }

    @Test
    void effectiveConsumablesRestoreHealthFirstThenShield() {
        ConsumableDefinition slurpFish = new ConsumableDefinition(
                "test_slurp_fish", "Slurp Fish", FortniteRarity.RARE, 1.0D, 0, 0, 0, 0, false, "slurp_fish", 40
        );

        assertTrue(ConsumableItem.canBenefit(slurpFish, 20.0F, 20.0F, 0.0F, 20.0F));
        assertFalse(ConsumableItem.canBenefit(slurpFish, 20.0F, 20.0F, 8.0F, 8.0F));
        assertEquals(20.0F, ConsumableItem.healthAfterUse(slurpFish, 14.0F, 20.0F), 0.001F);
        assertEquals(8.0F, ConsumableItem.shieldAfterUse(slurpFish, 0.0F, 0.0F), 0.001F);
    }

    @Test
    void consumableProgressTextUsesElapsedAndNeededSecondsFromTicks() {
        ConsumableDefinition smallShield = new ConsumableDefinition(
                "test_small_shield", "Small Shield", FortniteRarity.UNCOMMON, 2.03D, 0, 0, 25, 50, true, "small_shield"
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
        assertTrue(ModItems.CONSUMABLES.size() >= 11);

        ConsumableItem apple = consumable("consumable_apple");
        assertEquals("Apple", apple.definition().displayName());
        assertEquals("WID_Athena_Apple", apple.definition().sourceItemId());
        assertEquals(5, apple.definition().healthRestore());
        assertFalse(apple.definition().movementLocked());

        ConsumableItem banana = consumable("consumable_banana");
        assertEquals("Banana", banana.definition().displayName());
        assertEquals("WID_Athena_Banana", banana.definition().sourceItemId());
        assertEquals(5, banana.definition().healthRestore());

        ConsumableItem cabbage = consumable("consumable_cabbage");
        assertEquals("Cabbage", cabbage.definition().displayName());
        assertEquals("WID_Athena_Cabbage", cabbage.definition().sourceItemId());
        assertEquals(5, cabbage.definition().healthRestore());

        ConsumableItem slurpFish = consumable("consumable_effective_fish");
        assertEquals("Slurp Fish", slurpFish.definition().displayName());
        assertEquals(40, slurpFish.definition().effectiveRestore());
        assertFalse(slurpFish.definition().movementLocked());
    }

    @Test
    void consumableRaritiesMatchSourceBackedFortniteData() {
        ConsumableItem bandage = consumable("consumable_bandage");
        ConsumableItem medkit = consumable("consumable_medkit");
        ConsumableItem smallShield = consumable("consumable_small_shield");
        ConsumableItem shieldPotion = consumable("consumable_shield_potion");
        ConsumableItem chugJug = consumable("consumable_full_restore_jug");

        assertEquals(FortniteRarity.COMMON, bandage.definition().rarity());
        assertEquals(FortniteRarity.UNCOMMON, medkit.definition().rarity());
        assertEquals(FortniteRarity.UNCOMMON, smallShield.definition().rarity());
        assertEquals(FortniteRarity.RARE, shieldPotion.definition().rarity());
        assertEquals(FortniteRarity.LEGENDARY, chugJug.definition().rarity());
        assertEquals("Athena_SuperMedkit", chugJug.definition().sourceItemId());
        assertEquals(TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GOLD), chugJug.getName(ItemStack.EMPTY).getStyle().getColor());
    }

    @Test
    void projectileWeaponCatalogSeparatesBallisticSnipersFromHitscan() {
        assertTrue(ModItems.PROJECTILE_WEAPONS.size() >= 64);

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
    void explosiveWeaponCatalogIncludesLaunchersAndBoomBow() {
        assertTrue(ModItems.EXPLOSIVE_WEAPONS.size() >= 50);

        ExplosiveProjectileWeaponItem rocket = explosiveWeapon("weapon_rocket_launcher_legendary");
        ExplosiveProjectileWeaponItem proximity = explosiveWeapon("weapon_proximity_grenade_launcher_legendary");
        ExplosiveProjectileWeaponItem boomBow = explosiveWeapon("weapon_boom_bow_legendary");
        ExplosiveProjectileWeaponItem shockwaveLauncher = explosiveWeapon("weapon_shockwave_launcher_epic");

        assertEquals("Rocket Launcher", rocket.definition().displayName());
        assertEquals(WeaponCategory.EXPLOSIVE, rocket.definition().category());
        assertEquals(TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GOLD), rocket.getName(ItemStack.EMPTY).getStyle().getColor());
        assertTrue(ModItems.isGun(rocket));
        assertEquals(net.minecraft.world.item.ItemUseAnimation.SPYGLASS, rocket.getUseAnimation(ItemStack.EMPTY));
        assertEquals("Rocket Launcher ADS: 1/1", rocket.statusText(1, true));
        assertTrue(rocket.explosiveDefinition().gravityFreeProjectile());
        assertTrue(proximity.explosiveDefinition().proximityTriggered());
        assertTrue(shockwaveLauncher.explosiveDefinition().hasImpulseOnly());
        assertEquals(ModItems.SHOCKWAVE_LAUNCHER_IMPACT_DELAY_TICKS, shockwaveLauncher.explosiveDefinition().impactExplosionDelayTicks());
        assertEquals(0L, boomBow.explosiveDefinition().impactExplosionDelayTicks());
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
        assertEquals(ModItems.WEAPONS.size() + ModItems.PROJECTILE_WEAPONS.size() + ModItems.EXPLOSIVE_WEAPONS.size()
                        + ModItems.THROWABLES.size() + ModItems.UTILITY_ITEMS.size() + ModItems.CONSUMABLES.size(),
                ModItems.COMBAT_ITEMS.size());
        assertEquals(ModItems.BUILD_PIECES.size() + ModItems.COMBAT_ITEMS.size() + ModItems.PICKUPS.size(),
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
    void hitFeedbackSoundDistinguishesHeadshotsFromNormalHits() {
        assertSame(SoundEvents.PLAYER_ATTACK_CRIT, WeaponItem.hitSound(false));
        assertSame(SoundEvents.PLAYER_LEVELUP, WeaponItem.hitSound(true));
        assertEquals(1.35F, WeaponItem.hitSoundPitch(false), 0.001F);
        assertEquals(1.65F, WeaponItem.hitSoundPitch(true), 0.001F);
    }

    @Test
    void heldWeaponInputGraceCanOutrunVanillaRightClickDelay() {
        assertTrue(WeaponAutoFire.HELD_USE_GRACE_TICKS > 4L);
    }

    @Test
    void harvestingToolCadenceRequiresFifteenTicksBetweenHits() {
        assertEquals(15, PickaxeItem.HARVEST_SWING_INTERVAL_TICKS);
        assertEquals(4.0F, PickaxeItem.DEFAULT_ENTITY_DAMAGE, 0.001F);
        assertFalse(PickaxeItem.canHarvestAt(100L, 114L));
        assertTrue(PickaxeItem.canHarvestAt(100L, 115L));
    }

    @Test
    void modCombatAndBuildItemsSuppressVanillaBlockBreaking() {
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.WALL));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.PICKAXE));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.WEAPONS.get(0)));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.PROJECTILE_WEAPONS.get(0)));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.EXPLOSIVE_WEAPONS.get(0)));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.CLINGER));
        assertTrue(ModItems.suppressesVanillaBlockBreaking(ModItems.RIFT_TO_GO));
        assertFalse(ModItems.suppressesVanillaBlockBreaking(Items.DIRT));
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
    void utilityCatalogIncludesMobilityToolsOnly() {
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.PICKAXE));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.GRAPPLER));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.LAUNCH_PAD));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.CLINGER));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.BOUNCER));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.RIFT_TO_GO));
        assertTrue(ModItems.UTILITY_ITEMS.contains(ModItems.PORT_A_FORT));
    }

    @Test
    void utilityItemsAreRealPlayableItems() {
        assertTrue(ModItems.CLINGER instanceof ExplosiveThrowableItem);
        assertTrue(ModItems.BOUNCER instanceof BouncerItem);
        assertTrue(ModItems.RIFT_TO_GO instanceof RiftToGoItem);
        assertTrue(ModItems.PORT_A_FORT instanceof PortAFortItem);
        assertTrue(ModItems.ALL_ITEMS.contains(ModItems.CLINGER));
        assertTrue(ModItems.ALL_ITEMS.contains(ModItems.BOUNCER));
        assertTrue(ModItems.ALL_ITEMS.contains(ModItems.RIFT_TO_GO));
        assertTrue(ModItems.ALL_ITEMS.contains(ModItems.PORT_A_FORT));
        assertEquals(FortniteRarity.RARE, ModItems.CLINGER.definition().rarity());
        assertEquals(FortniteRarity.EPIC, ModItems.RIFT_TO_GO.definition().rarity());
    }

    @Test
    void riftToGoPortalUsesFortniteActiveDuration() {
        assertEquals(200L, RiftToGoItem.DEFAULT_RIFT_PORTAL_ACTIVE_DURATION_TICKS);
    }

    @Test
    void projectileWeaponCooldownUsesReloadForSingleShotSnipers() {
        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");

        assertEquals(47, ProjectileWeaponItem.cooldownTicks(bolt.definition()));
        assertEquals(24.2F, ProjectileWeaponItem.minecraftDamage(bolt.definition()), 0.001F);
    }

    @Test
    void projectileWeaponsUseMagazineStateAndManualReload() {
        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");

        assertEquals("Bolt-Action Sniper Rifle: 1/1", bolt.statusText(1, false));
        assertEquals(WeaponItem.ManualReloadResult.STARTED,
                WeaponItem.manualReloadDecision(0, bolt.definition().stats().magazineSize(), 0L, 100L));
        assertEquals(WeaponItem.ManualReloadResult.ALREADY_RELOADING,
                WeaponItem.manualReloadDecision(0, bolt.definition().stats().magazineSize(), 200L, 100L));
    }

    @Test
    void headshotDamageUsesCriticalMultiplierAfterCartridgeCap() {
        WeaponItem assault = weapon("weapon_assault_rifle_legendary");
        assertEquals(7.2F, WeaponItem.minecraftDamage(assault.definition()), 0.001F);
        assertEquals(10.8F, WeaponItem.minecraftDamage(assault.definition(), true), 0.001F);

        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");
        assertEquals(24.2F, ProjectileWeaponItem.minecraftDamage(bolt.definition()), 0.001F);
        assertEquals(60.5F, ProjectileWeaponItem.minecraftDamage(bolt.definition(), true), 0.001F);

        WeaponItem pump = weapon("weapon_pump_shotgun_legendary");
        assertEquals(25.6F, WeaponItem.minecraftDamage(pump.definition()), 0.001F);
        assertEquals(47.36F, WeaponItem.minecraftDamage(pump.definition(), true), 0.001F);
    }

    @Test
    void sniperScopeSteadiesProjectileSpreadWithoutChangingBaseConfig() {
        assertEquals(0.4F, ProjectileWeaponItem.scopedInaccuracy(0.4F, false), 0.001F);
        assertEquals(0.1F, ProjectileWeaponItem.scopedInaccuracy(0.4F, true), 0.001F);
    }

    @Test
    void throwableImpulseCatalogUsesFortniteLaunchTuning() {
        ThrowableImpulseItem shockwave = throwable("shockwave_grenade");
        ThrowableImpulseItem impulse = throwable("impulse_grenade");

        assertEquals(5.0D, shockwave.definition().radius(), 1.0E-9D);
        assertEquals(1.9D, shockwave.definition().horizontalStrength(), 1.0E-9D);
        assertEquals(1.9D, shockwave.definition().verticalStrength(), 1.0E-9D);
        assertTrue(shockwave.definition().resetsFallDistance());
        assertEquals(3.15D, impulse.definition().horizontalStrength(), 1.0E-9D);
        assertEquals(2.1D, impulse.definition().verticalStrength(), 1.0E-9D);
    }

    @Test
    void grapplerUsesThirtyBlockProjectileWithFasterRope() {
        assertEquals(30.0D, ModItems.GRAPPLER.definition().rangeBlocks(), 1.0E-9D);
        assertEquals(1.8D, ModItems.GRAPPLER.definition().pullSpeed(), 1.0E-9D);
        assertEquals(0.85D, ModItems.GRAPPLER.definition().upwardBoost(), 1.0E-9D);
        assertEquals(4.0D, GrapplerProjectiles.PROJECTILE_SPEED_BLOCKS_PER_TICK, 1.0E-9D);
    }

    @Test
    void grapplerPullVelocityAimsAtTargetWithReducedLift() {
        Vec3 velocity = GrapplerItem.pullVelocity(new Vec3(0.0D, 64.0D, 0.0D), new Vec3(4.0D, 64.0D, 0.0D), 1.6D, 0.25D);

        assertEquals(1.6D, velocity.x(), 1.0E-9D);
        assertEquals(0.1125D, velocity.y(), 1.0E-9D);
        assertEquals(0.0D, velocity.z(), 1.0E-9D);
    }


    @Test
    void everyRegisteredItemHasGeneratedClientAssets() {
        Path itemDefinitionDir = Path.of("src/main/resources/assets/fortniteinminecraft/items");
        Path itemModelDir = Path.of("src/main/resources/assets/fortniteinminecraft/models/item");
        Path textureDir = Path.of("src/main/resources/assets/fortniteinminecraft/textures/item");

        for (Item item : ModItems.ALL_ITEMS) {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            assertTrue(Files.exists(itemDefinitionDir.resolve(path + ".json")), path + " item definition missing");
            assertTrue(Files.exists(itemModelDir.resolve(path + ".json")), path + " model missing");
            assertTrue(Files.exists(textureDir.resolve(path + ".png")), path + " texture missing");
        }
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

    private static ExplosiveProjectileWeaponItem explosiveWeapon(String path) {
        return ModItems.EXPLOSIVE_WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ThrowableImpulseItem throwable(String path) {
        return ModItems.THROWABLES.stream()
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

    private static Item.Properties properties(String path) {
        ResourceKey<Item> key = ResourceKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath("fortniteinminecraft_test", path)
        );
        return new Item.Properties().setId(key).stacksTo(1);
    }
}
