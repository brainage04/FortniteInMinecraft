package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildResourceHarvestTest {
    @Test
    void harvestDamagesTrackedBuildWithoutGrantingMaterial() {
        BuildWorldState world = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 0, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(world.addIfAbsent(BuildPieceState.placed(slot, MaterialType.WOOD, UUID.randomUUID(), 0L)));
        PlayerResourceState resources = new PlayerResourceState();

        BuildResourceHarvest.HarvestResult result = BuildResourceHarvest.harvest(world, slot, 25, 1L);

        assertTrue(result.hit());
        assertFalse(result.destroyed());
        assertEquals(MaterialType.WOOD, result.material());
        assertEquals(0, result.grantedResources());
        assertEquals(0, resources.material(MaterialType.WOOD));
        assertEquals(world.get(slot).currentHealth(), result.after().currentHealth());
    }

    @Test
    void harvestReportsDestroyedButLeavesRemovalToWorldMaterializerCaller() {
        BuildWorldState world = new BuildWorldState();
        BuildSlot slot = BuildSlot.of("overworld", 1, 0, 0, PieceType.WALL, Orientation.NORTH);
        assertTrue(world.addIfAbsent(BuildPieceState.placed(slot, MaterialType.STONE, UUID.randomUUID(), 0L)));

        BuildResourceHarvest.HarvestResult result = BuildResourceHarvest.harvest(
                world,
                slot,
                MaterialType.STONE.finalHealth(),
                1L
        );

        assertTrue(result.destroyed());
        assertTrue(world.contains(slot));
    }

    @Test
    void missingHarvestDoesNotGrantResources() {
        BuildWorldState world = new BuildWorldState();
        PlayerResourceState resources = new PlayerResourceState();
        BuildSlot missing = BuildSlot.of("overworld", 2, 0, 0, PieceType.FLOOR, Orientation.NORTH);

        BuildResourceHarvest.HarvestResult result = BuildResourceHarvest.harvest(world, missing, 25, 1L);

        assertFalse(result.hit());
        assertEquals(0, resources.material(MaterialType.WOOD));
        assertEquals(0, resources.material(MaterialType.STONE));
        assertEquals(0, resources.material(MaterialType.METAL));
    }
}
