package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public final class LaunchPadImpulse {
    public static final double DEFAULT_UPWARD_VELOCITY = 2.4D;
    public static final double DEFAULT_FORWARD_VELOCITY = 0.55D;

    private LaunchPadImpulse() {
    }

    public static Vec3 impulse(Vec3 lookDirection, double upwardVelocity, double forwardVelocity) {
        Objects.requireNonNull(lookDirection, "lookDirection");
        if (upwardVelocity < 0.0D || forwardVelocity < 0.0D) {
            throw new IllegalArgumentException("launch velocities cannot be negative");
        }
        Vec3 horizontal = new Vec3(lookDirection.x(), 0.0D, lookDirection.z());
        if (horizontal.lengthSqr() > 1.0E-9D) {
            horizontal = horizontal.normalize().scale(forwardVelocity);
        }
        return new Vec3(horizontal.x(), upwardVelocity, horizontal.z());
    }

    public static Vec3 defaultImpulse(Vec3 lookDirection) {
        return impulse(lookDirection, DEFAULT_UPWARD_VELOCITY, DEFAULT_FORWARD_VELOCITY);
    }
}
