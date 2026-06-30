package io.github.brainage04.fortniteinminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ClientAction;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ClientActionPayload;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ClientInputHooks {
    private static KeyMapping reloadWeapon;
    private static final KeyMapping.Category FORTNITE_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "controls")
    );
    private static KeyMapping editBuild;
    private static boolean registered;
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static boolean jumpWasDown;
    private static boolean editModeActive;

    private ClientInputHooks() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        reloadWeapon = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fortniteinminecraft.reload_weapon",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                FORTNITE_CATEGORY
        ));
        editBuild = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fortniteinminecraft.edit_build",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                FORTNITE_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ClientInputHooks::tick);
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    public static String editKeyLabel() {
        return editBuild == null ? "G" : editBuild.getTranslatedKeyMessage().getString();
    }

    public static boolean isEditModeActive() {
        return editModeActive;
    }

    public static void setEditModeActive(boolean active) {
        editModeActive = active;
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            attackWasDown = false;
            useWasDown = false;
            jumpWasDown = false;
            editModeActive = false;
            return;
        }

        while (reloadWeapon.consumeClick()) {
            requestReload(client);
        }
        while (editBuild.consumeClick()) {
            send(ClientAction.EDIT, true);
        }

        boolean jumpDown = client.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown) {
            send(ClientAction.GLIDER_TOGGLE, true);
        }
        jumpWasDown = jumpDown;

        ItemStack stack = client.player.getMainHandItem();
        boolean attackDown = client.options.keyAttack.isDown();
        boolean sendsPrimary = editModeActive || shouldSendPrimary(stack);
        if (attackDown && sendsPrimary) {
            send(ClientAction.PRIMARY, true);
        } else if (attackWasDown) {
            send(ClientAction.PRIMARY, false);
        }
        attackWasDown = attackDown && sendsPrimary;

        boolean useDown = client.options.keyUse.isDown();
        boolean sendsSecondary = editModeActive || shouldSendSecondary(stack);
        if (useDown && sendsSecondary) {
            if (editModeActive || !useWasDown) {
                send(ClientAction.SECONDARY, true);
            }
        } else if (useWasDown) {
            send(ClientAction.SECONDARY, false);
        }
        useWasDown = useDown && sendsSecondary;
    }

    private static void requestReload(Minecraft client) {
        Item item = client.player.getMainHandItem().getItem();
        if (!ModItems.isGun(item)) {
            return;
        }
        send(ClientAction.RELOAD, true);
    }

    private static boolean shouldSendPrimary(ItemStack stack) {
        return ModItems.suppressesVanillaBlockBreaking(stack);
    }

    private static boolean shouldSendSecondary(ItemStack stack) {
        Item item = stack.getItem();
        return ModItems.isGun(item)
                || ModItems.asBuildPiece(stack) != null;
    }

    private static void send(ClientAction action, boolean pressed) {
        if (ClientPlayNetworking.canSend(ClientActionPayload.TYPE)) {
            ClientPlayNetworking.send(new ClientActionPayload(action, pressed));
        }
    }
}
