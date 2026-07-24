package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CommandRegistrar {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final BuildSessionManager sessions;
    private final BuildWorldState state;
    private final BuildRules rules;
    private final WorldBuildMaterializer materializer;

    private CommandRegistrar(
            CommandDispatcher<CommandSourceStack> dispatcher,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        this.dispatcher = dispatcher;
        this.sessions = sessions;
        this.state = state;
        this.rules = rules;
        this.materializer = materializer;
    }

    public static void initialize(
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        FortniteInMinecraft.platform().registerServerCommand(dispatcher -> register(
                dispatcher,
                sessions,
                state,
                rules,
                materializer
        ));
    }

    static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        CommandRegistrar registrar = new CommandRegistrar(dispatcher, sessions, state, rules, materializer);
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fim");
        BuildSelectionCommandRegistrations.register(root, registrar);
        BuildPlacementCommandRegistrations.register(root, registrar);
        KitCommandRegistrations.register(root);
        CombatCommandRegistrations.register(root);
        GameplaySettingCommandRegistrations.register(root);
        ResourceDebugCommandRegistrations.register(root);
        registrar.register(root);
    }

    void register(LiteralArgumentBuilder<CommandSourceStack> command) {
        dispatcher.register(command);
    }

    BuildSessionManager sessions() {
        return sessions;
    }

    BuildWorldState state() {
        return state;
    }

    BuildRules rules() {
        return rules;
    }

    WorldBuildMaterializer materializer() {
        return materializer;
    }
}
