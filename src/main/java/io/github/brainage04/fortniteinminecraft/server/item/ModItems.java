package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModItems {
    public static final BuildPieceItem WALL = registerBuildPiece(
            "build_wall",
            PieceType.WALL,
            Items.OAK_PLANKS,
            Items.COBBLESTONE,
            Items.COPPER_BLOCK.waxed().unaffected()
    );
    public static final BuildPieceItem FLOOR = registerBuildPiece(
            "build_floor",
            PieceType.FLOOR,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.waxed().unaffected()
    );
    public static final BuildPieceItem STAIR = registerBuildPiece(
            "build_stair",
            PieceType.STAIR,
            Items.OAK_STAIRS,
            Items.COBBLESTONE_STAIRS,
            Items.CUT_COPPER_STAIRS.waxed().unaffected()
    );
    public static final BuildPieceItem ROOF = registerBuildPiece(
            "build_roof",
            PieceType.ROOF,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.waxed().unaffected()
    );
    public static final List<BuildPieceItem> BUILD_PIECES = List.of(WALL, FLOOR, STAIR, ROOF);

    public static final List<WeaponItem> WEAPONS = List.of(
            weapon("assault_rifle", "Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.COMMON,
                    30, 1.5, 30, 5.5, 2.75, 1, 81.28, "WID_Assault_Auto_Athena_C_Ore_T02", "Assault_Auto_Athena_C_Ore_T03", Items.IRON_HOE),
            weapon("assault_rifle", "Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.UNCOMMON,
                    31, 1.5, 30, 5.5, 2.625, 1, 81.28, "WID_Assault_Auto_Athena_UC_Ore_T03", "Assault_Auto_Athena_UC_Ore_T03", Items.IRON_HOE),
            weapon("assault_rifle", "Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.RARE,
                    33, 1.5, 30, 5.5, 2.5, 1, 81.28, "WID_Assault_Auto_Athena_R_Ore_T03", "Assault_Auto_Athena_R_Ore_T03", Items.IRON_HOE),
            weapon("assault_rifle", "Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.EPIC,
                    35, 1.5, 30, 5.5, 2.375, 1, 81.28, "WID_Assault_Auto_Athena_VR_Ore_T03", "Assault_Auto_Athena_VR_Ore_T03", Items.IRON_HOE),
            weapon("assault_rifle", "Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.LEGENDARY,
                    36, 1.5, 30, 5.5, 2.25, 1, 81.28, "WID_Assault_Auto_Athena_SR_Ore_T03", "Assault_Auto_Athena_SR_Ore_T03", Items.IRON_HOE),

            weapon("pump_shotgun", "Pump Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.COMMON,
                    9.2, 1.85, 5, 0.7, 5.104, 10, 21.12, 165, "WID_Shotgun_Standard_Athena_C_Ore_T03", "Shotgun_Standard_Athena_C_Ore_T03", Items.IRON_AXE),
            weapon("pump_shotgun", "Pump Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.UNCOMMON,
                    10.1, 1.85, 5, 0.7, 4.752, 10, 21.12, 170, "WID_Shotgun_Standard_Athena_UC_Ore_T03", "Shotgun_Standard_Athena_UC_Ore_T03", Items.IRON_AXE),
            weapon("pump_shotgun", "Pump Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.RARE,
                    11.0, 1.85, 5, 0.7, 4.4, 10, 21.12, 175, "WID_Shotgun_Standard_Athena_R_Ore_T03", "Shotgun_Standard_Athena_R_Ore_T03", Items.IRON_AXE),
            weapon("pump_shotgun", "Pump Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.EPIC,
                    11.9, 1.85, 5, 0.7, 4.048, 10, 21.12, 180, "WID_Shotgun_Standard_Athena_VR_Ore_T03", "Shotgun_Standard_Athena_VR_Ore_T03", Items.IRON_AXE),
            weapon("pump_shotgun", "Pump Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.LEGENDARY,
                    12.8, 1.85, 5, 0.7, 3.696, 10, 21.12, 185, "WID_Shotgun_Standard_Athena_SR_Ore_T03", "Shotgun_Standard_Athena_SR_Ore_T03", Items.IRON_AXE),

            weapon("drum_shotgun", "Drum Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.COMMON,
                    4.2, 1.75, 12, 3.0, 3.85, 12, 21.12, "WID_Shotgun_AutoDrum_Athena_C_Ore_T03", "Shotgun_AutoDrum_Athena_C_Ore_T03", Items.IRON_AXE),
            weapon("drum_shotgun", "Drum Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.UNCOMMON,
                    4.5, 1.75, 12, 3.0, 3.675, 12, 21.12, "WID_Shotgun_AutoDrum_Athena_UC_Ore_T03", "Shotgun_AutoDrum_Athena_UC_Ore_T03", Items.IRON_AXE),
            weapon("drum_shotgun", "Drum Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.RARE,
                    4.7, 1.75, 12, 3.0, 3.5, 12, 21.12, "WID_Shotgun_AutoDrum_Athena_R_Ore_T03", "Shotgun_AutoDrum_Athena_R_Ore_T03", Items.IRON_AXE),
            weapon("drum_shotgun", "Drum Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.EPIC,
                    4.9, 1.75, 12, 3.0, 3.325, 12, 21.12, "WID_Shotgun_AutoDrum_Athena_VR_Ore_T03", "Shotgun_AutoDrum_Athena_VR_Ore_T03", Items.IRON_AXE),
            weapon("drum_shotgun", "Drum Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.LEGENDARY,
                    5.2, 1.75, 12, 3.0, 3.15, 12, 21.12, "WID_Shotgun_AutoDrum_Athena_SR_Ore_T03", "Shotgun_AutoDrum_Athena_SR_Ore_T03", Items.IRON_AXE),

            weapon("submachine_gun", "Submachine Gun", WeaponCategory.SMG, FortniteRarity.COMMON,
                    15, 1.75, 36, 11.0, 2.42, 1, 32.48, "WID_Pistol_AutoHeavyPDW_Athena_C_Ore_T03", "Pistol_Light_PDW_Athena_C_Ore_T03", Items.CROSSBOW),
            weapon("submachine_gun", "Submachine Gun", WeaponCategory.SMG, FortniteRarity.UNCOMMON,
                    16, 1.75, 36, 11.0, 2.31, 1, 32.48, "WID_Pistol_AutoHeavyPDW_Athena_UC_Ore_T03", "Pistol_Light_PDW_Athena_UC_Ore_T03", Items.CROSSBOW),
            weapon("submachine_gun", "Submachine Gun", WeaponCategory.SMG, FortniteRarity.RARE,
                    17, 1.75, 36, 11.0, 2.2, 1, 32.48, "WID_Pistol_AutoHeavyPDW_Athena_R_Ore_T03", "Pistol_Light_PDW_Athena_R_Ore_T03", Items.CROSSBOW),
            weapon("submachine_gun", "Submachine Gun", WeaponCategory.SMG, FortniteRarity.EPIC,
                    18, 1.75, 36, 11.0, 2.09, 1, 32.48, "WID_Pistol_AutoHeavyPDW_Athena_VR_Ore_T03", "Pistol_Light_PDW_Athena_VR_Ore_T03", Items.CROSSBOW),
            weapon("submachine_gun", "Submachine Gun", WeaponCategory.SMG, FortniteRarity.LEGENDARY,
                    19, 1.75, 36, 11.0, 1.98, 1, 32.48, "WID_Pistol_AutoHeavyPDW_Athena_SR_Ore_T03", "Pistol_Light_PDW_Athena_SR_Ore_T03", Items.CROSSBOW),

            weapon("pistol", "Pistol", WeaponCategory.PISTOL, FortniteRarity.COMMON,
                    24, 2.0, 16, 6.75, 1.54, 1, 53.12, "WID_Pistol_Auto_Athena_C", "Pistol_Standard_Athena_C_Ore_T03", Items.IRON_NUGGET),
            weapon("pistol", "Pistol", WeaponCategory.PISTOL, FortniteRarity.UNCOMMON,
                    25, 2.0, 16, 6.75, 1.47, 1, 53.12, "WID_Pistol_Auto_Athena_UC", "Pistol_Standard_Athena_UC_Ore_T03", Items.IRON_NUGGET),
            weapon("pistol", "Pistol", WeaponCategory.PISTOL, FortniteRarity.RARE,
                    26, 2.0, 16, 6.75, 1.4, 1, 53.12, "WID_Pistol_Auto_Athena_R", "Pistol_Standard_Athena_R_Ore_T03", Items.IRON_NUGGET),
            weapon("pistol", "Pistol", WeaponCategory.PISTOL, FortniteRarity.EPIC,
                    29, 2.0, 18, 6.8, 1.2825, 1, 53.12, "WID_Pistol_Auto_Athena_VR", "Pistol_SemiAuto_Athena_VR_Ore_T03", Items.IRON_NUGGET),
            weapon("pistol", "Pistol", WeaponCategory.PISTOL, FortniteRarity.LEGENDARY,
                    31, 2.0, 18, 6.8, 1.215, 1, 53.12, "WID_Pistol_Auto_Athena_SR", "Pistol_SemiAuto_Athena_SR_Ore_T03", Items.IRON_NUGGET),


            weapon("warforged_assault_rifle", "Warforged Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.COMMON,
                    22, 1.5, 35, 7.2, 3.685, 1, 81.28, "WID_Assault_SunRose_HS_Athena_C", "Assault_Sunrose_Athena_C_Ore_T03", Items.IRON_HOE),
            weapon("warforged_assault_rifle", "Warforged Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.UNCOMMON,
                    23, 1.5, 35, 7.2, 3.5175, 1, 81.28, "WID_Assault_SunRose_HS_Athena_UC", "Assault_Sunrose_Athena_UC_Ore_T03", Items.IRON_HOE),
            weapon("warforged_assault_rifle", "Warforged Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.RARE,
                    24, 1.5, 35, 7.2, 3.35, 1, 81.28, "WID_Assault_SunRose_HS_Athena_R", "Assault_Sunrose_Athena_R_Ore_T03", Items.IRON_HOE),
            weapon("warforged_assault_rifle", "Warforged Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.EPIC,
                    25, 1.5, 35, 7.2, 3.1825, 1, 81.28, "WID_Assault_SunRose_HS_Athena_VR", "Assault_Sunrose_Athena_VR_Ore_T03", Items.IRON_HOE),
            weapon("warforged_assault_rifle", "Warforged Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.LEGENDARY,
                    26, 1.5, 35, 7.2, 3.015, 1, 81.28, "WID_Assault_SunRose_HS_Athena_SR", "Assault_Sunrose_Athena_SR_Ore_T03", Items.IRON_HOE),

            weapon("infantry_rifle", "Infantry Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.COMMON,
                    36, 2, 8, 4, 2.53, 1, 81.28, "WID_Assault_Infantry_Athena_C", "Assault_Infantry_Athena_C_Ore_T03", Items.IRON_HOE),
            weapon("infantry_rifle", "Infantry Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.UNCOMMON,
                    38, 2, 8, 4, 2.415, 1, 81.28, "WID_Assault_Infantry_Athena_UC", "Assault_Infantry_Athena_UC_Ore_T03", Items.IRON_HOE),
            weapon("infantry_rifle", "Infantry Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.RARE,
                    40, 2, 8, 4, 2.3, 1, 81.28, "WID_Assault_Infantry_Athena_R", "Assault_Infantry_Athena_R_Ore_T03", Items.IRON_HOE),
            weapon("infantry_rifle", "Infantry Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.EPIC,
                    42, 2, 10, 4, 2.185, 1, 81.28, "WID_Assault_Infantry_Athena_VR", "Assault_Infantry_Athena_VR_Ore_T03", Items.IRON_HOE),
            weapon("infantry_rifle", "Infantry Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.LEGENDARY,
                    44, 2, 10, 4, 2.07, 1, 81.28, "WID_Assault_Infantry_Athena_SR", "Assault_Infantry_Athena_SR_Ore_T03", Items.IRON_HOE),

            weapon("scoped_assault_rifle", "Scoped Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.UNCOMMON,
                    32, 2, 15, 2, 2.415, 1, 78.72, "WID_Assault_Surgical_Athena_UC_Ore_T03", "Assault_Surgical_Athena_UC_Ore_T03", Items.IRON_HOE),
            weapon("scoped_assault_rifle", "Scoped Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.RARE,
                    34, 2, 15, 2, 2.3, 1, 78.72, "WID_Assault_Surgical_Athena_R_Ore_T03", "Assault_Surgical_Athena_R_Ore_T03", Items.IRON_HOE),
            weapon("scoped_assault_rifle", "Scoped Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.EPIC,
                    36, 2, 20, 2, 2.185, 1, 78.72, "WID_Assault_Surgical_Athena_VR_Ore_T03", "Assault_Surgical_Athena_VR_Ore_T03", Items.IRON_HOE),
            weapon("scoped_assault_rifle", "Scoped Assault Rifle", WeaponCategory.ASSAULT_RIFLE, FortniteRarity.LEGENDARY,
                    37, 2, 20, 2, 2.07, 1, 78.72, "WID_Assault_Surgical_Athena_SR_Ore_T03", "Assault_Surgical_Athena_SR_Ore_T03", Items.IRON_HOE),

            weapon("thermal_scoped_assault_rifle", "Thermal Scoped Assault Rifle", WeaponCategory.SNIPER, FortniteRarity.COMMON,
                    25, 2, 15, 4, 2.53, 1, 6.4, "WID_Assault_Surgical_Thermal_Athena_C_Ore_T03", "Assault_Surgical_Thermal_Athena_C_Ore_T03", Items.IRON_HOE),
            weapon("thermal_scoped_assault_rifle", "Thermal Scoped Assault Rifle", WeaponCategory.SNIPER, FortniteRarity.UNCOMMON,
                    27, 2, 15, 4, 2.415, 1, 6.4, "WID_Assault_Surgical_Thermal_Athena_UC_Ore_T03", "Assault_Surgical_Thermal_Athena_UC_Ore_T03", Items.IRON_HOE),
            weapon("thermal_scoped_assault_rifle", "Thermal Scoped Assault Rifle", WeaponCategory.SNIPER, FortniteRarity.RARE,
                    28, 2, 15, 4, 2.3, 1, 6.4, "WID_Assault_Surgical_Thermal_Athena_R_Ore_T03", "Assault_Surgical_Thermal_Athena_R_Ore_T03", Items.IRON_HOE),
            weapon("thermal_scoped_assault_rifle", "Thermal Scoped Assault Rifle", WeaponCategory.SNIPER, FortniteRarity.EPIC,
                    29, 2, 15, 4, 2.185, 1, 6.4, "WID_Assault_Surgical_Thermal_Athena_VR_Ore_T03", "Assault_Surgical_Thermal_Athena_VR_Ore_T03", Items.IRON_HOE),
            weapon("thermal_scoped_assault_rifle", "Thermal Scoped Assault Rifle", WeaponCategory.SNIPER, FortniteRarity.LEGENDARY,
                    31, 2, 15, 4, 2.07, 1, 6.4, "WID_Assault_Surgical_Thermal_Athena_SR_Ore_T03", "Assault_Surgical_Thermal_Athena_SR_Ore_T03", Items.IRON_HOE),

            weapon("suppressed_submachine_gun", "Suppressed Submachine Gun", WeaponCategory.SMG, FortniteRarity.COMMON,
                    23, 1.75, 36, 8, 2.2, 1, 32.48, "WID_Pistol_AutoHeavySuppressed_Athena_C_Ore_T02", "Pistol_AutoHeavySuppressed_Athena_C_Ore_T03", Items.CROSSBOW),
            weapon("suppressed_submachine_gun", "Suppressed Submachine Gun", WeaponCategory.SMG, FortniteRarity.UNCOMMON,
                    24, 1.75, 36, 8, 2.1, 1, 32.48, "WID_Pistol_AutoHeavySuppressed_Athena_UC_Ore_T03", "Pistol_AutoHeavySuppressed_Athena_UC_Ore_T03", Items.CROSSBOW),
            weapon("suppressed_submachine_gun", "Suppressed Submachine Gun", WeaponCategory.SMG, FortniteRarity.RARE,
                    25, 1.75, 36, 8, 2, 1, 32.48, "WID_Pistol_AutoHeavySuppressed_Athena_R_Ore_T03", "Pistol_AutoHeavySuppressed_Athena_R_Ore_T03", Items.CROSSBOW),
            weapon("suppressed_submachine_gun", "Suppressed Submachine Gun", WeaponCategory.SMG, FortniteRarity.EPIC,
                    26, 1.75, 36, 8, 1.9, 1, 32.48, "WID_Pistol_AutoHeavySuppressed_Athena_VR_Ore_T03", "Pistol_AutoHeavySuppressed_Athena_VR_Ore_T03", Items.CROSSBOW),
            weapon("suppressed_submachine_gun", "Suppressed Submachine Gun", WeaponCategory.SMG, FortniteRarity.LEGENDARY,
                    28, 1.75, 36, 8, 1.8, 1, 32.48, "WID_Pistol_AutoHeavySuppressed_Athena_SR_Ore_T03", "Pistol_AutoHeavySuppressed_Athena_SR_Ore_T03", Items.CROSSBOW),

            weapon("tactical_shotgun", "Tactical Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.COMMON,
                    7.9, 1.75, 8, 1.5, 6.27, 10, 19.68, 120, "WID_Shotgun_SemiAuto_Athena_C", "Shotgun_SemiAuto_Athena_C_Ore_T03", Items.IRON_AXE),
            weapon("tactical_shotgun", "Tactical Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.UNCOMMON,
                    8.4, 1.75, 8, 1.5, 5.985, 10, 19.68, 125, "WID_Shotgun_SemiAuto_Athena_UC", "Shotgun_SemiAuto_Athena_UC_Ore_T03", Items.IRON_AXE),
            weapon("tactical_shotgun", "Tactical Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.RARE,
                    8.8, 1.75, 8, 1.5, 5.7, 10, 19.68, 130, "WID_Shotgun_SemiAuto_Athena_R", "Shotgun_SemiAuto_Athena_R_Ore_T03", Items.IRON_AXE),
            weapon("tactical_shotgun", "Tactical Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.EPIC,
                    9.2, 1.75, 8, 1.5, 5.415, 10, 19.68, 135, "WID_Shotgun_SemiAuto_Athena_VR", "Shotgun_SemiAuto_Athena_VR_Ore_T03", Items.IRON_AXE),
            weapon("tactical_shotgun", "Tactical Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.LEGENDARY,
                    9.7, 1.75, 8, 1.5, 5.13, 10, 19.68, 140, "WID_Shotgun_SemiAuto_Athena_SR", "Shotgun_SemiAuto_Athena_SR_Ore_T03", Items.IRON_AXE),

            weapon("heavy_shotgun", "Heavy Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.RARE,
                    7, 2.5, 7, 1, 6.25, 10, 23.04, 145, "WID_Shotgun_SlugFire_Athena_R", "Shotgun_SlugFire_Athena_R_Ore_T03", Items.IRON_AXE),
            weapon("heavy_shotgun", "Heavy Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.EPIC,
                    7.4, 2.5, 7, 1, 5.9375, 10, 23.04, 150, "WID_Shotgun_SlugFire_Athena_VR", "Shotgun_SlugFire_Athena_VR_Ore_T03", Items.IRON_AXE),
            weapon("heavy_shotgun", "Heavy Shotgun", WeaponCategory.SHOTGUN, FortniteRarity.LEGENDARY,
                    7.7, 2.5, 7, 1, 5.625, 10, 23.04, 155, "WID_Shotgun_SlugFire_Athena_SR", "Shotgun_SlugFire_Athena_SR_Ore_T03", Items.IRON_AXE)
    );

    public static final List<ProjectileWeaponItem> PROJECTILE_WEAPONS = List.of(
            projectileWeapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.COMMON,
                    99, 2.5, 1, 0.75, 3.3, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_C_Ore_T03", "Sniper_BoltAction_Scope_Athena_C_Ore_T03", Items.BOW, 4.5F, 0.0F),
            projectileWeapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.UNCOMMON,
                    105, 2.5, 1, 0.75, 3.15, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_UC_Ore_T03", "Sniper_BoltAction_Scope_Athena_UC_Ore_T03", Items.BOW, 4.5F, 0.0F),
            projectileWeapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.RARE,
                    110, 2.5, 1, 0.75, 3.0, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_R_Ore_T03", "Sniper_BoltAction_Scope_Athena_R_Ore_T03", Items.BOW, 4.5F, 0.0F),
            projectileWeapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.EPIC,
                    116, 2.5, 1, 0.75, 2.5, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_VR_Ore_T03", "Sniper_BoltAction_Scope_Athena_VR_Ore_T03", Items.BOW, 4.5F, 0.0F),
            projectileWeapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.LEGENDARY,
                    121, 2.5, 1, 0.75, 2.35, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_SR_Ore_T03", "Sniper_BoltAction_Scope_Athena_SR_Ore_T03", Items.BOW, 4.5F, 0.0F),
            projectileWeapon("hunting_rifle", "Hunting Rifle", WeaponCategory.SNIPER, FortniteRarity.UNCOMMON,
                    86, 2.75, 1, 0.8, 2.1, 1, 80.0, "WID_Sniper_NoScope_Athena_UC_Ore_T03", "Sniper_NoScope_Athena_UC_Ore_T03", Items.BOW, 4.2F, 0.4F),
            projectileWeapon("hunting_rifle", "Hunting Rifle", WeaponCategory.SNIPER, FortniteRarity.RARE,
                    91, 2.75, 1, 0.8, 2.0, 1, 80.0, "WID_Sniper_NoScope_Athena_R_Ore_T03", "Sniper_NoScope_Athena_R_Ore_T03", Items.BOW, 4.2F, 0.35F),
            projectileWeapon("hunting_rifle", "Hunting Rifle", WeaponCategory.SNIPER, FortniteRarity.EPIC,
                    96, 2.75, 1, 0.8, 1.9, 1, 80.0, "WID_Sniper_NoScope_Athena_VR_Ore_T03", "Sniper_NoScope_Athena_VR_Ore_T03", Items.BOW, 4.2F, 0.3F),
            projectileWeapon("hunting_rifle", "Hunting Rifle", WeaponCategory.SNIPER, FortniteRarity.LEGENDARY,
                    100, 2.75, 1, 0.8, 1.8, 1, 80.0, "WID_Sniper_NoScope_Athena_SR_Ore_T03", "Sniper_NoScope_Athena_SR_Ore_T03", Items.BOW, 4.2F, 0.25F)
    );

    public static final List<ConsumableItem> CONSUMABLES = List.of(
            consumable("bandage", "Bandage", 3.53, 15, 75, 0, 0, true, "bandage", Items.PAPER),
            consumable("medkit", "Medkit", 10.03, 100, 100, 0, 0, true, "medkit", Items.IRON_INGOT),
            consumable("small_shield", "Small Shield Potion", 2.03, 0, 0, 25, 50, true, "small_shield", Items.AMETHYST_SHARD),
            consumable("shield_potion", "Shield Potion", 5.03, 0, 0, 50, 100, true, "shield_potion", Items.PRISMARINE_SHARD),
            consumable("full_restore_jug", "Full Restore Jug", 15.03, 100, 100, 100, 100, true, "full_restore_jug", Items.ECHO_SHARD),
            consumable("apple", "Apple", 0.6, 5, 100, 0, 0, false, "WID_Athena_Apple", Items.APPLE),
            consumable("banana", "Banana", 0.6, 5, 100, 0, 0, false, "WID_Athena_Banana", Items.PAPER)
    );

    public static final List<Item> DEFERRED_WEAPONS = List.of(
            placeholder("weapon_rocket_launcher_legendary", "Rocket Launcher", "WID_Launcher_Rocket_Athena_SR_Ore_T03", Items.FIREWORK_ROCKET),
            placeholder("weapon_grenade_launcher_legendary", "Grenade Launcher", "WID_Launcher_Grenade_Athena_SR_Ore_T03", Items.FIRE_CHARGE),
            placeholder("weapon_proximity_grenade_launcher_legendary", "Proximity Grenade Launcher", "WID_Launcher_Prox_Athena_SR_Ore_T03", Items.FIRE_CHARGE),
            placeholder("weapon_boom_bow_legendary", "Boom Bow", "WID_ExplosiveBow_Athena_SR", Items.BOW),
            placeholder("weapon_shockwave_launcher_epic", "Shockwave Launcher", "WID_Launcher_Shockwave_Athena_VR_Ore_T03", Items.CROSSBOW)
    );

    public static final List<PickupItem> PICKUPS = List.of(
            pickup("pickup_wood", "Wood", PickupPayload.material(MaterialType.WOOD, 30), Items.OAK_PLANKS),
            pickup("pickup_stone", "Stone", PickupPayload.material(MaterialType.STONE, 30), Items.COBBLESTONE),
            pickup("pickup_metal", "Metal", PickupPayload.material(MaterialType.METAL, 30), Items.IRON_INGOT),
            pickup("pickup_gold", "Gold", PickupPayload.gold(100), Items.GOLD_INGOT),
            pickup("pickup_light_ammo", "Light Ammo", PickupPayload.ammo(AmmoType.LIGHT, 18), Items.IRON_NUGGET),
            pickup("pickup_medium_ammo", "Medium Ammo", PickupPayload.ammo(AmmoType.MEDIUM, 18), Items.IRON_NUGGET),
            pickup("pickup_shells", "Shells", PickupPayload.ammo(AmmoType.SHELLS, 6), Items.FLINT),
            pickup("pickup_heavy_ammo", "Heavy Ammo", PickupPayload.ammo(AmmoType.HEAVY, 6), Items.IRON_NUGGET),
            pickup("pickup_rockets", "Rockets", PickupPayload.ammo(AmmoType.ROCKETS, 2), Items.FIREWORK_ROCKET)
    );

    public static final List<ThrowableImpulseItem> THROWABLES = List.of(
            throwableImpulse(new ThrowableImpulseItem.Definition(
                    "shockwave_grenade",
                    "Shockwave Grenade",
                    6.0D,
                    1.55D,
                    1.0D,
                    60,
                    20,
                    true,
                    1.15F,
                    ChatFormatting.LIGHT_PURPLE
            ), Items.ENDER_PEARL),
            throwableImpulse(new ThrowableImpulseItem.Definition(
                    "impulse_grenade",
                    "Impulse Grenade",
                    4.5D,
                    1.05D,
                    0.7D,
                    60,
                    15,
                    false,
                    0.85F,
                    ChatFormatting.BLUE
            ), Items.SNOWBALL)
    );

    public static final PickaxeItem PICKAXE = registerPickaxe("harvesting_tool", Items.IRON_PICKAXE);
    public static final GrapplerItem GRAPPLER = registerGrappler(new GrapplerItem.Definition(
            "grappler",
            "Grappler",
            32.0D,
            1.6D,
            0.25D,
            20,
            "Adventure_Special_HookGun_Athena_SR_Ore_T03"
    ), Items.FISHING_ROD);
    public static final LaunchPadItem LAUNCH_PAD = registerLaunchPad("launch_pad", "Launch Pad", 40, 140, Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
    public static final GliderItem GLIDER = registerGlider("glider", "Glider", 160, Items.PHANTOM_MEMBRANE);
    public static final List<Item> UTILITY_ITEMS = List.of(PICKAXE, GRAPPLER, LAUNCH_PAD, GLIDER);

    public static final List<ResourceNodeItem> RESOURCE_NODE_ITEMS = List.of(
            resourceNode(
                    new ResourceNodeItem.Definition(
                            "resource_node_wood",
                            "Wood Resource Node",
                            MaterialType.WOOD,
                            100,
                            12,
                            List.of(new BlockOffset(0, 0, 0), new BlockOffset(0, 1, 0), new BlockOffset(0, 2, 0))
                    ),
                    Blocks.OAK_LOG.defaultBlockState(),
                    Items.OAK_LOG
            ),
            resourceNode(
                    new ResourceNodeItem.Definition(
                            "resource_node_stone",
                            "Stone Resource Node",
                            MaterialType.STONE,
                            120,
                            12,
                            List.of(
                                    new BlockOffset(0, 0, 0),
                                    new BlockOffset(1, 0, 0),
                                    new BlockOffset(0, 0, 1),
                                    new BlockOffset(0, 1, 0)
                            )
                    ),
                    Blocks.COBBLESTONE.defaultBlockState(),
                    Items.COBBLESTONE
            ),
            resourceNode(
                    new ResourceNodeItem.Definition(
                            "resource_node_metal",
                            "Metal Resource Node",
                            MaterialType.METAL,
                            150,
                            12,
                            List.of(
                                    new BlockOffset(0, 0, 0),
                                    new BlockOffset(1, 0, 0),
                                    new BlockOffset(0, 1, 0)
                            )
                    ),
                    Blocks.CUT_COPPER.weathering().unaffected().defaultBlockState(),
                    Items.CUT_COPPER.weathering().unaffected()
            )
    );

    public static final List<Item> COMBAT_ITEMS = combatItems();
    public static final List<Item> ALL_ITEMS = allItems();

    private static BuildSessionManager sessions;
    private static boolean creativeTabsRegistered;
    private static final String MATERIAL_COMPONENT_KEY = "build_material";

    private ModItems() {
    }

    public static void initialize(BuildSessionManager sessionManager) {
        sessions = Objects.requireNonNull(sessionManager, "sessionManager");
        if (!creativeTabsRegistered) {
            registerCreativeTabs();
            creativeTabsRegistered = true;
        }
    }

    public static BuildPieceItem asBuildPiece(ItemStack stack) {
        if (stack.getItem() instanceof BuildPieceItem item) {
            return item;
        }
        return null;
    }

    public static void refreshBuildItemAppearances(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MaterialType material = selectedMaterialFor(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (asBuildPiece(stack) == null) {
                continue;
            }
            setSelectedMaterial(stack, material);
            player.connection.send(player.getInventory().createInventoryUpdatePacket(slot));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastFullState();
        player.containerMenu.broadcastFullState();
    }

    static MaterialType selectedMaterialFor(ItemStack stack, PacketContext context) {
        MaterialType material = materialFromStack(stack);
        return material == null ? selectedMaterialFor(context) : material;
    }

    static void setSelectedMaterial(ItemStack stack, MaterialType material) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(material, "material");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MATERIAL_COMPONENT_KEY, material.name()));
    }

    private static void registerCreativeTabs() {
        CreativeModeTab buildTab = FabricCreativeModeTab.builder()
                .title(Component.literal("Build Pieces"))
                .icon(() -> new ItemStack(WALL))
                .displayItems((parameters, output) -> BUILD_PIECES.forEach(output::accept))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("build_pieces"), buildTab);

        CreativeModeTab combatTab = FabricCreativeModeTab.builder()
                .title(Component.literal("Fortnite Items"))
                .icon(() -> new ItemStack(WEAPONS.get(0)))
                .displayItems((parameters, output) -> COMBAT_ITEMS.forEach(output::accept))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("items"), combatTab);


        registerVanillaCreativeTabEntries();
    }

    private static void registerVanillaCreativeTabEntries() {
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("building_blocks")).register(
                output -> acceptAll(output, BUILD_PIECES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("combat")).register(output -> {
            acceptAll(output, WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, PROJECTILE_WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, DEFERRED_WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, THROWABLES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, UTILITY_ITEMS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("food_and_drinks")).register(
                output -> acceptAll(output, CONSUMABLES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("ingredients")).register(output -> {
            acceptAll(output, PICKUPS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, RESOURCE_NODE_ITEMS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("search")).register(
                output -> acceptAll(output, ALL_ITEMS, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)
        );
    }

    private static void acceptAll(
            FabricCreativeModeTabOutput output,
            List<? extends Item> items,
            CreativeModeTab.TabVisibility visibility
    ) {
        for (Item item : items) {
            output.accept(new ItemStack(item), visibility);
        }
    }

    private static ResourceKey<CreativeModeTab> creativeTabKey(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(path));
    }

    private static MaterialType selectedMaterialFor(PacketContext context) {
        if (context == null) {
            return MaterialType.WOOD;
        }

        ServerPlayer player = PolymerCommonUtils.getPlayer(context);
        return player == null ? MaterialType.WOOD : selectedMaterialFor(player);
    }

    private static MaterialType selectedMaterialFor(ServerPlayer player) {
        if (sessions == null) {
            return MaterialType.WOOD;
        }
        PlayerBuildSession session = sessions.get(player.getUUID());
        return session == null ? MaterialType.WOOD : session.selectedMaterial();
    }

    private static MaterialType materialFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(MATERIAL_COMPONENT_KEY, "");
        try {
            return name.isBlank() ? null : MaterialType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static WeaponItem weapon(
            String path,
            String displayName,
            WeaponCategory category,
            FortniteRarity rarity,
            double damage,
            double criticalMultiplier,
            int magazineSize,
            double fireRatePerSecond,
            double reloadSeconds,
            int pellets,
            double rangeBlocks,
            String sourceItemId,
            String sourceStatRow,
            Item clientItem
    ) {
        return weapon(
                path,
                displayName,
                category,
                rarity,
                damage,
                criticalMultiplier,
                magazineSize,
                fireRatePerSecond,
                reloadSeconds,
                pellets,
                rangeBlocks,
                0.0D,
                sourceItemId,
                sourceStatRow,
                clientItem
        );
    }

    private static WeaponItem weapon(
            String path,
            String displayName,
            WeaponCategory category,
            FortniteRarity rarity,
            double damage,
            double criticalMultiplier,
            int magazineSize,
            double fireRatePerSecond,
            double reloadSeconds,
            int pellets,
            double rangeBlocks,
            double maxDamagePerShot,
            String sourceItemId,
            String sourceStatRow,
            Item clientItem
    ) {
        String itemPath = "weapon_" + path + "_" + rarity.pathPrefix();
        WeaponDefinition definition = new WeaponDefinition(
                itemPath,
                displayName,
                category,
                rarity,
                new WeaponStats(damage, criticalMultiplier, magazineSize, fireRatePerSecond, reloadSeconds, pellets, rangeBlocks, maxDamagePerShot),
                sourceItemId,
                sourceStatRow
        );
        return registerWeapon(definition, clientItem);
    }

    private static ProjectileWeaponItem projectileWeapon(
            String path,
            String displayName,
            WeaponCategory category,
            FortniteRarity rarity,
            double damage,
            double criticalMultiplier,
            int magazineSize,
            double fireRatePerSecond,
            double reloadSeconds,
            int pellets,
            double rangeBlocks,
            String sourceItemId,
            String sourceStatRow,
            Item clientItem,
            float projectileSpeed,
            float inaccuracy
    ) {
        String itemPath = "weapon_" + path + "_" + rarity.pathPrefix();
        WeaponDefinition definition = new WeaponDefinition(
                itemPath,
                displayName,
                category,
                rarity,
                new WeaponStats(damage, criticalMultiplier, magazineSize, fireRatePerSecond, reloadSeconds, pellets, rangeBlocks, 0.0D),
                sourceItemId,
                sourceStatRow
        );
        return registerProjectileWeapon(definition, clientItem, projectileSpeed, inaccuracy);
    }

    private static ConsumableItem consumable(
            String path,
            String displayName,
            double castSeconds,
            int healthRestore,
            int healthCap,
            int shieldRestore,
            int shieldCap,
            boolean movementLocked,
            String sourceItemId,
            Item clientItem
    ) {
        ConsumableDefinition definition = new ConsumableDefinition(
                "consumable_" + path,
                displayName,
                castSeconds,
                healthRestore,
                healthCap,
                shieldRestore,
                shieldCap,
                movementLocked,
                sourceItemId
        );
        return registerConsumable(definition, clientItem);
    }

    private static SourceBackedPlaceholderItem placeholder(String path, String displayName, String sourceItemId, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new SourceBackedPlaceholderItem(displayName, sourceItemId, placeholderProperties(key, 1), clientItem)
        );
    }

    private static PickupItem pickup(String path, String displayName, PickupPayload payload, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new PickupItem(displayName, payload, placeholderProperties(key, 64), clientItem)
        );
    }

    private static ThrowableImpulseItem throwableImpulse(ThrowableImpulseItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ThrowableImpulseItem(definition, throwableProperties(key, definition), clientItem)
        );
    }

    private static PickaxeItem registerPickaxe(String path, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new PickaxeItem(singleStackProperties(key), clientItem)
        );
    }

    private static GrapplerItem registerGrappler(GrapplerItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new GrapplerItem(definition, singleStackProperties(key), clientItem)
        );
    }

    private static LaunchPadItem registerLaunchPad(String path, String displayName, int cooldownTicks, long redeployTicks, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new LaunchPadItem(displayName, cooldownTicks, redeployTicks, singleStackProperties(key), clientItem)
        );
    }

    private static GliderItem registerGlider(String path, String displayName, long redeployTicks, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new GliderItem(displayName, redeployTicks, singleStackProperties(key), clientItem)
        );
    }

    private static ResourceNodeItem resourceNode(ResourceNodeItem.Definition definition, net.minecraft.world.level.block.state.BlockState blockState, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ResourceNodeItem(definition, blockState, placeholderProperties(key, 16), clientItem)
        );
    }

    private static List<Item> combatItems() {
        ArrayList<Item> items = new ArrayList<>(WEAPONS.size() + PROJECTILE_WEAPONS.size() + DEFERRED_WEAPONS.size() + THROWABLES.size() + UTILITY_ITEMS.size() + CONSUMABLES.size());
        items.addAll(WEAPONS);
        items.addAll(PROJECTILE_WEAPONS);
        items.addAll(DEFERRED_WEAPONS);
        items.addAll(THROWABLES);
        items.addAll(UTILITY_ITEMS);
        items.addAll(CONSUMABLES);
        return List.copyOf(items);
    }

    private static List<Item> allItems() {
        ArrayList<Item> items = new ArrayList<>(BUILD_PIECES.size() + COMBAT_ITEMS.size() + PICKUPS.size() + RESOURCE_NODE_ITEMS.size());
        items.addAll(BUILD_PIECES);
        items.addAll(COMBAT_ITEMS);
        items.addAll(PICKUPS);
        items.addAll(RESOURCE_NODE_ITEMS);
        return List.copyOf(items);
    }

    private static BuildPieceItem registerBuildPiece(
            String path,
            PieceType pieceType,
            Item woodClientItem,
            Item stoneClientItem,
            Item metalClientItem
    ) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new BuildPieceItem(
                        pieceType,
                        new Item.Properties().setId(key).stacksTo(1),
                        woodClientItem,
                        stoneClientItem,
                        metalClientItem
                )
        );
    }

    private static WeaponItem registerWeapon(WeaponDefinition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new WeaponItem(definition, weaponProperties(key, definition), clientItem)
        );
    }

    private static ProjectileWeaponItem registerProjectileWeapon(
            WeaponDefinition definition,
            Item clientItem,
            float projectileSpeed,
            float inaccuracy
    ) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ProjectileWeaponItem(definition, projectileWeaponProperties(key, definition), clientItem, projectileSpeed, inaccuracy)
        );
    }

    private static ConsumableItem registerConsumable(ConsumableDefinition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ConsumableItem(definition, consumableProperties(key), clientItem)
        );
    }

    private static Item.Properties weaponProperties(ResourceKey<Item> key, WeaponDefinition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.USE_COOLDOWN, WeaponItem.cooldownComponent(definition));
    }

    private static Item.Properties projectileWeaponProperties(ResourceKey<Item> key, WeaponDefinition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.USE_COOLDOWN, ProjectileWeaponItem.cooldownComponent(definition));
    }

    private static Item.Properties consumableProperties(ResourceKey<Item> key) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(16);
    }

    private static Item.Properties throwableProperties(ResourceKey<Item> key, ThrowableImpulseItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(6)
                .component(DataComponents.USE_COOLDOWN, ThrowableImpulseItem.cooldownComponent(definition));
    }

    private static Item.Properties singleStackProperties(ResourceKey<Item> key) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(1);
    }

    private static Item.Properties placeholderProperties(ResourceKey<Item> key, int stackSize) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(stackSize);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }

    private static final class SourceBackedPlaceholderItem extends SimplePolymerItem {
        private final String displayName;
        private final String sourceItemId;

        private SourceBackedPlaceholderItem(String displayName, String sourceItemId, Item.Properties settings, Item clientItem) {
            super(settings, clientItem);
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.sourceItemId = Objects.requireNonNull(sourceItemId, "sourceItemId");
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal(displayName);
        }

        @Override
        public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
            tooltip.add(Component.literal("Placeholder: implementation pending"));
            tooltip.add(Component.literal("Source: " + sourceItemId));
        }
    }
}
