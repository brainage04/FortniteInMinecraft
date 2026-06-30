package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildHooks;
import io.github.brainage04.fortniteinminecraft.client.command.ClientCommandRegistrar;
import net.fabricmc.api.ClientModInitializer;

public final class FortniteInMinecraftClient implements ClientModInitializer {
    private static volatile boolean initialized;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrar.initialize();
        ClientBuildHooks.initialize();
        initialized = true;

        FortniteInMinecraft.LOGGER.info("{} client initialized.", FortniteInMinecraft.MOD_NAME);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
