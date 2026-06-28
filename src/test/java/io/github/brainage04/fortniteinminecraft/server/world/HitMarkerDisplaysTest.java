package io.github.brainage04.fortniteinminecraft.server.world;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HitMarkerDisplaysTest {
    @Test
    void markerPositionLeavesCloseTargetsAtTarget() {
        Vec3 shooterEye = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 targetCenter = new Vec3(3.0D, 65.0D, 4.0D);

        assertEquals(targetCenter, HitMarkerDisplays.clampedMarkerPosition(shooterEye, targetCenter));
    }

    @Test
    void markerPositionClampsLongRangeTargetsToReadableDistance() {
        Vec3 shooterEye = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 targetCenter = new Vec3(0.0D, 64.0D, 80.0D);
        Vec3 marker = HitMarkerDisplays.clampedMarkerPosition(shooterEye, targetCenter);

        assertEquals(HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS, shooterEye.distanceTo(marker), 1.0E-6D);
        assertEquals(0.0D, marker.x(), 1.0E-6D);
        assertEquals(64.0D, marker.y(), 1.0E-6D);
        assertEquals(20.0D, marker.z(), 1.0E-6D);
    }

    @Test
    void markerScaleDefaultsToCurrentConstantSize() {
        HitMarkerDisplays.resetScaleModel();

        assertEquals(1.0F, HitMarkerDisplays.scaleAtDistance(0.0D), 1.0E-6F);
        assertEquals(1.0F, HitMarkerDisplays.scaleAtDistance(HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS), 1.0E-6F);
        assertEquals(1.0F, HitMarkerDisplays.scaleAtDistance(80.0D), 1.0E-6F);
    }

    @Test
    void markerScaleInterpolatesBetweenNearAndFarScaleByDistance() {
        try {
            HitMarkerDisplays.setScaleModel(0.5F, 2.0F);

            assertEquals(0.5F, HitMarkerDisplays.scaleAtDistance(-1.0D), 1.0E-6F);
            assertEquals(0.5F, HitMarkerDisplays.scaleAtDistance(0.0D), 1.0E-6F);
            assertEquals(1.25F, HitMarkerDisplays.scaleAtDistance(10.0D), 1.0E-6F);
            assertEquals(2.0F, HitMarkerDisplays.scaleAtDistance(HitMarkerDisplays.MAX_MARKER_DISTANCE_BLOCKS), 1.0E-6F);
            assertEquals(2.0F, HitMarkerDisplays.scaleAtDistance(80.0D), 1.0E-6F);
        } finally {
            HitMarkerDisplays.resetScaleModel();
        }
    }

    @Test
    void markerScaleRejectsInvisibleOrUnboundedValues() {
        assertThrows(IllegalArgumentException.class, () -> HitMarkerDisplays.setScaleModel(0.0F, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> HitMarkerDisplays.setScaleModel(1.0F, Float.NaN));
        HitMarkerDisplays.resetScaleModel();
    }
}
