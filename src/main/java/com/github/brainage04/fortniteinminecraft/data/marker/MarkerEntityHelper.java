package com.github.brainage04.fortniteinminecraft.data.marker;

import com.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import com.github.brainage04.fortniteinminecraft.item.building.PieceType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class MarkerEntityHelper {
    public static void addCustomData(MarkerEntity marker, BlockPos pos, PieceType pieceType) {
        ErrorReporter.Logging logging = new ErrorReporter.Logging(marker.getErrorReporterContext(), FortniteInMinecraft.LOGGER);

        NbtWriteView nbtWriteView = NbtWriteView.create(logging, marker.getRegistryManager());

        nbtWriteView.put("data", MarkerData.CODEC, new MarkerData(
                pos,
                FortniteInMinecraft.MOD_ID,
                pieceType
        ));
        nbtWriteView.put("Pos", Vec3d.CODEC, pos.toCenterPos());

        marker.readData(NbtReadView.create(logging, marker.getRegistryManager(), nbtWriteView.getNbt()));
    }

    public static @Nullable MarkerData getCustomData(MarkerEntity marker) {
        ErrorReporter.Logging logging = new ErrorReporter.Logging(marker.getErrorReporterContext(), FortniteInMinecraft.LOGGER);

        NbtWriteView nbtWriteView = NbtWriteView.create(logging, marker.getRegistryManager());
        marker.saveData(nbtWriteView);

        ReadView readView = NbtReadView.create(logging, marker.getRegistryManager(), nbtWriteView.getNbt());

        return readView.read("data", MarkerData.CODEC).orElse(null);
    }
}