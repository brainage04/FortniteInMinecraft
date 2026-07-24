package io.github.brainage04.fortniteinminecraft.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Loader services used by the loader-neutral server and content bootstrap. */
public interface LoaderPlatform {
    enum CreativeTabVisibility {
        PARENT_AND_SEARCH,
        SEARCH_ONLY
    }
    @FunctionalInterface
    interface ChunkLoadCallback {
        void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean generated);
    }


    String loaderName();

    void registerEndLevelTick(Consumer<ServerLevel> callback);

    void registerEndServerTick(Consumer<MinecraftServer> callback);

    void registerServerStopping(Consumer<MinecraftServer> callback);

    void registerPlayerJoin(Consumer<ServerPlayer> callback);

    void registerPlayerDisconnect(Consumer<ServerPlayer> callback);

    void registerPlayerRespawn(Consumer<ServerPlayer> callback);

    void registerChunkLoad(ChunkLoadCallback callback);

    void registerServerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback);

    <T extends CustomPacketPayload> void registerServerboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<ServerPlayer, T> handler
    );

    <T extends CustomPacketPayload> void registerClientboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    );

    boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    <T extends Item> T registerItem(ResourceKey<Item> key, T item);

    <T extends Block> T registerBlock(ResourceKey<Block> key, T block);

    <T extends BlockEntityType<?>> T registerBlockEntityType(Identifier id, T blockEntityType);

    void registerCreativeTab(Identifier id, Component title, Supplier<ItemStack> icon, List<? extends Item> items);

    void addCreativeTabItems(
            ResourceKey<CreativeModeTab> tab,
            List<? extends Item> items,
            CreativeTabVisibility visibility
    );
}
