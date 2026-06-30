package io.github.brainage04.fortniteinminecraft.mixin;

import io.github.brainage04.fortniteinminecraft.server.item.ExplosiveProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.ProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void fortniteinminecraft$applyWeaponAdsZoom(boolean useFovSetting, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (!player.isUsingItem()) {
            return;
        }
        Item item = player.getUseItem().getItem();
        if (item instanceof WeaponItem weapon) {
            cir.setReturnValue(weapon.adsFovMultiplier());
        } else if (item instanceof ProjectileWeaponItem projectile) {
            cir.setReturnValue(projectile.adsFovMultiplier());
        } else if (item instanceof ExplosiveProjectileWeaponItem explosive) {
            cir.setReturnValue(explosive.adsFovMultiplier());
        }
    }
}
