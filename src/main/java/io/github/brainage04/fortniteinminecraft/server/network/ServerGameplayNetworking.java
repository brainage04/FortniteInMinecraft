package io.github.brainage04.fortniteinminecraft.server.network;

import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ClientActionPayload;
import io.github.brainage04.fortniteinminecraft.server.item.BuildEditInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.ExplosiveProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.GrapplerItem;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.PickaxeItem;
import io.github.brainage04.fortniteinminecraft.server.item.ProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponAutoFire;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerAimStates;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class ServerGameplayNetworking {
    private static boolean registered;
    private static BuildSessionManager sessions;
    private static BuildWorldState state;
    private static BuildRules rules;
    private static WorldBuildMaterializer materializer;

    private ServerGameplayNetworking() {
    }

    public static void initialize(
            BuildSessionManager buildSessions,
            BuildWorldState buildState,
            BuildRules buildRules,
            WorldBuildMaterializer worldMaterializer
    ) {
        if (registered) {
            return;
        }

        sessions = Objects.requireNonNull(buildSessions, "buildSessions");
        state = Objects.requireNonNull(buildState, "buildState");
        rules = Objects.requireNonNull(buildRules, "buildRules");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");

        FortnitePayloads.register();
        ServerPlayNetworking.registerGlobalReceiver(ClientActionPayload.TYPE, (payload, context) -> handle(context.player(), payload));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerResourceStateSync.send(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BuildEditInteractions.cancelEditing(handler.player, state, rules, materializer));
        registered = true;
    }

    private static void handle(ServerPlayer player, ClientActionPayload payload) {
        switch (payload.action()) {
            case PRIMARY -> handlePrimary(player, payload.pressed());
            case SECONDARY -> handleSecondary(player, payload.pressed());
            case RELOAD -> {
                if (payload.pressed()
                        && WeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                        && ProjectileWeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS) {
                    ExplosiveProjectileWeaponItem.handleManualReload(player, InteractionHand.MAIN_HAND);
                }
            }
            case EDIT -> {
                if (payload.pressed()) {
                    BuildEditInteractions.handleEditKey(player, state, rules, materializer);
                }
            }
            case EDIT_RESET -> {
                if (payload.pressed()) {
                    BuildEditInteractions.handleEditResetKey(player, state, rules, materializer);
                }
            }
            case SELECT_WALL -> selectBuildPiece(player, payload.pressed(), PieceType.WALL);
            case SELECT_FLOOR -> selectBuildPiece(player, payload.pressed(), PieceType.FLOOR);
            case SELECT_STAIR -> selectBuildPiece(player, payload.pressed(), PieceType.STAIR);
            case SELECT_ROOF -> selectBuildPiece(player, payload.pressed(), PieceType.ROOF);
            case DESELECT_BUILD -> {
                if (payload.pressed()) {
                    BuildItemInteractions.deactivateBuildMode(player, sessions);
                }
            }
            case ROTATE_BUILD -> {
                if (payload.pressed()) {
                    BuildItemInteractions.rotateSelectedPiece(player, sessions, rules);
                }
            }
            case REPAIR_BUILD -> {
                if (payload.pressed()) {
                    BuildItemInteractions.repairTargetedPiece(player, state, materializer);
                }
            }
            case GLIDER_TOGGLE -> {
                if (payload.pressed()) {
                    MobilityItemInteractions.toggleGlider(player);
                }
            }
        }
    }

    private static void selectBuildPiece(ServerPlayer player, boolean pressed, PieceType pieceType) {
        if (pressed) {
            BuildItemInteractions.selectPiece(player, pieceType, sessions, rules);
        }
    }

    private static void handlePrimary(ServerPlayer player, boolean pressed) {
        if (BuildEditInteractions.handlePrimaryInput(player, pressed, state, rules, materializer)) {
            return;
        }
        if (!pressed) {
            WeaponAutoFire.forgetInput(player);
            BuildItemInteractions.stopPrimaryInput(player, sessions);
            return;
        }

        if (BuildItemInteractions.handlePrimaryInput(player, sessions, state, rules, materializer) != InteractionResult.PASS) {
            return;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (ModItems.asBuildPiece(stack) != null) {
            BuildItemInteractions.handlePrimaryInput(player, InteractionHand.MAIN_HAND, sessions, state, rules, materializer);
            return;
        }
        if (stack.getItem() instanceof WeaponItem weapon) {
            ServerLevel level = player.level();
            WeaponAutoFire.rememberInput(player, InteractionHand.MAIN_HAND, weapon, level.getGameTime());
            weapon.fireFromHeldItem(level, player, InteractionHand.MAIN_HAND);
            return;
        }
        if (stack.getItem() instanceof ProjectileWeaponItem projectile) {
            projectile.fireFromHeldItem(player.level(), player, InteractionHand.MAIN_HAND, PlayerAimStates.isAiming(player));
            return;
        }
        if (stack.getItem() instanceof ExplosiveProjectileWeaponItem explosive) {
            explosive.fireFromHeldItem(player.level(), player, InteractionHand.MAIN_HAND);
            return;
        }
        if (stack.getItem() instanceof GrapplerItem grappler) {
            grappler.fireFromHeldItem(player.level(), player, InteractionHand.MAIN_HAND);
            return;
        }
        if (stack.getItem() instanceof PickaxeItem pickaxe) {
            pickaxe.use(player.level(), player, InteractionHand.MAIN_HAND);
        }
    }

    private static void handleSecondary(ServerPlayer player, boolean pressed) {
        if (BuildEditInteractions.handleSecondaryInput(player, pressed, state, rules, materializer)) {
            PlayerAimStates.setAiming(player, false);
            return;
        }
        if (!pressed) {
            PlayerAimStates.setAiming(player, false);
            return;
        }

        if (BuildItemInteractions.handleSecondaryInput(player, sessions) != InteractionResult.PASS) {
            PlayerAimStates.setAiming(player, false);
            return;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof WeaponItem
                || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof ExplosiveProjectileWeaponItem) {
            PlayerAimStates.setAiming(player, true);
            return;
        }
        PlayerAimStates.setAiming(player, false);
        if (ModItems.asBuildPiece(stack) != null) {
            BuildItemInteractions.handleSecondaryInput(player, InteractionHand.MAIN_HAND, sessions);
        }
    }
}
