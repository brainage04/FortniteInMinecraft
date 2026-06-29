package io.github.brainage04.fortniteinminecraft;

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
}
