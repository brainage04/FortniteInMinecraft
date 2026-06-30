package io.github.brainage04.fortniteinminecraft.core.edit;

public record EditGridCell(int column, int row) {
    public EditGridCell {
        if (column < 0) {
            throw new IllegalArgumentException("column must be non-negative");
        }
        if (row < 0) {
            throw new IllegalArgumentException("row must be non-negative");
        }
    }
}
