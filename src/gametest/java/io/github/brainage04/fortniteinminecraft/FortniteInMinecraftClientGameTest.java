package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildHooks;
import io.github.brainage04.fortniteinminecraft.client.ClientBuildPreview;
import io.github.brainage04.fortniteinminecraft.client.ClientResourceWalletHud;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class FortniteInMinecraftClientGameTest implements FabricClientGameTest {
    private static final int DEDICATED_SERVER_JOIN_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;
    private static final String RECORDING_START_MARKER = "FIM_CLIENT_GAMETEST_RECORDING_START";
    private static final String RECORDING_END_MARKER = "FIM_CLIENT_GAMETEST_RECORDING_END";
    private static final BuildRules VISUAL_BUILD_RULES = BuildRules.defaults();
    private static final FootprintProjector VISUAL_FOOTPRINTS = new FootprintProjector(VISUAL_BUILD_RULES);


    @Override
    public void runTest(ClientGameTestContext context) {
        assertClientInitializerRan(context);

        Properties serverProperties = new Properties();
        serverProperties.setProperty("server-port", "25566");
        serverProperties.setProperty("simulation-distance", "5");
        serverProperties.setProperty("view-distance", "5");
        serverProperties.setProperty("level-type", "minecraft:flat");
        serverProperties.setProperty("generate-structures", "false");
        serverProperties.setProperty("spawn-protection", "0");

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
            connectToDedicatedServer(context, server);
            assertClientWorldAndPlayerAvailable(context);
            markRecordingStart();
            demonstrateBuildPreviewAndHolographicPieces(context, server);
            demonstrateGliderRedeploy(context, server);
            markRecordingEnd();
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

    private static void markRecordingStart() {
        System.out.println(RECORDING_START_MARKER);
    }

    private static void markRecordingEnd() {
        System.out.println(RECORDING_END_MARKER);
    }

    private static void demonstrateBuildPreviewAndHolographicPieces(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            prepareHologramDemoScene(level);
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(0.5D, 69.0D, 22.5D);
            player.setYRot(180.0F);
            player.setXRot(18.0F);
            placeHolographicBuildPiece(level, player);
        });

        context.waitTicks(10);
        context.runOnClient(client -> client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WALL)));
        String dimension = context.computeOnClient(client -> client.level.dimension().identifier().toString());
        showBuildPreview(context, server, BuildSlot.of(dimension, 1, 16, 0, PieceType.WALL, Orientation.SOUTH), MaterialType.WOOD, true, 50);
        showBuildPreview(context, server, BuildSlot.of(dimension, -3, 16, 0, PieceType.WALL, Orientation.SOUTH), MaterialType.STONE, false, 50);
        clearBuildPreview(context, server);
    }

    private static void prepareHologramDemoScene(ServerLevel level) {
        for (int x = -18; x <= 12; x++) {
            for (int z = -4; z <= 24; z++) {
                level.setBlock(new BlockPos(x, 62, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                for (int y = 63; y <= 72; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void placeHolographicBuildPiece(ServerLevel level, ServerPlayer player) {
        BuildSlot slot = BuildSlot.of(level.dimension().identifier().toString(), -1, 16, 0, PieceType.WALL, Orientation.SOUTH);
        BuildPieceState piece = new BuildPieceState(
                UUID.randomUUID(),
                player.getUUID(),
                slot,
                MaterialType.METAL,
                MaterialType.METAL.finalHealth() / 2,
                MaterialType.METAL.finalHealth(),
                level.getGameTime(),
                level.getGameTime(),
                BuildPieceState.BASE_VARIANT
        );
        PieceFootprint footprint = VISUAL_FOOTPRINTS.project(piece);
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(VISUAL_BUILD_RULES);
        WorldBuildWriteResult result = materializer.place(level, piece, footprint);
        if (!result.success()) {
            throw new AssertionError("Expected demo build piece to materialize: " + result.message());
        }

        int hologramBlocks = 0;
        int solidBlocks = 0;
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            Block block = level.getBlockState(pos).getBlock();
            if (block == BuildVisualBlocks.HOLOGRAM_METAL) {
                hologramBlocks++;
            } else if (block == Blocks.COPPER_BLOCK.waxed().unaffected()) {
                solidBlocks++;
            }
        }
        if (hologramBlocks == 0 || solidBlocks == 0) {
            throw new AssertionError("Expected damaged demo piece to show both holographic and solid blocks, saw "
                    + hologramBlocks + " holographic and " + solidBlocks + " solid.");
        }
    }

    private static void showBuildPreview(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            BuildSlot slot,
            MaterialType material,
            boolean valid,
            int ticks
    ) {
        for (int remaining = ticks; remaining > 0; remaining -= 2) {
            context.runOnClient(client -> client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WALL)));
            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                if (!ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
                    throw new AssertionError("Expected the client to accept build preview payloads.");
                }
                ServerPlayNetworking.send(player, BuildPreviewPayload.active(slot, material, valid));
            });
            context.waitTicks(2);
            assertPreviewSnapshot(context, slot, material, valid);
        }
    }

    private static void clearBuildPreview(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
                ServerPlayNetworking.send(player, BuildPreviewPayload.inactive());
            }
        });
        context.waitTicks(6);
        context.runOnClient(client -> {
            if (ClientBuildPreview.snapshot().active()) {
                throw new AssertionError("Expected build preview displays to clear before the glider segment.");
            }
        });
    }

    private static void assertPreviewSnapshot(ClientGameTestContext context, BuildSlot slot, MaterialType material, boolean valid) {
        context.runOnClient(client -> {
            ClientBuildPreview.Snapshot snapshot = ClientBuildPreview.snapshot();
            if (!snapshot.active()) {
                throw new AssertionError("Expected active build preview snapshot.");
            }
            if (snapshot.valid() != valid || snapshot.material() != material || !slot.equals(snapshot.slot())) {
                throw new AssertionError("Unexpected build preview snapshot: " + snapshot);
            }
            if (snapshot.boxes().isEmpty()) {
                throw new AssertionError("Expected preview to render block-display boxes.");
            }
        });
    }

    private static void demonstrateGliderRedeploy(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(0.5D, 82.0D, 18.5D);
            player.setYRot(0.0F);
            player.setXRot(12.0F);
            player.setDeltaMovement(0.0D, -0.8D, 0.6D);
            player.setOnGround(false);
            MobilityItemInteractions.enableRedeploy(player, 100L);
            if (!MobilityItemInteractions.toggleGlider(player)) {
                throw new AssertionError("Expected dedicated-server glider redeploy to start.");
            }
        });

        context.waitTicks(100);
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
