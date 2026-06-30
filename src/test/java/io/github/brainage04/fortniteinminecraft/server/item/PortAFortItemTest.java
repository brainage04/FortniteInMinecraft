package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
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
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortAFortItemTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fortTemplateUsesGridSnappedTrackedBuildSlots() {
        BuildGridPos anchor = new BuildGridPos("overworld", 10, 4, -3);

        List<BuildSlot> slots = PortAFortItem.fortSlots(anchor, 2, 4, Orientation.EAST);

        assertEquals(80, slots.size());
        assertEquals(slots.size(), slots.stream().distinct().count());
        assertTrue(slots.contains(BuildSlot.of("overworld", 9, 4, -4, PieceType.FLOOR, Orientation.NORTH)));
        assertTrue(slots.contains(BuildSlot.of("overworld", 10, 4, -3, PieceType.STAIR, Orientation.EAST)));
        assertTrue(slots.contains(BuildSlot.of("overworld", 10, 7, -3, PieceType.ROOF, Orientation.NORTH)));
        assertTrue(slots.stream().anyMatch(slot -> slot.pieceType() == PieceType.WALL && slot.gridPos().y() == 4));
        assertTrue(slots.stream().anyMatch(slot -> slot.pieceType() == PieceType.WALL && slot.gridPos().y() == 7));
    }

    @Test
    void fortTemplateMaterializesAsTrackedMetalPiecesOnRestoredGrid() {
        BuildGridPos anchor = new BuildGridPos("overworld", 10, 4, -3);
        PortAFortItem.Definition definition = new PortAFortItem.Definition(
                "utility_port_a_fort",
                "Port-A-Fort",
                FortniteRarity.EPIC,
                "Athena_SuperTowerGrenade_A",
                40,
                2,
                4
        );
        UUID owner = UUID.fromString("00000000-0000-0000-0000-0000000000af");
        BuildRules rules = BuildRules.defaults();
        FootprintProjector projector = new FootprintProjector(rules);
        SnapGrid snapGrid = new SnapGrid(rules);
        BuildWorldState state = new BuildWorldState();

        List<BuildPieceState> pieces = PortAFortItem.fortPieces(anchor, definition, owner, Orientation.EAST, 100L);

        assertEquals(80, pieces.size());
        assertTrue(pieces.stream().allMatch(piece -> piece.material() == MaterialType.METAL));
        assertTrue(pieces.stream().allMatch(piece -> piece.owner().equals(owner)));
        for (BuildPieceState piece : pieces) {
            PieceFootprint footprint = projector.project(piece);
            List<BlockOffset> absoluteBlocks = footprint.absoluteBlocks(snapGrid.blockOrigin(piece.slot().gridPos()));
            assertTrue(state.addIfNotConflicting(piece, absoluteBlocks), piece.slot().toString());
        }
        assertEquals(80, state.size());
    }
}
