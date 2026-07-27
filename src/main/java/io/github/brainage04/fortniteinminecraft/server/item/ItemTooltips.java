package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

final class ItemTooltips {
    private ItemTooltips() {
    }

    static Item.Properties withLore(Item.Properties properties, Component... lines) {
        return withLore(properties, lore(lines));
    }

    static Item.Properties withLore(Item.Properties properties, List<Component> lines) {
        return withLore(properties, lore(lines));
    }

    static Item.Properties withLore(Item.Properties properties, ItemLore lore) {
        return properties.component(DataComponents.LORE, lore);
    }

    static ItemLore lore(Component... lines) {
        List<Component> immutableLines = List.of(lines);
        return new ItemLore(immutableLines, immutableLines);
    }

    static ItemLore lore(List<Component> lines) {
        List<Component> immutableLines = List.copyOf(lines);
        return new ItemLore(immutableLines, immutableLines);
    }
}
