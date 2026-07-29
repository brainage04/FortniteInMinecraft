package io.github.brainage04.fortniteinminecraft.core.model;

public record BlockOffset(int x, int y, int z) {
    public BlockOffset add(BlockOffset other) {
        return new BlockOffset(x + other.x, y + other.y, z + other.z);
    }
}
