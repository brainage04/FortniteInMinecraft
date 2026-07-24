package io.github.brainage04.fortniteinminecraft.fabric;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.fabric.platform.FabricLoaderPlatform;
import net.fabricmc.api.ModInitializer;

public final class FortniteInMinecraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FortniteInMinecraft.initialize(FabricLoaderPlatform.INSTANCE);
    }
}
