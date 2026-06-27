package io.github.brainage04.fortniteinminecraft.core.model;

public enum Orientation {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Orientation clockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public BlockOffset rotateWithinTile(BlockOffset offset, int tileSize) {
        int max = tileSize - 1;
        return switch (this) {
            case NORTH -> offset;
            case EAST -> new BlockOffset(max - offset.z(), offset.y(), offset.x());
            case SOUTH -> new BlockOffset(max - offset.x(), offset.y(), max - offset.z());
            case WEST -> new BlockOffset(offset.z(), offset.y(), max - offset.x());
        };
    }
}
