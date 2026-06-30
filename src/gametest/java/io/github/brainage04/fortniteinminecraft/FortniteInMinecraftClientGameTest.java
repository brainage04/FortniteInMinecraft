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
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.player.GliderState;
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
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class FortniteInMinecraftClientGameTest implements FabricClientGameTest {
    private static final int DEDICATED_SERVER_JOIN_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;
    private static final String RECORDING_START_MARKER = "FIM_CLIENT_GAMETEST_RECORDING_START";
    private static final String RECORDING_END_MARKER = "FIM_CLIENT_GAMETEST_RECORDING_END";
    private static final BuildRules VISUAL_BUILD_RULES = BuildRules.defaults();
    private static final FootprintProjector VISUAL_FOOTPRINTS = new FootprintProjector(VISUAL_BUILD_RULES);
    private static final SnapGrid VISUAL_SNAP_GRID = new SnapGrid(VISUAL_BUILD_RULES);
    private static final int VISUAL_BUILD_Z_GRID = 4;

    @Override
    public void runTest(ClientGameTestContext context) {
        assertClientInitializerRan(context);

        Properties serverProperties = new Properties();
        serverProperties.setProperty("server-port", "25566");
        serverProperties.setProperty("simulation-distance", "5");
        serverProperties.setProperty("view-distance", "5");
        serverProperties.setProperty("level-type", "minecraft:flat");
        serverProperties.setProperty("generate-structures", "false");
        serverProperties.setProperty("generator-settings", "{}");
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
        int buildGridY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int surfaceY = prepareHologramDemoScene(level);
            String dimension = level.dimension().identifier().toString();
            int gridY = VISUAL_SNAP_GRID.snap(dimension, 0, surfaceY, 0).y();
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(5.5D, surfaceY, 30.5D);
            player.setYRot(180.0F);
            player.setXRot(8.0F);
            placeHolographicBuildPiece(level, player, gridY);
            return gridY;
        });

        context.waitTicks(20);
        context.runOnClient(client -> client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WALL)));
        String dimension = context.computeOnClient(client -> client.level.dimension().identifier().toString());
        showBuildPreview(context, server, BuildSlot.of(dimension, 1, buildGridY, VISUAL_BUILD_Z_GRID, PieceType.WALL, Orientation.SOUTH), MaterialType.WOOD, true, 80);
        showBuildPreview(context, server, BuildSlot.of(dimension, 3, buildGridY, VISUAL_BUILD_Z_GRID, PieceType.WALL, Orientation.SOUTH), MaterialType.STONE, false, 80);
        clearBuildPreview(context, server);
    }

    private static int prepareHologramDemoScene(ServerLevel level) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        for (int x = -24; x <= 40; x++) {
            for (int z = -8; z <= 64; z++) {
                for (int y = surfaceY; y <= surfaceY + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        placeChunkCornerPillars(level, surfaceY);
        placeFallRuler(level, surfaceY);
        return surfaceY;
    }

    private static void placeChunkCornerPillars(ServerLevel level, int surfaceY) {
        for (int chunkX = -1; chunkX <= 2; chunkX++) {
            for (int chunkZ = 0; chunkZ <= 4; chunkZ++) {
                int x = chunkX * 16 + 15;
                int z = chunkZ * 16 + 15;
                for (int y = surfaceY; y <= surfaceY + 28; y++) {
                    Block block = ((y - surfaceY) / 4) % 2 == 0 ? Blocks.WOOL.yellow() : Blocks.WOOL.black();
                    level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.setBlock(new BlockPos(x, surfaceY + 29, z), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void placeFallRuler(ServerLevel level, int surfaceY) {
        for (int y = surfaceY; y <= surfaceY + 72; y++) {
            Block block = ((y - surfaceY) / 4) % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.WOOL.blue();
            level.setBlock(new BlockPos(10, y, 47), block.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(new BlockPos(11, y, 47), block.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void placeHolographicBuildPiece(ServerLevel level, ServerPlayer player, int gridY) {
        BuildSlot slot = BuildSlot.of(level.dimension().identifier().toString(), -1, gridY, VISUAL_BUILD_Z_GRID, PieceType.WALL, Orientation.SOUTH);
        BuildPieceState piece = new BuildPieceState(
                UUID.randomUUID(),
                player.getUUID(),
                slot,
                MaterialType.METAL,
                MaterialType.METAL.finalHealth() / 5,
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
            ServerLevel level = player.level();
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(6.5D, surfaceY + 42.0D, 44.5D);
            player.setYRot(0.0F);
            player.setXRot(55.0F);
            player.setDeltaMovement(0.0D, -0.8D, 0.45D);
            player.setOnGround(false);
            MobilityItemInteractions.enableRedeploy(player, 160L);
            if (!MobilityItemInteractions.toggleGlider(player)) {
                throw new AssertionError("Expected dedicated-server glider redeploy to start.");
            }
        });

        context.waitTicks(140);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!MobilityItemInteractions.isGliding(player)) {
                throw new AssertionError("Expected dedicated-server glider to stay deployed.");
            }
            if (player.getDeltaMovement().y() < GliderState.DEFAULT_MAX_FALL_SPEED - 1.0E-6D) {
                throw new AssertionError("Expected dedicated-server glider to cap fall speed.");
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
