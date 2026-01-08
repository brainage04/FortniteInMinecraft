package com.github.brainage04.fortnite_in_minecraft.data.marker;

import com.github.brainage04.fortnite_in_minecraft.item.building.PieceType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public record MarkerData(BlockPos blockPos, String modId, PieceType pieceType) {
    public static final Codec<MarkerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("blockPos").forGetter(MarkerData::blockPos),
                    Codec.STRING.fieldOf("modId").forGetter(MarkerData::modId),
                    PieceType.CODEC.fieldOf("pieceType").forGetter(MarkerData::pieceType)
            ).apply(instance, MarkerData::new)
    );

    @Override
    public @NotNull String toString() {
        return "MarkerData{" +
                "blockPos=" + blockPos +
                ", modId='" + modId + '\'' +
                ", pieceType=" + pieceType +
                '}';
    }
}