package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.core.edit.BuildEditGrids;
import io.github.brainage04.fortniteinminecraft.core.edit.EditGridCell;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.BuildSupportCascade;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.item.ConsumableItem;
import io.github.brainage04.fortniteinminecraft.server.item.ModItems;
import io.github.brainage04.fortniteinminecraft.server.item.ProjectileWeaponItem;
import io.github.brainage04.fortniteinminecraft.server.item.WeaponItem;
import io.github.brainage04.fortniteinminecraft.server.player.GliderState;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class FortniteInMinecraftGameTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BuildRules RULES = BuildRules.defaults();
    private static final FootprintProjector FOOTPRINTS = new FootprintProjector(RULES);
    private static final SnapGrid SNAP_GRID = new SnapGrid(RULES);

    @GameTest
    public void serverCommandRootIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("fim") == null) {
            throw new AssertionError("Expected the /fim command root to be registered on the dedicated server.");
        }

        context.succeed();
    }

    @GameTest(maxTicks = 80)
    public void gliderStaysDeployedAndTravelsAfterRedeploy(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        Vec3 start = context.absoluteVec(new Vec3(2.0D, 20.0D, 2.0D));
        player.snapTo(start.x(), start.y(), start.z(), 0.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        player.setDeltaMovement(0.0D, -0.6D, 0.0D);

        MobilityItemInteractions.enableRedeploy(player, 100L);
        context.assertTrue(MobilityItemInteractions.toggleGlider(player), "Expected redeploy window to trigger glider.");
        context.assertTrue(MobilityItemInteractions.isGliding(player), "Expected glider to be active immediately after toggle.");

        context.runAtTickTime(40, () -> {
            context.assertTrue(MobilityItemInteractions.isGliding(player), "Expected glider to remain active after redeploy tick flow.");
            context.assertTrue(player.getDeltaMovement().y() >= GliderState.DEFAULT_MAX_FALL_SPEED - 1.0E-6D,
                    "Expected glider to cap fall speed instead of entering freefall.");
            double horizontalSpeedSqr = player.getDeltaMovement().x() * player.getDeltaMovement().x()
                    + player.getDeltaMovement().z() * player.getDeltaMovement().z();
            context.assertTrue(horizontalSpeedSqr > 0.4D * 0.4D,
                    "Expected deployed glider to keep generating traversal velocity.");
            context.succeed();
        });
    }

    @GameTest
    public void hitscanWeaponDamagesLivingTarget(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        Vec3 shooterPos = context.absoluteVec(new Vec3(2.0D, 2.0D, 2.0D));
        player.snapTo(shooterPos.x(), shooterPos.y(), shooterPos.z(), 0.0F, 0.0F);

        WeaponItem assault = weapon("weapon_assault_rifle_common");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(assault));
        Mob target = context.spawnWithNoFreeWill(entityType("zombie"), new Vec3(2.0D, 2.0D, 7.0D));
        target.setNoGravity(true);
        target.setHealth(20.0F);
        float before = target.getHealth();

        InteractionResult result = assault.fireFromHeldItem(level, player, InteractionHand.MAIN_HAND);

        context.assertTrue(result.consumesAction(), "Expected hitscan fire input to be consumed.");
        context.assertTrue(target.getHealth() < before, "Expected hitscan weapon to damage the target.");
        context.succeed();
    }

    @GameTest(maxTicks = 80)
    public void projectileWeaponDamagesLivingTarget(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        Vec3 shooterPos = context.absoluteVec(new Vec3(2.0D, 2.0D, 2.0D));
        player.snapTo(shooterPos.x(), shooterPos.y(), shooterPos.z(), 0.0F, 0.0F);

        ProjectileWeaponItem bolt = projectileWeapon("weapon_bolt_action_sniper_legendary");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bolt));
        Mob target = context.spawnWithNoFreeWill(entityType("zombie"), new Vec3(2.0D, 2.0D, 8.0D));
        target.setNoGravity(true);
        target.setHealth(20.0F);
        float before = target.getHealth();

        InteractionResult result = bolt.fireFromHeldItem(level, player, InteractionHand.MAIN_HAND, true);
        context.assertTrue(result.consumesAction(), "Expected projectile fire input to be consumed.");
        context.runAtTickTime(20, () -> {
            context.assertTrue(target.getHealth() < before, "Expected projectile weapon to damage the target.");
            context.succeed();
        });
    }

    @GameTest
    public void consumablesRestoreHealthAndShieldInWorld(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(10.0F);

        ConsumableItem bandage = consumable("consumable_bandage");
        bandage.finishUsingItem(new ItemStack(bandage), level, player);
        context.assertTrue(player.getHealth() > 10.0F, "Expected health consumable to restore health.");
        context.assertTrue(player.getHealth() <= 15.0F, "Expected bandage to respect the Fortnite 75-health cap.");

        ConsumableItem smallShield = consumable("consumable_small_shield");
        smallShield.finishUsingItem(new ItemStack(smallShield), level, player);
        context.assertTrue(player.getAbsorptionAmount() >= 5.0F, "Expected shield consumable to add shield absorption.");
        context.succeed();
    }

    @GameTest
    public void launchPadAndBouncerEnableRedeploy(GameTestHelper context) {
        ServerPlayer launchPlayer = context.makeMockServerPlayerInLevel();
        launchPlayer.setGameMode(GameType.SURVIVAL);
        Vec3 launchPos = context.absoluteVec(new Vec3(2.0D, 2.0D, 2.0D));
        launchPlayer.snapTo(launchPos.x(), launchPos.y(), launchPos.z(), 0.0F, 0.0F);

        context.assertTrue(MobilityItemInteractions.activateLaunchPad(launchPlayer, 100L, true),
                "Expected launch pad activation to apply launch impulse.");
        context.assertTrue(launchPlayer.getDeltaMovement().y() > 2.0D, "Expected launch pad to throw the player upward.");
        context.assertTrue(MobilityItemInteractions.toggleGlider(launchPlayer), "Expected launch pad redeploy window to allow glider.");

        ServerPlayer bouncerPlayer = context.makeMockServerPlayerInLevel();
        bouncerPlayer.setGameMode(GameType.SURVIVAL);
        Vec3 bouncerPos = context.absoluteVec(new Vec3(4.0D, 2.0D, 2.0D));
        bouncerPlayer.snapTo(bouncerPos.x(), bouncerPos.y(), bouncerPos.z(), 0.0F, 0.0F);
        context.assertTrue(MobilityItemInteractions.activateLaunchPad(bouncerPlayer, ModItems.BOUNCER.definition().redeployTicks(), true),
                "Expected bouncer activation to reuse launch-pad impulse handling.");
        context.assertTrue(MobilityItemInteractions.toggleGlider(bouncerPlayer), "Expected bouncer redeploy window to allow glider.");
        context.succeed();
    }

    @GameTest
    public void buildPiecesMaterializeEveryPieceAndMaterial(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        String dimension = level.dimension().identifier().toString();
        BuildGridPos baseGrid = SNAP_GRID.snap(dimension, context.absolutePos(new BlockPos(1, 3, 1)).getX(),
                context.absolutePos(new BlockPos(1, 3, 1)).getY(), context.absolutePos(new BlockPos(1, 3, 1)).getZ());
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(RULES);
        int index = 0;

        for (PieceType pieceType : PieceType.values()) {
            for (MaterialType material : MaterialType.values()) {
                BuildSlot slot = BuildSlot.of(
                        dimension,
                        baseGrid.x() + index * 3,
                        baseGrid.y(),
                        baseGrid.z(),
                        pieceType,
                        pieceType.ignoresOrientation() ? Orientation.NORTH : Orientation.EAST
                );
                BuildPieceState piece = fullHealthPiece(slot, material, level.getGameTime());
                PieceFootprint footprint = FOOTPRINTS.project(piece);
                WorldBuildWriteResult result = materializer.place(level, piece, footprint);

                context.assertTrue(result.success(), "Expected " + material + " " + pieceType + " to materialize: " + result.message());
                context.assertTrue(materializer.trackedBlockCount(slot) == footprint.localBlocks().size(),
                        "Expected materializer to track every block in " + slot + ".");
                index++;
            }
        }

        context.succeed();
    }

    @GameTest
    public void supportCascadeSeparatesGroundedAndUnsupportedPieces(GameTestHelper context) {
        BuildWorldState state = new BuildWorldState();
        BuildSupportCascade cascade = new BuildSupportCascade(RULES);
        String dimension = "overworld";
        BuildSlot support = floorSlot(dimension, 0, 0, 0);
        BuildSlot connected = floorSlot(dimension, 1, 0, 0);
        BuildSlot floating = floorSlot(dimension, 4, 0, 0);
        state.addIfAbsent(fullHealthPiece(support, MaterialType.WOOD, 1L));
        state.addIfAbsent(fullHealthPiece(connected, MaterialType.WOOD, 1L));
        state.addIfAbsent(fullHealthPiece(floating, MaterialType.WOOD, 1L));

        WorldObstruction groundOnlyUnderSupport = (candidateDimension, x, y, z) ->
                dimension.equals(candidateDimension) && y == -2 && x >= -1 && x <= 2 && z >= -1 && z <= 3;

        List<BuildSlot> initiallyUnsupported = cascade.unsupportedPieces(state, dimension, groundOnlyUnderSupport)
                .stream()
                .map(BuildPieceState::slot)
                .toList();
        context.assertTrue(initiallyUnsupported.equals(List.of(floating)),
                "Expected only the disconnected piece to start unsupported.");

        state.remove(support);
        List<BuildSlot> collapsePlan = cascade.collapsePlan(state, dimension, groundOnlyUnderSupport, support)
                .stream()
                .map(step -> step.piece().slot())
                .toList();
        context.assertTrue(collapsePlan.contains(connected), "Expected dependent piece to collapse after support removal.");
        context.assertTrue(collapsePlan.contains(floating), "Expected already-floating piece to remain collapse-eligible.");
        context.succeed();
    }

    @GameTest
    public void editMasksKeepPartialPiecesAndRejectEmptyPieces(GameTestHelper context) {
        BuildSlot wall = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        BuildPieceState baseWall = fullHealthPiece(wall, MaterialType.WOOD, 1L);
        int centerWindow = BuildEditGrids.bit(PieceType.WALL, new EditGridCell(1, 1));
        BuildPieceState editedWall = baseWall.withEditVariant(BuildEditGrids.variantFor(PieceType.WALL, centerWindow));

        context.assertTrue(FOOTPRINTS.project(editedWall).localBlocks().size() < FOOTPRINTS.project(baseWall).localBlocks().size(),
                "Expected wall edit to remove some blocks.");
        for (PieceType pieceType : PieceType.values()) {
            int fullMask = BuildEditGrids.validMask(pieceType);
            int allButOne = fullMask & ~(1 << (BuildEditGrids.cellCount(pieceType) - 1));
            context.assertTrue(!BuildEditGrids.isConfirmableMask(pieceType, fullMask),
                    "Expected fully removed " + pieceType + " edit to be rejected.");
            context.assertTrue(BuildEditGrids.isConfirmableMask(pieceType, allButOne),
                    "Expected one-cell-remaining " + pieceType + " edit to be confirmable.");
        }
        context.succeed();
    }

    private static BuildPieceState fullHealthPiece(BuildSlot slot, MaterialType material, long tick) {
        return new BuildPieceState(
                UUID.randomUUID(),
                PLAYER,
                slot,
                material,
                material.finalHealth(),
                material.finalHealth(),
                tick,
                tick,
                BuildPieceState.BASE_VARIANT
        );
    }

    private static BuildSlot floorSlot(String dimension, int x, int y, int z) {
        return BuildSlot.of(dimension, x, y, z, PieceType.FLOOR, Orientation.NORTH);
    }

    private static WeaponItem weapon(String path) {
        return ModItems.WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ProjectileWeaponItem projectileWeapon(String path) {
        return ModItems.PROJECTILE_WEAPONS.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static ConsumableItem consumable(String path) {
        return ModItems.CONSUMABLES.stream()
                .filter(item -> item.definition().path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Mob> EntityType<E> entityType(String path) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:" + path + " entity type");
        }
        return (EntityType<E>) type;
    }
}
