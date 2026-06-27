package io.github.brainage04.fortniteinminecraft.core.model;

import java.util.Objects;

public record BuildGridPos(String dimension, int x, int y, int z) {
    public BuildGridPos {
        Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank()) {
            throw new IllegalArgumentException("dimension cannot be blank");
        }
    }
}
