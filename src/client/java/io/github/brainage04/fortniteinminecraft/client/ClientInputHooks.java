package io.github.brainage04.fortniteinminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
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
    private static final KeyMapping.Category FORTNITE_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "controls")
    );

    private static KeyMapping reloadWeapon;
    private static KeyMapping editBuildPrimary;
    private static KeyMapping editBuildSecondary;
    private static KeyMapping editResetPrimary;
    private static KeyMapping editResetSecondary;
    private static KeyMapping buildWall;
    private static KeyMapping buildFloor;
    private static KeyMapping buildStair;
    private static KeyMapping buildRoof;
    private static KeyMapping rotateBuild;
    private static KeyMapping repairBuild;
    private static boolean registered;
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static boolean jumpWasDown;
    private static boolean editModeActive;
    private static PieceType selectedBuildPiece;
    private static int selectedHotbarSlot = -1;

    private ClientInputHooks() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        reloadWeapon = key("key.fortniteinminecraft.reload_weapon", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R);
        editBuildPrimary = key("key.fortniteinminecraft.edit_build", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F);
        editBuildSecondary = key("key.fortniteinminecraft.edit_build.secondary", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue());
        editResetPrimary = key("key.fortniteinminecraft.edit_reset", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue());
        editResetSecondary = key("key.fortniteinminecraft.edit_reset.secondary", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue());
        buildWall = key("key.fortniteinminecraft.build_wall", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5);
        buildFloor = key("key.fortniteinminecraft.build_floor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT);
        buildStair = key("key.fortniteinminecraft.build_stair", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4);
        buildRoof = key("key.fortniteinminecraft.build_roof", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C);
        rotateBuild = key("key.fortniteinminecraft.rotate_build", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R);
        repairBuild = key("key.fortniteinminecraft.repair_build", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H);

        ClientTickEvents.END_CLIENT_TICK.register(ClientInputHooks::tick);
        registered = true;
    }

    public static boolean isInitialized() {
        return registered;
    }

    public static String editKeyLabel() {
        return keyLabel(editBuildPrimary, "F");
    }

    public static String editResetKeyLabel() {
        return keyLabel(editResetPrimary, "Unbound");
    }

    public static boolean isEditModeActive() {
        return editModeActive;
    }

    public static void setEditModeActive(boolean active) {
        editModeActive = active;
    }

    public static boolean isBuildModeActive() {
        return selectedBuildPiece != null;
    }

    public static PieceType selectedBuildPiece() {
        return selectedBuildPiece;
    }

    public static String buildKeyLabel(PieceType pieceType) {
        return keyLabel(buildKey(pieceType), "");
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            attackWasDown = false;
            useWasDown = false;
            jumpWasDown = false;
            editModeActive = false;
            selectedBuildPiece = null;
            selectedHotbarSlot = -1;
            return;
        }

        updateHotbarSelection(client);
        handleBuildPieceSelection();
        handleEditKeys();
        handleBuildUtilityKeys();
        handleReload(client);
        handleGliderToggle(client);
        handlePrimarySecondaryInputs(client);
    }

    private static KeyMapping key(String translationKey, InputConstants.Type type, int keyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(translationKey, type, keyCode, FORTNITE_CATEGORY));
    }

    private static void updateHotbarSelection(Minecraft client) {
        int hotbarSlot = client.player.getInventory().getSelectedSlot();
        if (selectedHotbarSlot == -1) {
            selectedHotbarSlot = hotbarSlot;
            return;
        }
        if (selectedBuildPiece != null && selectedHotbarSlot != hotbarSlot) {
            selectedBuildPiece = null;
            send(ClientAction.DESELECT_BUILD, true);
        }
        selectedHotbarSlot = hotbarSlot;
    }

    private static void handleBuildPieceSelection() {
        while (buildWall.consumeClick()) {
            selectBuildPiece(PieceType.WALL, ClientAction.SELECT_WALL);
        }
        while (buildFloor.consumeClick()) {
            selectBuildPiece(PieceType.FLOOR, ClientAction.SELECT_FLOOR);
        }
        while (buildStair.consumeClick()) {
            selectBuildPiece(PieceType.STAIR, ClientAction.SELECT_STAIR);
        }
        while (buildRoof.consumeClick()) {
            selectBuildPiece(PieceType.ROOF, ClientAction.SELECT_ROOF);
        }
    }

    private static void selectBuildPiece(PieceType pieceType, ClientAction action) {
        selectedBuildPiece = pieceType;
        send(action, true);
    }

    private static void handleEditKeys() {
        while (editBuildPrimary.consumeClick()) {
            send(ClientAction.EDIT, true);
        }
        while (editBuildSecondary.consumeClick()) {
            send(ClientAction.EDIT, true);
        }
        while (editResetPrimary.consumeClick()) {
            send(ClientAction.EDIT_RESET, true);
        }
        while (editResetSecondary.consumeClick()) {
            send(ClientAction.EDIT_RESET, true);
        }
    }

    private static void handleBuildUtilityKeys() {
        while (rotateBuild.consumeClick()) {
            if (selectedBuildPiece != null) {
                send(ClientAction.ROTATE_BUILD, true);
            }
        }
        while (repairBuild.consumeClick()) {
            if (selectedBuildPiece != null) {
                send(ClientAction.REPAIR_BUILD, true);
            }
        }
    }

    private static void handleReload(Minecraft client) {
        while (reloadWeapon.consumeClick()) {
            if (selectedBuildPiece == null) {
                requestReload(client);
            }
        }
    }

    private static void handleGliderToggle(Minecraft client) {
        boolean jumpDown = client.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown) {
            send(ClientAction.GLIDER_TOGGLE, true);
        }
        jumpWasDown = jumpDown;
    }

    private static void handlePrimarySecondaryInputs(Minecraft client) {
        ItemStack stack = client.player.getMainHandItem();
        boolean attackDown = client.options.keyAttack.isDown();
        boolean sendsPrimary = editModeActive || selectedBuildPiece != null || shouldSendPrimary(stack);
        if (attackDown && sendsPrimary) {
            send(ClientAction.PRIMARY, true);
        } else if (attackWasDown) {
            send(ClientAction.PRIMARY, false);
        }
        attackWasDown = attackDown && sendsPrimary;

        boolean useDown = client.options.keyUse.isDown();
        boolean sendsSecondary = editModeActive || selectedBuildPiece != null || shouldSendSecondary(stack);
        if (useDown && sendsSecondary) {
            if (editModeActive || selectedBuildPiece != null || !useWasDown) {
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

    private static KeyMapping buildKey(PieceType pieceType) {
        return switch (pieceType) {
            case WALL -> buildWall;
            case FLOOR -> buildFloor;
            case STAIR -> buildStair;
            case ROOF -> buildRoof;
        };
    }

    private static String keyLabel(KeyMapping mapping, String fallback) {
        return mapping == null ? fallback : mapping.getTranslatedKeyMessage().getString();
    }

    private static void send(ClientAction action, boolean pressed) {
        if (ClientPlayNetworking.canSend(ClientActionPayload.TYPE)) {
            ClientPlayNetworking.send(new ClientActionPayload(action, pressed));
        }
    }
}
