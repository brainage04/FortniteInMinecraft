package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.client.ClientInputHooks;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyArg(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 0
            ),
            index = 5
    )
    private ItemStack fortniteinminecraft$showBuildPieceInMainHand(ItemStack original) {
        ItemStack buildPiece = ClientInputHooks.selectedBuildPieceStack();
        return buildPiece.isEmpty() ? original : buildPiece;
    }
}
