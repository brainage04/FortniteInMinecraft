package io.github.brainage04.fortniteinminecraft.core.placement;

@FunctionalInterface
public interface WorldObstruction {
    boolean isSolid(String dimension, int blockX, int blockY, int blockZ);

    static WorldObstruction none() {
        return (dimension, blockX, blockY, blockZ) -> false;
    }
}
