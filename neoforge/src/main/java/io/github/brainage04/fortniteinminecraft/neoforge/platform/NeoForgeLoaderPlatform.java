package io.github.brainage04.fortniteinminecraft.neoforge.platform;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.server.item.DeployableTriggerBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.ModBlockEntities;
import io.github.brainage04.fortniteinminecraft.server.item.ModBlocks;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.world.BuildVisualBlocks;
import io.github.brainage04.fortniteinminecraft.platform.LoaderPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NeoForgeLoaderPlatform implements LoaderPlatform {
    private static final String NETWORK_VERSION = "1";

    private final List<Consumer<PayloadRegistrar>> payloadRegistrations = new ArrayList<>();
    private final List<CreativeTabAddition> creativeTabAdditions = new ArrayList<>();
    private RegisterEvent activeRegisterEvent;
    private boolean blocksRegistered;
    private boolean itemsRegistered;
    private boolean blockEntityTypesRegistered;
    private boolean creativeTabsRegistered;

    public NeoForgeLoaderPlatform(IEventBus modBus) {
        FortniteInMinecraft.installPlatform(this);
        modBus.addListener(this::registerContent);
        modBus.addListener(this::registerPayloadHandlers);
        modBus.addListener(this::buildCreativeModeTabContents);
    }

    private void registerContent(RegisterEvent event) {
        activeRegisterEvent = event;
        try {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                BuildVisualBlocks.bootstrap();
                DeployableTriggerBlocks.bootstrap();
                ModBlocks.bootstrap();
                blocksRegistered = true;
            } else if (event.getRegistryKey().equals(Registries.ITEM)) {
                ModItems.bootstrap();
                itemsRegistered = true;
            } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                ModBlockEntities.bootstrap();
                blockEntityTypesRegistered = true;
            } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
                ModItems.bootstrapCreativeTabs();
                creativeTabsRegistered = true;
            }
            if (blocksRegistered && itemsRegistered && blockEntityTypesRegistered && creativeTabsRegistered
                    && !FortniteInMinecraft.isInitialized()) {
                FortniteInMinecraft.initialize(this);
            }
        } finally {
            activeRegisterEvent = null;
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        payloadRegistrations.forEach(registration -> registration.accept(registrar));
    }

    private void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        for (CreativeTabAddition addition : creativeTabAdditions) {
            if (event.getTabKey().equals(addition.tab())) {
                for (Item item : addition.items()) {
                    event.accept(new ItemStack(item), addition.visibility());
                }
            }
        }
    }

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public void registerEndLevelTick(Consumer<ServerLevel> callback) {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                callback.accept(level);
            }
        });
    }

    @Override
    public void registerEndServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerStopping(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerPlayerJoin(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void registerPlayerDisconnect(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void registerPlayerRespawn(Consumer<ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void registerChunkLoad(ChunkLoadCallback callback) {
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                callback.onChunkLoad(level, event.getChunk(), event.isNewChunk());
            }
        });
    }

    @Override
    public void registerServerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> callback.accept(event.getDispatcher()));
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<ServerPlayer, T> handler
    ) {
        payloadRegistrations.add(registrar -> registrar.playToServer(type, codec, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handler.accept(player, payload);
                    }
                })));
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundPayload(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        payloadRegistrations.add(registrar -> registrar.playToClient(type, codec));
    }

    @Override
    public boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ((ICommonPacketListener) player.connection).hasChannel(type);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public <T extends Item> T registerItem(ResourceKey<Item> key, T item) {
        requireModNamespace(key.identifier());
        requireActive(Registries.ITEM).register(Registries.ITEM, key.identifier(), () -> item);
        return item;
    }

    @Override
    public <T extends Block> T registerBlock(ResourceKey<Block> key, T block) {
        requireModNamespace(key.identifier());
        requireActive(Registries.BLOCK).register(Registries.BLOCK, key.identifier(), () -> block);
        return block;
    }

    @Override
    public <T extends BlockEntityType<?>> T registerBlockEntityType(Identifier id, T blockEntityType) {
        requireModNamespace(id);
        requireActive(Registries.BLOCK_ENTITY_TYPE).register(Registries.BLOCK_ENTITY_TYPE, id, () -> blockEntityType);
        return blockEntityType;
    }

    @Override
    public void registerCreativeTab(
            Identifier id,
            Component title,
            Supplier<ItemStack> icon,
            List<? extends Item> tabItems
    ) {
        requireModNamespace(id);
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(title)
                .icon(icon)
                .displayItems((parameters, output) -> tabItems.forEach(output::accept))
                .build();
        requireActive(Registries.CREATIVE_MODE_TAB).register(Registries.CREATIVE_MODE_TAB, id, () -> tab);
    }

    @Override
    public void addCreativeTabItems(
            ResourceKey<CreativeModeTab> tab,
            List<? extends Item> tabItems,
            CreativeTabVisibility visibility
    ) {
        CreativeModeTab.TabVisibility neoForgeVisibility = switch (visibility) {
            case PARENT_AND_SEARCH -> CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            case SEARCH_ONLY -> CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
        };
        creativeTabAdditions.add(new CreativeTabAddition(tab, List.copyOf(tabItems), neoForgeVisibility));
    }

    private RegisterEvent requireActive(ResourceKey<? extends Registry<?>> registryKey) {
        RegisterEvent event = activeRegisterEvent;
        if (event == null || !event.getRegistryKey().equals(registryKey)) {
            throw new IllegalStateException("Attempted to register content outside the " + registryKey.identifier() + " registry event");
        }
        return event;
    }

    private record CreativeTabAddition(
            ResourceKey<CreativeModeTab> tab,
            List<? extends Item> items,
            CreativeModeTab.TabVisibility visibility
    ) {
    }

    private static void requireModNamespace(Identifier id) {
        if (!FortniteInMinecraft.MOD_ID.equals(id.getNamespace())) {
            throw new IllegalArgumentException("Cannot register foreign identifier " + id);
        }
    }
}
