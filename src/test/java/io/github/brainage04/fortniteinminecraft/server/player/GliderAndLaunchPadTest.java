package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderAndLaunchPadTest {
    @Test
    void launchPadUsesOnlyHorizontalLookForForwardImpulse() {
        Vec3 impulse = LaunchPadImpulse.impulse(new Vec3(3.0D, -4.0D, 4.0D), 1.2D, 0.5D);

        assertEquals(0.3D, impulse.x(), 1.0E-9D);
        assertEquals(1.2D, impulse.y(), 1.0E-9D);
        assertEquals(0.4D, impulse.z(), 1.0E-9D);
    }

    @Test
    void verticalLaunchPadLookStillLaunchesUpward() {
        Vec3 impulse = LaunchPadImpulse.impulse(new Vec3(0.0D, 1.0D, 0.0D), 1.25D, 0.55D);

        assertEquals(0.0D, impulse.x(), 1.0E-9D);
        assertEquals(1.25D, impulse.y(), 1.0E-9D);
        assertEquals(0.0D, impulse.z(), 1.0E-9D);
    }

    @Test
    void gliderRedeployWindowExtendsAndExpires() {
        GliderState state = new GliderState();

        state.enableRedeploy(20L, 40L);
        state.enableRedeploy(30L, 10L);

        assertTrue(state.canRedeploy(60L));
        assertFalse(state.canRedeploy(61L));
    }

    @Test
    void gliderFallSpeedCapsOnlyFastFalls() {
        assertEquals(-0.28D, GliderState.fallSpeed(-1.0D, -0.28D), 1.0E-9D);
        assertEquals(-0.1D, GliderState.fallSpeed(-0.1D, -0.28D), 1.0E-9D);
    }
}
