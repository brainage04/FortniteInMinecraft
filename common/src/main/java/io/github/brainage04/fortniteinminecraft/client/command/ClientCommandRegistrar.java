package io.github.brainage04.fortniteinminecraft.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ClientCommandRegistrar {
    private ClientCommandRegistrar() {
    }

    public static void initialize() {
        FortniteInMinecraftClient.platform().registerClientCommand(
                LiteralArgumentBuilder.<SharedSuggestionProvider>literal("fimclient")
                        .executes(context -> showClientStatus())
        );
    }


    private static int showClientStatus() {
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("fortniteinminecraft.client.status"));
        return 1;
    }
}
