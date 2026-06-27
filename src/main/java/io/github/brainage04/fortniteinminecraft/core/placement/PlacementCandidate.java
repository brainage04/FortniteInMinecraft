package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;

import java.util.Objects;

public record PlacementCandidate(BuildSlot slot, MaterialType material) {
    public PlacementCandidate {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(material, "material");
    }
}
