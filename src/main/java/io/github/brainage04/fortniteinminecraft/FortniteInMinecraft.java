package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrar;
import io.github.brainage04.fortniteinminecraft.server.item.BuildEditInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
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
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import io.github.brainage04.fortniteinminecraft.server.world.BuildCollapseScheduler;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPieceHealthDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.HitMarkerDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceDebugDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceWorldgen;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceHarvest;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FortniteInMinecraft implements ModInitializer {
    public static final String MOD_ID = "fortniteinminecraft";
    public static final String MOD_NAME = "FortniteInMinecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

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

    @Override
    public void onInitialize() {
        BuildVisualBlocks.initialize();
        ModItems.initialize(sessions);
        PortAFortItem.configureBuildPlacement(buildWorld, buildRules, materializer);
        BuildCollapseScheduler.configure(buildWorld, buildRules, materializer);
        CommandRegistrar.initialize(sessions, buildWorld, buildRules, materializer);
        ServerGameplayNetworking.initialize(sessions, buildWorld, buildRules, materializer);
        WeaponItem.configureBuildDamage(buildWorld, materializer, buildRules);
        PickaxeItem.configureHarvesting(buildWorld, materializer);
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
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerAimStates.clear(handler.player);
            PlayerResourceStates.clear(handler.player);
            PickaxeItem.clearHarvestCooldown(handler.player);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            HitMarkerDisplays.clearAll();
            BuildPieceHealthDisplays.clearAll();
            BuildWeakPoints.clearAll();
            TerrainResourceDebugDisplays.clearAll();
            MobilityItemInteractions.clearAll();
            GrapplerProjectiles.clearAll();
            PlayerAimStates.clearAll();
            PickaxeItem.clearAllHarvestCooldowns();
            BuildEditInteractions.clearAll();
            TerrainResourceHarvest.clearAll();
        });
        LOGGER.info("{} server core initialized.", MOD_NAME);
    }
}
