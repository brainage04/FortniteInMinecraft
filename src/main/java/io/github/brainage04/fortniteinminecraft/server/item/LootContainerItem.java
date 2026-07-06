package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Consumer;

public final class LootContainerItem extends BlockItem {
    private final String displayName;

    public LootContainerItem(Block block, String displayName, Item.Properties settings) {
        super(block, settings);
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Hold Interact to open."));
    }
}
