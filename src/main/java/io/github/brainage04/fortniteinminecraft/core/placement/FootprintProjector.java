package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class FootprintProjector {
    private final BuildRules rules;

    public FootprintProjector(BuildRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public PieceFootprint project(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        ArrayList<BlockOffset> base = new ArrayList<>();
        int tile = rules.footprintSizeBlocks();
        int height = rules.wallHeightBlocks();

        switch (slot.pieceType()) {
            case WALL -> {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < tile; x++) {
                        base.add(new BlockOffset(x, y, 0));
                    }
                }
            }
            case FLOOR -> {
                for (int z = 0; z < tile; z++) {
                    for (int x = 0; x < tile; x++) {
                        base.add(new BlockOffset(x, 0, z));
                    }
                }
            }
            case ROOF -> {
                int radius = rules.footprintRadiusBlocks();
                int y = radius - 1;
                for (int shellRadius = radius; shellRadius >= 0; shellRadius--) {
                    for (int x = -shellRadius; x <= shellRadius; x++) {
                        for (int z = -shellRadius; z <= shellRadius; z++) {
                            if (Math.abs(x) != shellRadius && Math.abs(z) != shellRadius) {
                                continue;
                            }
                            base.add(new BlockOffset(x + radius, y, z + radius));
                        }
                    }
                    y++;
                }
            }
            case STAIR -> {
                for (int z = 0; z < tile; z++) {
                    int y = tile - 1 - z;
                    for (int x = 0; x < tile; x++) {
                        base.add(new BlockOffset(x, y, z));
                    }
                }
            }
        }

        LinkedHashSet<BlockOffset> rotated = new LinkedHashSet<>(base.size());
        for (BlockOffset offset : base) {
            rotated.add(slot.orientation().rotateWithinTile(offset, tile));
        }
        ArrayList<BlockOffset> ordered = new ArrayList<>(rotated);
        ordered.sort(PieceFootprint.BLOCK_ORDER);
        return new PieceFootprint(slot, ordered);
    }
}
