package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;
import io.github.brainage04.fortniteinminecraft.server.item.PickupPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerResourceStateTest {
    @Test
    void pickupsApplyToSeparateResourceChannels() {
        PlayerResourceState state = new PlayerResourceState();

        assertEquals(30, PickupPayload.material(MaterialType.WOOD, 30).applyTo(state).accepted());
        assertEquals(18, PickupPayload.ammo(AmmoType.MEDIUM, 18).applyTo(state).accepted());
        assertEquals(100, PickupPayload.gold(100).applyTo(state).accepted());

        assertEquals(30, state.material(MaterialType.WOOD));
        assertEquals(18, state.ammo(AmmoType.MEDIUM));
        assertEquals(100, state.gold());
    }

    @Test
    void resourceStateCapsMaterialsAmmoAndGold() {
        PlayerResourceState state = new PlayerResourceState();

        assertEquals(PlayerResourceState.MAX_MATERIAL, state.addMaterial(MaterialType.STONE, PlayerResourceState.MAX_MATERIAL + 50));
        assertEquals(0, state.addMaterial(MaterialType.STONE, 1));
        assertEquals(PlayerResourceState.MAX_AMMO, state.addAmmo(AmmoType.SHELLS, PlayerResourceState.MAX_AMMO + 1));
        assertEquals(PlayerResourceState.MAX_GOLD, state.addGold(PlayerResourceState.MAX_GOLD + 500));

        assertEquals(PlayerResourceState.MAX_MATERIAL, state.material(MaterialType.STONE));
        assertEquals(PlayerResourceState.MAX_AMMO, state.ammo(AmmoType.SHELLS));
        assertEquals(PlayerResourceState.MAX_GOLD, state.gold());
    }

    @Test
    void pickupResultReportsWhetherAnythingWasGranted() {
        PlayerResourceState state = new PlayerResourceState();
        state.addGold(PlayerResourceState.MAX_GOLD);

        assertFalse(PickupPayload.gold(25).applyTo(state).granted());
        assertTrue(PickupPayload.ammo(AmmoType.LIGHT, 1).applyTo(state).granted());
    }

    @Test
    void debugMutatorsSetClearAndToggleInfiniteFlags() {
        PlayerResourceState state = new PlayerResourceState();

        assertEquals(123, state.setMaterial(MaterialType.WOOD, 123));
        assertEquals(45, state.setAmmo(AmmoType.ROCKETS, 45));
        assertEquals(123, state.material(MaterialType.WOOD));
        assertEquals(45, state.ammo(AmmoType.ROCKETS));

        state.clearMaterial(MaterialType.WOOD);
        state.clearAmmo(AmmoType.ROCKETS);
        assertEquals(0, state.material(MaterialType.WOOD));
        assertEquals(0, state.ammo(AmmoType.ROCKETS));

        state.setInfiniteMaterials(true);
        state.setInfiniteAmmo(true);
        assertTrue(state.infiniteMaterials());
        assertTrue(state.infiniteAmmo());
        assertTrue(state.materials().spend(MaterialType.METAL, PlayerResourceState.MAX_MATERIAL));
        assertEquals(0, state.material(MaterialType.METAL));

        state.addMaterial(MaterialType.STONE, 5);
        state.addAmmo(AmmoType.LIGHT, 7);
        state.clearMaterials();
        state.clearAmmo();
        assertEquals(0, state.material(MaterialType.STONE));
        assertEquals(0, state.ammo(AmmoType.LIGHT));
    }
}
