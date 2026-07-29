package io.github.brainage04.fortniteinminecraft.core.model;

public enum PieceType {
    WALL,
    FLOOR,
    STAIR,
    ROOF;

    public boolean ignoresOrientation() {
        return this == FLOOR || this == ROOF;
    }

    public boolean usesStairConeGrid() {
        return this == STAIR || this == ROOF;
    }

    public boolean permitsFootprintOverlapWith(PieceType other) {
        return this == other || usesStairConeGrid() != other.usesStairConeGrid() || sharesFloorWallGridWith(other);
    }

    private boolean sharesFloorWallGridWith(PieceType other) {
        return !usesStairConeGrid() && !other.usesStairConeGrid();
    }
}
