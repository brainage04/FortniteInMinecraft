package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class BuildPreviewRenderers {
    private final Map<PreviewMode, BuildPreviewRenderer> renderers = new EnumMap<>(PreviewMode.class);

    public BuildPreviewRenderers(BuildPreviewRenderer... renderers) {
        for (BuildPreviewRenderer renderer : renderers) {
            BuildPreviewRenderer previous = this.renderers.put(renderer.mode(), renderer);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate preview renderer for " + renderer.mode());
            }
        }
        for (PreviewMode mode : PreviewMode.values()) {
            if (!this.renderers.containsKey(mode)) {
                throw new IllegalArgumentException("missing preview renderer for " + mode);
            }
        }
    }

    public int show(PreviewMode mode, ServerLevel level, ServerPlayer player, PieceFootprint footprint, boolean valid) {
        return renderer(mode).show(level, player, footprint, valid);
    }

    public void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        for (BuildPreviewRenderer renderer : renderers.values()) {
            renderer.clear(player);
        }
    }

    public String renderedUnit(PreviewMode mode, boolean valid) {
        return renderer(mode).renderedUnit(valid);
    }

    private BuildPreviewRenderer renderer(PreviewMode mode) {
        return Objects.requireNonNull(renderers.get(Objects.requireNonNull(mode, "mode")), "mode");
    }
}
