package io.github.brainage04.fortniteinminecraft.server.item;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

public final class FortniteItemMechanicsGameTest {
    @GameTest
    public void impulseAndShockwaveGrenadesPushSeveralRelativePositions(GameTestHelper context) {
        ThrowableImpulseItem shockwave = throwable("shockwave_grenade");
        ThrowableImpulseItem impulse = throwable("impulse_grenade");

        assertRadialLaunches(context, shockwave.definition());
        assertRadialLaunches(context, impulse.definition());
        context.assertTrue(shockwave.definition().resetsFallDistance(), "Expected shockwave grenade to cancel fall damage.");
        context.assertTrue(!impulse.definition().resetsFallDistance(), "Expected impulse grenade to preserve fall damage risk.");
        context.succeed();
    }

    @GameTest
    public void explosiveGrenadesAndClingersCarryDamageAndDetonationRules(GameTestHelper context) {
        ExplosiveProjectileWeaponItem grenadeLauncher = explosiveWeapon("weapon_grenade_launcher_legendary");
        ExplosiveProjectileWeaponItem proximityLauncher = explosiveWeapon("weapon_proximity_grenade_launcher_legendary");
        ExplosiveProjectileWeaponItem shockwaveLauncher = explosiveWeapon("weapon_shockwave_launcher_epic");
        ExplosiveThrowableItem.Definition clinger = ModItems.CLINGER.definition();

        context.assertTrue(grenadeLauncher.explosiveDefinition().environmentDamage() > 0,
                "Expected grenade launcher to damage builds/environment.");
        context.assertTrue(!grenadeLauncher.explosiveDefinition().proximityTriggered(),
                "Expected regular grenade launcher not to use proximity detonation.");
        context.assertTrue(proximityLauncher.explosiveDefinition().proximityTriggered(),
                "Expected proximity launcher to arm proximity detonation.");
        context.assertTrue(shockwaveLauncher.explosiveDefinition().hasImpulseOnly(),
                "Expected shockwave launcher to be impulse-only instead of damage-first.");
        context.assertTrue(clinger.damage() > 0.0D && clinger.environmentDamage() > 0,
                "Expected clinger to damage players and builds.");
        context.assertTrue(clinger.stickDelayTicks() > 0 && clinger.explosionRadiusBlocks() > 0.0D,
                "Expected clinger to stick before exploding in a non-zero radius.");
        context.succeed();
    }

    private static void assertRadialLaunches(GameTestHelper context, ThrowableImpulseItem.Definition definition) {
        Vec3 origin = Vec3.ZERO;
        Vec3 east = ImpulsePhysics.radialImpulse(origin, new Vec3(definition.radius() * 0.5D, 0.0D, 0.0D),
                definition.radius(), definition.horizontalStrength(), definition.verticalStrength());
        Vec3 north = ImpulsePhysics.radialImpulse(origin, new Vec3(0.0D, 0.0D, -definition.radius() * 0.5D),
                definition.radius(), definition.horizontalStrength(), definition.verticalStrength());
        Vec3 overhead = ImpulsePhysics.radialImpulse(origin, new Vec3(0.0D, definition.radius() * 0.5D, 0.0D),
                definition.radius(), definition.horizontalStrength(), definition.verticalStrength());
        Vec3 outside = ImpulsePhysics.radialImpulse(origin, new Vec3(definition.radius() + 1.0D, 0.0D, 0.0D),
                definition.radius(), definition.horizontalStrength(), definition.verticalStrength());

        context.assertTrue(east.x() > 0.0D && east.y() > 0.0D && Math.abs(east.z()) < 1.0E-9D,
                "Expected " + definition.displayName() + " to push east targets up and outward.");
        context.assertTrue(north.z() < 0.0D && north.y() > 0.0D && Math.abs(north.x()) < 1.0E-9D,
                "Expected " + definition.displayName() + " to push north targets up and outward.");
        context.assertTrue(Math.abs(overhead.x()) < 1.0E-9D && Math.abs(overhead.z()) < 1.0E-9D && overhead.y() > 0.0D,
                "Expected " + definition.displayName() + " to launch overhead targets vertically.");
        context.assertTrue(outside.equals(Vec3.ZERO),
                "Expected " + definition.displayName() + " to ignore targets outside its radius.");
    }

    private static ThrowableImpulseItem throwable(String path) {
        return ModItems.THROWABLES.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ExplosiveProjectileWeaponItem explosiveWeapon(String path) {
        return ModItems.EXPLOSIVE_WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }
}
