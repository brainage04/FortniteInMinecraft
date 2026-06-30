package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.client.network.ClientGameplayNetworking;

public final class ClientBuildHooks {
    private static volatile boolean initialized;

    private ClientBuildHooks() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        ClientGameplayNetworking.initialize();
        ClientBuildPreview.initialize();
        ClientResourceWalletHud.initialize();
        ClientInputHooks.initialize();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
