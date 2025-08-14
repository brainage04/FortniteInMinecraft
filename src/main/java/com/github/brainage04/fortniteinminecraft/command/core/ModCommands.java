package com.github.brainage04.fortniteinminecraft.command.core;

import com.github.brainage04.fortniteinminecraft.command.ExampleCommand;
import com.github.brainage04.fortniteinminecraft.command.screen.OpenExampleScreenCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class ModCommands {
    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OpenExampleScreenCommand.initialize(dispatcher);

            ExampleCommand.initialize(dispatcher);
        });
    }
}
