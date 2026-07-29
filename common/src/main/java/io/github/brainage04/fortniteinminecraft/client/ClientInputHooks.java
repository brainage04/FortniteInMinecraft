package io.github.brainage04.fortniteinminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraftClient;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ClientAction;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ClientActionPayload;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ClientInputHooks {
    private static KeyMapping.Category fortniteCategory;

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
    private static ItemStack wallBuildStack;
    private static ItemStack floorBuildStack;
    private static ItemStack stairBuildStack;
    private static ItemStack roofBuildStack;

    private ClientInputHooks() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }

        fortniteCategory = FortniteInMinecraftClient.platform().registerKeyCategory(
                Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "controls")
        );
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

        FortniteInMinecraftClient.platform().registerClientTickEnd(ClientInputHooks::tick);
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

    public static ItemStack selectedBuildPieceStack() {
        if (selectedBuildPiece == null) {
            return ItemStack.EMPTY;
        }
        return buildPieceStack(selectedBuildPiece);
    }

    public static boolean suppressesVanillaHotbarSelection() {
        return selectedBuildPiece != null;
    }

    public static int suppressedVanillaHotbarSlot() {
        return selectedBuildPiece == null ? -1 : selectedHotbarSlot;
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
        return FortniteInMinecraftClient.platform().registerKeyMapping(new KeyMapping(translationKey, type, keyCode, fortniteCategory));
    }

    private static void updateHotbarSelection(Minecraft client) {
        int hotbarSlot = client.player.getInventory().getSelectedSlot();
        if (selectedHotbarSlot == -1) {
            selectedHotbarSlot = hotbarSlot;
            return;
        }
        if (selectedBuildPiece != null && selectedHotbarSlot != hotbarSlot) {
            deselectBuildPiece();
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
        if (selectedBuildPiece == pieceType) {
            deselectBuildPiece();
            return;
        }
        selectedBuildPiece = pieceType;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            selectedHotbarSlot = client.player.getInventory().getSelectedSlot();
        }
        send(action, true);
    }

    private static void deselectBuildPiece() {
        if (selectedBuildPiece == null) {
            return;
        }
        selectedBuildPiece = null;
        send(ClientAction.DESELECT_BUILD, true);
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
            if (canRotateSelectedBuildPiece()) {
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

    private static boolean canRotateSelectedBuildPiece() {
        return selectedBuildPiece == PieceType.STAIR || selectedBuildPiece == PieceType.ROOF;
    }

    private static ItemStack buildPieceStack(PieceType pieceType) {
        return switch (pieceType) {
            case WALL -> wallBuildStack();
            case FLOOR -> floorBuildStack();
            case STAIR -> stairBuildStack();
            case ROOF -> roofBuildStack();
        };
    }

    private static ItemStack wallBuildStack() {
        if (wallBuildStack == null) {
            wallBuildStack = new ItemStack(ModItems.WALL);
        }
        return wallBuildStack;
    }

    private static ItemStack floorBuildStack() {
        if (floorBuildStack == null) {
            floorBuildStack = new ItemStack(ModItems.FLOOR);
        }
        return floorBuildStack;
    }

    private static ItemStack stairBuildStack() {
        if (stairBuildStack == null) {
            stairBuildStack = new ItemStack(ModItems.STAIR);
        }
        return stairBuildStack;
    }

    private static ItemStack roofBuildStack() {
        if (roofBuildStack == null) {
            roofBuildStack = new ItemStack(ModItems.ROOF);
        }
        return roofBuildStack;
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
        if (FortniteInMinecraftClient.platform().canSendToServer(ClientActionPayload.TYPE)) {
            FortniteInMinecraftClient.platform().sendToServer(new ClientActionPayload(action, pressed));
        }
    }
}
