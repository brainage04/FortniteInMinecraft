package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class ClientFortniteHud {
    private static final int EDGE_MARGIN = 8;
    private static final int SLOT_COUNT = 9;
    private static final int SLOT_WIDTH = 31;
    private static final int SLOT_HEIGHT = 37;
    private static final int SLOT_GAP = 2;
    private static final int ITEM_SIZE = 16;
    private static final int RESOURCE_WIDTH = 53;
    private static final int RESOURCE_HEIGHT = 22;
    private static final int RESOURCE_GAP = 3;
    private static final int BACKGROUND = 0xD8273142;
    private static final int EMPTY_BACKGROUND = 0xC51A2130;
    private static final int BORDER = 0xFF73819A;
    private static final int SELECTED_BORDER = 0xFFFFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int COMMON = 0xFFB8B8B8;
    private static final int UNCOMMON = 0xFF53B73A;
    private static final int RARE = 0xFF2F91E8;
    private static final int EPIC = 0xFFAA55D6;
    private static ItemStack woodIcon;
    private static ItemStack stoneIcon;
    private static ItemStack metalIcon;
    private static boolean registered;

    private ClientFortniteHud() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        HudRendererLib.registerHudElement(new FortniteHudRendererElement(
                "Resource wallet and Fortnite hotbar",
                Identifier.withDefaultNamespace("hotbar"),
                true,
                ClientFortniteHud::render
        ));
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    public static boolean replacesVanillaHotbar() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.isCreative() && !client.player.isSpectator();
    }

    static int topY(int guiHeight) {
        return guiHeight - EDGE_MARGIN - SLOT_HEIGHT - RESOURCE_HEIGHT - 5;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!replacesVanillaHotbar() || client.gui.hud.isHidden()) {
            return;
        }

        Inventory inventory = client.player.getInventory();
        Font font = client.font;
        int barWidth = SLOT_COUNT * SLOT_WIDTH + (SLOT_COUNT - 1) * SLOT_GAP;
        int startX = graphics.guiWidth() - EDGE_MARGIN - barWidth;
        int baselineY = graphics.guiHeight() - EDGE_MARGIN - SLOT_HEIGHT;

        drawResources(graphics, font, startX, baselineY - RESOURCE_HEIGHT - 5);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = startX + slot * (SLOT_WIDTH + SLOT_GAP);
            int y = baselineY - (inventory.getSelectedSlot() == slot ? 4 : 0);
            drawSlot(graphics, font, inventory.getItem(slot), slot, x, y, inventory.getSelectedSlot() == slot);
        }
    }

    private static void drawResources(GuiGraphicsExtractor graphics, Font font, int startX, int y) {
        String wood = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.WOOD));
        String stone = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.STONE));
        String metal = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.METAL));
        int resourcesWidth = RESOURCE_WIDTH * 3 + RESOURCE_GAP * 2;
        int x = startX + SLOT_COUNT * SLOT_WIDTH + (SLOT_COUNT - 1) * SLOT_GAP - resourcesWidth;

        drawResource(graphics, font, x, y, woodIcon(), wood, 0xDC6D4027);
        drawResource(graphics, font, x + RESOURCE_WIDTH + RESOURCE_GAP, y, stoneIcon(), stone, 0xDC8A7671);
        drawResource(graphics, font, x + (RESOURCE_WIDTH + RESOURCE_GAP) * 2, y, metalIcon(), metal, 0xDC596675);
    }

    private static void drawResource(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            ItemStack icon,
            String amount,
            int background
    ) {
        graphics.fill(x, y, x + RESOURCE_WIDTH, y + RESOURCE_HEIGHT, background);
        graphics.outline(x, y, RESOURCE_WIDTH, RESOURCE_HEIGHT, 0xFFB7C0CC);
        graphics.item(icon, x + 3, y + 3);
        graphics.text(font, amount, x + RESOURCE_WIDTH - 5 - font.width(amount), y + 7, TEXT, true);
    }

    private static void drawSlot(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            int slot,
            int x,
            int y,
            boolean selected
    ) {
        int background = stack.isEmpty() ? EMPTY_BACKGROUND : BACKGROUND;
        graphics.fill(x, y, x + SLOT_WIDTH, y + SLOT_HEIGHT, background);
        graphics.fill(x, y, x + SLOT_WIDTH, y + 4, rarityColor(stack));
        graphics.outline(x, y, SLOT_WIDTH, SLOT_HEIGHT, selected ? SELECTED_BORDER : BORDER);
        if (selected) {
            graphics.outline(x - 1, y - 1, SLOT_WIDTH + 2, SLOT_HEIGHT + 2, SELECTED_BORDER);
        }

        graphics.text(font, Integer.toString(slot + 1), x + 3, y + 6, 0xFFE8ECF4, true);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + (SLOT_WIDTH - ITEM_SIZE) / 2, y + 11);
            String count = stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "";
            if (!count.isEmpty()) {
                graphics.text(font, count, x + SLOT_WIDTH - 3 - font.width(count), y + SLOT_HEIGHT - 10, TEXT, true);
            }
        }
    }

    private static int rarityColor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0xFF566174;
        }
        Rarity rarity = stack.getRarity();
        return switch (rarity) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case EPIC -> EPIC;
        };
    }

    private static String amount(boolean infinite, int amount) {
        return infinite ? "∞" : Integer.toString(amount);
    }

    private static ItemStack woodIcon() {
        if (woodIcon == null) {
            woodIcon = new ItemStack(Items.OAK_LOG);
        }
        return woodIcon;
    }

    private static ItemStack stoneIcon() {
        if (stoneIcon == null) {
            stoneIcon = new ItemStack(Items.BRICK);
        }
        return stoneIcon;
    }

    private static ItemStack metalIcon() {
        if (metalIcon == null) {
            metalIcon = new ItemStack(Items.IRON_INGOT);
        }
        return metalIcon;
    }
}
