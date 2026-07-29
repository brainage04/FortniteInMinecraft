package io.github.brainage04.fortniteinminecraft.platform;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** Loader services used by the loader-neutral client bootstrap. */
public interface ClientPlatform {

    String loaderName();

    void registerClientTickEnd(Consumer<Minecraft> callback);

    KeyMapping.Category registerKeyCategory(Identifier id);

    KeyMapping registerKeyMapping(KeyMapping keyMapping);

    void registerClientCommand(LiteralArgumentBuilder<SharedSuggestionProvider> command);

    <T extends CustomPacketPayload> void registerClientboundHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<T> handler
    );

    boolean canSendToServer(CustomPacketPayload.Type<?> type);

    void sendToServer(CustomPacketPayload payload);
}
