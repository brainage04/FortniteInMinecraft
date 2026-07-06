package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.BuildEditInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.HarvestingToolInventory;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.ProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void ackBlockChangesUpTo(int sequence);
    @Inject(
            method = "handleUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fortniteinminecraft$suppressEditUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (BuildEditInteractions.hasActiveEditSession(player) || BuildItemInteractions.hasActiveBuildMode(player)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fortniteinminecraft$suppressEditUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (BuildEditInteractions.hasActiveEditSession(player) || BuildItemInteractions.hasActiveBuildMode(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At("TAIL"))
    private void fortniteinminecraft$resyncWeaponCooldown(ServerboundUseItemPacket packet, CallbackInfo ci) {
        WeaponItem.resyncCooldownOverlay(player, player.getItemInHand(packet.getHand()));
        ProjectileWeaponItem.resyncCooldownOverlay(player, player.getItemInHand(packet.getHand()));
    }

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fortniteinminecraft$suppressModItemBlockBreaking(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (isDropAction(packet.getAction()) && HarvestingToolInventory.isHarvestingTool(player.getMainHandItem())) {
            HarvestingToolInventory.enforce(player);
            HarvestingToolInventory.sync(player);
            ci.cancel();
            return;
        }
        if (isDestroyAction(packet.getAction()) && suppressesVanillaActions()) {
            ackBlockChangesUpTo(packet.getSequence());
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fortniteinminecraft$blockHarvestingToolContainerDrop(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (dropsHarvestingToolFromContainer(packet)) {
            HarvestingToolInventory.enforce(player);
            HarvestingToolInventory.sync(player);
            ci.cancel();
        }
    }

    @Inject(
            method = "handleAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fortniteinminecraft$suppressModItemMelee(ServerboundAttackPacket packet, CallbackInfo ci) {
        if (suppressesVanillaActions()) {
            ci.cancel();
        }
    }

    private boolean suppressesVanillaActions() {
        return BuildEditInteractions.hasActiveEditSession(player)
                || BuildItemInteractions.hasActiveBuildMode(player)
                || ModItems.suppressesVanillaBlockBreaking(player.getMainHandItem());
    }

    private boolean dropsHarvestingToolFromContainer(ServerboundContainerClickPacket packet) {
        if (player.containerMenu.containerId != packet.containerId()
                || player.isSpectator()
                || player.isDeadOrDying()
                || !player.containerMenu.stillValid(player)) {
            return false;
        }
        if (packet.containerInput() == ContainerInput.THROW) {
            return isHarvestingToolSlot(packet.slotNum());
        }
        return packet.containerInput() == ContainerInput.PICKUP
                && packet.slotNum() == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
                && HarvestingToolInventory.isHarvestingTool(player.containerMenu.getCarried());
    }

    private boolean isHarvestingToolSlot(int slot) {
        return slot >= 0
                && player.containerMenu.isValidSlotIndex(slot)
                && HarvestingToolInventory.isHarvestingTool(player.containerMenu.getSlot(slot).getItem());
    }

    private static boolean isDropAction(ServerboundPlayerActionPacket.Action action) {
        return switch (action) {
            case DROP_ALL_ITEMS, DROP_ITEM -> true;
            default -> false;
        };
    }

    private static boolean isDestroyAction(ServerboundPlayerActionPacket.Action action) {
        return switch (action) {
            case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> true;
            default -> false;
        };
    }
}
