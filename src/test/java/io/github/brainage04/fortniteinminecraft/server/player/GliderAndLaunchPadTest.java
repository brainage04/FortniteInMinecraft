package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderAndLaunchPadTest {
    private static final int GLIDER_SUSTAIN_TICKS = 40;
    private static final double VANILLA_SLOW_FALLING_GRAVITY = 0.01D;
    private static final double VANILLA_AIR_DRAG = 0.98D;

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
    void defaultLaunchPadImpulseUsesHigherVerticalVelocity() {
        Vec3 impulse = LaunchPadImpulse.defaultImpulse(new Vec3(0.0D, 0.0D, 1.0D));

        assertEquals(0.0D, impulse.x(), 1.0E-9D);
        assertEquals(2.4D, impulse.y(), 1.0E-9D);
        assertEquals(0.55D, impulse.z(), 1.0E-9D);
    }

    @Test
    void impulseLaunchRestoresHorizontalVelocityAfterAirDrag() {
        Vec3 corrected = MobilityItemInteractions.withoutLaunchAirDrag(
                new Vec3(1.82D, 0.35D, -0.91D),
                new Vec3(2.0D, 0.0D, -1.0D)
        );

        assertEquals(2.0D, corrected.x(), 1.0E-9D);
        assertEquals(0.35D, corrected.y(), 1.0E-9D);
        assertEquals(-1.0D, corrected.z(), 1.0E-9D);
    }

    @Test
    void impulseLaunchKeepsHigherHorizontalVelocityGainedInAir() {
        Vec3 velocity = new Vec3(2.5D, 0.2D, 0.0D);
        Vec3 corrected = MobilityItemInteractions.withoutLaunchAirDrag(velocity, new Vec3(2.0D, 0.0D, 0.0D));

        assertEquals(velocity, corrected);
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
        assertEquals(-0.125D, GliderState.fallSpeed(-1.0D, -0.125D), 1.0E-9D);
        assertEquals(-0.1D, GliderState.fallSpeed(-0.1D, -0.125D), 1.0E-9D);
    }

    @Test
    void gliderVelocityKeepsLowAirFrictionWhileCappingFastFalls() {
        Vec3 velocity = MobilityItemInteractions.glideVelocity(new Vec3(0.5D, -1.0D, 0.0D), Vec3.ZERO);

        assertTrue(velocity.x() > 0.49D);
        assertEquals(-0.125D, velocity.y(), 1.0E-9D);
    }

    @Test
    void slideVelocityAcceleratesMoreWhenDroppingDownhill() {
        Vec3 velocity = new Vec3(0.2D, 0.0D, 0.0D);
        Vec3 flat = MobilityItemInteractions.slideVelocity(velocity, new Vec3(1.0D, 0.0D, 0.0D), 0.0D);
        Vec3 downhill = MobilityItemInteractions.slideVelocity(velocity, new Vec3(1.0D, 0.0D, 0.0D), -0.5D);

        assertTrue(horizontalDistance(downhill) > horizontalDistance(flat));
    }

    @Test
    void slideVelocityUsesLookDirectionWhenStartingFromTinyVelocity() {
        Vec3 velocity = MobilityItemInteractions.slideVelocity(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D), 0.0D);

        assertTrue(velocity.z() > 0.0D);
        assertEquals(0.0D, velocity.x(), 1.0E-9D);
    }

    @Test
    void gliderRequiresFortniteRedeployHeightOrTemporaryRedeployWindow() {
        GliderState state = new GliderState();

        assertFalse(state.canDeploy(100L, GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS - 0.01D));
        assertTrue(state.canDeploy(100L, GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS));

        state.enableRedeploy(100L, 20L);
        assertTrue(state.canDeploy(120L, 1.0D));
        assertFalse(state.canDeploy(121L, 1.0D));
    }

    @Test
    void gliderToggleDeploysAndCancelsUntilLanding() {
        GliderState state = new GliderState();

        assertFalse(state.isDeployed());
        assertTrue(state.toggle(10L, GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS));
        assertTrue(state.isDeployed());
        assertTrue(state.toggle(11L, 1.0D));
        assertFalse(state.isDeployed());
        assertTrue(state.toggle(12L, GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS));
        state.land();
        assertFalse(state.isDeployed());
    }

    @Test
    void gliderDeploymentOutlivesRedeployWindowUntilToggleOrLanding() {
        GliderState state = new GliderState();

        state.enableRedeploy(10L, 5L);
        assertTrue(state.toggle(10L, 1.0D));
        assertFalse(state.canRedeploy(16L));
        assertTrue(state.isDeployed());
        assertTrue(state.toggle(16L, 1.0D));
        assertFalse(state.isDeployed());

        assertTrue(state.toggle(20L, GliderState.MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS));
        state.land();
        assertFalse(state.isDeployed());
    }

    @Test
    void deployedGliderSustainsTravelAndFallsNoFasterThanVanillaSlowFalling() {
        GliderState state = new GliderState();
        state.enableRedeploy(0L, 1L);
        assertTrue(state.toggle(0L, 1.0D));

        Vec3 look = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 velocity = new Vec3(0.0D, -1.0D, 0.0D);
        double vanillaYVelocity = velocity.y();
        double gliderFall = 0.0D;
        double vanillaFall = 0.0D;
        double gliderTravel = 0.0D;

        for (int tick = 0; tick < GLIDER_SUSTAIN_TICKS; tick++) {
            assertTrue(state.isDeployed());
            velocity = MobilityItemInteractions.glideVelocity(velocity, look);
            gliderFall += -velocity.y();
            gliderTravel += horizontalDistance(velocity);

            velocity = new Vec3(velocity.x(), vanillaSlowFallingNextYVelocity(velocity.y()), velocity.z());
            vanillaYVelocity = vanillaSlowFallingNextYVelocity(vanillaYVelocity);
            vanillaFall += -vanillaYVelocity;
        }

        assertTrue(state.isDeployed());
        assertTrue(gliderTravel > 10.0D);
        assertTrue(gliderFall <= vanillaFall);
    }

    @Test
    void riftPortalExpiresAfterTwoHundredTicksAndAllowsEachPlayerIncludingOpenerOnce() {
        long openedTick = 100L;
        UUID opener = UUID.randomUUID();
        UUID visitor = UUID.randomUUID();
        MobilityItemInteractions.ActiveRiftPortal portal = new MobilityItemInteractions.ActiveRiftPortal(
                testDimension(),
                Vec3.ZERO,
                MobilityItemInteractions.riftPortalExpireTick(openedTick, 200L),
                200L,
                64.0D,
                0.85D,
                1.2D
        );

        assertEquals(openedTick + 200L, portal.expireTick());
        assertTrue(portal.isActive(openedTick + 199L));
        assertFalse(portal.isActive(openedTick + 200L));
        assertFalse(portal.hasUsed(opener));
        assertTrue(portal.tryUse(opener));
        assertFalse(portal.tryUse(opener));
        assertTrue(portal.tryUse(visitor));
        assertFalse(portal.tryUse(visitor));
    }

    private static ResourceKey<Level> testDimension() {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));
    }

    private static double vanillaSlowFallingNextYVelocity(double yVelocity) {
        return (yVelocity - VANILLA_SLOW_FALLING_GRAVITY) * VANILLA_AIR_DRAG;
    }

    private static double horizontalDistance(Vec3 velocity) {
        return Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());
    }
}
