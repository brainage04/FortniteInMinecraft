package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

final class ImpulsePhysics {
    private static final double EPSILON = 1.0E-6D;

    private ImpulsePhysics() {
    }

    static Vec3 radialImpulse(
            Vec3 origin,
            Vec3 targetCenter,
            double radius,
            double horizontalStrength,
            double verticalStrength
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(targetCenter, "targetCenter");
        if (radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be positive");
        }
        if (horizontalStrength < 0.0D) {
            throw new IllegalArgumentException("horizontalStrength must be non-negative");
        }
        if (verticalStrength < 0.0D) {
            throw new IllegalArgumentException("verticalStrength must be non-negative");
        }

        Vec3 offset = targetCenter.subtract(origin);
        double distance = offset.length();
        if (distance > radius) {
            return Vec3.ZERO;
        }

        double falloff = 1.0D - (distance / radius);
        double horizontalDistance = Math.sqrt(offset.x() * offset.x() + offset.z() * offset.z());
        double x = 0.0D;
        double z = 0.0D;
        if (horizontalDistance > EPSILON) {
            double horizontalImpulse = horizontalStrength * falloff;
            x = (offset.x() / horizontalDistance) * horizontalImpulse;
            z = (offset.z() / horizontalDistance) * horizontalImpulse;
        }
        return new Vec3(x, verticalStrength * falloff, z);
    }
}
