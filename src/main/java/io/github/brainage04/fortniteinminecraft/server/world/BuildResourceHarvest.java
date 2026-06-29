package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;

import java.util.Objects;

public final class BuildResourceHarvest {
    private BuildResourceHarvest() {
    }

    public static HarvestResult harvest(
            BuildWorldState buildWorld,
            BuildSlot slot,
            int damage,
            int resourceReward,
            long tick,
            PlayerResourceState resources
    ) {
        Objects.requireNonNull(buildWorld, "buildWorld");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(resources, "resources");
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (resourceReward < 0) {
            throw new IllegalArgumentException("resourceReward cannot be negative");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }

        BuildWorldState.DamageResult damageResult = buildWorld.damage(slot, damage, tick);
        if (!damageResult.hit()) {
            return HarvestResult.missing();
        }
        MaterialType material = damageResult.before().material();
        int granted = resources.addMaterial(material, resourceReward);
        return new HarvestResult(damageResult.before(), damageResult.after(), material, granted);
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
