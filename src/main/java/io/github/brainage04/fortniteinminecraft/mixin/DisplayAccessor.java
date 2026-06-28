package io.github.brainage04.fortniteinminecraft.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setTransformation")
    void fortniteinminecraft$setTransformation(Transformation transformation);

    @Invoker("setBillboardConstraints")
    void fortniteinminecraft$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setBrightnessOverride")
    void fortniteinminecraft$setBrightnessOverride(Brightness brightness);

    @Invoker("setViewRange")
    void fortniteinminecraft$setViewRange(float viewRange);

    @Invoker("setShadowRadius")
    void fortniteinminecraft$setShadowRadius(float shadowRadius);

    @Invoker("setShadowStrength")
    void fortniteinminecraft$setShadowStrength(float shadowStrength);

    @Invoker("setWidth")
    void fortniteinminecraft$setWidth(float width);

    @Invoker("setHeight")
    void fortniteinminecraft$setHeight(float height);
}
