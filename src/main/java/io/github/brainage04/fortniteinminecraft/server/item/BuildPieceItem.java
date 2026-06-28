package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BuildPieceItem extends SimplePolymerItem {
    private final PieceType pieceType;
    private final EnumMap<MaterialType, Item> clientItemsByMaterial = new EnumMap<>(MaterialType.class);

    public BuildPieceItem(
            PieceType pieceType,
            Item.Properties settings,
            Item woodClientItem,
            Item stoneClientItem,
            Item metalClientItem
    ) {
        super(settings, woodClientItem);
        this.pieceType = Objects.requireNonNull(pieceType, "pieceType");
        clientItemsByMaterial.put(MaterialType.WOOD, Objects.requireNonNull(woodClientItem, "woodClientItem"));
        clientItemsByMaterial.put(MaterialType.STONE, Objects.requireNonNull(stoneClientItem, "stoneClientItem"));
        clientItemsByMaterial.put(MaterialType.METAL, Objects.requireNonNull(metalClientItem, "metalClientItem"));
    }

    public PieceType pieceType() {
        return pieceType;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItemFor(ModItems.selectedMaterialFor(stack, context));
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Material: " + label(ModItems.selectedMaterialFor(stack, context))));
        tooltip.add(Component.literal("Right-click: place / hold turbo"));
        tooltip.add(Component.literal("Left-click: cycle material"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Build " + label(pieceType));
    }

    Item clientItemFor(MaterialType material) {
        return clientItemsByMaterial.get(Objects.requireNonNull(material, "material"));
    }

    private static String label(Enum<?> value) {
        String lower = value.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
