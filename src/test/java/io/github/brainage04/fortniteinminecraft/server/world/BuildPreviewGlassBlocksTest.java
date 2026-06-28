package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.FootprintProjector;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
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
    void wallAndFloorUseChunkLocalStretchedPreviewDisplays() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());

        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> wall = volumes(materializer, PieceType.WALL, Orientation.NORTH);
        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> floor = volumes(materializer, PieceType.FLOOR, Orientation.NORTH);

        assertEquals(25, totalBlocks(wall));
        assertEquals(25, totalBlocks(floor));
        assertTrue(wall.size() <= 4);
        assertTrue(floor.size() <= 4);
        for (BuildPreviewGlassBlocks.PreviewVolume volume : wall) {
            assertEquals(1, volume.sizeZ());
        }
        for (BuildPreviewGlassBlocks.PreviewVolume volume : floor) {
            assertEquals(1, volume.sizeY());
        }
    }

    @Test
    void stairPreviewUsesChunkLocalDisplaysPerDiagonalRow() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        PieceFootprint footprint = footprint(PieceType.STAIR, Orientation.NORTH);

        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes = BuildPreviewGlassBlocks.previewVolumes(footprint, materializer);

        assertEquals(25, totalBlocks(volumes));
        assertTrue(volumes.size() >= 5);
        for (BuildPreviewGlassBlocks.PreviewVolume volume : volumes) {
            assertEquals(1, volume.sizeY());
            assertEquals(1, volume.sizeZ());
        }
    }

    @Test
    void previewDisplaysAreOutsetSlightlySoTheyReadBiggerThanBlocks() {
        assertTrue(BuildPreviewGlassBlocks.PREVIEW_OUTSET_BLOCKS > 0.0F);
        assertTrue(BuildPreviewGlassBlocks.PREVIEW_OUTSET_BLOCKS < 0.5F);
    }

    @Test
    void largePreviewVolumesSplitAtChunkBorders() {
        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes = BuildPreviewGlassBlocks.splitAtChunkBorders(
                new BuildPreviewGlassBlocks.PreviewVolume(new BlockPos(14, 64, 14), 5, 1, 5)
        );

        assertEquals(4, volumes.size());
        assertTrue(volumes.contains(new BuildPreviewGlassBlocks.PreviewVolume(new BlockPos(14, 64, 14), 2, 1, 2)));
        assertTrue(volumes.contains(new BuildPreviewGlassBlocks.PreviewVolume(new BlockPos(14, 64, 16), 2, 1, 3)));
        assertTrue(volumes.contains(new BuildPreviewGlassBlocks.PreviewVolume(new BlockPos(16, 64, 14), 3, 1, 2)));
        assertTrue(volumes.contains(new BuildPreviewGlassBlocks.PreviewVolume(new BlockPos(16, 64, 16), 3, 1, 3)));
    }

    private static LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes(
            WorldBuildMaterializer materializer,
            PieceType pieceType,
            Orientation orientation
    ) {
        return BuildPreviewGlassBlocks.previewVolumes(
                footprint(pieceType, orientation),
                materializer
        );
    }

    private static int totalBlocks(Collection<BuildPreviewGlassBlocks.PreviewVolume> volumes) {
        return volumes.stream()
                .mapToInt(volume -> volume.sizeX() * volume.sizeY() * volume.sizeZ())
                .sum();
    }

    private static PieceFootprint footprint(PieceType pieceType, Orientation orientation) {
        return new FootprintProjector(BuildRules.defaults()).project(
                BuildSlot.of("overworld", 0, 0, 0, pieceType, orientation)
        );
    }
}
