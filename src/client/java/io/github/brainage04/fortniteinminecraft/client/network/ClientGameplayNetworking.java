package io.github.brainage04.fortniteinminecraft.client.network;

import io.github.brainage04.fortniteinminecraft.client.ClientBuildPreview;
import io.github.brainage04.fortniteinminecraft.client.ClientInputHooks;
import io.github.brainage04.fortniteinminecraft.client.ClientResourceState;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.BuildPreviewPayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.EditModePayload;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ResourceStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientGameplayNetworking {
    private static boolean registered;

    private ClientGameplayNetworking() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        FortnitePayloads.register();
        ClientPlayNetworking.registerGlobalReceiver(EditModePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientInputHooks.setEditModeActive(payload.active())));
        ClientPlayNetworking.registerGlobalReceiver(ResourceStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientResourceState.update(payload)));
        ClientPlayNetworking.registerGlobalReceiver(BuildPreviewPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientBuildPreview.acceptServerPreview(payload)));
        registered = true;
    }
}
