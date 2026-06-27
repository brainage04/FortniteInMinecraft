package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.session.PreviewMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class BuildPreviewParticles implements BuildPreviewRenderer {
    private static final DustParticleOptions VALID = new DustParticleOptions(0x66A3FF, 1.0F);
    private static final DustParticleOptions INVALID = new DustParticleOptions(0xFF6666, 1.0F);

    private final WorldBuildMaterializer materializer;

    public BuildPreviewParticles(WorldBuildMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    @Override
    public PreviewMode mode() {
        return PreviewMode.PARTICLES;
    }

    @Override
    public int show(ServerLevel level, ServerPlayer player, PieceFootprint footprint, boolean valid) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(footprint, "footprint");

        DustParticleOptions particle = valid ? VALID : INVALID;
        int sent = 0;
        for (BlockPos pos : materializer.blockPositions(footprint)) {
            if (level.sendParticles(
                    player,
                    particle,
                    true,
                    true,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            )) {
                sent++;
            }
        }
        return sent;
    }

    @Override
    public void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
    }

    @Override
    public String renderedUnit(boolean valid) {
        return valid ? "blue particles" : "red particles";
    }
}
