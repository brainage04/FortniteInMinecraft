package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.placement.BuildSupportCascade;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class BuildCollapseScheduler {
    private static BuildWorldState state;
    private static WorldBuildMaterializer materializer;
    private static BuildSupportCascade cascade;
    private static boolean registered;

    private BuildCollapseScheduler() {
    }

    public static void configure(BuildWorldState buildState, BuildRules rules, WorldBuildMaterializer worldMaterializer) {
        state = Objects.requireNonNull(buildState, "buildState");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");
        cascade = new BuildSupportCascade(Objects.requireNonNull(rules, "rules"));
        if (!registered) {
            FortniteInMinecraft.platform().registerEndLevelTick(BuildCollapseScheduler::tickLevel);
            registered = true;
        }
    }

    public static int scheduleAfterSupportRemoved(ServerLevel level, BuildSlot removedSupport, long tick) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(removedSupport, "removedSupport");
        if (state == null || materializer == null || cascade == null) {
            return 0;
        }
        String dimension = level.dimension().identifier().toString();
        List<BuildSupportCascade.CollapseStep> plan = cascade.collapsePlan(state, dimension, staticWorld(level, dimension), removedSupport);
        return state.scheduleCollapse(plan, tick);
    }

    private static void tickLevel(ServerLevel level) {
        if (state == null || materializer == null || cascade == null || state.scheduledCollapseCount() == 0) {
            return;
        }
        String dimension = level.dimension().identifier().toString();
        HashSet<BuildSlot> stillUnsupported = new HashSet<>();
        for (BuildPieceState piece : cascade.unsupportedPieces(state, dimension, staticWorld(level, dimension))) {
            stillUnsupported.add(piece.slot());
        }
        for (BuildPieceState piece : state.drainDueCollapses(dimension, level.getGameTime(), stillUnsupported)) {
            WorldBuildWriteResult clearResult = materializer.clear(level, piece);
            if (clearResult.success()) {
                state.remove(piece.slot());
                BuildWeakPoints.clear(piece.slot());
            } else {
                FortniteInMinecraft.LOGGER.warn("Scheduled build collapse failed for {}: {}", piece.slot(), clearResult.message());
            }
        }
    }

    private static WorldObstruction staticWorld(ServerLevel level, String dimension) {
        return (candidateDimension, x, y, z) -> dimension.equals(candidateDimension)
                && !materializer.isTrackedBlock(candidateDimension, x, y, z)
                && WorldObstructions.isBlockingCollision(level, new BlockPos(x, y, z));
    }
}
