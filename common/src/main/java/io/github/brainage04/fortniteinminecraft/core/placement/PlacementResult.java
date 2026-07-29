package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;

import java.util.Objects;

public record PlacementResult(
        boolean placed,
        BuildPieceState piece,
        PieceFootprint footprint,
        PlacementFailure failure,
        String message
) {
    public PlacementResult {
        if (placed) {
            Objects.requireNonNull(piece, "piece");
            Objects.requireNonNull(footprint, "footprint");
            if (failure != null) {
                throw new IllegalArgumentException("successful placement cannot carry failure");
            }
        } else {
            Objects.requireNonNull(failure, "failure");
            Objects.requireNonNull(message, "message");
            if (piece != null) {
                throw new IllegalArgumentException("failed placement cannot carry piece");
            }
        }
    }

    public static PlacementResult placed(BuildPieceState piece, PieceFootprint footprint) {
        return new PlacementResult(true, piece, footprint, null, "placed");
    }

    public static PlacementResult rejected(PlacementFailure failure, String message) {
        return new PlacementResult(false, null, null, failure, message);
    }
}
