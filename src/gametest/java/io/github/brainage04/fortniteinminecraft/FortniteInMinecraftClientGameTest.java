package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildHooks;
import io.github.brainage04.fortniteinminecraft.client.ClientBuildPreview;
import io.github.brainage04.fortniteinminecraft.client.ClientGameTestFeedbackHud;
import io.github.brainage04.fortniteinminecraft.client.ClientResourceWalletHud;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.DeployableTriggerBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.DeployableGameTestHooks;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.ExplosiveProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.PickaxeItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.TrapTriggerBlock;
import io.github.brainage04.fortniteinminecraft.server.item.ThrowableImpulseItem;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.WorldObstructions;
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
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
public final class FortniteInMinecraftClientGameTest implements FabricClientGameTest {
    private static final int DEDICATED_SERVER_JOIN_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;
    private static final BuildRules VISUAL_BUILD_RULES = BuildRules.defaults();
    private static final FootprintProjector VISUAL_FOOTPRINTS = new FootprintProjector(VISUAL_BUILD_RULES);
    private static final SnapGrid VISUAL_SNAP_GRID = new SnapGrid(VISUAL_BUILD_RULES);
    private static final int PREVIEW_DEMONSTRATION_TICKS = 20;
    private static final int VISUAL_BUILD_Z_GRID = 8;
    private static final int VISUAL_GALLERY_MATERIAL_ROW_STRIDE_GRID = 1;
    private static final int VISUAL_GALLERY_COLUMN_STRIDE_GRID = 2;
    private static final int VISUAL_GALLERY_Z_GRID = 8;

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
            assertPreviewCellsUseExactBlockSize();
            context.runOnClient(client -> ClientGameTestFeedbackHud.clear());
            FimRecordingSignal.signalReadyToRecord(context);
            try {
                demonstrateBuildPreviewAndHolographicPieces(context, server);
                demonstrateBuildGrowthGallery(context, server);
                demonstrateBuildDamageAndCollapse(context, server);
                demonstrateDeployableTrapTriggers(context, server);
                demonstrateMotionItems(context, server);
                demonstrateGliderRedeploy(context, server);
            } finally {
                disconnectFromDedicatedServer(context);
            }
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
            if (!ClientGameTestFeedbackHud.isInitialized()) {
                throw new AssertionError("Expected the gametest feedback HUD to initialize.");
            }
        });
    }

    private static void assertPreviewCellsUseExactBlockSize() {
        double cellSize = ClientBuildPreview.previewCellSizeBlocks();
        double expectedCellSize = 1.0D;
        if (Math.abs(cellSize - expectedCellSize) > 0.000_01D) {
            throw new AssertionError("Expected preview cells to render at exactly 1.0 block, saw " + cellSize + ".");
        }
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


    private static void announceGametestStep(ClientGameTestContext context, String id, String title, String subtitle) {
        String message = "[FIM_CLIENT_GAMETEST] " + id + " | " + title + (subtitle.isBlank() ? "" : " | " + subtitle);
        System.out.println(message);
        context.runOnClient(client -> ClientGameTestFeedbackHud.showStep(id, title, subtitle));
    }

    private static void demonstrateBuildPreviewAndHolographicPieces(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        int buildGridY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int gridY = prepareHologramDemoScene(level);
            String dimension = level.dimension().identifier().toString();
            int previewGroundY = visualOriginY(dimension, gridY);
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(5.5D, previewGroundY, 27.5D);
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            player.setDeltaMovement(Vec3.ZERO);
            return gridY;
        });

        context.waitTicks(20);
        announceGametestStep(context, "preview.setup", "Build previews", "valid area, then invalid area");
        String dimension = context.computeOnClient(client -> client.level.dimension().identifier().toString());
        suppressAutomaticPreview(server, true);
        try {
            cycleBuildPreviewArea(context, server, dimension, 0, buildGridY, VISUAL_BUILD_Z_GRID, MaterialType.WOOD, true, "valid placement");
            cycleBuildPreviewArea(context, server, dimension, 2, buildGridY, VISUAL_BUILD_Z_GRID, MaterialType.METAL, false, "invalid placement");
        } finally {
            clearBuildPreview(context, server);
            suppressAutomaticPreview(server, false);
        }
    }

    private static void cycleBuildPreviewArea(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            String dimension,
            int gridX,
            int gridY,
            int gridZ,
            MaterialType material,
            boolean valid,
            String areaLabel
    ) {
        for (PieceType pieceType : PieceType.values()) {
            BuildSlot slot = BuildSlot.of(dimension, gridX, gridY, gridZ, pieceType, Orientation.SOUTH);
            String validity = valid ? "Valid" : "Invalid";
            announceGametestStep(
                    context,
                    "preview." + validity.toLowerCase() + "." + pieceType.name(),
                    validity + " " + pieceType.name() + " preview",
                    areaLabel + " • " + material.name()
            );
            showBuildPreview(context, server, slot, material, valid, PREVIEW_DEMONSTRATION_TICKS);
        }
    }

    private static int prepareHologramDemoScene(ServerLevel level) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        String dimension = level.dimension().identifier().toString();
        int gridY = VISUAL_SNAP_GRID.snap(dimension, 0, surfaceY, 0).y();
        int originY = visualOriginY(dimension, gridY);
        clearAir(level, -24, 40, originY, originY + 12, -8, 64);
        fillFloor(level, -24, 40, originY - 1, -8, 64, Blocks.GRASS_BLOCK.defaultBlockState());
        return gridY;
    }


    private static void demonstrateBuildGrowthGallery(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        announceGametestStep(context, "growth.gallery", "Build growth gallery", "wood, stone, and metal pieces finishing");
        VisualBuildContext gallery = new VisualBuildContext(
                new BuildWorldState(),
                WorldBuildMaterializer.defaults(VISUAL_BUILD_RULES)
        );
        int gridY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int surfaceY = prepareGrowthGalleryScene(level);
            int galleryGridY = VISUAL_SNAP_GRID.snap(level.dimension().identifier().toString(), 0, surfaceY, 0).y();
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(1.0D, surfaceY + 7.0D, 15.5D);
            player.setYRot(0.0F);
            player.setXRot(10.0F);
            player.setDeltaMovement(Vec3.ZERO);

            long tick = level.getGameTime();
            for (BuildSlot slot : growthGallerySlots(level.dimension().identifier().toString(), galleryGridY)) {
                placeVisualPiece(level, gallery, BuildPieceState.placed(slot, galleryMaterial(slot, galleryGridY), player.getUUID(), tick));
            }
            assertConstructionVisible(level, growthGallerySlots(level.dimension().identifier().toString(), galleryGridY), "growth gallery");
            return galleryGridY;
        });

        for (int elapsed = 0; elapsed < 520; elapsed += 10) {
            context.waitTicks(10);
            server.runOnServer(minecraftServer -> progressVisualPieces(
                    minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                    gallery
            ));
        }
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertFullyBuilt(level, growthGallerySlots(level.dimension().identifier().toString(), gridY), "growth gallery");
        });
        context.waitTicks(40);
    }

    private static int prepareGrowthGalleryScene(ServerLevel level) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 48);
        clearAir(level, -24, 28, surfaceY, surfaceY + 18, 12, 56);
        return surfaceY;
    }

    private static List<BuildSlot> growthGallerySlots(String dimension, int gridY) {
        ArrayList<BuildSlot> slots = new ArrayList<>();
        int pieceCount = PieceType.values().length;
        int centeredStartX = -((pieceCount - 1) * VISUAL_GALLERY_COLUMN_STRIDE_GRID) / 2;
        int materialIndex = 0;
        for (MaterialType ignored : MaterialType.values()) {
            int pieceIndex = 0;
            for (PieceType pieceType : PieceType.values()) {
                slots.add(BuildSlot.of(
                        dimension,
                        centeredStartX + pieceIndex * VISUAL_GALLERY_COLUMN_STRIDE_GRID,
                        gridY + materialIndex * VISUAL_GALLERY_MATERIAL_ROW_STRIDE_GRID,
                        VISUAL_GALLERY_Z_GRID,
                        pieceType,
                        Orientation.SOUTH
                ));
                pieceIndex++;
            }
            materialIndex++;
        }
        return List.copyOf(slots);
    }

    private static MaterialType galleryMaterial(BuildSlot slot, int baseGridY) {
        int row = (slot.gridPos().y() - baseGridY) / VISUAL_GALLERY_MATERIAL_ROW_STRIDE_GRID;
        return MaterialType.values()[row];
    }

    private static void demonstrateBuildDamageAndCollapse(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        announceGametestStep(context, "damage.setup", "Build damage and collapse", "pickaxe, rifle, and support break");
        VisualBuildContext combat = new VisualBuildContext(
                new BuildWorldState(),
                WorldBuildMaterializer.defaults(VISUAL_BUILD_RULES)
        );
        int gridY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int surfaceY = prepareDamageScene(level);
            int damageGridY = VISUAL_SNAP_GRID.snap(level.dimension().identifier().toString(), 0, surfaceY, 0).y();
            player.setGameMode(GameType.CREATIVE);
            player.setYRot(0.0F);
            player.setXRot(0.0F);

            configureCombatVisualSystems(combat);
            long tick = level.getGameTime();
            placeVisualPiece(level, combat, fullyBuiltPiece(pickaxeWeakPointDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            placeVisualPiece(level, combat, fullyBuiltPiece(pickaxeDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            placeVisualPiece(level, combat, fullyBuiltPiece(rifleWeakPointDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            placeVisualPiece(level, combat, fullyBuiltPiece(rifleDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            placeVisualPiece(level, combat, fullyBuiltPiece(supportDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            placeVisualPiece(level, combat, fullyBuiltPiece(dependentDemoSlot(level, damageGridY), MaterialType.WOOD, player.getUUID(), tick));
            stagePickaxeAt(minecraftServer, pickaxeWeakPointDemoSlot(level, damageGridY));
            return damageGridY;
        });

        context.waitTicks(20);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertFullyBuilt(level, List.of(
                    pickaxeWeakPointDemoSlot(level, gridY),
                    pickaxeDemoSlot(level, gridY),
                    rifleDemoSlot(level, gridY),
                    rifleWeakPointDemoSlot(level, gridY),
                    supportDemoSlot(level, gridY),
                    dependentDemoSlot(level, gridY)
            ), "damage targets");
        });

        announceGametestStep(context, "damage.pickaxe", "Pickaxe damage", "harvesting tool clears a wooden wall");
        server.runOnServer(minecraftServer -> stagePickaxeAt(minecraftServer, pickaxeDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(12);
        server.runOnServer(minecraftServer -> swingPickaxeAt(minecraftServer, pickaxeDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(16);
        server.runOnServer(minecraftServer -> swingPickaxeAt(minecraftServer, pickaxeDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(16);
        server.runOnServer(minecraftServer -> swingPickaxeAt(minecraftServer, pickaxeDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(24);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertCleared(level, pickaxeDemoSlot(level, gridY), "pickaxe target");
        });

        announceGametestStep(context, "damage.pickaxe.weakpoint", "Pickaxe weak-point damage", "one active weak spot clears a wooden wall");
        server.runOnServer(minecraftServer -> stagePickaxeAt(minecraftServer, pickaxeWeakPointDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(20);
        server.runOnServer(minecraftServer -> swingPickaxeAtWeakPoint(minecraftServer, combat, pickaxeWeakPointDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        )));
        context.waitTicks(16);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertCleared(level, pickaxeWeakPointDemoSlot(level, gridY), "pickaxe weak-point target");
        });

        announceGametestStep(context, "damage.rifle", "Rifle damage", "hitscan weapon chips a wooden wall");
        server.runOnServer(minecraftServer -> stageWeaponAt(minecraftServer, rifleDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        ), "weapon_assault_rifle_common", 3.5D));
        context.waitTicks(12);
        server.runOnServer(minecraftServer -> fireWeaponAt(minecraftServer, rifleDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        ), "weapon_assault_rifle_common"));
        context.waitTicks(24);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertConstructionVisible(level, List.of(rifleDemoSlot(level, gridY)), "rifle-damaged wall");
        });

        announceGametestStep(context, "damage.rifle.weakpoint", "Rifle weak-point damage", "server-authoritative weak spot applies boosted build damage");
        server.runOnServer(minecraftServer -> stageWeaponAt(minecraftServer, rifleWeakPointDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        ), "weapon_assault_rifle_common", 3.5D));
        context.waitTicks(20);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            BuildSlot slot = rifleWeakPointDemoSlot(level, gridY);
            int before = combat.state().get(slot).currentHealth();
            int baseDamage = weaponBuildDamage(weapon("weapon_assault_rifle_common"));
            damageWithWeaponAtWeakPoint(minecraftServer, combat, slot, "weapon_assault_rifle_common", baseDamage);
            int expectedHealth = Math.max(0, before - scaledWeakPointDamage(baseDamage));
            assertPieceHealth(combat.state(), slot, expectedHealth, "rifle weak-point target");
        });
        context.waitTicks(24);

        announceGametestStep(context, "damage.support", "Support break collapse", "shotgun destroys support and drops dependent floor");
        server.runOnServer(minecraftServer -> stageWeaponAt(minecraftServer, supportDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        ), "weapon_shotgun_break_barrel_dragon_athena_sr", 3.5D));
        context.waitTicks(12);
        server.runOnServer(minecraftServer -> fireWeaponAt(minecraftServer, supportDemoSlot(
                minecraftServer.getPlayerList().getPlayers().getFirst().level(),
                gridY
        ), "weapon_shotgun_break_barrel_dragon_athena_sr"));
        context.waitTicks(80);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            assertCleared(level, dependentDemoSlot(level, gridY), "dependent floor after support break");
        });
        context.waitTicks(40);
    }

    private static void demonstrateDeployableTrapTriggers(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        announceGametestStep(context, "deployables.setup", "Deployable trap triggers", "floor and wall bouncer grids");
        int groundY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int sceneGroundY = prepareDeployableScene(level);
            MobilityItemInteractions.clearAll();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(0.5D, sceneGroundY, 124.5D);
            player.setYRot(0.0F);
            player.setXRot(10.0F);
            player.setDeltaMovement(Vec3.ZERO);
            return sceneGroundY;
        });

        BlockPos floorSupportCenter = new BlockPos(-7, groundY - 1, 124);
        List<BlockPos> floorTriggerFootprint = centeredSurfaceSquare(
                floorSupportCenter.relative(Direction.UP),
                Direction.UP,
                VISUAL_BUILD_RULES.footprintSizeBlocks()
        );
        announceGametestStep(context, "deployables.floor", "Floor bouncer", "5x5 trigger grid launches upward");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            prepareTriggerSupport(level, floorTriggerFootprint, Direction.UP);
            placeBouncerWithItem(player, floorSupportCenter, Direction.UP);
            assertTrapTriggerFootprint(level, floorTriggerFootprint, Direction.UP, "floor bouncer");
            BlockPos triggerCenter = centerBlock(floorTriggerFootprint);
            player.teleportTo(triggerCenter.getX() + 0.5D, groundY + 2.0D, triggerCenter.getZ() + 8.0D);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(triggerCenter));
            player.setDeltaMovement(Vec3.ZERO);
        });
        context.waitTicks(18);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BlockPos triggerCenter = centerBlock(floorTriggerFootprint);
            player.teleportTo(triggerCenter.getX() + 0.5D, triggerCenter.getY() + 0.05D, triggerCenter.getZ() + 0.5D);
            player.setOnGround(false);
            player.setDeltaMovement(0.0D, -0.35D, 0.0D);
        });
        context.waitTicks(12);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (player.getDeltaMovement().y() <= 1.0D) {
                throw new AssertionError("Expected floor bouncer trigger to launch the player upward.");
            }
        });
        context.waitTicks(24);

        BlockPos wallSupportCenter = new BlockPos(7, groundY + 2, 130);
        Direction wallNormal = Direction.NORTH;
        List<BlockPos> wallTriggerFootprint = centeredSurfaceSquare(
                wallSupportCenter.relative(wallNormal),
                wallNormal,
                VISUAL_BUILD_RULES.footprintSizeBlocks()
        );
        announceGametestStep(context, "deployables.wall", "Wall bouncer", "5x5 wall trigger launches outward");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            prepareTriggerSupport(level, wallTriggerFootprint, wallNormal);
            placeBouncerWithItem(player, wallSupportCenter, wallNormal);
            assertTrapTriggerFootprint(level, wallTriggerFootprint, wallNormal, "wall bouncer");
            BlockPos triggerCenter = centerBlock(wallTriggerFootprint);
            player.teleportTo(triggerCenter.getX() + 0.5D, groundY + 1.5D, triggerCenter.getZ() - 7.0D);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(triggerCenter));
            player.setDeltaMovement(Vec3.ZERO);
        });
        context.waitTicks(18);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BlockPos triggerCenter = centerBlock(wallTriggerFootprint);
            player.teleportTo(triggerCenter.getX() + 0.5D, triggerCenter.getY() + 0.1D, triggerCenter.getZ() + 0.5D);
            player.setOnGround(false);
            player.setDeltaMovement(0.0D, 0.25D, 0.45D);
        });
        context.waitTicks(3);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.z() >= -0.5D || velocity.y() <= 0.1D) {
                throw new AssertionError("Expected wall bouncer trigger to launch the player outward and upward.");
            }
        });
        context.waitTicks(24);

        VisualBuildContext supportDemo = new VisualBuildContext(
                new BuildWorldState(),
                WorldBuildMaterializer.defaults(VISUAL_BUILD_RULES)
        );
        int supportGridY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            configureCombatVisualSystems(supportDemo);
            int gridY = VISUAL_SNAP_GRID.snap(level.dimension().identifier().toString(), 0, groundY, 0).y();
            BuildSlot supportSlot = wallBouncerSupportSlot(level, gridY);
            BuildPieceState supportPiece = fullyBuiltPiece(supportSlot, MaterialType.WOOD, player.getUUID(), level.getGameTime());
            placeVisualPiece(level, supportDemo, supportPiece);
            BlockPos supportCenter = centerBlock(supportDemo.materializer().trackedBlockPositions(supportSlot));
            List<BlockPos> triggerFootprint = centeredSurfaceSquare(
                    supportCenter.relative(wallNormal),
                    wallNormal,
                    VISUAL_BUILD_RULES.footprintSizeBlocks()
            );
            placeTrapTriggerFootprint(level, triggerFootprint, wallNormal);
            assertTrapTriggerFootprint(level, triggerFootprint, wallNormal, "wall bouncer support");
            BlockPos triggerCenter = centerBlock(triggerFootprint);
            player.teleportTo(triggerCenter.getX() + 0.5D, groundY + 1.5D, triggerCenter.getZ() - 8.0D);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(triggerCenter));
            player.setDeltaMovement(Vec3.ZERO);
            return gridY;
        });
        announceGametestStep(context, "deployables.damage", "Trigger damage routing", "damage through trigger breaks support and clears traps");
        context.waitTicks(24);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            BuildSlot supportSlot = wallBouncerSupportSlot(level, supportGridY);
            BlockPos supportCenter = centerBlock(supportDemo.materializer().trackedBlockPositions(supportSlot));
            List<BlockPos> triggerFootprint = centeredSurfaceSquare(
                    supportCenter.relative(wallNormal),
                    wallNormal,
                    VISUAL_BUILD_RULES.footprintSizeBlocks()
            );
            BlockPos triggerCenter = centerBlock(triggerFootprint);
            boolean damaged = DeployableGameTestHooks.damageBuild(
                    level,
                    player,
                    triggerCenter,
                    Vec3.atCenterOf(triggerCenter),
                    MaterialType.WOOD.finalHealth(),
                    "bouncer trigger"
            );
            if (!damaged) {
                throw new AssertionError("Expected damage through a bouncer trigger to hit the supporting wall.");
            }
            assertCleared(level, supportSlot, "wall bouncer support");
        });
        context.waitTicks(2);
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
            BuildSlot supportSlot = wallBouncerSupportSlot(level, supportGridY);
            BlockPos supportCenter = centerBlock(blockPositions(supportSlot));
            List<BlockPos> triggerFootprint = centeredSurfaceSquare(
                    supportCenter.relative(wallNormal),
                    wallNormal,
                    VISUAL_BUILD_RULES.footprintSizeBlocks()
            );
            assertNoTrapTriggerBlocks(level, triggerFootprint, "wall bouncer support");
        });
        context.waitTicks(38);
    }

    private static void demonstrateMotionItems(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        announceGametestStep(context, "motion.setup", "Motion item scenarios", "launch pad, bouncer, impulse, shockwave, grappler, and rift");
        int groundY = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int sceneGroundY = prepareMotionScene(level);
            MobilityItemInteractions.clearAll();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(0.5D, sceneGroundY + 1.0D, 168.5D);
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            player.setDeltaMovement(Vec3.ZERO);
            return sceneGroundY;
        });
        context.waitTicks(10);

        announceGametestStep(context, "motion.launch_pad", "Launch Pad", "real item placement launches immediately");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            BlockPos supportCenter = new BlockPos(-14, groundY - 1, 176);
            List<BlockPos> footprint = centeredSurfaceSquare(supportCenter.above(), Direction.UP, 3);
            prepareTriggerSupport(level, footprint, Direction.UP);
            player.teleportTo(supportCenter.getX() + 0.5D, groundY + 1.0D, supportCenter.getZ() - 8.0D);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(supportCenter.above()));
            player.setDeltaMovement(Vec3.ZERO);
            placeLaunchPadWithItem(player, supportCenter);
            assertPlayerLaunched(player, 0.25D, 2.0D, "launch pad");
        });
        context.waitTicks(28);

        announceGametestStep(context, "motion.bouncer.server_launch", "Bouncer launch", "server-authoritative bouncer impulse path");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.teleportTo(-7.5D, groundY + 1.0D, 184.5D);
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            player.setDeltaMovement(Vec3.ZERO);
            if (!MobilityItemInteractions.activateLaunchPad(player, ModItems.BOUNCER.definition().redeployTicks(), true)) {
                throw new AssertionError("Expected server-authoritative bouncer launch to activate.");
            }
            assertPlayerLaunched(player, 0.25D, 2.0D, "bouncer");
        });
        context.waitTicks(28);

        announceGametestStep(context, "motion.impulse_grenade.server_launch", "Impulse Grenade", "server-authoritative impulse launch using catalog tuning");
        server.runOnServer(minecraftServer -> applyThrowableImpulseVisual(
                minecraftServer,
                "impulse_grenade",
                new Vec3(0.5D, groundY + 1.0D, 192.5D)
        ));
        context.waitTicks(28);

        announceGametestStep(context, "motion.shockwave_grenade.server_launch", "Shockwave Grenade", "server-authoritative fall-safe impulse launch");
        server.runOnServer(minecraftServer -> applyThrowableImpulseVisual(
                minecraftServer,
                "shockwave_grenade",
                new Vec3(7.5D, groundY + 1.0D, 200.5D)
        ));
        context.waitTicks(28);

        announceGametestStep(context, "motion.shockwave_launcher.server_launch", "Shockwave Launcher", "impulse-only launcher launch path");
        server.runOnServer(minecraftServer -> applyShockwaveLauncherVisual(
                minecraftServer,
                new Vec3(-7.5D, groundY + 1.0D, 208.5D)
        ));
        context.waitTicks(28);

        announceGametestStep(context, "motion.grappler", "Grappler", "real projectile pulls toward target wall");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            fillWall(level, -2, 2, groundY + 1, groundY + 7, 222, Blocks.STONE.defaultBlockState());
            Vec3 target = new Vec3(0.5D, groundY + 4.0D, 221.5D);
            player.teleportTo(0.5D, groundY + 1.0D, 210.5D);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
            player.setDeltaMovement(Vec3.ZERO);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAPPLER));
            InteractionResult result = ModItems.GRAPPLER.fireFromHeldItem(level, player, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                throw new AssertionError("Expected grappler to fire at the target wall.");
            }
        });
        context.waitTicks(8);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            assertPlayerLaunched(player, 0.35D, 0.05D, "grappler");
        });
        context.waitTicks(24);

        announceGametestStep(context, "motion.rift_to_go", "Rift-To-Go", "real item teleports upward into redeploy");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            player.teleportTo(7.5D, groundY + 1.0D, 216.5D);
            player.setYRot(0.0F);
            player.setXRot(15.0F);
            player.setDeltaMovement(Vec3.ZERO);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RIFT_TO_GO));
            double beforeY = player.getY();
            InteractionResult result = ModItems.RIFT_TO_GO.use(level, player, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                throw new AssertionError("Expected Rift-To-Go item use to launch the player.");
            }
            if (player.getY() <= beforeY + 8.0D || player.getDeltaMovement().y() <= 0.5D) {
                throw new AssertionError("Expected Rift-To-Go to teleport upward and apply launch velocity.");
            }
        });
        context.waitTicks(32);
    }

    private static int prepareMotionScene(ServerLevel level) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 188);
        clearAir(level, -28, 28, groundY, groundY + 80, 160, 232);
        fillFloor(level, -28, 28, groundY - 1, 160, 232, Blocks.STONE.defaultBlockState());
        return groundY;
    }

    private static int prepareDeployableScene(ServerLevel level) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 124);
        clearAir(level, -20, 20, groundY, groundY + 16, 116, 152);
        fillFloor(level, -20, 20, groundY - 1, 116, 152, Blocks.STONE.defaultBlockState());
        return groundY;
    }

    private static BuildSlot wallBouncerSupportSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), 0, gridY + 1, 36, PieceType.WALL, Orientation.NORTH);
    }

    private static int prepareDamageScene(ServerLevel level) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 80);
        clearAir(level, -36, 28, surfaceY, surfaceY + 16, 80, 110);
        return surfaceY;
    }

    private static BuildSlot pickaxeDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), -4, gridY, 23, PieceType.WALL, Orientation.SOUTH);
    }

    private static BuildSlot rifleDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), 0, gridY, 23, PieceType.WALL, Orientation.SOUTH);
    }

    private static BuildSlot pickaxeWeakPointDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), -7, gridY, 23, PieceType.WALL, Orientation.SOUTH);
    }

    private static BuildSlot rifleWeakPointDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), -2, gridY, 23, PieceType.WALL, Orientation.SOUTH);
    }

    private static BuildSlot supportDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), 3, gridY, 23, PieceType.WALL, Orientation.SOUTH);
    }

    private static BuildSlot dependentDemoSlot(ServerLevel level, int gridY) {
        return BuildSlot.of(level.dimension().identifier().toString(), 3, gridY + 1, 23, PieceType.FLOOR, Orientation.SOUTH);
    }

    private static void clearAir(ServerLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static int visualOriginY(String dimension, int gridY) {
        return VISUAL_SNAP_GRID.blockOrigin(new BuildGridPos(dimension, 0, gridY, 0)).y();
    }

    private static void fillFloor(ServerLevel level, int minX, int maxX, int y, int minZ, int maxZ, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
            }
        }
    }

    private static List<BlockPos> centeredSurfaceSquare(BlockPos center, Direction surfaceNormal, int sizeBlocks) {
        int radius = sizeBlocks / 2;
        ArrayList<BlockPos> positions = new ArrayList<>(sizeBlocks * sizeBlocks);
        for (int vertical = -radius; vertical <= radius; vertical++) {
            for (int lateral = -radius; lateral <= radius; lateral++) {
                positions.add(offsetOnSurface(center, surfaceNormal, lateral, vertical));
            }
        }
        return List.copyOf(positions);
    }

    private static BlockPos offsetOnSurface(BlockPos center, Direction surfaceNormal, int lateral, int vertical) {
        return switch (surfaceNormal.getAxis()) {
            case Y -> center.offset(lateral, 0, vertical);
            case X -> center.offset(0, vertical, lateral);
            case Z -> center.offset(lateral, vertical, 0);
        };
    }

    private static void prepareTriggerSupport(ServerLevel level, Iterable<BlockPos> triggerFootprint, Direction surfaceNormal) {
        for (BlockPos triggerPos : triggerFootprint) {
            BlockPos supportPos = triggerPos.relative(surfaceNormal.getOpposite());
            level.setBlock(triggerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(supportPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void placeTrapTriggerFootprint(ServerLevel level, Iterable<BlockPos> triggerFootprint, Direction surfaceNormal) {
        BlockState triggerState = DeployableTriggerBlocks.TRAP_TRIGGER.defaultBlockState()
                .setValue(TrapTriggerBlock.FACING, surfaceNormal);
        for (BlockPos triggerPos : triggerFootprint) {
            if (!triggerState.canSurvive(level, triggerPos)) {
                throw new AssertionError("Expected trap trigger at " + triggerPos + " to survive on " + surfaceNormal + " support.");
            }
            level.setBlock(triggerPos, triggerState, Block.UPDATE_ALL);
        }
    }

    private static void placeBouncerWithItem(ServerPlayer player, BlockPos supportCenter, Direction surfaceNormal) {
        clearBouncerCooldown(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOUNCER));
        InteractionResult result = ModItems.BOUNCER.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(supportCenter), surfaceNormal, supportCenter, false)
        ));
        if (!result.consumesAction()) {
            throw new AssertionError("Expected bouncer item to place on " + surfaceNormal + " support.");
        }
    }

    private static void placeLaunchPadWithItem(ServerPlayer player, BlockPos supportCenter) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LAUNCH_PAD));
        InteractionResult result = ModItems.LAUNCH_PAD.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(supportCenter), Direction.UP, supportCenter, false)
        ));
        if (!result.consumesAction()) {
            throw new AssertionError("Expected launch pad item to place on floor support.");
        }
    }

    private static void applyThrowableImpulseVisual(MinecraftServer minecraftServer, String itemPath, Vec3 position) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ThrowableImpulseItem item = throwable(itemPath);
        Vec3 velocity = new Vec3(item.definition().horizontalStrength(), item.definition().verticalStrength(), 0.0D);
        player.teleportTo(position.x(), position.y(), position.z());
        player.setYRot(90.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(velocity);
        player.setOnGround(false);
        player.hurtMarked = true;
        MobilityItemInteractions.enableImpulseLaunch(player, item.definition().resetsFallDistance());
        emitMotionBurst(player.level(), player.position(), item.definition().resetsFallDistance());
        assertPlayerLaunched(player, Math.min(0.35D, item.definition().horizontalStrength() * 0.5D), 0.2D, item.definition().displayName());
    }

    private static void applyShockwaveLauncherVisual(MinecraftServer minecraftServer, Vec3 position) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ExplosiveProjectileWeaponItem launcher = explosiveWeapon("weapon_shockwave_launcher_epic");
        ExplosiveProjectileWeaponItem.Definition definition = launcher.explosiveDefinition();
        Vec3 velocity = new Vec3(-definition.impulseHorizontalStrength(), definition.impulseVerticalStrength(), 0.0D);
        player.teleportTo(position.x(), position.y(), position.z());
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(launcher));
        player.setDeltaMovement(velocity);
        player.setOnGround(false);
        player.hurtMarked = true;
        MobilityItemInteractions.enableImpulseLaunch(player, definition.resetsFallDistance());
        emitMotionBurst(player.level(), player.position(), true);
        assertPlayerLaunched(player, Math.min(0.35D, definition.impulseHorizontalStrength() * 0.5D), 0.2D, "shockwave launcher");
    }

    private static void emitMotionBurst(ServerLevel level, Vec3 origin, boolean electric) {
        level.sendParticles(ParticleTypes.EXPLOSION, true, true, origin.x(), origin.y(), origin.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(electric ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CLOUD,
                true, true, origin.x(), origin.y() + 0.3D, origin.z(), 28, 0.35D, 0.2D, 0.35D, 0.08D);
    }

    private static void assertPlayerLaunched(ServerPlayer player, double minHorizontalSpeed, double minYVelocity, String label) {
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());
        if (horizontalSpeed < minHorizontalSpeed || velocity.y() < minYVelocity) {
            throw new AssertionError("Expected " + label + " to launch player, saw velocity " + velocity + ".");
        }
    }

    private static void fillWall(ServerLevel level, int minX, int maxX, int minY, int maxY, int z, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
            }
        }
    }


    private static void clearBouncerCooldown(ServerPlayer player) {
        player.getCooldowns().removeCooldown(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                ModItems.BOUNCER.definition().path()
        ));
    }

    private static void assertTrapTriggerFootprint(ServerLevel level, Iterable<BlockPos> footprint, Direction surfaceNormal, String label) {
        for (BlockPos pos : footprint) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(DeployableTriggerBlocks.TRAP_TRIGGER)) {
                throw new AssertionError("Expected " + label + " trigger at " + pos + ", got " + state + ".");
            }
            if (!state.hasProperty(TrapTriggerBlock.FACING) || state.getValue(TrapTriggerBlock.FACING) != surfaceNormal) {
                throw new AssertionError("Expected " + label + " trigger at " + pos + " to face " + surfaceNormal + ".");
            }
        }
    }

    private static void assertNoTrapTriggerBlocks(ServerLevel level, Iterable<BlockPos> footprint, String label) {
        for (BlockPos pos : footprint) {
            if (level.getBlockState(pos).is(DeployableTriggerBlocks.TRAP_TRIGGER)) {
                throw new AssertionError("Expected " + label + " trigger to clear at " + pos + ".");
            }
        }
    }

    private static BlockPos centerBlock(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            throw new AssertionError("Expected at least one block position.");
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    private static void configureCombatVisualSystems(VisualBuildContext context) {
        PickaxeItem.configureHarvesting(context.state(), context.materializer());
        WeaponItem.configureBuildDamage(context.state(), context.materializer(), VISUAL_BUILD_RULES);
        BuildWeakPoints.register(context.state(), context.materializer());
        BuildCollapseScheduler.configure(context.state(), VISUAL_BUILD_RULES, context.materializer());
    }

    private static void placeVisualPiece(ServerLevel level, VisualBuildContext context, BuildPieceState piece) {
        PieceFootprint footprint = VISUAL_FOOTPRINTS.project(piece);
        if (!context.state().addIfAbsent(piece)) {
            throw new AssertionError("Expected visual piece slot to be empty: " + piece.slot() + ".");
        }
        WorldBuildWriteResult result = context.materializer().place(level, piece, footprint);
        if (!result.success()) {
            context.state().remove(piece.slot());
            throw new AssertionError("Expected visual piece placement to succeed for " + piece.slot() + ": " + result.message());
        }
    }

    private static BuildPieceState fullyBuiltPiece(BuildSlot slot, MaterialType material, UUID owner, long tick) {
        return BuildPieceState.placed(slot, material, owner, tick).progressedTo(tick + buildTicks(material));
    }

    private static long buildTicks(MaterialType material) {
        return Math.max(1L, Math.round(material.buildTimeSeconds() * 20.0D));
    }

    private static void progressVisualPieces(ServerLevel level, VisualBuildContext context) {
        String dimension = level.dimension().identifier().toString();
        long tick = level.getGameTime();
        for (BuildPieceState piece : context.state().progressConstruction(dimension, tick)) {
            checkBuildWrite(context.materializer().refresh(level, piece), "refresh " + piece.slot());
        }
    }

    private static void checkBuildWrite(WorldBuildWriteResult result, String action) {
        if (!result.success()) {
            throw new AssertionError("Expected visual build " + action + " to succeed: " + result.message());
        }
    }



    private static void stagePickaxeAt(MinecraftServer minecraftServer, BuildSlot slot) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        positionPlayerForTarget(player, slot, 4.5D);
        equipPickaxe(player);
    }

    private static void swingPickaxeAt(MinecraftServer minecraftServer, BuildSlot slot) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        equipPickaxe(player);
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        BlockPos hitPos = centerBlock(blockPositions(slot));
        InteractionResult result = DeployableGameTestHooks.damageWithPickaxe(
                level,
                player,
                InteractionHand.MAIN_HAND,
                stack,
                level.getGameTime(),
                new BlockHitResult(Vec3.atCenterOf(hitPos), Direction.NORTH, hitPos, false)
        );
        if (!result.consumesAction() && buildPieceVisible(level, slot)) {
            throw new AssertionError("Expected pickaxe swing to hit " + slot + ".");
        }
    }

    private static void stageWeaponAt(MinecraftServer minecraftServer, BuildSlot slot, String weaponPath, double distance) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        positionPlayerForTarget(player, slot, distance);
        equipWeapon(player, weapon(weaponPath));
    }

    private static void fireWeaponAt(MinecraftServer minecraftServer, BuildSlot slot, String weaponPath) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        WeaponItem weapon = weapon(weaponPath);
        aimPlayerAtTarget(player, slot);
        equipWeapon(player, weapon);
        InteractionResult result = weapon.fireFromHeldItem(level, player, InteractionHand.MAIN_HAND);
        if (!result.consumesAction()) {
            throw new AssertionError("Expected weapon shot to fire at " + slot + ".");
        }
    }

    private static void swingPickaxeAtWeakPoint(MinecraftServer minecraftServer, VisualBuildContext context, BuildSlot slot) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        equipPickaxe(player);
        WeakPointHitTarget target = activeWeakPointTarget(level, context.materializer(), slot, 0);
        positionPlayerForTarget(player, slot, 4.5D);
        InteractionResult result = DeployableGameTestHooks.damageWithPickaxe(
                level,
                player,
                InteractionHand.MAIN_HAND,
                player.getItemInHand(InteractionHand.MAIN_HAND),
                level.getGameTime(),
                new BlockHitResult(target.hitLocation(), Direction.NORTH, target.blockPos(), false)
        );
        if (!result.consumesAction()) {
            throw new AssertionError("Expected pickaxe weak-point swing to hit " + slot + ".");
        }
    }

    private static void damageWithWeaponAtWeakPoint(
            MinecraftServer minecraftServer,
            VisualBuildContext context,
            BuildSlot slot,
            String weaponPath,
            int baseDamage
    ) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        WeaponItem weapon = weapon(weaponPath);
        equipWeapon(player, weapon);
        positionPlayerForTarget(player, slot, 3.5D);
        WeakPointHitTarget target = activeWeakPointTarget(level, context.materializer(), slot, 0);
        boolean damaged = DeployableGameTestHooks.damageBuild(
                level,
                player,
                target.blockPos(),
                target.hitLocation(),
                baseDamage,
                " weak point"
        );
        if (!damaged) {
            throw new AssertionError("Expected weapon weak-point hit to damage " + slot + ".");
        }
    }

    private static WeakPointHitTarget activeWeakPointTarget(ServerLevel level, WorldBuildMaterializer materializer, BuildSlot slot, int sequence) {
        ArrayList<BlockPos> positions = new ArrayList<>(visibleWeakPointPositions(level, materializer, slot));
        if (positions.isEmpty()) {
            throw new AssertionError("Expected visible tracked build blocks for weak-point target " + slot + ".");
        }
        positions.sort((left, right) -> {
            int y = Integer.compare(left.getY(), right.getY());
            if (y != 0) {
                return y;
            }
            int z = Integer.compare(left.getZ(), right.getZ());
            if (z != 0) {
                return z;
            }
            return Integer.compare(left.getX(), right.getX());
        });
        BlockPos blockPos = positions.get(Math.floorMod(slot.hashCode() + sequence * 7, positions.size()));
        return new WeakPointHitTarget(blockPos, Vec3.atCenterOf(blockPos));
    }

    private static List<BlockPos> visibleWeakPointPositions(ServerLevel level, WorldBuildMaterializer materializer, BuildSlot slot) {
        String dimension = slot.gridPos().dimension();
        ArrayList<BlockPos> visible = new ArrayList<>();
        for (BlockPos pos : materializer.trackedBlockPositions(slot)) {
            if (!slot.equals(materializer.topOwnerAt(dimension, pos))) {
                continue;
            }
            BlockState originalState = materializer.originalBlockState(dimension, pos.getX(), pos.getY(), pos.getZ());
            if (originalState != null && WorldObstructions.isBlockingCollision(level, pos, originalState)) {
                continue;
            }
            visible.add(pos);
        }
        return List.copyOf(visible);
    }

    private static int weaponBuildDamage(WeaponItem weapon) {
        return Math.max(1, (int) Math.round(weapon.definition().stats().totalDamagePerShot()));
    }

    private static int scaledWeakPointDamage(int baseDamage) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(baseDamage * BuildWeakPoints.WEAK_POINT_DAMAGE_MULTIPLIER));
    }


    private static void equipPickaxe(ServerPlayer player) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != ModItems.PICKAXE) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PICKAXE));
        }
    }

    private static void equipWeapon(ServerPlayer player, WeaponItem weapon) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != weapon) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
        }
    }

    private static void positionPlayerForTarget(ServerPlayer player, BuildSlot slot, double distance) {
        Vec3 center = centerOf(slot);
        player.teleportTo(center.x(), center.y(), center.z() - distance);
        player.setDeltaMovement(Vec3.ZERO);
        aimPlayerAtTarget(player, slot);
    }

    private static void aimPlayerAtTarget(ServerPlayer player, BuildSlot slot) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, centerOf(slot));
    }

    private static void assertConstructionVisible(ServerLevel level, List<BuildSlot> slots, String label) {
        VisualBlockCounts counts = visualBlockCounts(level, slots);
        if (counts.hologramBlocks() == 0 || counts.solidBlocks() == 0) {
            throw new AssertionError("Expected " + label + " to contain both holographic and solid blocks, saw "
                    + counts.hologramBlocks() + " holographic and " + counts.solidBlocks() + " solid.");
        }
    }

    private static void assertFullyBuilt(ServerLevel level, List<BuildSlot> slots, String label) {
        VisualBlockCounts counts = visualBlockCounts(level, slots);
        int expectedBlocks = slots.size() * VISUAL_BUILD_RULES.footprintSizeBlocks() * VISUAL_BUILD_RULES.footprintSizeBlocks();
        if (counts.hologramBlocks() != 0 || counts.solidBlocks() < expectedBlocks) {
            throw new AssertionError("Expected " + label + " to finish solid, saw "
                    + counts.hologramBlocks() + " holographic and " + counts.solidBlocks()
                    + "/" + expectedBlocks + " solid blocks.");
        }
    }

    private static void assertCleared(ServerLevel level, BuildSlot slot, String label) {
        VisualBlockCounts counts = visualBlockCounts(level, List.of(slot));
        if (counts.hologramBlocks() != 0 || counts.solidBlocks() != 0) {
            throw new AssertionError("Expected " + label + " to clear, saw "
                    + counts.hologramBlocks() + " holographic and " + counts.solidBlocks() + " solid build blocks.");
        }
    }

    private static void assertPieceHealth(BuildWorldState state, BuildSlot slot, int expectedHealth, String label) {
        BuildPieceState piece = state.get(slot);
        if (expectedHealth == 0) {
            if (piece != null) {
                throw new AssertionError("Expected " + label + " to be destroyed, saw " + piece.currentHealth() + " health.");
            }
            return;
        }
        if (piece == null || piece.currentHealth() != expectedHealth) {
            throw new AssertionError("Expected " + label + " to have " + expectedHealth + " health, saw "
                    + (piece == null ? "<destroyed>" : piece.currentHealth()) + ".");
        }
    }

    private static boolean buildPieceVisible(ServerLevel level, BuildSlot slot) {
        for (BlockPos pos : blockPositions(slot)) {
            Block block = level.getBlockState(pos).getBlock();
            if (isHologramBlock(block) || isSolidBuildBlock(block)) {
                return true;
            }
        }
        return false;
    }

    private static VisualBlockCounts visualBlockCounts(ServerLevel level, List<BuildSlot> slots) {
        int hologramBlocks = 0;
        int solidBlocks = 0;
        for (BuildSlot slot : slots) {
            for (BlockPos pos : blockPositions(slot)) {
                Block block = level.getBlockState(pos).getBlock();
                if (isHologramBlock(block)) {
                    hologramBlocks++;
                } else if (isSolidBuildBlock(block)) {
                    solidBlocks++;
                }
            }
        }
        return new VisualBlockCounts(hologramBlocks, solidBlocks);
    }

    private static boolean isHologramBlock(Block block) {
        return block == BuildVisualBlocks.HOLOGRAM_WOOD
                || block == BuildVisualBlocks.HOLOGRAM_STONE
                || block == BuildVisualBlocks.HOLOGRAM_METAL
                || block == BuildVisualBlocks.INVALID_HOLOGRAM_WOOD
                || block == BuildVisualBlocks.INVALID_HOLOGRAM_STONE
                || block == BuildVisualBlocks.INVALID_HOLOGRAM_METAL;
    }

    private static boolean isSolidBuildBlock(Block block) {
        return block == Blocks.OAK_PLANKS
                || block == Blocks.STONE_BRICKS
                || block == Blocks.COPPER_BLOCK.waxed().unaffected();
    }

    private static List<BlockPos> blockPositions(BuildSlot slot) {
        PieceFootprint footprint = VISUAL_FOOTPRINTS.project(slot);
        BlockOffset origin = VISUAL_SNAP_GRID.blockOrigin(slot.gridPos());
        ArrayList<BlockPos> positions = new ArrayList<>(footprint.localBlocks().size());
        for (BlockOffset local : footprint.localBlocks()) {
            positions.add(new BlockPos(origin.x() + local.x(), origin.y() + local.y(), origin.z() + local.z()));
        }
        return List.copyOf(positions);
    }


    private static Vec3 centerOf(BuildSlot slot) {
        List<BlockPos> positions = blockPositions(slot);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1.0D);
            maxY = Math.max(maxY, pos.getY() + 1.0D);
            maxZ = Math.max(maxZ, pos.getZ() + 1.0D);
        }
        return new Vec3((minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
    }

    private static WeaponItem weapon(String path) {
        return ModItems.WEAPONS.stream()
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

    private static ExplosiveProjectileWeaponItem explosiveWeapon(String path) {
        return ModItems.EXPLOSIVE_WEAPONS.stream()
                .filter(item -> item.explosiveDefinition().weapon().path().equals(path))
                .findFirst()
                .orElseThrow();
    }


    private record VisualBuildContext(BuildWorldState state, WorldBuildMaterializer materializer) {
    }

    private record VisualBlockCounts(int hologramBlocks, int solidBlocks) {
    }

    private record WeakPointHitTarget(BlockPos blockPos, Vec3 hitLocation) {
    }

    private static void showBuildPreview(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            BuildSlot slot,
            MaterialType material,
            boolean valid,
            int ticks
    ) {
        waitForPreviewSnapshot(context, server, slot, material, valid, 100);

        for (int remaining = ticks; remaining > 0; remaining -= 2) {
            sendBuildPreview(server, slot, material, valid);
            context.waitTicks(2);
        }
    }

    private static void sendBuildPreview(
            TestDedicatedServerContext server,
            BuildSlot slot,
            MaterialType material,
            boolean valid
    ) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
                throw new AssertionError("Expected the client to accept build preview payloads.");
            }
            ServerPlayNetworking.send(player, BuildPreviewPayload.active(slot, material, valid));
        });
    }

    private static void waitForPreviewSnapshot(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            BuildSlot slot,
            MaterialType material,
            boolean valid,
            int maxTicks
    ) {
        AtomicReference<String> failure = new AtomicReference<>("Expected active build preview snapshot.");
        for (int tick = 0; tick < maxTicks; tick++) {
            sendBuildPreview(server, slot, material, valid);
            context.waitTicks(1);
            context.runOnClient(client -> failure.set(previewFailure(slot, material, valid)));
            if (failure.get() == null) {
                return;
            }
        }
        throw new AssertionError(failure.get());
    }

    private static void suppressAutomaticPreview(TestDedicatedServerContext server, boolean suppressed) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BuildItemInteractions.suppressAutomaticPreview(player, suppressed);
        });
    }

    private static void clearBuildPreview(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (ServerPlayNetworking.canSend(player, BuildPreviewPayload.TYPE)) {
                ServerPlayNetworking.send(player, BuildPreviewPayload.inactive());
            }
        });
        context.waitTicks(20);
        context.runOnClient(client -> {
            if (ClientBuildPreview.snapshot().active()) {
                throw new AssertionError("Expected build preview displays to clear before the glider segment.");
            }
        });
    }

    private static String previewFailure(BuildSlot slot, MaterialType material, boolean valid) {
        ClientBuildPreview.Snapshot snapshot = ClientBuildPreview.snapshot();
        if (!snapshot.active()) {
            return "Expected active build preview snapshot.";
        }
        if (!slot.equals(snapshot.slot())) {
            return "Expected preview slot " + slot + " but saw " + snapshot.slot() + ".";
        }
        if (snapshot.material() != material) {
            return "Expected preview material " + material + " but saw " + snapshot.material() + ".";
        }
        if (snapshot.valid() != valid) {
            return "Expected preview validity " + valid + " but saw " + snapshot.valid() + ".";
        }
        if (Float.compare(ClientBuildPreview.previewCellSizeBlocks(), 1.0F) != 0) {
            return "Expected preview display scale to stay exactly 1.0 block, saw "
                    + ClientBuildPreview.previewCellSizeBlocks() + ".";
        }
        int expectedBoxes = expectedPreviewBoxCount(slot.pieceType());
        if (snapshot.boxes().size() != expectedBoxes) {
            return "Expected " + slot.pieceType() + " preview to keep " + expectedBoxes
                    + " projected preview cells, saw " + snapshot.boxes().size() + ".";
        }
        for (ClientBuildPreview.PreviewBox box : snapshot.boxes()) {
            if (box.sizeX() != 1 || box.sizeY() != 1 || box.sizeZ() != 1) {
                return "Expected preview box at " + box.origin() + " to cover one projected cell, saw "
                        + box.sizeX() + "x" + box.sizeY() + "x" + box.sizeZ() + ".";
            }
        }
        if (slot.pieceType() == PieceType.STAIR) {
            String stairFailure = stairPreviewFootprintFailure(snapshot, slot);
            if (stairFailure != null) {
                return stairFailure;
            }
        }
        return null;
    }

    private static String stairPreviewFootprintFailure(ClientBuildPreview.Snapshot snapshot, BuildSlot slot) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ClientBuildPreview.PreviewBox box : snapshot.boxes()) {
            minX = Math.min(minX, box.origin().getX());
            maxX = Math.max(maxX, box.origin().getX());
            minZ = Math.min(minZ, box.origin().getZ());
            maxZ = Math.max(maxZ, box.origin().getZ());
        }
        boolean widthRunsAlongX = slot.orientation() == Orientation.NORTH || slot.orientation() == Orientation.SOUTH;
        int width = widthRunsAlongX ? maxX - minX + 1 : maxZ - minZ + 1;
        int depth = widthRunsAlongX ? maxZ - minZ + 1 : maxX - minX + 1;
        if (width != VISUAL_BUILD_RULES.footprintSizeBlocks()) {
            return "Expected STAIR preview to span " + VISUAL_BUILD_RULES.footprintSizeBlocks()
                    + " blocks across its placed footprint width, saw " + width + ".";
        }
        if (depth != VISUAL_BUILD_RULES.footprintSizeBlocks()) {
            return "Expected STAIR preview to span " + VISUAL_BUILD_RULES.footprintSizeBlocks()
                    + " blocks across its placed footprint depth, saw " + depth + ".";
        }
        return null;
    }

    private static int expectedPreviewBoxCount(PieceType pieceType) {
        return VISUAL_BUILD_RULES.footprintSizeBlocks() * VISUAL_BUILD_RULES.footprintSizeBlocks();
    }

    private static void demonstrateGliderRedeploy(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        announceGametestStep(context, "glider.redeploy", "Glider redeploy", "launch window allows sustained glider flight");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            ServerLevel level = player.level();
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 6, 160);
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(6.5D, surfaceY + 24.0D, 160.5D);
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
