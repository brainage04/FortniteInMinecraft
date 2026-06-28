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
    void allPiecePreviewsUseStablePerBlockVolumes() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());

        for (PieceType pieceType : PieceType.values()) {
            LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> volumes = volumes(materializer, pieceType, Orientation.NORTH);

            assertEquals(25, volumes.size());
            assertEquals(25, totalBlocks(volumes));
            for (BuildPreviewGlassBlocks.PreviewVolume volume : volumes) {
                assertEquals(1, volume.sizeX());
                assertEquals(1, volume.sizeY());
                assertEquals(1, volume.sizeZ());
            }
        }
    }

    @Test
    void previewVolumesStayChunkLocalAndDeterministicAtChunkBorders() {
        WorldBuildMaterializer materializer = WorldBuildMaterializer.defaults(BuildRules.defaults());
        PieceFootprint crossingChunkBorder = new FootprintProjector(BuildRules.defaults()).project(
                BuildSlot.of("overworld", 4, 0, 4, PieceType.FLOOR, Orientation.NORTH)
        );

        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> first = BuildPreviewGlassBlocks.previewVolumes(crossingChunkBorder, materializer);
        LinkedHashSet<BuildPreviewGlassBlocks.PreviewVolume> second = BuildPreviewGlassBlocks.previewVolumes(crossingChunkBorder, materializer);

        assertEquals(first, second);
        assertEquals(25, first.size());
        for (BuildPreviewGlassBlocks.PreviewVolume volume : first) {
            assertEquals(Math.floorDiv(volume.origin().getX(), 16), Math.floorDiv(volume.origin().getX() + volume.sizeX() - 1, 16));
            assertEquals(Math.floorDiv(volume.origin().getZ(), 16), Math.floorDiv(volume.origin().getZ() + volume.sizeZ() - 1, 16));
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
