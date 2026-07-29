package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Objects;

public final class BuildPieceItem extends Item {
    private final PieceType pieceType;
    private final EnumMap<MaterialType, Item> clientItemsByMaterial = new EnumMap<>(MaterialType.class);

    public BuildPieceItem(
            PieceType pieceType,
            Item.Properties settings,
            Item woodClientItem,
            Item stoneClientItem,
            Item metalClientItem
    ) {
        super(ItemTooltips.withLore(settings, lore(MaterialType.WOOD)));
        this.pieceType = Objects.requireNonNull(pieceType, "pieceType");
        clientItemsByMaterial.put(MaterialType.WOOD, Objects.requireNonNull(woodClientItem, "woodClientItem"));
        clientItemsByMaterial.put(MaterialType.STONE, Objects.requireNonNull(stoneClientItem, "stoneClientItem"));
        clientItemsByMaterial.put(MaterialType.METAL, Objects.requireNonNull(metalClientItem, "metalClientItem"));
    }

    public PieceType pieceType() {
        return pieceType;
    }


    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Build " + label(pieceType));
    }

    Item clientItemFor(MaterialType material) {
        return clientItemsByMaterial.get(Objects.requireNonNull(material, "material"));
    }

    static ItemLore lore(MaterialType material) {
        return ItemTooltips.lore(
                Component.literal("Material: " + label(material)),
                Component.literal("Left-click: place / hold turbo"),
                Component.literal("Right-click: cycle material")
        );
    }

    private static String label(Enum<?> value) {
        String lower = value.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
