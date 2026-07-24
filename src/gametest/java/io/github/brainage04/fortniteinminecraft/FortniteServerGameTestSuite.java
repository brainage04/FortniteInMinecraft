package io.github.brainage04.fortniteinminecraft;

import io.github.brainage04.fortniteinminecraft.server.item.FortniteItemMechanicsGameTest;
import io.github.brainage04.fortniteinminecraft.server.player.FortniteMobilityGameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

public final class FortniteServerGameTestSuite {
    private FortniteServerGameTestSuite() {
    }

    public static List<TestCase> tests() {
        FortniteInMinecraftGameTest core = new FortniteInMinecraftGameTest();
        FortniteItemMechanicsGameTest items = new FortniteItemMechanicsGameTest();
        FortniteMobilityGameTest mobility = new FortniteMobilityGameTest();
        return List.of(
                test("server_command_root_is_registered", core::serverCommandRootIsRegistered),
                test("glider_stays_deployed_and_travels_after_redeploy", 80, core::gliderStaysDeployedAndTravelsAfterRedeploy),
                test("hitscan_weapon_damages_living_target", core::hitscanWeaponDamagesLivingTarget),
                test("hitscan_shielded_mob_keeps_health_and_shows_blue_hit_marker", core::hitscanShieldedMobKeepsHealthAndShowsBlueHitMarker),
                test("projectile_weapon_damages_living_target", 80, core::projectileWeaponDamagesLivingTarget),
                test("consumables_restore_health_and_shield_in_world", core::consumablesRestoreHealthAndShieldInWorld),
                test("launch_pad_and_bouncer_enable_redeploy", core::launchPadAndBouncerEnableRedeploy),
                test("build_pieces_materialize_every_piece_and_material", core::buildPiecesMaterializeEveryPieceAndMaterial),
                test("support_cascade_separates_grounded_and_unsupported_pieces", core::supportCascadeSeparatesGroundedAndUnsupportedPieces),
                test("damage_destroy_and_support_collapse_update_world_state", core::damageDestroyAndSupportCollapseUpdateWorldState),
                test("edit_masks_keep_partial_pieces_and_reject_empty_pieces", core::editMasksKeepPartialPiecesAndRejectEmptyPieces),
                test("impulse_and_shockwave_grenades_push_several_relative_positions", items::impulseAndShockwaveGrenadesPushSeveralRelativePositions),
                test("explosive_grenades_and_clingers_carry_damage_and_detonation_rules", items::explosiveGrenadesAndClingersCarryDamageAndDetonationRules),
                test("weapon_burst_catalog_stats_drive_cartridge_counts", items::weaponBurstCatalogStatsDriveCartridgeCounts),
                test("weapon_damage_at_zero_health_clears_build_piece", items::weaponDamageAtZeroHealthClearsBuildPiece),
                test("pickaxe_weak_point_hit_applies_multiplier_through_damage_block_hit", items::pickaxeWeakPointHitAppliesMultiplierThroughDamageBlockHit),
                test("weapon_weak_point_hit_applies_multiplier_through_damage_build", items::weaponWeakPointHitAppliesMultiplierThroughDamageBuild),
                test("pickaxe_terrain_weak_point_harvest_destroys_log_and_grants_wood", items::pickaxeTerrainWeakPointHarvestDestroysLogAndGrantsWood),
                test("harvesting_tool_inventory_restores_missing_and_removes_duplicates", items::harvestingToolInventoryRestoresMissingAndRemovesDuplicates),
                test("harvesting_tool_inventory_counts_carried_tool_during_moves", items::harvestingToolInventoryCountsCarriedToolDuringMoves),
                test("launch_pad_footprint_places_nine_trigger_blocks", items::launchPadFootprintPlacesNineTriggerBlocks),
                test("bouncer_footprint_places_current_floor_sized_surface", items::bouncerFootprintPlacesCurrentFloorSizedSurface),
                test("bouncer_wall_footprint_places_current_floor_sized_trigger_plane", items::bouncerWallFootprintPlacesCurrentFloorSizedTriggerPlane),
                test("wall_bouncer_trigger_launches_touching_player_outward", 40, items::wallBouncerTriggerLaunchesTouchingPlayerOutward),
                test("bouncer_item_places_floor_and_wall_trigger_footprints", items::bouncerItemPlacesFloorAndWallTriggerFootprints),
                test("floor_bouncer_launches_jumping_player_upward", 40, items::floorBouncerLaunchesJumpingPlayerUpward),
                test("running_jumping_into_wall_bouncer_launches_player_outward", 40, items::runningJumpingIntoWallBouncerLaunchesPlayerOutward),
                test("harvesting_tool_damages_wall_through_bouncer_trigger", items::harvestingToolDamagesWallThroughBouncerTrigger),
                test("weapon_damage_breaks_wall_bouncer_support_and_clears_trigger", items::weaponDamageBreaksWallBouncerSupportAndClearsTrigger),
                test("explosive_damage_breaks_floor_bouncer_support_and_clears_trigger", items::explosiveDamageBreaksFloorBouncerSupportAndClearsTrigger),
                test("deployable_footprint_rejects_blocked_cells_without_partial_placement", items::deployableFootprintRejectsBlockedCellsWithoutPartialPlacement),
                test("loot_container_open_clears_blocks_and_drops_matching_loot", items::lootContainerOpenClearsBlocksAndDropsMatchingLoot),
                test("port_a_fort_use_throws_projectile_consumes_stack_and_starts_cooldown", items::portAFortUseThrowsProjectileConsumesStackAndStartsCooldown),
                test("port_a_fort_deploy_materializes_tracked_metal_fort", items::portAFortDeployMaterializesTrackedMetalFort),
                test("port_a_fort_deploy_rolls_back_partial_fort_when_footprint_conflicts", items::portAFortDeployRollsBackPartialFortWhenFootprintConflicts),
                test("sprint_crouch_slide_preserves_direction_and_accelerates_downhill", mobility::sprintCrouchSlidePreservesDirectionAndAcceleratesDownhill)
        );
    }

    private static TestCase test(String path, Consumer<GameTestHelper> function) {
        return test(path, 20, function);
    }

    private static TestCase test(String path, int maxTicks, Consumer<GameTestHelper> function) {
        return new TestCase(path, maxTicks, function);
    }

    public record TestCase(String path, int maxTicks, Consumer<GameTestHelper> function) {
    }
}
