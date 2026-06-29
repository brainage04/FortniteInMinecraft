package io.github.brainage04.fortniteinminecraft.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class ClientCommandRegistrar {
    private ClientCommandRegistrar() {
    }

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("fimclient")
                .executes(context -> showClientStatus(context.getSource())));
    }

    private static int showClientStatus(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("fortniteinminecraft.client.status"));
        return 1;
    }
}
