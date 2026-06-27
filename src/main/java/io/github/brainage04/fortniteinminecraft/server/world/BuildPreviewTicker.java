package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementPreview;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementService;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import io.github.brainage04.fortniteinminecraft.core.session.ResourceWallet;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.PlayerFacingOrientation;
import io.github.brainage04.fortniteinminecraft.server.item.BuildItemInteractions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class BuildPreviewTicker {
    static final int PREVIEW_INTERVAL_TICKS = 4;

    private final BuildSessionManager sessions;
    private final BuildWorldState state;
    private final BuildRules rules;
    private final WorldBuildMaterializer materializer;
    private final BuildPreviewRenderers previewRenderers;
    private int ticks;

    public BuildPreviewTicker(
            BuildSessionManager sessions,
            BuildWorldState state,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            BuildPreviewRenderers previewRenderers
    ) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.state = Objects.requireNonNull(state, "state");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.previewRenderers = Objects.requireNonNull(previewRenderers, "previewRenderers");
    }

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ticks++;
        boolean renderPreview = shouldRender(ticks);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BuildItemInteractions.updatePreviewFromHeldItem(player, sessions, rules, previewRenderers);
            BuildItemInteractions.tickTurboPlacement(player, sessions, state, rules, materializer, previewRenderers);
            if (!renderPreview) {
                continue;
            }

            PlayerBuildSession session = sessions.get(player.getUUID());
            if (session == null) {
                previewRenderers.clear(player);
                continue;
            }
            PlacementCandidate activePreview = session.previewCandidate();
            if (activePreview == null) {
                previewRenderers.clear(player);
                continue;
            }
            PlacementCandidate candidate = session.candidateAt(
                    activePreview.slot().gridPos(),
                    PlayerFacingOrientation.horizontal(player)
            );
            session.rememberPreview(candidate);

            ServerLevel level = player.level();
            String dimension = level.dimension().identifier().toString();
            if (!dimension.equals(candidate.slot().gridPos().dimension())) {
                session.clearPreview();
                previewRenderers.clear(player);
                continue;
            }

            PlacementService placementService = new PlacementService(state, rules, obstructionFor(level, materializer));
            PlacementPreview preview = placementService.preview(candidate, playerContext(player, candidate));
            previewRenderers.show(session.previewMode(), level, player, preview.footprint(), preview.valid());
        }
    }

    static boolean shouldRender(int tick) {
        return tick % PREVIEW_INTERVAL_TICKS == 0;
    }

    private static PlayerBuildContext playerContext(ServerPlayer player, PlacementCandidate candidate) {
        if (player.isCreative()) {
            return PlayerBuildContext.creative(player.getUUID());
        }
        return PlayerBuildContext.survival(player.getUUID(), ResourceWallet.with(candidate.material(), 0));
    }

    private static WorldObstruction obstructionFor(ServerLevel level, WorldBuildMaterializer materializer) {
        return (dimension, blockX, blockY, blockZ) -> level.getBlockState(new BlockPos(blockX, blockY, blockZ)).blocksMotion()
                && !materializer.isTrackedBlock(dimension, blockX, blockY, blockZ);
    }
}
