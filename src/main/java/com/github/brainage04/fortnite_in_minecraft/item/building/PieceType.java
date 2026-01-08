package com.github.brainage04.fortnite_in_minecraft.item.building;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Supplier;

public enum PieceType implements StringIdentifiable {
    WALL,
    FLOOR,
    STAIR,
    CONE;

    @Override
    public String asString() {
        return name();
    }

    public static final Codec<PieceType> CODEC = StringIdentifiable.createCodec(PieceType::values);
}
