package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.client.ClientFortniteHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$replaceCreativeHotbar(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo callback
    ) {
        if (ClientFortniteHud.replacesVanillaHotbar()) {
            callback.cancel();
        }
    }
}
