package com.github.brainage04.fortnite_in_minecraft.item.building;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.StringIdentifiable;

import java.util.Map;

public enum Material implements StringIdentifiable {
    WOOD(Blocks.OAK_PLANKS),
    BRICK(Blocks.BRICKS),
    METAL(Blocks.CUT_COPPER);

    public final Block base;

    Material(Block base) {
        this.base = base;
    }

    @Override
    public String asString() {
        return name();
    }

    public Block getPiece(PieceType pieceType) {
        return switch (pieceType) {
            case WALL -> WALL_VARIANTS.get(base);
            case STAIR -> STAIR_VARIANTS.get(base);
            case FLOOR -> FLOOR_VARIANTS.get(base);
            case CONE -> CONE_VARIANTS.get(base);
        };
    }

    private static final Map<Block, Block> WALL_VARIANTS = Map.ofEntries(
            Map.entry(WOOD.base, WOOD.base),
            Map.entry(BRICK.base, BRICK.base),
            Map.entry(METAL.base, METAL.base)
    );

    private static final Map<Block, Block> STAIR_VARIANTS = Map.ofEntries(
            Map.entry(WOOD.base, Blocks.OAK_STAIRS),
            Map.entry(BRICK.base, Blocks.BRICK_STAIRS),
            Map.entry(METAL.base, Blocks.CUT_COPPER_STAIRS)
    );

    private static final Map<Block, Block> FLOOR_VARIANTS = Map.ofEntries(
            Map.entry(WOOD.base, Blocks.OAK_SLAB),
            Map.entry(BRICK.base, Blocks.BRICK_SLAB),
            Map.entry(METAL.base, Blocks.CUT_COPPER_SLAB)
    );

    private static final Map<Block, Block> CONE_VARIANTS = Map.ofEntries(
            Map.entry(WOOD.base, WOOD.base),
            Map.entry(BRICK.base, BRICK.base),
            Map.entry(METAL.base, METAL.base)
    );

    public static final Codec<Material> CODEC = StringIdentifiable.createCodec(Material::values);
}
