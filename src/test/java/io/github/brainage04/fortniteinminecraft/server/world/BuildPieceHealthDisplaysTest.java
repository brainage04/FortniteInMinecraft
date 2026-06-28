package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List<BlockPos> wallExtents = List.of(new BlockPos(0, 0, 0), new BlockPos(4, 4, 0));

        assertEquals(new Vec3(2.5D, 2.5D, 0.5D), BuildPieceHealthDisplays.rawPieceCenter(wallExtents, slot));
    }

    @Test
    void visibleHealthDisplayMovesOneBlockTowardViewer() {
        Vec3 center = new Vec3(2.5D, 2.5D, 0.5D);
        Vec3 viewer = new Vec3(2.5D, 2.5D, -10.0D);

        assertEquals(new Vec3(2.5D, 2.5D, -0.5D), BuildPieceHealthDisplays.visibleDisplayOrigin(center, viewer));
    }
}
