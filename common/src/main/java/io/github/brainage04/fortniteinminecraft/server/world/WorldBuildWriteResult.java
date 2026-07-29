package io.github.brainage04.fortniteinminecraft.server.world;

import java.util.Objects;

public record WorldBuildWriteResult(boolean success, int blockCount, String message) {
    public WorldBuildWriteResult {
        if (blockCount < 0) {
            throw new IllegalArgumentException("blockCount cannot be negative");
        }
        Objects.requireNonNull(message, "message");
    }

    public static WorldBuildWriteResult success(int blockCount, String message) {
        return new WorldBuildWriteResult(true, blockCount, message);
    }

    public static WorldBuildWriteResult failure(int blockCount, String message) {
        return new WorldBuildWriteResult(false, blockCount, message);
    }
}
