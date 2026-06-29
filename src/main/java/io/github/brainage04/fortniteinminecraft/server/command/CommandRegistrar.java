package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CommandRegistrar {
  private final CommandDispatcher<CommandSourceStack> dispatcher;
  private final BuildSessionManager sessions;
  private final BuildWorldState state;
  private final BuildRules rules;
  private final WorldBuildMaterializer materializer;
  private final BuildPreviewRenderers previewRenderers;

  private CommandRegistrar(
      CommandDispatcher<CommandSourceStack> dispatcher,
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    this.dispatcher = dispatcher;
    this.sessions = sessions;
    this.state = state;
    this.rules = rules;
    this.materializer = materializer;
    this.previewRenderers = previewRenderers;
  }

  public static void initialize(
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(
        dispatcher,
        sessions,
        state,
        rules,
        materializer,
        previewRenderers));
  }

  static void register(
      CommandDispatcher<CommandSourceStack> dispatcher,
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    CommandRegistrar registrar = new CommandRegistrar(dispatcher, sessions, state, rules, materializer, previewRenderers);
    LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fim");
    BuildSelectionCommandRegistrations.register(root, registrar);
    BuildPlacementCommandRegistrations.register(root, registrar);
    KitCommandRegistrations.register(root);
    GameplaySettingCommandRegistrations.register(root);
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

  BuildPreviewRenderers previewRenderers() {
    return previewRenderers;
  }
}
