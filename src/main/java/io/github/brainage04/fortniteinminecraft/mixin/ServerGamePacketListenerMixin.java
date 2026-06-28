package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;
    private InteractionHand fortniteinminecraft$lastWeaponUseHand;
    private long fortniteinminecraft$lastWeaponUseTick = Long.MIN_VALUE;


    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void fortniteinminecraft$handleHeldItemSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (!fortniteinminecraft$recentWeaponUse(packet.getHand())) {
            WeaponItem.handleManualReloadOnSwing(player, packet.getHand());
        }
        BuildItemInteractions.handleBuildItemSwing(player, packet.getHand());
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"))
    private void fortniteinminecraft$rememberWeaponUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (player.getItemInHand(packet.getHand()).getItem() instanceof WeaponItem) {
            fortniteinminecraft$lastWeaponUseHand = packet.getHand();
            fortniteinminecraft$lastWeaponUseTick = player.level().getGameTime();
        }
    }

    @Inject(method = "handleUseItem", at = @At("TAIL"))
    private void fortniteinminecraft$resyncWeaponCooldown(ServerboundUseItemPacket packet, CallbackInfo ci) {
        WeaponItem.resyncCooldownOverlay(player, player.getItemInHand(packet.getHand()));
    }
    private boolean fortniteinminecraft$recentWeaponUse(InteractionHand hand) {
        return hand == fortniteinminecraft$lastWeaponUseHand
                && player.level().getGameTime() - fortniteinminecraft$lastWeaponUseTick <= 1L;
    }
}
