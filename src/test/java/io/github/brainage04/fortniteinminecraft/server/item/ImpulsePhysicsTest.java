package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImpulsePhysicsTest {
    @Test
    void radialImpulseFallsOffToEdge() {
        Vec3 impulse = ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                new Vec3(2.0D, 0.0D, 0.0D),
                4.0D,
                1.2D,
                0.8D
        );

        assertEquals(0.6D, impulse.x(), 1.0E-6D);
        assertEquals(0.4D, impulse.y(), 1.0E-6D);
        assertEquals(0.0D, impulse.z(), 1.0E-6D);
    }

    @Test
    void radialImpulseUsesHorizontalDirectionOnly() {
        Vec3 impulse = ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, -1.0D),
                5.0D,
                2.0D,
                1.0D
        );

        assertEquals(0.0D, impulse.x(), 1.0E-6D);
        assertEquals(0.8D, impulse.y(), 1.0E-6D);
        assertEquals(-1.6D, impulse.z(), 1.0E-6D);
    }

    @Test
    void radialImpulseOutsideRadiusIsZero() {
        Vec3 impulse = ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                new Vec3(0.0D, 0.0D, 6.0D),
                5.0D,
                2.0D,
                1.0D
        );

        assertEquals(Vec3.ZERO, impulse);
    }

    @Test
    void radialImpulseRejectsInvalidTuning() {
        assertThrows(IllegalArgumentException.class, () -> ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                Vec3.ZERO,
                0.0D,
                1.0D,
                1.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                Vec3.ZERO,
                1.0D,
                -1.0D,
                1.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> ImpulsePhysics.radialImpulse(
                Vec3.ZERO,
                Vec3.ZERO,
                1.0D,
                1.0D,
                -1.0D
        ));
    }
}
