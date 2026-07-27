package io.github.brainage04.fortniteinminecraft.neoforge.platform;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.fortniteinminecraft.platform.ClientPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

import java.util.function.Consumer;

public final class NeoForgeClientPlatform implements ClientPlatform {
    private final IEventBus modBus;

    public NeoForgeClientPlatform(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public void registerClientTickEnd(Consumer<Minecraft> callback) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> callback.accept(Minecraft.getInstance()));
    }

    @Override
    public KeyMapping.Category registerKeyCategory(Identifier id) {
        KeyMapping.Category category = new KeyMapping.Category(id);
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.registerCategory(category));
        return category;
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(keyMapping));
        return keyMapping;
    }

    @Override
    public void registerClientCommand(LiteralArgumentBuilder<SharedSuggestionProvider> command) {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                event.getDispatcher().register(asNeoForgeCommand(command)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LiteralArgumentBuilder<CommandSourceStack> asNeoForgeCommand(
            LiteralArgumentBuilder<SharedSuggestionProvider> command
    ) {
        return (LiteralArgumentBuilder) command;
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundHandler(
            CustomPacketPayload.Type<T> type,
            Consumer<T> handler
    ) {
        modBus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(
                type,
                (payload, context) -> context.enqueueWork(() -> handler.accept(payload))
        ));
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && ((ICommonPacketListener) connection).hasChannel(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
