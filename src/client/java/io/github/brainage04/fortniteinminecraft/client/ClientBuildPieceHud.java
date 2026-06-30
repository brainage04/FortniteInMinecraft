package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class ClientBuildPieceHud {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "build_piece_selection");
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 72;
    private static final int PANEL_PADDING = 5;
    private static final int LINE_GAP = 2;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int ACTIVE_COLOR = 0xFFFFD54F;
    private static final int INACTIVE_COLOR = 0xFFE0E0E0;
    private static final int HEADER_COLOR = 0xFFFFFFFF;
    private static boolean registered;

    private ClientBuildPieceHud() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }
        HudElementRegistry.attachElementBefore(VanillaHudElements.SCOREBOARD, HUD_ID, ClientBuildPieceHud::render);
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        PieceType selected = ClientInputHooks.selectedBuildPiece();
        if (client.player == null || client.gui.hud.isHidden() || selected == null) {
            return;
        }

        Font font = client.font;
        String header = "Build Piece";
        int width = font.width(header);
        for (PieceType pieceType : PieceType.values()) {
            width = Math.max(width, font.width(entry(pieceType)));
        }

        int lineHeight = font.lineHeight + LINE_GAP;
        int panelWidth = width + PANEL_PADDING * 2;
        int panelHeight = (PieceType.values().length + 1) * lineHeight + PANEL_PADDING * 2;
        int x = graphics.guiWidth() - RIGHT_MARGIN - panelWidth;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - panelHeight;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_COLOR);
        graphics.text(font, header, x + PANEL_PADDING, y + PANEL_PADDING, HEADER_COLOR, true);

        int textY = y + PANEL_PADDING + lineHeight;
        for (PieceType pieceType : PieceType.values()) {
            int color = pieceType == selected ? ACTIVE_COLOR : INACTIVE_COLOR;
            graphics.text(font, entry(pieceType), x + PANEL_PADDING, textY, color, true);
            textY += lineHeight;
        }
    }

    private static String entry(PieceType pieceType) {
        return label(pieceType) + " [" + ClientInputHooks.buildKeyLabel(pieceType) + "]";
    }

    private static String label(PieceType pieceType) {
        String lower = pieceType.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
