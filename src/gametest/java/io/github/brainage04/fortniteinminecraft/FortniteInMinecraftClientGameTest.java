package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildHooks;
import io.github.brainage04.fortniteinminecraft.client.ClientResourceWalletHud;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class FortniteInMinecraftClientGameTest implements FabricClientGameTest {
    private static final int DEDICATED_SERVER_JOIN_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;

    @Override
    public void runTest(ClientGameTestContext context) {
        assertClientInitializerRan(context);

        Properties serverProperties = new Properties();
        serverProperties.setProperty("server-port", "25566");
        serverProperties.setProperty("simulation-distance", "5");
        serverProperties.setProperty("view-distance", "2");

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
            connectToDedicatedServer(context, server);
            assertClientWorldAndPlayerAvailable(context);
            demonstrateGliderRedeploy(context, server);
            disconnectFromDedicatedServer(context);
        }
    }

    private static void assertClientInitializerRan(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!FortniteInMinecraftClient.isInitialized()) {
                throw new AssertionError("Expected the client initializer to run before the client GameTest.");
            }
            if (!ClientBuildHooks.isInitialized()) {
                throw new AssertionError("Expected client preview, HUD, and input hooks to initialize.");
            }
            if (!ClientResourceWalletHud.isInitialized()) {
                throw new AssertionError("Expected the resource wallet HUD to initialize.");
            }
        });
    }

    private static void connectToDedicatedServer(ClientGameTestContext context, TestDedicatedServerContext server) {
        String address = "localhost:" + server.computeOnServer(minecraftServer -> minecraftServer.getPort());

        context.runOnClient(client -> {
            ServerData serverData = new ServerData("FortniteInMinecraft GameTest", address, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(
                    client.gui.screen(),
                    client,
                    ServerAddress.parseString(address),
                    serverData,
                    false,
                    null
            );
        });

        waitForDedicatedServerJoin(context);
    }

    private static void waitForDedicatedServerJoin(ClientGameTestContext context) {
        for (int tick = 0; tick < DEDICATED_SERVER_JOIN_TIMEOUT_TICKS; tick++) {
            acceptServerResourcePackPrompt(context);

            if (context.computeOnClient(client ->
                    client.level != null
                            && client.player != null
                            && !(client.gui.screen() instanceof LevelLoadingScreen))) {
                return;
            }

            context.waitTick();
        }

        String screenName = context.computeOnClient(client -> {
            Screen screen = client.gui.screen();
            return screen == null ? "<none>" : screen.getClass().getName();
        });
        throw new AssertionError("Timed out joining the dedicated server; current screen is " + screenName + ".");
    }

    private static void acceptServerResourcePackPrompt(ClientGameTestContext context) {
        if (!context.computeOnClient(client -> isServerResourcePackPrompt(client.gui.screen()))) {
            return;
        }

        if (context.tryClickScreenButton("gui.continue") || context.tryClickScreenButton("gui.yes")) {
            return;
        }

        throw new AssertionError("Detected a server resource-pack prompt, but could not find its accept button.");
    }

    private static boolean isServerResourcePackPrompt(Screen screen) {
        if (!(screen instanceof ConfirmScreen)) {
            return false;
        }

        if (!(screen.getTitle().getContents() instanceof TranslatableContents contents)) {
            return false;
        }

        return "multiplayer.texturePrompt.line1".equals(contents.getKey())
                || "multiplayer.requiredTexturePrompt.line1".equals(contents.getKey());
    }

    private static void demonstrateGliderRedeploy(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(0.5D, 100.0D, 0.5D);
            player.setYRot(0.0F);
            player.setXRot(15.0F);
            player.setDeltaMovement(0.0D, -0.8D, 0.6D);
            player.setOnGround(false);
            MobilityItemInteractions.enableRedeploy(player, 100L);
            if (!MobilityItemInteractions.toggleGlider(player)) {
                throw new AssertionError("Expected dedicated-server glider redeploy to start.");
            }
        });

        context.waitTicks(60);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!MobilityItemInteractions.isGliding(player)) {
                throw new AssertionError("Expected dedicated-server glider to stay deployed.");
            }
        });
        context.waitTicks(20);
    }

    private static void assertClientWorldAndPlayerAvailable(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.level == null) {
                throw new AssertionError("Expected a client world after joining the dedicated server.");
            }

            if (client.player == null) {
                throw new AssertionError("Expected a local client player after joining the dedicated server.");
            }
        });
    }

    private static void disconnectFromDedicatedServer(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.level == null) {
                return;
            }

            client.level.disconnect(Component.literal("Disconnecting"));
            client.disconnectWithSavingScreen();
        });

        context.waitFor(client -> client.level == null);
        context.waitTicks(2);
        context.setScreen(TitleScreen::new);
    }
}
