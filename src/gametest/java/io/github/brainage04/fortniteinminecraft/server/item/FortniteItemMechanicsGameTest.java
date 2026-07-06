package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.BuildWeakPoints;
import io.github.brainage04.fortniteinminecraft.server.world.WorldObstructions;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceHarvest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public final class FortniteItemMechanicsGameTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BuildRules RULES = BuildRules.defaults();
    private static final FootprintProjector FOOTPRINTS = new FootprintProjector(RULES);
    private static final SnapGrid SNAP_GRID = new SnapGrid(RULES);

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

    @GameTest
    public void weaponBurstCatalogStatsDriveCartridgeCounts(GameTestHelper context) {
        assertBurstStats(context, weapon("weapon_assault_semi_auto_athena_c_ore_t02"), 2, 8.0D);
        assertBurstStats(context, weapon("weapon_pistol_burst_fire_smg_athena_c_ore_t03"), 4, 15.0D);
        assertBurstStats(context, weapon("weapon_dual_pistol_semi_auto_athena_vr_ore_t03"), 2, 22.0D);
        assertExplosiveBurstStats(context, explosiveWeapon("weapon_waffle_truck_launcher_drunken_quad"), 2, 3.8D);

        WeaponItem pumpShotgun = weapon("weapon_pump_shotgun_common");
        context.assertTrue(pumpShotgun.definition().stats().pellets() == 10,
                "Expected shotgun pellets to remain pellet spread, not burst cartridges.");
        context.assertTrue(pumpShotgun.definition().stats().cartridgePerFire() == 1,
                "Expected shotgun pellet count not to schedule a burst.");
        context.succeed();
    }

    @GameTest
    public void weaponDamageAtZeroHealthClearsBuildPiece(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        String dimension = level.dimension().identifier().toString();
        BlockPos basePos = context.absolutePos(new BlockPos(2, 3, 2));
        BuildGridPos gridPos = SNAP_GRID.snap(dimension, basePos.getX(), basePos.getY(), basePos.getZ());
        BuildSlot slot = BuildSlot.of(dimension, gridPos.x(), gridPos.y(), gridPos.z(), PieceType.WALL, Orientation.NORTH);
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        WeaponItem dualPistols = weapon("weapon_dual_pistol_semi_auto_athena_vr_ore_t03");
        int dualPistolShotDamage = WeaponItem.buildDamage(dualPistols.definition());
        BuildPieceState wall = pieceWithHealth(
                slot,
                MaterialType.WOOD,
                dualPistolShotDamage * dualPistols.definition().stats().cartridgePerFire(),
                level.getGameTime()
        );
        PieceFootprint footprint = FOOTPRINTS.project(wall);
        context.assertTrue(state.addIfAbsent(wall), "Expected build state to accept the wall.");
        WorldBuildWriteResult placeResult = materializer.place(level, wall, footprint);
        context.assertTrue(placeResult.success(), "Expected wall to materialize: " + placeResult.message());
        BlockPos hitPos = materializer.trackedBlockPositions(slot).get(0);
        Vec3 nonWeakPointHit = Vec3.atCenterOf(hitPos).add(10.0D, 10.0D, 10.0D);
        WeaponItem.configureBuildDamage(state, materializer, RULES);

        boolean firstShotDamaged = WeaponItem.damageBuild(
                level,
                context.makeMockServerPlayerInLevel(),
                hitPos,
                nonWeakPointHit,
                dualPistolShotDamage,
                " (1/18)"
        );
        boolean secondShotDamaged = WeaponItem.damageBuild(
                level,
                context.makeMockServerPlayerInLevel(),
                hitPos,
                nonWeakPointHit,
                dualPistolShotDamage,
                " (0/18)"
        );

        context.assertTrue(firstShotDamaged && secondShotDamaged, "Expected both Dual Pistols shots to damage the wall.");
        context.assertTrue(!state.contains(slot), "Expected zero-health wall to be removed from build state.");
        context.assertTrue(materializer.trackedBlockCount(slot) == 0, "Expected zero-health wall world blocks to be untracked.");
        context.assertTrue(materializer.topOwnerAt(dimension, hitPos) == null, "Expected cleared wall blocks to lose their owner.");
        context.succeed();
    }

    @GameTest
    public void pickaxeWeakPointHitAppliesMultiplierThroughDamageBlockHit(GameTestHelper context) {
        PlacedBuild wall = placeWallWithHealth(context, new BlockPos(2, 3, 6), MaterialType.STONE,
                scaledWeakPointDamage(PickaxeItem.DEFAULT_STRUCTURE_DAMAGE));
        PickaxeItem.configureHarvesting(wall.state(), wall.materializer());
        BuildWeakPoints.clear(wall.piece().slot());
        BuildWeakPoints.register(wall.state(), wall.materializer());
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(ModItems.PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        WeakPointHitTarget target = activeWeakPointTarget(context.getLevel(), wall.materializer(), wall.piece().slot(), 0);

        InteractionResult result = DeployableGameTestHooks.damageWithPickaxe(
                context.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                pickaxe,
                context.getLevel().getGameTime(),
                new BlockHitResult(target.hitLocation(), Direction.NORTH, target.blockPos(), false)
        );

        context.assertTrue(result.consumesAction(), "Expected pickaxe weak-point hit to consume the swing.");
        context.assertTrue(!wall.state().contains(wall.piece().slot()),
                "Expected one boosted pickaxe weak-point hit to destroy a stone wall with 4.5x base pickaxe health.");
        context.succeed();
    }

    @GameTest
    public void weaponWeakPointHitAppliesMultiplierThroughDamageBuild(GameTestHelper context) {
        WeaponItem assault = weapon("weapon_assault_rifle_common");
        int baseDamage = WeaponItem.buildDamage(assault.definition());
        int startingHealth = Math.min(MaterialType.WOOD.finalHealth(), scaledWeakPointDamage(baseDamage));
        PlacedBuild wall = placeWallWithHealth(context, new BlockPos(2, 3, 6), startingHealth);
        WeaponItem.configureBuildDamage(wall.state(), wall.materializer(), RULES);
        BuildWeakPoints.clear(wall.piece().slot());
        BuildWeakPoints.register(wall.state(), wall.materializer());
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        WeakPointHitTarget target = activeWeakPointTarget(context.getLevel(), wall.materializer(), wall.piece().slot(), 0);

        boolean damaged = WeaponItem.damageBuild(
                context.getLevel(),
                player,
                target.blockPos(),
                target.hitLocation(),
                baseDamage,
                " weak point test"
        );

        context.assertTrue(damaged, "Expected weapon weak-point hit to damage the build through WeaponItem.damageBuild.");
        assertHealthAfterDamage(context, wall.state(), wall.piece().slot(), Math.max(0, startingHealth - scaledWeakPointDamage(baseDamage)),
                "boosted weapon weak-point hit");
        context.succeed();
    }

    @GameTest
    public void pickaxeTerrainWeakPointHarvestDestroysLogAndGrantsWood(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos logPos = context.absolutePos(new BlockPos(3, 3, 3));
        clearAdjacentBlocks(level, logPos);
        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
        TerrainResourceHarvest.clearDamage(level, logPos);
        BuildWeakPoints.clearTerrain(level, logPos);

        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        PickaxeItem.configureHarvesting(state, materializer);
        PickaxeItem.clearAllHarvestCooldowns();

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerResourceStates.clear(player);
        ItemStack pickaxe = new ItemStack(ModItems.PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        Vec3 weakPoint = activeTerrainWeakPoint(level, logPos);

        InteractionResult result = DeployableGameTestHooks.damageWithPickaxe(
                level,
                player,
                InteractionHand.MAIN_HAND,
                pickaxe,
                level.getGameTime(),
                new BlockHitResult(weakPoint, Direction.NORTH, logPos, false)
        );

        PlayerResourceState resources = PlayerResourceStates.stateFor(player);
        context.assertTrue(result.consumesAction(), "Expected terrain weak-point pickaxe hit to consume the swing.");
        context.assertTrue(level.getBlockState(logPos).isAir(), "Expected boosted weak-point terrain hit to destroy the log in one swing.");
        context.assertTrue(resources.material(MaterialType.WOOD) == TerrainResourceHarvest.RESOURCE_REWARD,
                "Expected destroyed log to grant Fortnite wood resources.");
        context.succeed();
    }

    @GameTest
    public void harvestingToolInventoryRestoresMissingAndRemovesDuplicates(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();

        HarvestingToolInventory.enforce(player);
        assertHarvestingToolCount(context, player, 1);

        int slot = firstHarvestingToolSlot(player.getInventory());
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        HarvestingToolInventory.enforce(player);
        assertHarvestingToolCount(context, player, 1);

        player.getInventory().setItem(nextNonToolSlot(player.getInventory()), new ItemStack(ModItems.PICKAXE));
        HarvestingToolInventory.enforce(player);
        assertHarvestingToolCount(context, player, 1);
        context.succeed();
    }

    @GameTest
    public void harvestingToolInventoryCountsCarriedToolDuringMoves(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.containerMenu.setCarried(new ItemStack(ModItems.PICKAXE));

        HarvestingToolInventory.enforce(player);

        context.assertTrue(countHarvestingTools(player.getInventory()) == 0,
                "Expected no duplicate harvesting tool to be restored while moving the carried tool.");
        context.assertTrue(HarvestingToolInventory.isHarvestingTool(player.containerMenu.getCarried()),
                "Expected the carried harvesting tool to remain available.");
        context.succeed();
    }

    @GameTest
    public void launchPadFootprintPlacesNineTriggerBlocks(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos center = context.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> footprint = DeployableFootprints.centeredFloorSquare(center, DeployableFootprints.LAUNCH_PAD_SIZE_BLOCKS);
        prepareSupportedSurface(level, footprint, Direction.UP);

        context.assertTrue(DeployableFootprints.placeAll(level, footprint, DeployableFootprints.floorTriggerState()),
                "Expected a clear supported launch-pad footprint to place.");
        assertAllTrapBlocks(context, level, footprint, Direction.UP);
        context.succeed();
    }

    @GameTest
    public void bouncerFootprintPlacesCurrentFloorSizedSurface(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos center = context.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> footprint = DeployableFootprints.centeredFloorSquare(center, DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS);
        prepareSupportedSurface(level, footprint, Direction.UP);

        context.assertTrue(DeployableFootprints.placeAll(level, footprint, DeployableFootprints.floorTriggerState()),
                "Expected a clear supported bouncer footprint to place.");
        context.assertTrue(footprint.size() == RULES.footprintSizeBlocks() * RULES.footprintSizeBlocks(),
                "Expected bouncer footprint to match current build floor size.");
        assertAllTrapBlocks(context, level, footprint, Direction.UP);
        context.succeed();
    }

    @GameTest
    public void bouncerWallFootprintPlacesCurrentFloorSizedTriggerPlane(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        Direction surfaceNormal = Direction.NORTH;
        BlockPos center = context.absolutePos(new BlockPos(3, 2, 2));
        List<BlockPos> footprint = DeployableFootprints.centeredSurfaceSquare(
                center,
                surfaceNormal,
                DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS
        );
        prepareSupportedSurface(level, footprint, surfaceNormal);

        context.assertTrue(DeployableFootprints.placeAll(level, footprint, DeployableFootprints.triggerState(surfaceNormal)),
                "Expected a clear supported wall bouncer footprint to place.");
        assertAllTrapBlocks(context, level, footprint, surfaceNormal);
        context.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wallBouncerTriggerLaunchesTouchingPlayerOutward(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        Direction surfaceNormal = Direction.NORTH;
        BlockPos center = context.absolutePos(new BlockPos(3, 2, 2));
        List<BlockPos> footprint = DeployableFootprints.centeredSurfaceSquare(
                center,
                surfaceNormal,
                DeployableFootprints.BUILD_FLOOR_SIZE_BLOCKS
        );
        prepareSupportedSurface(level, footprint, surfaceNormal);
        context.assertTrue(DeployableFootprints.placeAll(level, footprint, DeployableFootprints.triggerState(surfaceNormal)),
                "Expected a wall bouncer trigger footprint to place.");
        MobilityItemInteractions.registerLaunchPadFootprint(
                level,
                footprint,
                ModItems.BOUNCER.definition().redeployTicks(),
                DeployableTriggerBlocks.TRAP_TRIGGER
        );

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 triggerCenter = Vec3.atCenterOf(center);
        player.snapTo(triggerCenter.x(), triggerCenter.y(), triggerCenter.z(), 0.0F, 0.0F);

        context.runAtTickTime(2, () -> {
            context.assertTrue(player.getDeltaMovement().z() < -1.0D,
                    "Expected north-facing wall bouncer to launch the player away from the wall.");
            context.succeed();
        });
    }

    @GameTest
    public void bouncerItemPlacesFloorAndWallTriggerFootprints(GameTestHelper context) {
        ServerLevel level = context.getLevel();

        BlockPos floorSupportCenter = context.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> floorFootprint = BouncerItem.bouncerFootprint(floorSupportCenter, Direction.UP);
        prepareSupportedSurface(level, floorFootprint, Direction.UP);
        ServerPlayer floorPlayer = context.makeMockServerPlayerInLevel();
        floorPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOUNCER));

        InteractionResult floorResult = useBouncerOn(floorPlayer, floorSupportCenter, Direction.UP);

        context.assertTrue(floorResult.consumesAction(), "Expected bouncer item to place on a supported floor.");
        assertAllTrapBlocks(context, level, floorFootprint, Direction.UP);

        BlockPos wallSupportCenter = context.absolutePos(new BlockPos(3, 4, 7));
        Direction wallNormal = Direction.NORTH;
        List<BlockPos> wallFootprint = BouncerItem.bouncerFootprint(wallSupportCenter, wallNormal);
        prepareSupportedSurface(level, wallFootprint, wallNormal);
        ServerPlayer wallPlayer = context.makeMockServerPlayerInLevel();
        wallPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOUNCER));

        InteractionResult wallResult = useBouncerOn(wallPlayer, wallSupportCenter, wallNormal);

        context.assertTrue(wallResult.consumesAction(), "Expected bouncer item to place on a supported wall.");
        assertAllTrapBlocks(context, level, wallFootprint, wallNormal);
        context.succeed();
    }

    @GameTest(maxTicks = 40)
    public void floorBouncerLaunchesJumpingPlayerUpward(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos supportCenter = context.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> footprint = placeAndRegisterBouncer(level, supportCenter, Direction.UP);
        BlockPos triggerCenter = centerBlock(footprint);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(triggerCenter.getX() + 0.5D, triggerCenter.getY() + 0.05D, triggerCenter.getZ() + 0.5D, 0.0F, 0.0F);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.35D, 0.0D);

        context.runAtTickTime(2, () -> {
            context.assertTrue(player.getDeltaMovement().y() > 2.0D,
                    "Expected jumping/falling onto a floor bouncer to launch the player upward.");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void runningJumpingIntoWallBouncerLaunchesPlayerOutward(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        Direction surfaceNormal = Direction.NORTH;
        BlockPos supportCenter = context.absolutePos(new BlockPos(3, 3, 4));
        List<BlockPos> footprint = placeAndRegisterBouncer(level, supportCenter, surfaceNormal);
        BlockPos triggerCenter = centerBlock(footprint);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setSprinting(true);
        player.snapTo(triggerCenter.getX() + 0.5D, triggerCenter.getY() + 0.1D, triggerCenter.getZ() + 0.5D, 0.0F, 0.0F);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, 0.25D, 0.45D);

        context.runAtTickTime(2, () -> {
            context.assertTrue(player.getDeltaMovement().z() < -1.0D,
                    "Expected running/jumping into a north wall bouncer to launch the player outward.");
            context.assertTrue(player.getDeltaMovement().y() > 0.4D,
                    "Expected wall bouncer to preserve an upward launch component.");
            context.succeed();
        });
    }

    @GameTest
    public void harvestingToolDamagesWallThroughBouncerTrigger(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        PlacedBouncerBuild wall = placeBouncerOnBuild(
                context,
                PieceType.WALL,
                Orientation.NORTH,
                Direction.NORTH,
                new BlockPos(3, 3, 6),
                MaterialType.WOOD.finalHealth()
        );
        PickaxeItem.configureHarvesting(wall.state(), wall.materializer());
        PickaxeItem.clearAllHarvestCooldowns();
        BlockPos triggerPos = centerBlock(wall.triggerFootprint());
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(ModItems.PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

        InteractionResult result = ModItems.PICKAXE.damageBlockHit(
                level,
                player,
                InteractionHand.MAIN_HAND,
                pickaxe,
                level.getGameTime(),
                new BlockHitResult(Vec3.atCenterOf(triggerPos), Direction.NORTH, triggerPos, false)
        );

        BuildPieceState damaged = wall.state().get(wall.piece().slot());
        context.assertTrue(result.consumesAction(), "Expected harvesting tool swing through bouncer trigger to hit the wall.");
        context.assertTrue(damaged != null && damaged.currentHealth() < wall.piece().currentHealth(),
                "Expected harvesting tool to damage the supporting wall through the bouncer trigger.");
        context.assertTrue(level.getBlockState(triggerPos).is(DeployableTriggerBlocks.TRAP_TRIGGER),
                "Expected partial support damage to leave the bouncer trigger in place.");
        context.succeed();
    }

    @GameTest
    public void weaponDamageBreaksWallBouncerSupportAndClearsTrigger(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        WeaponItem weapon = weapon("weapon_dual_pistol_semi_auto_athena_vr_ore_t03");
        int damage = WeaponItem.buildDamage(weapon.definition());
        PlacedBouncerBuild wall = placeBouncerOnBuild(
                context,
                PieceType.WALL,
                Orientation.NORTH,
                Direction.NORTH,
                new BlockPos(3, 3, 6),
                damage
        );
        WeaponItem.configureBuildDamage(wall.state(), wall.materializer(), RULES);
        BlockPos triggerPos = centerBlock(wall.triggerFootprint());

        boolean damaged = WeaponItem.damageBuild(
                level,
                context.makeMockServerPlayerInLevel(),
                triggerPos,
                Vec3.atCenterOf(triggerPos),
                damage,
                ""
        );

        context.assertTrue(damaged, "Expected weapon damage through bouncer trigger to hit the supporting wall.");
        assertBuildDestroyedAndTriggersCleared(context, level, wall);
        context.succeed();
    }

    @GameTest
    public void explosiveDamageBreaksFloorBouncerSupportAndClearsTrigger(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        int damage = Math.max(1, ModItems.CLINGER.definition().environmentDamage());
        PlacedBouncerBuild floor = placeBouncerOnBuild(
                context,
                PieceType.FLOOR,
                Orientation.NORTH,
                Direction.UP,
                new BlockPos(3, 2, 3),
                Math.min(MaterialType.WOOD.finalHealth(), damage)
        );
        WeaponItem.configureBuildDamage(floor.state(), floor.materializer(), RULES);
        BlockPos triggerPos = centerBlock(floor.triggerFootprint());

        boolean damaged = WeaponItem.damageBuild(
                level,
                context.makeMockServerPlayerInLevel(),
                triggerPos,
                Vec3.atCenterOf(triggerPos),
                damage,
                ""
        );

        context.assertTrue(damaged, "Expected explosive environment damage through bouncer trigger to hit the supporting floor.");
        assertBuildDestroyedAndTriggersCleared(context, level, floor);
        context.succeed();
    }

    @GameTest
    public void deployableFootprintRejectsBlockedCellsWithoutPartialPlacement(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos center = context.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> footprint = DeployableFootprints.centeredFloorSquare(center, DeployableFootprints.LAUNCH_PAD_SIZE_BLOCKS);
        prepareSupportedSurface(level, footprint, Direction.UP);
        level.setBlock(center, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        context.assertTrue(!DeployableFootprints.placeAll(level, footprint, DeployableFootprints.floorTriggerState()),
                "Expected blocked footprint placement to fail.");
        for (BlockPos pos : footprint) {
            context.assertTrue(!level.getBlockState(pos).is(DeployableTriggerBlocks.TRAP_TRIGGER),
                    "Expected failed footprint placement not to leave a trap trigger at " + pos);
        }
        context.succeed();
    }

    @GameTest
    public void lootContainerOpenClearsBlocksAndDropsMatchingLoot(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        BlockPos ammoBoxPos = context.absolutePos(new BlockPos(2, 2, 3));
        level.setBlock(ammoBoxPos, ModItems.AMMO_BOX.defaultBlockState(), Block.UPDATE_ALL);
        ModItems.AMMO_BOX.open(level, ammoBoxPos, player);
        List<ItemStack> ammoBoxDrops = droppedStacksNear(level, ammoBoxPos);

        context.assertTrue(level.getBlockState(ammoBoxPos).isAir(), "Expected opening an ammo box to clear the container block.");
        context.assertTrue(ammoBoxDrops.size() == 1, "Expected ammo box to drop exactly one pickup stack.");
        context.assertTrue(ammoBoxDrops.getFirst().getItem() instanceof PickupItem pickup && pickup.payload().ammoType() != null,
                "Expected ammo box pickup to contain ammo payload.");

        BlockPos chestPos = context.absolutePos(new BlockPos(6, 2, 3));
        level.setBlock(chestPos, ModItems.LOOT_CHEST.defaultBlockState(), Block.UPDATE_ALL);
        ModItems.LOOT_CHEST.open(level, chestPos, player);
        List<ItemStack> chestDrops = droppedStacksNear(level, chestPos);
        List<ItemStack> guns = chestDrops.stream().filter(ModItems::isGun).toList();
        List<ItemStack> ammoPickups = chestDrops.stream()
                .filter(stack -> stack.getItem() instanceof PickupItem pickup && pickup.payload().ammoType() != null)
                .toList();

        context.assertTrue(level.getBlockState(chestPos).isAir(), "Expected opening a chest to clear the container block.");
        context.assertTrue(guns.size() == 1, "Expected chest to drop exactly one gun.");
        context.assertTrue(ammoPickups.size() == 1, "Expected chest to drop exactly one ammo pickup for the gun.");
        AmmoType expectedAmmo = ammoTypeForGun(guns.getFirst().getItem());
        AmmoType actualAmmo = ((PickupItem) ammoPickups.getFirst().getItem()).payload().ammoType();
        context.assertTrue(actualAmmo == expectedAmmo,
                "Expected chest ammo " + actualAmmo + " to match dropped gun category ammo " + expectedAmmo + ".");
        context.succeed();
    }

    @GameTest
    public void portAFortUseThrowsProjectileConsumesStackAndStartsCooldown(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        PortAFortItem.configureBuildPlacement(state, RULES, materializer);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 playerPos = context.absoluteVec(new Vec3(3.0D, 3.0D, 3.0D));
        player.snapTo(playerPos.x(), playerPos.y(), playerPos.z(), 0.0F, 0.0F);
        ItemStack stack = new ItemStack(ModItems.PORT_A_FORT, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        InteractionResult result = ModItems.PORT_A_FORT.use(level, player, InteractionHand.MAIN_HAND);
        List<Snowball> projectiles = level.getEntitiesOfClass(
                Snowball.class,
                player.getBoundingBox().inflate(4.0D),
                projectile -> !projectile.isRemoved()
        );

        context.assertTrue(result.consumesAction(), "Expected Port-A-Fort use to consume the interaction.");
        context.assertTrue(stack.getCount() == 1, "Expected survival Port-A-Fort use to consume one item from the stack.");
        context.assertTrue(player.getCooldowns().isOnCooldown(stack), "Expected Port-A-Fort use to start its item cooldown.");
        context.assertTrue(!projectiles.isEmpty(), "Expected Port-A-Fort use to spawn a thrown projectile.");
        context.succeed();
    }

    @GameTest
    public void portAFortDeployMaterializesTrackedMetalFort(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        PortAFortItem.configureBuildPlacement(state, RULES, materializer);
        BlockPos clicked = context.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(clicked, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BuildGridPos anchor = SNAP_GRID.snap(level.dimension().identifier().toString(), clicked.getX(), clicked.getY() + 1, clicked.getZ());
        List<BuildSlot> expectedSlots = PortAFortItem.fortSlots(anchor, ModItems.PORT_A_FORT.definition().radius(), ModItems.PORT_A_FORT.definition().height(), Orientation.NORTH);

        int placed = PortAFortItem.deployFort(
                level,
                ModItems.PORT_A_FORT.definition(),
                PLAYER,
                Orientation.NORTH,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)
        );

        context.assertTrue(placed == expectedSlots.size(), "Expected deployment to place the complete Port-A-Fort template.");
        context.assertTrue(state.size() == expectedSlots.size(), "Expected every deployed Port-A-Fort piece to be tracked in build state.");
        context.assertTrue(expectedSlots.stream().allMatch(slot -> state.get(slot) != null && state.get(slot).material() == MaterialType.METAL),
                "Expected every deployed Port-A-Fort build piece to use metal.");
        assertTrackedSlotMaterialized(context, materializer, expectedSlots.stream().filter(slot -> slot.pieceType() == PieceType.FLOOR).findFirst().orElseThrow());
        assertTrackedSlotMaterialized(context, materializer, expectedSlots.stream().filter(slot -> slot.pieceType() == PieceType.WALL).findFirst().orElseThrow());
        assertTrackedSlotMaterialized(context, materializer, expectedSlots.stream().filter(slot -> slot.pieceType() == PieceType.ROOF).findFirst().orElseThrow());
        context.succeed();
    }

    @GameTest
    public void portAFortDeployRollsBackPartialFortWhenFootprintConflicts(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        PortAFortItem.configureBuildPlacement(state, RULES, materializer);
        BlockPos clicked = context.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(clicked, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BuildGridPos anchor = SNAP_GRID.snap(level.dimension().identifier().toString(), clicked.getX(), clicked.getY() + 1, clicked.getZ());
        List<BuildSlot> templateSlots = PortAFortItem.fortSlots(anchor, ModItems.PORT_A_FORT.definition().radius(), ModItems.PORT_A_FORT.definition().height(), Orientation.NORTH);
        BuildSlot conflictSlot = templateSlots.get(templateSlots.size() - 1);
        BuildPieceState existing = pieceWithHealth(conflictSlot, MaterialType.WOOD, MaterialType.WOOD.finalHealth(), level.getGameTime());
        context.assertTrue(state.addIfAbsent(existing), "Expected pre-existing conflicting build piece to be accepted.");
        WorldBuildWriteResult existingPlaced = materializer.place(level, existing, FOOTPRINTS.project(existing));
        context.assertTrue(existingPlaced.success(), "Expected pre-existing conflicting build piece to materialize: " + existingPlaced.message());
        int existingTrackedBlocks = materializer.trackedBlockCount(conflictSlot);

        int placed = PortAFortItem.deployFort(
                level,
                ModItems.PORT_A_FORT.definition(),
                PLAYER,
                Orientation.NORTH,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)
        );

        context.assertTrue(placed == 0, "Expected conflicting Port-A-Fort deployment to abort.");
        context.assertTrue(state.size() == 1, "Expected rollback to remove every partial Port-A-Fort piece.");
        context.assertTrue(state.get(conflictSlot) != null && state.get(conflictSlot).id().equals(existing.id()),
                "Expected rollback to preserve the pre-existing conflicting build piece.");
        context.assertTrue(materializer.trackedBlockCount(conflictSlot) == existingTrackedBlocks,
                "Expected rollback to preserve tracked blocks for the pre-existing piece.");
        context.succeed();
    }


    private static List<ItemStack> droppedStacksNear(ServerLevel level, BlockPos pos) {
        List<ItemStack> stacks = new ArrayList<>();
        AABB box = new AABB(pos).move(0.0D, 1.0D, 0.0D).inflate(1.5D);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
            stacks.add(entity.getItem().copy());
        }
        return List.copyOf(stacks);
    }

    private static AmmoType ammoTypeForGun(Item gun) {
        WeaponDefinition definition = weaponDefinition(gun);
        return switch (definition.category()) {
            case ASSAULT_RIFLE -> AmmoType.MEDIUM;
            case SHOTGUN -> AmmoType.SHELLS;
            case PISTOL, SMG -> AmmoType.LIGHT;
            case SNIPER -> AmmoType.HEAVY;
            case EXPLOSIVE -> AmmoType.ROCKETS;
        };
    }

    private static WeaponDefinition weaponDefinition(Item gun) {
        if (gun instanceof WeaponItem item) {
            return item.definition();
        }
        if (gun instanceof ProjectileWeaponItem item) {
            return item.definition();
        }
        if (gun instanceof ExplosiveProjectileWeaponItem item) {
            return item.definition();
        }
        throw new AssertionError("Expected gun item, got " + gun);
    }

    private static void assertTrackedSlotMaterialized(GameTestHelper context, WorldBuildMaterializer materializer, BuildSlot slot) {
        context.assertTrue(materializer.trackedBlockCount(slot) > 0, "Expected " + slot + " to have materialized tracked blocks.");
    }

    private static void clearAdjacentBlocks(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            level.setBlock(pos.relative(direction), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static Vec3 activeTerrainWeakPoint(ServerLevel level, BlockPos pos) {
        BuildWeakPoints.clearTerrain(level, pos);
        for (Vec3 candidate : terrainWeakPointCandidates(level, pos)) {
            BuildWeakPoints.Damage damage = BuildWeakPoints.damageForTerrainHit(level, pos, candidate, PickaxeItem.DEFAULT_STRUCTURE_DAMAGE);
            if (damage.weakPointHit()) {
                BuildWeakPoints.clearTerrain(level, pos);
                return candidate;
            }
        }
        throw new AssertionError("Expected an active terrain weak point for " + pos + ".");
    }

    private static List<Vec3> terrainWeakPointCandidates(ServerLevel level, BlockPos pos) {
        ArrayList<Vec3> positions = new ArrayList<>();
        Vec3 center = Vec3.atCenterOf(pos);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacent);
            if (adjacentState.isAir() || adjacentState.canBeReplaced() && adjacentState.getFluidState().isEmpty()) {
                positions.add(center.add(
                        direction.getStepX() * 0.51D,
                        direction.getStepY() * 0.51D,
                        direction.getStepZ() * 0.51D
                ));
            }
        }
        return List.copyOf(positions);
    }
    private static InteractionResult useBouncerOn(ServerPlayer player, BlockPos clickedPos, Direction face) {
        return ModItems.BOUNCER.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(clickedPos), face, clickedPos, false)
        ));
    }

    private static List<BlockPos> placeAndRegisterBouncer(ServerLevel level, BlockPos supportCenter, Direction surfaceNormal) {
        List<BlockPos> footprint = BouncerItem.bouncerFootprint(supportCenter, surfaceNormal);
        prepareSupportedSurface(level, footprint, surfaceNormal);
        if (!DeployableFootprints.placeAll(level, footprint, DeployableFootprints.triggerState(surfaceNormal))) {
            throw new AssertionError("Expected bouncer trigger footprint to place on " + surfaceNormal + " support.");
        }
        MobilityItemInteractions.registerLaunchPadFootprint(
                level,
                footprint,
                ModItems.BOUNCER.definition().redeployTicks(),
                DeployableTriggerBlocks.TRAP_TRIGGER
        );
        return footprint;
    }

    private static PlacedBouncerBuild placeBouncerOnBuild(
            GameTestHelper context,
            PieceType pieceType,
            Orientation orientation,
            Direction surfaceNormal,
            BlockPos localTarget,
            int health
    ) {
        ServerLevel level = context.getLevel();
        String dimension = level.dimension().identifier().toString();
        BlockPos target = context.absolutePos(localTarget);
        BuildGridPos grid = SNAP_GRID.snap(dimension, target.getX(), target.getY(), target.getZ());
        BuildSlot slot = BuildSlot.of(dimension, grid.x(), grid.y(), grid.z(), pieceType, orientation);
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        BuildPieceState piece = pieceWithHealth(slot, MaterialType.WOOD, health, level.getGameTime());
        PieceFootprint footprint = FOOTPRINTS.project(piece);

        context.assertTrue(state.addIfAbsent(piece), "Expected build state to accept " + slot + ".");
        WorldBuildWriteResult placed = materializer.place(level, piece, footprint);
        context.assertTrue(placed.success(), "Expected " + slot + " to materialize: " + placed.message());

        BlockPos supportCenter = centerBlock(materializer.trackedBlockPositions(slot));
        List<BlockPos> triggerFootprint = BouncerItem.bouncerFootprint(supportCenter, surfaceNormal);
        assertBouncerFootprintCanPlace(context, level, triggerFootprint, surfaceNormal);
        context.assertTrue(DeployableFootprints.placeAll(level, triggerFootprint, DeployableFootprints.triggerState(surfaceNormal)),
                "Expected bouncer trigger to place on tracked " + pieceType + " support.");
        return new PlacedBouncerBuild(state, materializer, piece, triggerFootprint);
    }

    private static void assertBouncerFootprintCanPlace(
            GameTestHelper context,
            ServerLevel level,
            List<BlockPos> triggerFootprint,
            Direction surfaceNormal
    ) {
        for (BlockPos pos : triggerFootprint) {
            BlockState triggerState = level.getBlockState(pos);
            BlockPos supportPos = pos.relative(surfaceNormal.getOpposite());
            BlockState supportState = level.getBlockState(supportPos);
            context.assertTrue(triggerState.canBeReplaced(),
                    "Expected bouncer trigger cell to be replaceable at " + pos + ", got " + triggerState);
            context.assertTrue(supportState.isFaceSturdy(level, supportPos, surfaceNormal),
                    "Expected sturdy " + surfaceNormal + " support at " + supportPos + ", got " + supportState);
        }
    }

    private static void assertBuildDestroyedAndTriggersCleared(
            GameTestHelper context,
            ServerLevel level,
            PlacedBouncerBuild build
    ) {
        BuildSlot slot = build.piece().slot();
        context.assertTrue(!build.state().contains(slot), "Expected supporting build piece to be removed from state.");
        context.assertTrue(build.materializer().trackedBlockCount(slot) == 0,
                "Expected supporting build piece to lose tracked world blocks.");
        for (BlockPos pos : build.triggerFootprint()) {
            context.assertTrue(level.getBlockState(pos).isAir(), "Expected bouncer trigger to clear after support break at " + pos);
        }
    }

    private static BlockPos centerBlock(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            throw new AssertionError("Expected at least one block position.");
        }
        int minX = positions.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int minY = positions.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int maxY = positions.stream().mapToInt(BlockPos::getY).max().orElseThrow();
        int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }


    private static void prepareSupportedSurface(ServerLevel level, List<BlockPos> footprint, Direction surfaceNormal) {
        BlockState support = Blocks.STONE.defaultBlockState();
        for (BlockPos pos : footprint) {
            level.setBlock(pos.relative(surfaceNormal.getOpposite()), support, Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void assertAllTrapBlocks(GameTestHelper context, ServerLevel level, List<BlockPos> positions, Direction facing) {
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            context.assertTrue(state.is(DeployableTriggerBlocks.TRAP_TRIGGER), "Expected trap trigger at " + pos);
            context.assertTrue(state.getValue(TrapTriggerBlock.FACING) == facing, "Expected trap trigger facing " + facing + " at " + pos);
        }
    }

    private static void assertHarvestingToolCount(GameTestHelper context, ServerPlayer player, int expected) {
        context.assertTrue(countHarvestingTools(player.getInventory()) == expected,
                "Expected " + expected + " harvesting tool(s) in inventory.");
    }

    private static int countHarvestingTools(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (HarvestingToolInventory.isHarvestingTool(inventory.getItem(slot))) {
                count++;
            }
        }
        return count;
    }

    private static int firstHarvestingToolSlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (HarvestingToolInventory.isHarvestingTool(inventory.getItem(slot))) {
                return slot;
            }
        }
        throw new AssertionError("Expected a harvesting tool slot.");
    }

    private static int nextNonToolSlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!HarvestingToolInventory.isHarvestingTool(inventory.getItem(slot))) {
                return slot;
            }
        }
        throw new AssertionError("Expected a non-harvesting-tool slot.");
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

    private static BuildPieceState pieceWithHealth(BuildSlot slot, MaterialType material, int health, long tick) {
        return new BuildPieceState(
                UUID.randomUUID(),
                PLAYER,
                slot,
                material,
                health,
                material.finalHealth(),
                tick,
                tick,
                BuildPieceState.BASE_VARIANT
        );
    }

    private static PlacedBuild placeWallWithHealth(GameTestHelper context, BlockPos localTarget, int health) {
        return placeWallWithHealth(context, localTarget, MaterialType.WOOD, health);
    }

    private static PlacedBuild placeWallWithHealth(GameTestHelper context, BlockPos localTarget, MaterialType material, int health) {
        ServerLevel level = context.getLevel();
        String dimension = level.dimension().identifier().toString();
        BlockPos target = context.absolutePos(localTarget);
        BuildGridPos grid = SNAP_GRID.snap(dimension, target.getX(), target.getY(), target.getZ());
        BuildSlot slot = BuildSlot.of(dimension, grid.x(), grid.y(), grid.z(), PieceType.WALL, Orientation.NORTH);
        BuildWorldState state = new BuildWorldState();
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        BuildPieceState piece = pieceWithHealth(slot, material, health, level.getGameTime());
        PieceFootprint footprint = FOOTPRINTS.project(piece);
        context.assertTrue(state.addIfAbsent(piece), "Expected build state to accept weak-point wall.");
        WorldBuildWriteResult result = materializer.place(level, piece, footprint);
        context.assertTrue(result.success(), "Expected weak-point wall to materialize: " + result.message());
        return new PlacedBuild(state, materializer, piece);
    }

    private static WeakPointHitTarget activeWeakPointTarget(ServerLevel level, WorldBuildMaterializer materializer, BuildSlot slot, int sequence) {
        String dimension = slot.gridPos().dimension();
        List<BlockPos> positions = new java.util.ArrayList<>();
        for (BlockPos pos : materializer.trackedBlockPositions(slot)) {
            if (!slot.equals(materializer.topOwnerAt(dimension, pos))) {
                continue;
            }
            BlockState originalState = materializer.originalBlockState(dimension, pos.getX(), pos.getY(), pos.getZ());
            if (originalState != null && WorldObstructions.isBlockingCollision(level, pos, originalState)) {
                continue;
            }
            positions.add(pos);
        }
        if (positions.isEmpty()) {
            throw new AssertionError("Expected visible weak-point build blocks for " + slot + ".");
        }
        positions.sort((left, right) -> {
            int y = Integer.compare(left.getY(), right.getY());
            if (y != 0) {
                return y;
            }
            int z = Integer.compare(left.getZ(), right.getZ());
            if (z != 0) {
                return z;
            }
            return Integer.compare(left.getX(), right.getX());
        });
        BlockPos blockPos = positions.get(Math.floorMod(slot.hashCode() + sequence * 7, positions.size()));
        return new WeakPointHitTarget(blockPos, Vec3.atCenterOf(blockPos));
    }

    private static void assertHealthAfterDamage(GameTestHelper context, BuildWorldState state, BuildSlot slot, int expectedHealth, String label) {
        BuildPieceState piece = state.get(slot);
        if (expectedHealth == 0) {
            context.assertTrue(piece == null, "Expected " + label + " to destroy the build piece.");
            return;
        }
        context.assertTrue(piece != null && piece.currentHealth() == expectedHealth,
                "Expected " + label + " to leave " + expectedHealth + " health, saw "
                        + (piece == null ? "<destroyed>" : piece.currentHealth()) + ".");
    }

    private static int scaledWeakPointDamage(int baseDamage) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(baseDamage * BuildWeakPoints.WEAK_POINT_DAMAGE_MULTIPLIER));
    }

    private static void assertBurstStats(GameTestHelper context, WeaponItem weapon, int cartridgePerFire, double burstFiringRatePerSecond) {
        context.assertTrue(weapon.definition().stats().cartridgePerFire() == cartridgePerFire,
                "Expected " + weapon.definition().path() + " to fire " + cartridgePerFire + " burst cartridges.");
        context.assertTrue(Math.abs(weapon.definition().stats().burstFiringRatePerSecond() - burstFiringRatePerSecond) < 1.0E-9D,
                "Expected " + weapon.definition().path() + " to use catalog burst firing rate " + burstFiringRatePerSecond + ".");
    }

    private static void assertExplosiveBurstStats(GameTestHelper context, ExplosiveProjectileWeaponItem weapon, int cartridgePerFire, double burstFiringRatePerSecond) {
        context.assertTrue(weapon.definition().stats().cartridgePerFire() == cartridgePerFire,
                "Expected " + weapon.definition().path() + " to fire " + cartridgePerFire + " burst projectiles.");
        context.assertTrue(Math.abs(weapon.definition().stats().burstFiringRatePerSecond() - burstFiringRatePerSecond) < 1.0E-9D,
                "Expected " + weapon.definition().path() + " to use catalog burst firing rate " + burstFiringRatePerSecond + ".");
    }

    private static WeaponItem weapon(String path) {
        return ModItems.WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
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

    private record PlacedBuild(
            BuildWorldState state,
            WorldBuildMaterializer materializer,
            BuildPieceState piece
    ) {
    }

    private record WeakPointHitTarget(BlockPos blockPos, Vec3 hitLocation) {
    }

    private record PlacedBouncerBuild(
            BuildWorldState state,
            WorldBuildMaterializer materializer,
            BuildPieceState piece,
            List<BlockPos> triggerFootprint
    ) {
    }
}
