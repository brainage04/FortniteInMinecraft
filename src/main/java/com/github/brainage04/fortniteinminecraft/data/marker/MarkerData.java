package com.github.brainage04.fortniteinminecraft.data.marker;

import com.github.brainage04.fortniteinminecraft.item.building.PieceType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public class MarkerData {
    public static final Codec<MarkerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                BlockPos.CODEC.fieldOf("blockPos").forGetter(MarkerData::getBlockPos),
                Codec.STRING.fieldOf("modId").forGetter(MarkerData::getModId),
                PieceType.CODEC.fieldOf("pieceType").forGetter(MarkerData::getPieceType)
        ).apply(instance, MarkerData::new)
    );
    
    private final BlockPos blockPos;
    private final String modId;
    private final PieceType pieceType;
    
    public MarkerData(BlockPos blockPos, String modId, PieceType pieceType) {
        this.blockPos = blockPos;
        this.modId = modId;
        this.pieceType = pieceType;
    }
    
    public String getModId() {
        return modId;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }

    public PieceType getPieceType() {
        return pieceType;
    }

    @Override
    public String toString() {
        return "MarkerData{" +
                "blockPos=" + blockPos +
                ", modId='" + modId + '\'' +
                ", pieceType=" + pieceType +
                '}';
    }
}