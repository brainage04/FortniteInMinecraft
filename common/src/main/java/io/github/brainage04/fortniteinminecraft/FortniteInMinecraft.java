package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.platform.LoaderPlatform;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrar;
import io.github.brainage04.fortniteinminecraft.server.item.BuildEditInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.HarvestingToolInventory;
import io.github.brainage04.fortniteinminecraft.server.item.DeployableTriggerBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.ModBlockEntities;
import io.github.brainage04.fortniteinminecraft.server.item.ModBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.PickaxeItem;
import io.github.brainage04.fortniteinminecraft.server.item.PortAFortItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponAutoFire;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import io.github.brainage04.fortniteinminecraft.server.network.ServerGameplayNetworking;
import io.github.brainage04.fortniteinminecraft.server.player.GrapplerProjectiles;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerAimStates;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.BuildInteractionTicker;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPieceHealthDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceDebugDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceWorldgen;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceHarvest;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FortniteInMinecraft {
    public static final String MOD_ID = "fortniteinminecraft";
    public static final String MOD_NAME = "Fortnite In Minecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static LoaderPlatform platform;
    private static boolean initialized;

    private final BuildSessionManager sessions = new BuildSessionManager();
    private final BuildWorldState buildWorld = new BuildWorldState();
    private final BuildRules buildRules = BuildRules.defaults();
    private final WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(buildRules);
    private final BuildInteractionTicker interactionTicker = new BuildInteractionTicker(
            sessions,
            buildWorld,
            buildRules,
            materializer
    );

    private FortniteInMinecraft() {
    }

    public static synchronized void installPlatform(LoaderPlatform loaderPlatform) {
        LoaderPlatform requestedPlatform = Objects.requireNonNull(loaderPlatform, "loaderPlatform");
        if (platform != null && platform != requestedPlatform) {
            throw new IllegalStateException(MOD_NAME + " loader platform is already installed");
        }
        platform = requestedPlatform;
    }

    public static synchronized void initialize(LoaderPlatform loaderPlatform) {
        if (initialized) {
            throw new IllegalStateException(MOD_NAME + " is already initialized");
        }
        installPlatform(loaderPlatform);
        new FortniteInMinecraft().initializeCommon();
        initialized = true;
    }

    public static LoaderPlatform platform() {
        LoaderPlatform initializedPlatform = platform;
        if (initializedPlatform == null) {
            throw new IllegalStateException(MOD_NAME + " loader platform is not installed");
        }
        return initializedPlatform;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private void initializeCommon() {
        BuildVisualBlocks.bootstrap();
        DeployableTriggerBlocks.bootstrap();
        ModBlocks.bootstrap();
        ModItems.bootstrap();
        ModBlockEntities.bootstrap();
        ModItems.initialize(sessions);
        PortAFortItem.configureBuildPlacement(buildWorld, buildRules, materializer);
        BuildCollapseScheduler.configure(buildWorld, buildRules, materializer);
        CommandRegistrar.initialize(sessions, buildWorld, buildRules, materializer);
        ServerGameplayNetworking.initialize(sessions, buildWorld, buildRules, materializer);
        WeaponItem.configureBuildDamage(buildWorld, materializer, buildRules);
        PickaxeItem.configureHarvesting(buildWorld, materializer);
        HarvestingToolInventory.register();
        BuildItemInteractions.register(sessions, buildWorld, buildRules, materializer);
        WeaponAutoFire.register();
        MobilityItemInteractions.register();
        GrapplerProjectiles.register();
        HitMarkerDisplays.register();
        TerrainResourceWorldgen.register();
        TerrainResourceDebugDisplays.register();
        BuildPieceHealthDisplays.register(buildWorld, materializer);
        BuildWeakPoints.register(buildWorld, materializer);
        interactionTicker.register();
        platform.registerPlayerDisconnect(player -> {
            PlayerAimStates.clear(player);
            PlayerResourceStates.clear(player);
            PickaxeItem.clearHarvestCooldown(player);
            BuildItemInteractions.suppressAutomaticPreview(player, false);
        });
        platform.registerServerStopping(server -> {
            HitMarkerDisplays.clearAll();
            BuildPieceHealthDisplays.clearAll();
            BuildWeakPoints.clearAll();
            TerrainResourceDebugDisplays.clearAll();
            MobilityItemInteractions.clearAll();
            GrapplerProjectiles.clearAll();
            PlayerAimStates.clearAll();
            PickaxeItem.clearAllHarvestCooldowns();
            BuildEditInteractions.clearAll();
            BuildItemInteractions.clearAutomaticPreviewSuppressions();
            TerrainResourceHarvest.clearAll();
        });
        LOGGER.info("{} common server core initialized on {}.", MOD_NAME, platform.loaderName());
    }
}
