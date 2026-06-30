package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class BuildInteractionTicker {
    private final BuildSessionManager sessions;
    private final BuildWorldState state;
    private final BuildRules rules;
    private final WorldBuildMaterializer materializer;

    public BuildInteractionTicker(
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer
    ) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.state = Objects.requireNonNull(state, "state");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BuildItemInteractions.updateHeldItemState(player, sessions, rules);
            BuildItemInteractions.tickTurboPlacement(player, sessions, state, rules, materializer);
        }
    }
}
