package io.github.brainage04.fortniteinminecraft.server.player;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricFortniteMobilityGameTest {
    private final FortniteMobilityGameTest tests = new FortniteMobilityGameTest();

    @GameTest
    public void sprintCrouchSlidePreservesDirectionAndAcceleratesDownhill(GameTestHelper context) {
        tests.sprintCrouchSlidePreservesDirectionAndAcceleratesDownhill(context);
    }
}
