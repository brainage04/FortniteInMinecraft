package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.client.network.ClientGameplayNetworking;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ClientBuildHooks {
    private static volatile boolean initialized;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_SLOT_STRIDE = 20;
    private static final int SELECTED_SLOT_SIZE = 24;
    private static final int SELECTED_SLOT_EDGE = 3;
    private static final int HOTBAR_BOTTOM_OFFSET = 22;
    private static final int SUPPRESSION_COLOR = 0xB0000000;

    private ClientBuildHooks() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        ClientGameplayNetworking.initialize();
        ClientBuildPreview.initialize();
        ClientFortniteHud.initialize();
        ClientBuildPieceHud.initialize();
        ClientLootContainerProgressHud.initialize();
        ClientInputHooks.initialize();
        HudRendererLib.registerHudElement(new FortniteHudRendererElement(
                "Build mode hotbar selection suppression",
                Identifier.withDefaultNamespace("hotbar"),
                false,
                ClientBuildHooks::suppressSelectedHotbarSlot
        ));
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static void suppressSelectedHotbarSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden() || !ClientInputHooks.suppressesVanillaHotbarSelection()) {
            return;
        }

        int selectedSlot = ClientInputHooks.suppressedVanillaHotbarSlot();
        if (selectedSlot < 0) {
            return;
        }

        int x = graphics.guiWidth() / 2 - HOTBAR_WIDTH / 2 - 1 + selectedSlot * HOTBAR_SLOT_STRIDE;
        int y = graphics.guiHeight() - HOTBAR_BOTTOM_OFFSET - 1;
        graphics.fill(x, y, x + SELECTED_SLOT_SIZE, y + SELECTED_SLOT_EDGE, SUPPRESSION_COLOR);
        graphics.fill(x, y + SELECTED_SLOT_SIZE - SELECTED_SLOT_EDGE, x + SELECTED_SLOT_SIZE, y + SELECTED_SLOT_SIZE, SUPPRESSION_COLOR);
        graphics.fill(x, y + SELECTED_SLOT_EDGE, x + SELECTED_SLOT_EDGE, y + SELECTED_SLOT_SIZE - SELECTED_SLOT_EDGE, SUPPRESSION_COLOR);
        graphics.fill(x + SELECTED_SLOT_SIZE - SELECTED_SLOT_EDGE, y + SELECTED_SLOT_EDGE, x + SELECTED_SLOT_SIZE, y + SELECTED_SLOT_SIZE - SELECTED_SLOT_EDGE, SUPPRESSION_COLOR);
    }
}
