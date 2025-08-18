package com.github.brainage04.fortniteinminecraft.item.building;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

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
