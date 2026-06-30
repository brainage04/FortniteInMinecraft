package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.Objects;

public final class BuildResourceHarvest {
    private BuildResourceHarvest() {
    }

    public static HarvestResult harvest(
            BuildWorldState buildWorld,
            BuildSlot slot,
            int damage,
            long tick
    ) {
        Objects.requireNonNull(buildWorld, "buildWorld");
        Objects.requireNonNull(slot, "slot");
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }

        BuildWorldState.DamageResult damageResult = buildWorld.damage(slot, damage, tick);
        if (!damageResult.hit()) {
            return HarvestResult.missing();
        }
        MaterialType material = damageResult.before().material();
        return new HarvestResult(damageResult.before(), damageResult.after(), material, 0);
    }

    public record HarvestResult(
            BuildPieceState before,
            BuildPieceState after,
            MaterialType material,
            int grantedResources
    ) {
        public HarvestResult {
            if (grantedResources < 0) {
                throw new IllegalArgumentException("grantedResources cannot be negative");
            }
        }

        public static HarvestResult missing() {
            return new HarvestResult(null, null, null, 0);
        }

        public boolean hit() {
            return after != null;
        }

        public boolean destroyed() {
            return after != null && after.destroyed();
        }
    }
}
