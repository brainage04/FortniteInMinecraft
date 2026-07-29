package io.github.brainage04.fortniteinminecraft.client.network;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraftClient;
import io.github.brainage04.fortniteinminecraft.client.ClientBuildPreview;
import io.github.brainage04.fortniteinminecraft.client.ClientLootContainerProgressHud;
import io.github.brainage04.fortniteinminecraft.client.ClientInputHooks;
import io.github.brainage04.fortniteinminecraft.client.ClientResourceState;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.EditModePayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.LootContainerProgressPayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ResourceStatePayload;

public final class ClientGameplayNetworking {
    private static boolean registered;

    private ClientGameplayNetworking() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        FortnitePayloads.registerClientbound(FortniteInMinecraft.platform());
        FortniteInMinecraftClient.platform().registerClientboundHandler(EditModePayload.TYPE, payload -> {
            ClientInputHooks.setEditModeActive(payload.active());
            ClientBuildPreview.acceptEditMode(payload);
        });
        FortniteInMinecraftClient.platform().registerClientboundHandler(
                ResourceStatePayload.TYPE,
                ClientResourceState::update
        );
        FortniteInMinecraftClient.platform().registerClientboundHandler(
                BuildPreviewPayload.TYPE,
                ClientBuildPreview::acceptServerPreview
        );
        FortniteInMinecraftClient.platform().registerClientboundHandler(
                LootContainerProgressPayload.TYPE,
                ClientLootContainerProgressHud::acceptProgress
        );
        registered = true;
    }
}
