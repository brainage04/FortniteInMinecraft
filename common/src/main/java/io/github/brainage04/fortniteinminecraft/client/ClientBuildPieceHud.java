package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ClientBuildPieceHud {
    private static final PieceType[] DISPLAY_ORDER = {PieceType.WALL, PieceType.FLOOR, PieceType.STAIR, PieceType.ROOF};
    private static final int BOTTOM_MARGIN = 40;
    private static final int CUSTOM_HOTBAR_GAP = 5;
    private static final int RIGHT_MARGIN = 24;
    private static final int PANEL_PADDING = 5;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 4;
    private static final int KEY_GAP = 2;
    private static final int SLOT_TEXT_PADDING = 6;
    private static final int ICON_SIZE = 16;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int ACTIVE_BORDER_COLOR = 0xFFFFD54F;
    private static final int ACTIVE_SLOT_COLOR = 0xAA1E3D5C;
    private static final int INACTIVE_BORDER_COLOR = 0x884A5568;
    private static final int INACTIVE_SLOT_COLOR = 0x66333A45;
    private static final int ACTIVE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int INACTIVE_TEXT_COLOR = 0xFF9AA0AA;
    private static final int MUTED_OVERLAY_COLOR = 0x66000000;
    private static ItemStack wallIcon;
    private static ItemStack floorIcon;
    private static ItemStack stairIcon;
    private static ItemStack roofIcon;
    private static boolean registered;

    private ClientBuildPieceHud() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }
        HudRendererLib.registerHudElement(new FortniteHudRendererElement(
                "Build piece selection",
                Identifier.withDefaultNamespace("scoreboard"),
                true,
                ClientBuildPieceHud::render
        ));
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) {
            return;
        }

        Font font = client.font;
        PieceType selected = ClientInputHooks.selectedBuildPiece();
        if (selected == null) {
            return;
        }
        int slotWidth = slotWidth(font);
        int rowWidth = DISPLAY_ORDER.length * slotWidth + (DISPLAY_ORDER.length - 1) * SLOT_GAP;
        int panelWidth = rowWidth + PANEL_PADDING * 2;
        int panelHeight = font.lineHeight + KEY_GAP + SLOT_SIZE + PANEL_PADDING * 2;
        int x = graphics.guiWidth() - RIGHT_MARGIN - panelWidth;
        int panelBottom = ClientFortniteHud.replacesVanillaHotbar()
                ? ClientFortniteHud.topY(graphics.guiHeight()) - CUSTOM_HOTBAR_GAP
                : graphics.guiHeight() - BOTTOM_MARGIN;
        int y = panelBottom - panelHeight;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_COLOR);

        int slotX = x + PANEL_PADDING;
        int keyY = y + PANEL_PADDING;
        int slotY = keyY + font.lineHeight + KEY_GAP;
        for (PieceType pieceType : DISPLAY_ORDER) {
            drawSlot(graphics, font, pieceType, selected == pieceType, slotX, keyY, slotY, slotWidth);
            slotX += slotWidth + SLOT_GAP;
        }
    }

    private static int slotWidth(Font font) {
        int width = SLOT_SIZE;
        for (PieceType pieceType : DISPLAY_ORDER) {
            width = Math.max(width, font.width(keyLabel(pieceType)) + SLOT_TEXT_PADDING);
        }
        return width;
    }

    private static void drawSlot(
            GuiGraphicsExtractor graphics,
            Font font,
            PieceType pieceType,
            boolean selected,
            int x,
            int keyY,
            int slotY,
            int slotWidth
    ) {
        String key = keyLabel(pieceType);
        int textColor = selected ? ACTIVE_TEXT_COLOR : INACTIVE_TEXT_COLOR;
        graphics.text(font, key, x + (slotWidth - font.width(key)) / 2, keyY, textColor, true);

        int slotX = x + (slotWidth - SLOT_SIZE) / 2;
        graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, selected ? ACTIVE_BORDER_COLOR : INACTIVE_BORDER_COLOR);
        graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, selected ? ACTIVE_SLOT_COLOR : INACTIVE_SLOT_COLOR);
        int iconX = slotX + (SLOT_SIZE - ICON_SIZE) / 2;
        int iconY = slotY + (SLOT_SIZE - ICON_SIZE) / 2;
        graphics.item(icon(pieceType), iconX, iconY);
        if (!selected) {
            graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, MUTED_OVERLAY_COLOR);
        }
    }

    private static String keyLabel(PieceType pieceType) {
        String label = ClientInputHooks.buildKeyLabel(pieceType);
        return label.isBlank() ? "—" : label;
    }

    private static ItemStack icon(PieceType pieceType) {
        return switch (pieceType) {
            case WALL -> wallIcon();
            case FLOOR -> floorIcon();
            case STAIR -> stairIcon();
            case ROOF -> roofIcon();
        };
    }

    private static ItemStack wallIcon() {
        if (wallIcon == null) {
            wallIcon = new ItemStack(ModItems.WALL);
        }
        return wallIcon;
    }

    private static ItemStack floorIcon() {
        if (floorIcon == null) {
            floorIcon = new ItemStack(ModItems.FLOOR);
        }
        return floorIcon;
    }

    private static ItemStack stairIcon() {
        if (stairIcon == null) {
            stairIcon = new ItemStack(ModItems.STAIR);
        }
        return stairIcon;
    }

    private static ItemStack roofIcon() {
        if (roofIcon == null) {
            roofIcon = new ItemStack(ModItems.ROOF);
        }
        return roofIcon;
    }
}
