package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.command.BuildCommands;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewParticles;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewGlassBlocks;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewTicker;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;

import net.fabricmc.api.ModInitializer;
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
    private final BuildPreviewParticles previewParticles = new BuildPreviewParticles(materializer);
    private final BuildPreviewGlassBlocks previewGlassBlocks = new BuildPreviewGlassBlocks(materializer);
    private final BuildPreviewRenderers previewRenderers = new BuildPreviewRenderers(previewParticles, previewGlassBlocks);
    private final BuildPreviewTicker previewTicker = new BuildPreviewTicker(
            sessions,
            buildWorld,
            buildRules,
            materializer,
            previewRenderers
    );

    @Override
    public void onInitialize() {
        ModItems.initialize(sessions);
        BuildCommands.register(sessions, buildWorld, buildRules, materializer, previewRenderers);
        BuildItemInteractions.register(sessions, buildWorld, buildRules, materializer, previewRenderers);
        previewTicker.register();
        LOGGER.info("{} server core initialized.", MOD_NAME);
    }
}
