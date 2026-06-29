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

        assertFullyParses(dispatcher, "fim preview-mode particles");
        assertFullyParses(dispatcher, "fim preview-mode glass");
        assertLeavesUnparsed(dispatcher, "fim preview-mode glass extra", "extra");

        assertFullyParses(dispatcher, "fim kit");
        assertFullyParses(dispatcher, "fim kit build");
        assertFullyParses(dispatcher, "fim kit combat");
        assertFullyParses(dispatcher, "fim kit all");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback true");
        assertFullyParses(dispatcher, "fim prevent-bullet-knockback false");
        assertLeavesUnparsed(dispatcher, "fim prevent-bullet-knockback maybe", "maybe");
        assertFullyParses(dispatcher, "fim hitmarker-scale");
        assertFullyParses(dispatcher, "fim hitmarker-scale reset");
        assertFullyParses(dispatcher, "fim hitmarker-scale 0.5 2");
        assertLeavesUnparsed(dispatcher, "fim hitmarker-scale 0.5 2 extra", "extra");
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandRegistrar.register(
                dispatcher,
                new BuildSessionManager(),
                new BuildWorldState(),
                BuildRules.defaults(),
                null,
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
