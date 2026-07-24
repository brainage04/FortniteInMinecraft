package io.github.brainage04.fortniteinminecraft.fabric;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraftClient;
import io.github.brainage04.fortniteinminecraft.fabric.platform.FabricClientPlatform;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import io.github.brainage04.hudrendererlib.fabric.FabricHudRendererPlatform;
import net.fabricmc.api.ClientModInitializer;

public final class FortniteInMinecraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRendererLib.initialize(new FabricHudRendererPlatform());
        FortniteInMinecraftClient.initialize(new FabricClientPlatform());
    }
}
