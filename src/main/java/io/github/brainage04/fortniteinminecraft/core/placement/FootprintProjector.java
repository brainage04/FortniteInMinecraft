package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.edit.BuildEditGrids;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.EditVariantId;
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
        return project(slot, EditVariantId.BASE);
    }

    public PieceFootprint project(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        return project(piece.slot(), piece.editVariant());
    }

    private PieceFootprint project(BuildSlot slot, EditVariantId editVariant) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(editVariant, "editVariant");

        ArrayList<BlockOffset> base = new ArrayList<>();
        int tile = rules.footprintSizeBlocks();
        int height = rules.wallHeightBlocks();

        switch (slot.pieceType()) {
            case WALL -> {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < tile; x++) {
                        addIfKept(base, slot, editVariant, new BlockOffset(x, y, 0), tile, height);
                    }
                }
            }
            case FLOOR -> {
                for (int z = 0; z < tile; z++) {
                    for (int x = 0; x < tile; x++) {
                        addIfKept(base, slot, editVariant, new BlockOffset(x, 0, z), tile, height);
                    }
                }
            }
            case ROOF -> {
                int layers = (tile + 1) / 2;
                int baseY = Math.max(0, rules.footprintRadiusBlocks() - 1);
                for (int layer = 0; layer < layers; layer++) {
                    int min = layer;
                    int max = tile - 1 - layer;
                    int y = Math.min(height - 1, baseY + layer);
                    for (int z = min; z <= max; z++) {
                        for (int x = min; x <= max; x++) {
                            if (x != min && x != max && z != min && z != max) {
                                continue;
                            }
                            addIfKept(base, slot, editVariant, new BlockOffset(x, y, z), tile, height);
                        }
                    }
                }
            }
            case STAIR -> {
                for (int z = 0; z < tile; z++) {
                    int y = tile - 1 - z;
                    for (int x = 0; x < tile; x++) {
                        addIfKept(base, slot, editVariant, new BlockOffset(x, y, z), tile, height);
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

    private static void addIfKept(
            ArrayList<BlockOffset> base,
            BuildSlot slot,
            EditVariantId editVariant,
            BlockOffset offset,
            int tile,
            int height
    ) {
        if (BuildEditGrids.keepsBlock(slot.pieceType(), editVariant, offset, tile, height)) {
            base.add(offset);
        }
    }

}
