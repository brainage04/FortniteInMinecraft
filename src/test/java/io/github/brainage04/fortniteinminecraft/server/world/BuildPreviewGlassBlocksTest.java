package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPreviewGlassBlocksTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void usesLightBlueGlassForValidAndRedGlassForRejectedPreviews() {
        assertSame(Blocks.STAINED_GLASS.lightBlue(), BuildPreviewGlassBlocks.previewState(true).getBlock());
        assertSame(Blocks.STAINED_GLASS.red(), BuildPreviewGlassBlocks.previewState(false).getBlock());
    }

    @Test
    void wallAndFloorUseOneStretchedPreviewDisplay() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());

        BuildPreviewGlassBlocks.PreviewVolume wall = onlyVolume(materializer, PieceType.WALL, Orientation.NORTH);
        BuildPreviewGlassBlocks.PreviewVolume floor = onlyVolume(materializer, PieceType.FLOOR, Orientation.NORTH);

        assertEquals(5, wall.sizeX());
        assertEquals(5, wall.sizeY());
        assertEquals(1, wall.sizeZ());
        assertEquals(5, floor.sizeX());
        assertEquals(1, floor.sizeY());
        assertEquals(5, floor.sizeZ());
    }

    @Test
    void stairPreviewUsesOneDisplayPerDiagonalRow() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        PieceFootprint footprint = footprint(PieceType.STAIR, Orientation.NORTH);

        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes = BuildPreviewGlassBlocks.previewVolumes(footprint, materializer);

        assertEquals(5, volumes.size());
        for (BuildPreviewGlassBlocks.PreviewVolume volume : volumes) {
            assertEquals(5, volume.sizeX());
            assertEquals(1, volume.sizeY());
            assertEquals(1, volume.sizeZ());
        }
    }

    @Test
    void previewDisplaysAreOutsetSlightlySoTheyReadBiggerThanBlocks() {
        assertTrue(BuildPreviewGlassBlocks.PREVIEW_OUTSET_BLOCKS > 0.0F);
        assertTrue(BuildPreviewGlassBlocks.PREVIEW_OUTSET_BLOCKS < 0.5F);
    }

    private static BuildPreviewGlassBlocks.PreviewVolume onlyVolume(
            WorldBuildMaterializer materializer,
            PieceType pieceType,
            Orientation orientation
    ) {
        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes = BuildPreviewGlassBlocks.previewVolumes(
                footprint(pieceType, orientation),
                materializer
        );
        assertEquals(1, volumes.size());
        return volumes.iterator().next();
    }

    private static PieceFootprint footprint(PieceType pieceType, Orientation orientation) {
        return new FootprintProjector(BuildRules.defaults()).project(
                BuildSlot.of("overworld", 0, 0, 0, pieceType, orientation)
        );
    }
}
