package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.server.player.GliderState;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FortniteInMinecraftGameTest {
    @GameTest
    public void serverCommandRootIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("fim") == null) {
            throw new AssertionError("Expected the /fim command root to be registered on the dedicated server.");
        }

        context.succeed();
    }

    @GameTest(maxTicks = 80)
    public void gliderStaysDeployedAndTravelsAfterRedeploy(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        Vec3 start = context.absoluteVec(new Vec3(2.0D, 20.0D, 2.0D));
        player.snapTo(start.x(), start.y(), start.z(), 0.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        player.setDeltaMovement(0.0D, -0.6D, 0.0D);

        MobilityItemInteractions.enableRedeploy(player, 100L);
        context.assertTrue(MobilityItemInteractions.toggleGlider(player), "Expected redeploy window to trigger glider.");
        context.assertTrue(MobilityItemInteractions.isGliding(player), "Expected glider to be active immediately after toggle.");

        context.runAtTickTime(40, () -> {
            context.assertTrue(MobilityItemInteractions.isGliding(player), "Expected glider to remain active after redeploy tick flow.");
            context.assertTrue(player.getDeltaMovement().y() >= GliderState.DEFAULT_MAX_FALL_SPEED - 1.0E-6D,
                    "Expected glider to cap fall speed instead of entering freefall.");
            double horizontalSpeedSqr = player.getDeltaMovement().x() * player.getDeltaMovement().x()
                    + player.getDeltaMovement().z() * player.getDeltaMovement().z();
            context.assertTrue(horizontalSpeedSqr > 0.4D * 0.4D,
                    "Expected deployed glider to keep generating traversal velocity.");
            context.succeed();
        });
    }
}
