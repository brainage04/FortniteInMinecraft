package io.github.brainage04.fortniteinminecraft.core.model;

import java.util.Objects;

public record BuildSlot(BuildGridPos gridPos, PieceType pieceType, Orientation orientation) {
    public BuildSlot {
        Objects.requireNonNull(gridPos, "gridPos");
        Objects.requireNonNull(pieceType, "pieceType");
        Objects.requireNonNull(orientation, "orientation");
        if (pieceType.ignoresOrientation()) {
            orientation = Orientation.NORTH;
        } else if (pieceType == PieceType.WALL) {
            switch (orientation) {
                case NORTH -> {
                    gridPos = new BuildGridPos(gridPos.dimension(), gridPos.x(), gridPos.y(), gridPos.z() - 1);
                    orientation = Orientation.SOUTH;
                }
                case WEST -> {
                    gridPos = new BuildGridPos(gridPos.dimension(), gridPos.x() - 1, gridPos.y(), gridPos.z());
                    orientation = Orientation.EAST;
                }
                default -> {
                }
            }
        }
    }

    public static BuildSlot of(String dimension, int x, int y, int z, PieceType pieceType, Orientation orientation) {
        return new BuildSlot(new BuildGridPos(dimension, x, y, z), pieceType, orientation);
    }
}
