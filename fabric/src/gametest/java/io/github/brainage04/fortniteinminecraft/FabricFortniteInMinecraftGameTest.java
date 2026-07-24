package io.github.brainage04.fortniteinminecraft;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricFortniteInMinecraftGameTest {
    private final FortniteInMinecraftGameTest tests = new FortniteInMinecraftGameTest();

    @GameTest
    public void serverCommandRootIsRegistered(GameTestHelper context) {
        tests.serverCommandRootIsRegistered(context);
    }

    @GameTest(maxTicks = 80)
    public void gliderStaysDeployedAndTravelsAfterRedeploy(GameTestHelper context) {
        tests.gliderStaysDeployedAndTravelsAfterRedeploy(context);
    }

    @GameTest
    public void hitscanWeaponDamagesLivingTarget(GameTestHelper context) {
        tests.hitscanWeaponDamagesLivingTarget(context);
    }

    @GameTest
    public void hitscanShieldedMobKeepsHealthAndShowsBlueHitMarker(GameTestHelper context) {
        tests.hitscanShieldedMobKeepsHealthAndShowsBlueHitMarker(context);
    }

    @GameTest(maxTicks = 80)
    public void projectileWeaponDamagesLivingTarget(GameTestHelper context) {
        tests.projectileWeaponDamagesLivingTarget(context);
    }

    @GameTest
    public void consumablesRestoreHealthAndShieldInWorld(GameTestHelper context) {
        tests.consumablesRestoreHealthAndShieldInWorld(context);
    }

    @GameTest
    public void launchPadAndBouncerEnableRedeploy(GameTestHelper context) {
        tests.launchPadAndBouncerEnableRedeploy(context);
    }

    @GameTest
    public void buildPiecesMaterializeEveryPieceAndMaterial(GameTestHelper context) {
        tests.buildPiecesMaterializeEveryPieceAndMaterial(context);
    }

    @GameTest
    public void supportCascadeSeparatesGroundedAndUnsupportedPieces(GameTestHelper context) {
        tests.supportCascadeSeparatesGroundedAndUnsupportedPieces(context);
    }

    @GameTest
    public void damageDestroyAndSupportCollapseUpdateWorldState(GameTestHelper context) {
        tests.damageDestroyAndSupportCollapseUpdateWorldState(context);
    }

    @GameTest
    public void editMasksKeepPartialPiecesAndRejectEmptyPieces(GameTestHelper context) {
        tests.editMasksKeepPartialPiecesAndRejectEmptyPieces(context);
    }
}
