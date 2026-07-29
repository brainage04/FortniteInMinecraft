package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;

import java.util.Objects;

public record PlacementPreview(
        boolean valid,
        PieceFootprint footprint,
        PlacementFailure failure,
        String message
) {
    public PlacementPreview {
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(message, "message");
        if (valid && failure != null) {
            throw new IllegalArgumentException("valid preview cannot carry failure");
        }
        if (!valid) {
            Objects.requireNonNull(failure, "failure");
        }
    }

    public static PlacementPreview valid(PieceFootprint footprint) {
        return new PlacementPreview(true, footprint, null, "placement valid");
    }

    public static PlacementPreview rejected(PieceFootprint footprint, PlacementFailure failure, String message) {
        return new PlacementPreview(false, footprint, failure, message);
    }
}
