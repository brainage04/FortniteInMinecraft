package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPieceHealthDisplaysTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void healthTextShowsMaterialBarCurrentAndFinalHealth() {
        BuildPieceState piece = BuildPieceState.placed(
                BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH),
                MaterialType.WOOD,
                UUID.randomUUID(),
                0
        );

        assertEquals("wood ██████░░░░ 90/150", BuildPieceHealthDisplays.healthText(piece));
    }

    @Test
    void rawHealthDisplayCenterUsesPieceBoundingBoxCenter() {
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        List<BlockPos> wallExtents = List.of(new BlockPos(0, 0, 0), new BlockPos(3, 2, 0));

        assertEquals(new Vec3(2.0D, 1.5D, 0.5D), BuildPieceHealthDisplays.rawPieceCenter(wallExtents, slot));
    }

    @Test
    void visibleHealthDisplayMovesTwoBlocksTowardViewer() {
        Vec3 center = new Vec3(2.5D, 2.5D, 0.5D);
        Vec3 viewer = new Vec3(2.5D, 2.5D, -10.0D);

        assertEquals(new Vec3(2.5D, 2.5D, -1.5D), BuildPieceHealthDisplays.visibleDisplayOrigin(center, viewer));
    }

    @Test
    void weakPointHitUsesFortniteDamageMultiplierAndMovesPoint() {
        BuildWeakPoints.clearAll();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        List<BlockPos> positions = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
        Vec3 first = BuildWeakPoints.weakPointPosition(slot, positions, 0);
        Vec3 second = BuildWeakPoints.weakPointPosition(slot, positions, 1);

        BuildWeakPoints.Damage hit = BuildWeakPoints.damageForHit(slot, positions, first, 50);
        BuildWeakPoints.Damage missAtOldPoint = BuildWeakPoints.damageForHit(slot, positions, first, 50);

        assertFalse(first.equals(second));
        assertTrue(hit.weakPointHit());
        assertEquals(225, hit.amount());
        assertFalse(missAtOldPoint.weakPointHit());
        assertEquals(50, missAtOldPoint.amount());
    }

    @Test
    void terrainWeakPointUsesAirFacingSurfacePositions() {
        BuildWeakPoints.clearAll();
        BlockPos terrain = new BlockPos(4, 64, 4);
        List<Vec3> surfaces = List.of(
                BuildWeakPoints.terrainSurfacePosition(terrain, Direction.NORTH),
                BuildWeakPoints.terrainSurfacePosition(terrain, Direction.SOUTH)
        );
        Vec3 first = BuildWeakPoints.terrainWeakPointPosition("overworld", terrain, surfaces, 0);
        Vec3 second = BuildWeakPoints.terrainWeakPointPosition("overworld", terrain, surfaces, 1);

        BuildWeakPoints.Damage hit = BuildWeakPoints.damageForTerrainHit("overworld", terrain, surfaces, first, 50);
        BuildWeakPoints.Damage missAtOldPoint = BuildWeakPoints.damageForTerrainHit("overworld", terrain, surfaces, first, 50);

        assertTrue(surfaces.contains(first));
        assertFalse(Vec3.atCenterOf(terrain).equals(first));
        assertFalse(first.equals(second));
        assertTrue(hit.weakPointHit());
        assertEquals(225, hit.amount());
        assertFalse(missAtOldPoint.weakPointHit());
        assertEquals(50, missAtOldPoint.amount());
    }
}
