package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.client.ClientInputHooks;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientMultiPlayerGameModeMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressModItemStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (suppressesVanillaBlockBreaking()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressModItemContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (suppressesVanillaBlockBreaking()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressModItemDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (suppressesVanillaBlockBreaking()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressModItemAttack(Player player, Entity target, CallbackInfo ci) {
        if (suppressesVanillaBlockBreaking()) {
            ci.cancel();
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressEditUseItem(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (ClientInputHooks.isEditModeActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$suppressEditUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (ClientInputHooks.isEditModeActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    private static boolean suppressesVanillaBlockBreaking() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null
                && (ClientInputHooks.isEditModeActive()
                        || ModItems.suppressesVanillaBlockBreaking(client.player.getMainHandItem()));
    }
}
