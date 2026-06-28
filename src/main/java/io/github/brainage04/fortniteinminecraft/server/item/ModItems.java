package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Objects;

public final class ModItems {
    public static final BuildPieceItem WALL = register(
            "build_wall",
            PieceType.WALL,
            Items.OAK_PLANKS,
            Items.COBBLESTONE,
            Items.COPPER_BLOCK.weathering().unaffected()
    );
    public static final BuildPieceItem FLOOR = register(
            "build_floor",
            PieceType.FLOOR,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.weathering().unaffected()
    );
    public static final BuildPieceItem STAIR = register(
            "build_stair",
            PieceType.STAIR,
            Items.OAK_STAIRS,
            Items.COBBLESTONE_STAIRS,
            Items.CUT_COPPER_STAIRS.weathering().unaffected()
    );
    public static final BuildPieceItem ROOF = register(
            "build_roof",
            PieceType.ROOF,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.weathering().unaffected()
    );
    public static final List<BuildPieceItem> BUILD_PIECES = List.of(WALL, FLOOR, STAIR, ROOF);

    private static BuildSessionManager sessions;
    private static final String MATERIAL_COMPONENT_KEY = "build_material";

    private ModItems() {
    }

    public static void initialize(BuildSessionManager sessionManager) {
        sessions = Objects.requireNonNull(sessionManager, "sessionManager");
        CreativeModeTab tab = PolymerCreativeModeTabUtils.builder()
                .title(Component.literal(FortniteInMinecraft.MOD_NAME))
                .icon(() -> new ItemStack(WALL))
                .displayItems((parameters, output) -> BUILD_PIECES.forEach(output::accept))
                .build();
        PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(id("build_pieces"), tab);
    }

    public static BuildPieceItem asBuildPiece(ItemStack stack) {
        if (stack.getItem() instanceof BuildPieceItem item) {
            return item;
        }
        return null;
    }

    public static void refreshBuildItemAppearances(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MaterialType material = selectedMaterialFor(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (asBuildPiece(stack) == null) {
                continue;
            }
            setSelectedMaterial(stack, material);
            player.connection.send(player.getInventory().createInventoryUpdatePacket(slot));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastFullState();
        player.containerMenu.broadcastFullState();
    }

    static MaterialType selectedMaterialFor(ItemStack stack, PacketContext context) {
        MaterialType material = materialFromStack(stack);
        return material == null ? selectedMaterialFor(context) : material;
    }

    static void setSelectedMaterial(ItemStack stack, MaterialType material) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(material, "material");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MATERIAL_COMPONENT_KEY, material.name()));
    }

    private static MaterialType selectedMaterialFor(PacketContext context) {
        if (context == null) {
            return MaterialType.WOOD;
        }

        ServerPlayer player = PolymerCommonUtils.getPlayer(context);
        return player == null ? MaterialType.WOOD : selectedMaterialFor(player);
    }

    private static MaterialType selectedMaterialFor(ServerPlayer player) {
        if (sessions == null) {
            return MaterialType.WOOD;
        }
        PlayerBuildSession session = sessions.get(player.getUUID());
        return session == null ? MaterialType.WOOD : session.selectedMaterial();
    }

    private static MaterialType materialFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(MATERIAL_COMPONENT_KEY, "");
        try {
            return name.isBlank() ? null : MaterialType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static BuildPieceItem register(
            String path,
            PieceType pieceType,
            Item woodClientItem,
            Item stoneClientItem,
            Item metalClientItem
    ) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new BuildPieceItem(
                        pieceType,
                        new Item.Properties().setId(key).stacksTo(1),
                        woodClientItem,
                        stoneClientItem,
                        metalClientItem
                )
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }
}
