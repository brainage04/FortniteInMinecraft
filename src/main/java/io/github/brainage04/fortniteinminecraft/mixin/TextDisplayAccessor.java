package io.github.brainage04.fortniteinminecraft.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("setText")
    void fortniteinminecraft$setText(Component text);

    @Invoker("setLineWidth")
    void fortniteinminecraft$setLineWidth(int lineWidth);

    @Invoker("setTextOpacity")
    void fortniteinminecraft$setTextOpacity(byte opacity);

    @Invoker("setBackgroundColor")
    void fortniteinminecraft$setBackgroundColor(int color);

    @Invoker("setFlags")
    void fortniteinminecraft$setFlags(byte flags);
}
