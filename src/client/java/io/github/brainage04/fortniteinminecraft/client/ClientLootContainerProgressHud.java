package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.LootContainerProgressPayload;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ClientLootContainerProgressHud {
    private static final Identifier HOTBAR_LAYER = Identifier.withDefaultNamespace("hotbar");
    private static final int BAR_WIDTH = 112;
    private static final int BAR_HEIGHT = 8;
    private static final int PANEL_PADDING = 5;
    private static final int HOTBAR_OFFSET = 58;
    private static final int BACKGROUND_COLOR = 0xA0000000;
    private static final int BAR_BACKGROUND_COLOR = 0xFF241B10;
    private static final int BAR_BORDER_COLOR = 0xFFE2B14C;
    private static final int BAR_FILL_COLOR = 0xFFFFC94A;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static volatile Progress progress;
    private static boolean registered;

    private ClientLootContainerProgressHud() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        HudRendererLib.registerHudElement(new FortniteHudRendererElement(
                "Loot container progress",
                HOTBAR_LAYER,
                true,
                ClientLootContainerProgressHud::render
        ));
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    public static void acceptProgress(LootContainerProgressPayload payload) {
        if (!payload.active()) {
            progress = null;
            return;
        }
        progress = new Progress(payload.label(), payload.elapsedTicks(), payload.totalTicks());
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Progress current = progress;
        if (client.player == null || client.gui.hud.isHidden() || current == null) {
            return;
        }

        Font font = client.font;
        int textWidth = font.width(current.label());
        int panelWidth = Math.max(BAR_WIDTH, textWidth) + PANEL_PADDING * 2;
        int panelHeight = font.lineHeight + 3 + BAR_HEIGHT + PANEL_PADDING * 2;
        int x = (graphics.guiWidth() - panelWidth) / 2;
        int y = graphics.guiHeight() - HOTBAR_OFFSET - panelHeight;
        int barX = x + (panelWidth - BAR_WIDTH) / 2;
        int barY = y + PANEL_PADDING + font.lineHeight + 3;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_COLOR);
        graphics.text(font, current.label(), x + (panelWidth - textWidth) / 2, y + PANEL_PADDING, TEXT_COLOR, true);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BORDER_COLOR);
        graphics.fill(barX + 1, barY + 1, barX + BAR_WIDTH - 1, barY + BAR_HEIGHT - 1, BAR_BACKGROUND_COLOR);
        int fillWidth = Math.round((BAR_WIDTH - 2) * current.ratio());
        if (fillWidth > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + BAR_HEIGHT - 1, BAR_FILL_COLOR);
        }
    }

    private record Progress(String label, int elapsedTicks, int totalTicks) {
        private Progress {
            label = label == null || label.isBlank() ? "Opening" : label;
            totalTicks = Math.max(1, totalTicks);
            elapsedTicks = Math.clamp(elapsedTicks, 0, totalTicks);
        }

        private float ratio() {
            return (float) elapsedTicks / (float) totalTicks;
        }
    }
}
