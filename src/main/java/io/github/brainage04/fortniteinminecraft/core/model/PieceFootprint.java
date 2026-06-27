package io.github.brainage04.fortniteinminecraft.core.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record PieceFootprint(BuildSlot slot, List<BlockOffset> localBlocks) {
    public static final Comparator<BlockOffset> BLOCK_ORDER = Comparator
            .comparingInt(BlockOffset::y)
            .thenComparingInt(BlockOffset::z)
            .thenComparingInt(BlockOffset::x);

    public PieceFootprint {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(localBlocks, "localBlocks");
        localBlocks = List.copyOf(localBlocks);
        if (localBlocks.isEmpty()) {
            throw new IllegalArgumentException("footprint must contain at least one block");
        }
    }

    public List<BlockOffset> absoluteBlocks(BlockOffset origin) {
        Objects.requireNonNull(origin, "origin");
        ArrayList<BlockOffset> result = new ArrayList<>(localBlocks.size());
        for (BlockOffset localBlock : localBlocks) {
            result.add(origin.add(localBlock));
        }
        return List.copyOf(result);
    }
}
