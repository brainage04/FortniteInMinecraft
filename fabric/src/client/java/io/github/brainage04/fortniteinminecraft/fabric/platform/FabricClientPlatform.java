package io.github.brainage04.fortniteinminecraft.fabric.platform;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.fortniteinminecraft.platform.ClientPlatform;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public void registerClientTickEnd(Consumer<Minecraft> callback) {
        ClientTickEvents.END_CLIENT_TICK.register(callback::accept);
    }

    @Override
    public KeyMapping.Category registerKeyCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        return KeyMappingHelper.registerKeyMapping(keyMapping);
    }

    @Override
    public void registerClientCommand(LiteralArgumentBuilder<SharedSuggestionProvider> command) {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(asFabricCommand(command))
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LiteralArgumentBuilder<FabricClientCommandSource> asFabricCommand(
            LiteralArgumentBuilder<SharedSuggestionProvider> command
    ) {
        return (LiteralArgumentBuilder) command;
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<T> handler
    ) {
        ClientPlayNetworking.registerGlobalReceiver(
                type,
                (payload, context) -> context.client().execute(() -> handler.accept(payload))
        );
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return ClientPlayNetworking.canSend(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
