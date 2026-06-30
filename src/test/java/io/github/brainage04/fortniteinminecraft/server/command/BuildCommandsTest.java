package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildCommandsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void commandsUsePlayerFacingInsteadOfOrientationArguments() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        assertFullyParses(dispatcher, "fim select wall wood");
        assertLeavesUnparsed(dispatcher, "fim select wall wood north", "north");

        assertFullyParses(dispatcher, "fim preview 0 64 0");
        assertLeavesUnparsed(dispatcher, "fim preview 0 64 0 north", "north");

        assertFullyParses(dispatcher, "fim place 0 64 0");
        assertLeavesUnparsed(dispatcher, "fim place 0 64 0 north", "north");


        assertFullyParses(dispatcher, "fim kit");
        assertFullyParses(dispatcher, "fim kit build");
        assertFullyParses(dispatcher, "fim kit combat");
        assertFullyParses(dispatcher, "fim kit all");
        assertFullyParses(dispatcher, "fim reload");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback true");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback false");
        assertLeavesUnparsed(dispatcher, "fim prevent-bullet-knockback maybe", "maybe");
        assertFullyParses(dispatcher, "fim hitmarker-scale");
        assertFullyParses(dispatcher, "fim hitmarker-scale reset");
        assertFullyParses(dispatcher, "fim hitmarker-scale 0.5 2");
        assertLeavesUnparsed(dispatcher, "fim hitmarker-scale 0.5 2 extra", "extra");

        assertFullyParses(dispatcher, "fim resource locate");
        assertFullyParses(dispatcher, "fim resource locate 128");
        assertFullyParses(dispatcher, "fim resource debug");
        assertFullyParses(dispatcher, "fim resource debug 32");
        assertFullyParses(dispatcher, "fim resource clear_debug");
        assertFullyParses(dispatcher, "fim resource material set wood 100");
        assertFullyParses(dispatcher, "fim resource materials add metal 25");
        assertFullyParses(dispatcher, "fim resource materials clear");
        assertFullyParses(dispatcher, "fim resource materials clear stone");
        assertFullyParses(dispatcher, "fim resource materials infinite");
        assertFullyParses(dispatcher, "fim resource materials infinite true");
        assertFullyParses(dispatcher, "fim resource ammo set medium 30");
        assertFullyParses(dispatcher, "fim resource ammo set light_ammo 30");
        assertFullyParses(dispatcher, "fim resource ammo add rockets 2");
        assertFullyParses(dispatcher, "fim resource ammo clear");
        assertFullyParses(dispatcher, "fim resource ammo clear shells");
        assertFullyParses(dispatcher, "fim resource ammo infinite");
        assertFullyParses(dispatcher, "fim resource ammo infinite false");
        assertFullyParses(dispatcher, "fim resource infinite_materials false");
        assertFullyParses(dispatcher, "fim resource infinite_ammo");
        assertLeavesUnparsed(dispatcher, "fim resource ammo set medium 30 extra", "extra");
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandRegistrar.register(
                dispatcher,
                new BuildSessionManager(),
                new BuildWorldState(),
                BuildRules.defaults(),
                null
        );
        return dispatcher;
    }

    private static void assertFullyParses(CommandDispatcher<CommandSourceStack> dispatcher, String command) {
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, null);
        assertFalse(parsed.getReader().canRead(), parsed.getReader().getRemaining());
    }

    private static void assertLeavesUnparsed(CommandDispatcher<CommandSourceStack> dispatcher, String command, String remaining) {
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, null);
        assertTrue(parsed.getReader().canRead());
        assertEquals(remaining, parsed.getReader().getRemaining());
    }
}
