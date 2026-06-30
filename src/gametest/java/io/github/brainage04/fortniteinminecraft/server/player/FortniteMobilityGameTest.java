package io.github.brainage04.fortniteinminecraft.server.player;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

public final class FortniteMobilityGameTest {
    @GameTest
    public void sprintCrouchSlidePreservesDirectionAndAcceleratesDownhill(GameTestHelper context) {
        Vec3 look = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 startingVelocity = new Vec3(0.2D, 0.0D, 0.0D);

        Vec3 flat = MobilityItemInteractions.slideVelocity(startingVelocity, look, 0.0D);
        Vec3 downhill = MobilityItemInteractions.slideVelocity(startingVelocity, look, -0.5D);
        Vec3 fromRest = MobilityItemInteractions.slideVelocity(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D), 0.0D);

        context.assertTrue(flat.x() > startingVelocity.x(), "Expected sprint-crouch sliding to accelerate on flat ground.");
        context.assertTrue(downhill.x() > flat.x(), "Expected downhill sprint-crouch sliding to accelerate more than flat sliding.");
        context.assertTrue(fromRest.z() > 0.0D && Math.abs(fromRest.x()) < 1.0E-9D,
                "Expected sprint-crouch sliding from rest to use the player's look direction.");
        context.succeed();
    }
}
