package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}
