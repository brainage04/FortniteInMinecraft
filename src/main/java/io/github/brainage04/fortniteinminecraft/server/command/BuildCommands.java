package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.world.BuildPreviewRenderers;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.minecraft.commands.CommandSourceStack;

public final class BuildCommands {
  private BuildCommands() {
  }

  public static void register(
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    CommandRegistrar.initialize(sessions, state, rules, materializer, previewRenderers);
  }

  static void register(
      CommandDispatcher<CommandSourceStack> dispatcher,
      BuildSessionManager sessions,
      BuildWorldState state,
      BuildRules rules,
      WorldBuildMaterializer materializer,
      BuildPreviewRenderers previewRenderers) {
    CommandRegistrar.register(dispatcher, sessions, state, rules, materializer, previewRenderers);
  }
}
