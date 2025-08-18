package com.github.brainage04.fortniteinminecraft.util;

import com.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import com.github.brainage04.fortniteinminecraft.data.marker.MarkerData;
import com.github.brainage04.fortniteinminecraft.data.marker.MarkerEntityHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityUtils {
    public static @Nullable MarkerData getNearbyMarker(World world, BlockPos pos) {
        // todo: find better way to do this start
        double range = 0.5;
        Box box = new Box(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + 1 + range, pos.getY() + 1 + range, pos.getZ() + 1 + range
        );

        List<MarkerEntity> markers = world.getEntitiesByType(
                EntityType.MARKER,
                box,
                entity -> true
        );
        // todo: find better way to do this end

        for (MarkerEntity marker : markers) {
            if (marker.getPos().equals(pos.toCenterPos())) {
                MarkerData markerData = MarkerEntityHelper.getCustomData(marker);

                if (markerData != null && markerData.getModId().equals(FortniteInMinecraft.MOD_ID)) return markerData;
            }
        }

        return null;
    }
}
