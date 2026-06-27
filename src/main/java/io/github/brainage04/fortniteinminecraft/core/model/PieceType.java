package io.github.brainage04.fortniteinminecraft.core.model;

public enum PieceType {
    WALL,
    FLOOR,
    STAIR,
    ROOF;

    public boolean ignoresOrientation() {
        return this == FLOOR || this == ROOF;
    }
}
