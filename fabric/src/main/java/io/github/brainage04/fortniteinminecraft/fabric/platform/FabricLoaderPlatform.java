package io.github.brainage04.fortniteinminecraft.fabric.platform;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.fortniteinminecraft.platform.LoaderPlatform;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FabricLoaderPlatform implements LoaderPlatform {
    public static final FabricLoaderPlatform INSTANCE = new FabricLoaderPlatform();

    private FabricLoaderPlatform() {
    }
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public void registerEndLevelTick(Consumer<ServerLevel> callback) {
        ServerTickEvents.END_LEVEL_TICK.register(callback::accept);
    }

    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.END_SERVER_TICK.register(callback::accept);
    }

    @Override
    public void registerServerStopping(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept);
    }

    @Override
    public void registerPlayerJoin(Consumer<ServerPlayer> callback) {
        ServerPlayerEvents.JOIN.register(callback::accept);
    }

    @Override
    public void registerPlayerDisconnect(Consumer<ServerPlayer> callback) {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> callback.accept(handler.player));
    }

    @Override
    public void registerPlayerRespawn(Consumer<ServerPlayer> callback) {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> callback.accept(newPlayer));
    }

    @Override
    public void registerChunkLoad(ChunkLoadCallback callback) {
        ServerChunkEvents.CHUNK_LOAD.register(callback::onChunkLoad);
    }

    @Override
    public void registerServerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> callback.accept(dispatcher));
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<ServerPlayer, T> handler
    ) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(
                type,
                (payload, context) -> context.server().execute(() -> handler.accept(context.player(), payload))
        );
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    @Override
    public boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public <T extends Item> T registerItem(ResourceKey<Item> key, T item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    @Override
    public <T extends Block> T registerBlock(ResourceKey<Block> key, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    @Override
    public <T extends BlockEntityType<?>> T registerBlockEntityType(Identifier id, T blockEntityType) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType);
    }

    @Override
    public void registerCreativeTab(
            Identifier id,
            Component title,
            Supplier<ItemStack> icon,
            List<? extends Item> items
    ) {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems((parameters, output) -> items.forEach(output::accept))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
    }

    @Override
    public void addCreativeTabItems(
            ResourceKey<CreativeModeTab> tab,
            List<? extends Item> items,
            CreativeTabVisibility visibility
    ) {
        CreativeModeTab.TabVisibility fabricVisibility = switch (visibility) {
            case PARENT_AND_SEARCH -> CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            case SEARCH_ONLY -> CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
        };
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
            for (Item item : items) {
                output.accept(new ItemStack(item), fabricVisibility);
            }
        });
    }
}
