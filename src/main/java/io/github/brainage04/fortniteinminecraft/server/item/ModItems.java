package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModItems {
    public static final BuildPieceItem WALL = registerBuildPiece(
            "build_wall",
            PieceType.WALL,
            Items.OAK_PLANKS,
            Items.COBBLESTONE,
            Items.COPPER_BLOCK.weathering().unaffected()
    );
    public static final BuildPieceItem FLOOR = registerBuildPiece(
            "build_floor",
            PieceType.FLOOR,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.weathering().unaffected()
    );
    public static final BuildPieceItem STAIR = registerBuildPiece(
            "build_stair",
            PieceType.STAIR,
            Items.OAK_STAIRS,
            Items.COBBLESTONE_STAIRS,
            Items.CUT_COPPER_STAIRS.weathering().unaffected()
    );
    public static final BuildPieceItem ROOF = registerBuildPiece(
            "build_roof",
            PieceType.ROOF,
            Items.OAK_SLAB,
            Items.COBBLESTONE_SLAB,
            Items.CUT_COPPER_SLAB.weathering().unaffected()
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

            weapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.COMMON,
                    99, 2.5, 1, 0.75, 3.3, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_C_Ore_T03", "Sniper_BoltAction_Scope_Athena_C_Ore_T03", Items.BOW),
            weapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.UNCOMMON,
                    105, 2.5, 1, 0.75, 3.15, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_UC_Ore_T03", "Sniper_BoltAction_Scope_Athena_UC_Ore_T03", Items.BOW),
            weapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.RARE,
                    110, 2.5, 1, 0.75, 3.0, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_R_Ore_T03", "Sniper_BoltAction_Scope_Athena_R_Ore_T03", Items.BOW),
            weapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.EPIC,
                    116, 2.5, 1, 0.75, 2.5, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_VR_Ore_T03", "Sniper_BoltAction_Scope_Athena_VR_Ore_T03", Items.BOW),
            weapon("bolt_action_sniper", "Bolt-Action Sniper Rifle", WeaponCategory.SNIPER, FortniteRarity.LEGENDARY,
                    121, 2.5, 1, 0.75, 2.35, 1, 80.0, "WID_Sniper_BoltAction_Scope_Athena_SR_Ore_T03", "Sniper_BoltAction_Scope_Athena_SR_Ore_T03", Items.BOW)
    );

    public static final List<ConsumableItem> CONSUMABLES = List.of(
            consumable("bandage", "Bandage", 3.53, 15, 75, 0, 0, true, "bandage", Items.PAPER),
            consumable("medkit", "Medkit", 10.03, 100, 100, 0, 0, true, "medkit", Items.IRON_INGOT),
            consumable("small_shield", "Small Shield Potion", 2.03, 0, 0, 25, 50, true, "small_shield", Items.AMETHYST_SHARD),
            consumable("shield_potion", "Shield Potion", 5.03, 0, 0, 50, 100, true, "shield_potion", Items.PRISMARINE_SHARD),
            consumable("full_restore_jug", "Full Restore Jug", 15.03, 100, 100, 100, 100, true, "full_restore_jug", Items.ECHO_SHARD)
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
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("combat")).register(
                output -> acceptAll(output, WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("food_and_drinks")).register(
                output -> acceptAll(output, CONSUMABLES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
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

    private static List<Item> combatItems() {
        ArrayList<Item> items = new ArrayList<>(WEAPONS.size() + CONSUMABLES.size());
        items.addAll(WEAPONS);
        items.addAll(CONSUMABLES);
        return List.copyOf(items);
    }

    private static List<Item> allItems() {
        ArrayList<Item> items = new ArrayList<>(BUILD_PIECES.size() + COMBAT_ITEMS.size());
        items.addAll(BUILD_PIECES);
        items.addAll(COMBAT_ITEMS);
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

    private static Item.Properties consumableProperties(ResourceKey<Item> key) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(16);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }
}
