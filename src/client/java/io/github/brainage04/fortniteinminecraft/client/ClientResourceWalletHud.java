package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ClientResourceWalletHud {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "resource_wallet");
    private static final int RIGHT_MARGIN = 8;
    private static final int EDGE_MARGIN = 8;
    private static final int ICON_SIZE = 16;
    private static final int ENTRY_HEIGHT = 27;
    private static final int ENTRY_GAP = 4;
    private static final int ENTRY_HORIZONTAL_PADDING = 5;
    private static final int SCOREBOARD_MAX_LINES = 17;
    private static ItemStack woodIcon;
    private static ItemStack stoneIcon;
    private static ItemStack metalIcon;
    private static ItemStack goldIcon;
    private static ItemStack lightAmmoIcon;
    private static ItemStack mediumAmmoIcon;
    private static ItemStack shellsIcon;
    private static ItemStack heavyAmmoIcon;
    private static ItemStack rocketsIcon;
    private static boolean registered;

    private ClientResourceWalletHud() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.SCOREBOARD, HUD_ID, ClientResourceWalletHud::render);
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
        String wood = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.WOOD));
        String stone = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.STONE));
        String metal = amount(ClientResourceState.infiniteMaterials(), ClientResourceState.material(MaterialType.METAL));
        String gold = Integer.toString(ClientResourceState.gold());
        String lightAmmo = amount(ClientResourceState.infiniteAmmo(), ClientResourceState.ammo(AmmoType.LIGHT));
        String mediumAmmo = amount(ClientResourceState.infiniteAmmo(), ClientResourceState.ammo(AmmoType.MEDIUM));
        String shells = amount(ClientResourceState.infiniteAmmo(), ClientResourceState.ammo(AmmoType.SHELLS));
        String heavyAmmo = amount(ClientResourceState.infiniteAmmo(), ClientResourceState.ammo(AmmoType.HEAVY));
        String rockets = amount(ClientResourceState.infiniteAmmo(), ClientResourceState.ammo(AmmoType.ROCKETS));

        int woodWidth = entryWidth(font, wood);
        int stoneWidth = entryWidth(font, stone);
        int metalWidth = entryWidth(font, metal);
        int goldWidth = entryWidth(font, gold);
        int materialWidth = woodWidth + stoneWidth + metalWidth + goldWidth + ENTRY_GAP * 3;
        int lightAmmoWidth = entryWidth(font, lightAmmo);
        int mediumAmmoWidth = entryWidth(font, mediumAmmo);
        int shellsWidth = entryWidth(font, shells);
        int heavyAmmoWidth = entryWidth(font, heavyAmmo);
        int rocketsWidth = entryWidth(font, rockets);
        int ammoWidth = lightAmmoWidth + mediumAmmoWidth + shellsWidth + heavyAmmoWidth + rocketsWidth + ENTRY_GAP * 4;
        int walletWidth = materialWidth + ENTRY_GAP + ammoWidth;

        int lineHeight = font.lineHeight + 1;
        int scoreboardHeight = SCOREBOARD_MAX_LINES * lineHeight;
        int scoreboardTop = Math.max(EDGE_MARGIN, (graphics.guiHeight() - scoreboardHeight) / 2);
        int y = scoreboardTop - EDGE_MARGIN - ENTRY_HEIGHT;
        if (y < EDGE_MARGIN) {
            int belowScoreboard = scoreboardTop + scoreboardHeight + EDGE_MARGIN;
            y = belowScoreboard + ENTRY_HEIGHT <= graphics.guiHeight() - EDGE_MARGIN ? belowScoreboard : EDGE_MARGIN;
        }

        int materialX = graphics.guiWidth() - RIGHT_MARGIN - materialWidth;
        int ammoX = materialX - ENTRY_GAP - ammoWidth;
        graphics.fill(ammoX - 4, y - 4, ammoX + walletWidth + 4, y + ENTRY_HEIGHT + 3, 0x66000000);

        int entryX = ammoX;
        entryX = drawEntry(graphics, font, entryX, y, lightAmmoWidth, lightAmmoIcon(), lightAmmo);
        entryX = drawEntry(graphics, font, entryX, y, mediumAmmoWidth, mediumAmmoIcon(), mediumAmmo);
        entryX = drawEntry(graphics, font, entryX, y, shellsWidth, shellsIcon(), shells);
        entryX = drawEntry(graphics, font, entryX, y, heavyAmmoWidth, heavyAmmoIcon(), heavyAmmo);
        drawEntry(graphics, font, entryX, y, rocketsWidth, rocketsIcon(), rockets);

        entryX = materialX;
        entryX = drawEntry(graphics, font, entryX, y, woodWidth, woodIcon(), wood);
        entryX = drawEntry(graphics, font, entryX, y, stoneWidth, stoneIcon(), stone);
        entryX = drawEntry(graphics, font, entryX, y, metalWidth, metalIcon(), metal);
        drawEntry(graphics, font, entryX, y, goldWidth, goldIcon(), gold);
    }

    private static String amount(boolean infinite, int amount) {
        return infinite ? "∞" : Integer.toString(amount);
    }

    private static int entryWidth(Font font, String amount) {
        return Math.max(ICON_SIZE, font.width(amount)) + ENTRY_HORIZONTAL_PADDING * 2;
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

    private static ItemStack goldIcon() {
        if (goldIcon == null) {
            goldIcon = new ItemStack(Items.GOLD_INGOT);
        }
        return goldIcon;
    }

    private static ItemStack lightAmmoIcon() {
        if (lightAmmoIcon == null) {
            lightAmmoIcon = new ItemStack(Items.IRON_NUGGET);
        }
        return lightAmmoIcon;
    }

    private static ItemStack mediumAmmoIcon() {
        if (mediumAmmoIcon == null) {
            mediumAmmoIcon = new ItemStack(Items.IRON_INGOT);
        }
        return mediumAmmoIcon;
    }

    private static ItemStack shellsIcon() {
        if (shellsIcon == null) {
            shellsIcon = new ItemStack(Items.FLINT);
        }
        return shellsIcon;
    }

    private static ItemStack heavyAmmoIcon() {
        if (heavyAmmoIcon == null) {
            heavyAmmoIcon = new ItemStack(Items.COPPER_INGOT);
        }
        return heavyAmmoIcon;
    }

    private static ItemStack rocketsIcon() {
        if (rocketsIcon == null) {
            rocketsIcon = new ItemStack(Items.FIREWORK_ROCKET);
        }
        return rocketsIcon;
    }

    private static int drawEntry(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            ItemStack icon,
            String amount
    ) {
        graphics.item(icon, x + (width - ICON_SIZE) / 2, y);
        graphics.text(font, amount, x + (width - font.width(amount)) / 2, y + ICON_SIZE + 2, 0xFFFFFFFF, true);
        return x + width + ENTRY_GAP;
    }
}
