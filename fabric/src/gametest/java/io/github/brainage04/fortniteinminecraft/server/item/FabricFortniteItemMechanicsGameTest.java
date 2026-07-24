package io.github.brainage04.fortniteinminecraft.server.item;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricFortniteItemMechanicsGameTest {
    private final FortniteItemMechanicsGameTest tests = new FortniteItemMechanicsGameTest();

    @GameTest
    public void impulseAndShockwaveGrenadesPushSeveralRelativePositions(GameTestHelper context) {
        tests.impulseAndShockwaveGrenadesPushSeveralRelativePositions(context);
    }

    @GameTest
    public void explosiveGrenadesAndClingersCarryDamageAndDetonationRules(GameTestHelper context) {
        tests.explosiveGrenadesAndClingersCarryDamageAndDetonationRules(context);
    }

    @GameTest
    public void weaponBurstCatalogStatsDriveCartridgeCounts(GameTestHelper context) {
        tests.weaponBurstCatalogStatsDriveCartridgeCounts(context);
    }

    @GameTest
    public void weaponDamageAtZeroHealthClearsBuildPiece(GameTestHelper context) {
        tests.weaponDamageAtZeroHealthClearsBuildPiece(context);
    }

    @GameTest
    public void pickaxeWeakPointHitAppliesMultiplierThroughDamageBlockHit(GameTestHelper context) {
        tests.pickaxeWeakPointHitAppliesMultiplierThroughDamageBlockHit(context);
    }

    @GameTest
    public void weaponWeakPointHitAppliesMultiplierThroughDamageBuild(GameTestHelper context) {
        tests.weaponWeakPointHitAppliesMultiplierThroughDamageBuild(context);
    }

    @GameTest
    public void pickaxeTerrainWeakPointHarvestDestroysLogAndGrantsWood(GameTestHelper context) {
        tests.pickaxeTerrainWeakPointHarvestDestroysLogAndGrantsWood(context);
    }

    @GameTest
    public void harvestingToolInventoryRestoresMissingAndRemovesDuplicates(GameTestHelper context) {
        tests.harvestingToolInventoryRestoresMissingAndRemovesDuplicates(context);
    }

    @GameTest
    public void harvestingToolInventoryCountsCarriedToolDuringMoves(GameTestHelper context) {
        tests.harvestingToolInventoryCountsCarriedToolDuringMoves(context);
    }

    @GameTest
    public void launchPadFootprintPlacesNineTriggerBlocks(GameTestHelper context) {
        tests.launchPadFootprintPlacesNineTriggerBlocks(context);
    }

    @GameTest
    public void bouncerFootprintPlacesCurrentFloorSizedSurface(GameTestHelper context) {
        tests.bouncerFootprintPlacesCurrentFloorSizedSurface(context);
    }

    @GameTest
    public void bouncerWallFootprintPlacesCurrentFloorSizedTriggerPlane(GameTestHelper context) {
        tests.bouncerWallFootprintPlacesCurrentFloorSizedTriggerPlane(context);
    }

    @GameTest(maxTicks = 40)
    public void wallBouncerTriggerLaunchesTouchingPlayerOutward(GameTestHelper context) {
        tests.wallBouncerTriggerLaunchesTouchingPlayerOutward(context);
    }

    @GameTest
    public void bouncerItemPlacesFloorAndWallTriggerFootprints(GameTestHelper context) {
        tests.bouncerItemPlacesFloorAndWallTriggerFootprints(context);
    }

    @GameTest(maxTicks = 40)
    public void floorBouncerLaunchesJumpingPlayerUpward(GameTestHelper context) {
        tests.floorBouncerLaunchesJumpingPlayerUpward(context);
    }

    @GameTest(maxTicks = 40)
    public void runningJumpingIntoWallBouncerLaunchesPlayerOutward(GameTestHelper context) {
        tests.runningJumpingIntoWallBouncerLaunchesPlayerOutward(context);
    }

    @GameTest
    public void harvestingToolDamagesWallThroughBouncerTrigger(GameTestHelper context) {
        tests.harvestingToolDamagesWallThroughBouncerTrigger(context);
    }

    @GameTest
    public void weaponDamageBreaksWallBouncerSupportAndClearsTrigger(GameTestHelper context) {
        tests.weaponDamageBreaksWallBouncerSupportAndClearsTrigger(context);
    }

    @GameTest
    public void explosiveDamageBreaksFloorBouncerSupportAndClearsTrigger(GameTestHelper context) {
        tests.explosiveDamageBreaksFloorBouncerSupportAndClearsTrigger(context);
    }

    @GameTest
    public void deployableFootprintRejectsBlockedCellsWithoutPartialPlacement(GameTestHelper context) {
        tests.deployableFootprintRejectsBlockedCellsWithoutPartialPlacement(context);
    }

    @GameTest
    public void lootContainerOpenClearsBlocksAndDropsMatchingLoot(GameTestHelper context) {
        tests.lootContainerOpenClearsBlocksAndDropsMatchingLoot(context);
    }

    @GameTest
    public void portAFortUseThrowsProjectileConsumesStackAndStartsCooldown(GameTestHelper context) {
        tests.portAFortUseThrowsProjectileConsumesStackAndStartsCooldown(context);
    }

    @GameTest
    public void portAFortDeployMaterializesTrackedMetalFort(GameTestHelper context) {
        tests.portAFortDeployMaterializesTrackedMetalFort(context);
    }

    @GameTest
    public void portAFortDeployRollsBackPartialFortWhenFootprintConflicts(GameTestHelper context) {
        tests.portAFortDeployRollsBackPartialFortWhenFootprintConflicts(context);
    }
}
