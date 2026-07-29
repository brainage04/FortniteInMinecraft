package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildHooks;
import io.github.brainage04.fortniteinminecraft.client.command.ClientCommandRegistrar;
import io.github.brainage04.fortniteinminecraft.platform.ClientPlatform;
import java.util.Objects;

public final class FortniteInMinecraftClient {
    private static ClientPlatform platform;
    private static volatile boolean initialized;

    private FortniteInMinecraftClient() {
    }

    public static synchronized void initialize(ClientPlatform clientPlatform) {
        if (initialized) {
            throw new IllegalStateException(FortniteInMinecraft.MOD_NAME + " client is already initialized");
        }
        platform = Objects.requireNonNull(clientPlatform, "clientPlatform");
        ClientCommandRegistrar.initialize();
        ClientBuildHooks.initialize();
        initialized = true;

        FortniteInMinecraft.LOGGER.info(
                "{} common client initialized on {}.",
                FortniteInMinecraft.MOD_NAME,
                clientPlatform.loaderName()
        );
    }

    public static ClientPlatform platform() {
        ClientPlatform initializedPlatform = platform;
        if (initializedPlatform == null) {
            throw new IllegalStateException(FortniteInMinecraft.MOD_NAME + " client platform is not installed");
        }
        return initializedPlatform;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
