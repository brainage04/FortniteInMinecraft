package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface BuildPreviewRenderer {
    PreviewMode mode();

    int show(ServerLevel level, ServerPlayer player, PieceFootprint footprint, boolean valid);

    void clear(ServerPlayer player);

    default void clearAll() {
    }

    String renderedUnit(boolean valid);
}
