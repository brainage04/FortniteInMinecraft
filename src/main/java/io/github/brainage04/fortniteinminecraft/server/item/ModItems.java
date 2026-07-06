package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.session.BuildSessionManager;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildSession;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ModItems {
    static final long SHOCKWAVE_LAUNCHER_IMPACT_DELAY_TICKS = 10L;

    private static final ItemCatalog.Catalog CATALOG = ItemCatalog.load();

    public static final List<BuildPieceItem> BUILD_PIECES = registerBuildPieces(CATALOG.buildPieces());
    public static final BuildPieceItem WALL = buildPiece("build_wall");
    public static final BuildPieceItem FLOOR = buildPiece("build_floor");
    public static final BuildPieceItem STAIR = buildPiece("build_stair");
    public static final BuildPieceItem ROOF = buildPiece("build_roof");

    public static final List<WeaponItem> WEAPONS = registerHitscanWeapons(CATALOG.weapons());
    public static final List<ProjectileWeaponItem> PROJECTILE_WEAPONS = registerProjectileWeapons(CATALOG.weapons());
    public static final List<ExplosiveProjectileWeaponItem> EXPLOSIVE_WEAPONS = registerExplosiveWeapons(CATALOG.weapons());
    public static final List<ConsumableItem> CONSUMABLES = registerConsumables(CATALOG.consumables());
    public static final List<PickupItem> PICKUPS = registerPickups(CATALOG.pickups());
    public static final List<LootContainerBlock> LOOT_CONTAINERS = registerLootContainers(CATALOG.lootContainers());
    public static final LootContainerBlock LOOT_CHEST = lootContainer("loot_chest");
    public static final LootContainerBlock AMMO_BOX = lootContainer("ammo_box");
    public static final List<Item> LOOT_CONTAINER_ITEMS = lootContainerItems();
    public static final BlockEntityType<LootContainerBlockEntity> LOOT_CONTAINER_BLOCK_ENTITY_TYPE = registerLootContainerBlockEntity();
    public static final List<ThrowableImpulseItem> THROWABLES = registerThrowableImpulses(CATALOG.throwableImpulses());
    public static final List<Item> UTILITY_ITEMS = registerUtilityItems(CATALOG.utilities());

    public static final PickaxeItem PICKAXE = utilityItem("harvesting_tool", PickaxeItem.class);
    public static final GrapplerItem GRAPPLER = utilityItem("grappler", GrapplerItem.class);
    public static final LaunchPadItem LAUNCH_PAD = utilityItem("launch_pad", LaunchPadItem.class);
    public static final ExplosiveThrowableItem CLINGER = utilityItem("utility_clinger", ExplosiveThrowableItem.class);
    public static final BouncerItem BOUNCER = utilityItem("utility_bouncer", BouncerItem.class);
    public static final RiftToGoItem RIFT_TO_GO = utilityItem("utility_rift_to_go", RiftToGoItem.class);
    public static final PortAFortItem PORT_A_FORT = utilityItem("utility_port_a_fort", PortAFortItem.class);

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
        LootContainerInteractions.register();
    }

    public static BuildPieceItem asBuildPiece(ItemStack stack) {
        if (stack.getItem() instanceof BuildPieceItem item) {
            return item;
        }
        return null;
    }

    public static boolean isGun(Item item) {
        return item instanceof WeaponItem
                || item instanceof ProjectileWeaponItem
                || item instanceof ExplosiveProjectileWeaponItem;
    }

    public static boolean isGun(ItemStack stack) {
        return isGun(stack.getItem());
    }

    public static boolean suppressesVanillaBlockBreaking(Item item) {
        return isGun(item)
                || item instanceof ExplosiveThrowableItem
                || item instanceof PickaxeItem
                || item instanceof BuildPieceItem
                || item instanceof GrapplerItem
                || item instanceof LaunchPadItem
                || item instanceof BouncerItem
                || item instanceof RiftToGoItem
                || item instanceof PortAFortItem
                || item instanceof ConsumableItem;
    }

    public static boolean suppressesVanillaBlockBreaking(ItemStack stack) {
        return suppressesVanillaBlockBreaking(stack.getItem());
    }

    public static MaterialType selectedMaterialFor(ItemStack stack) {
        MaterialType material = materialFromStack(stack);
        return material == null ? MaterialType.WOOD : material;
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

    static void setSelectedMaterial(ItemStack stack, MaterialType material) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(material, "material");
        if (stack.getItem() instanceof BuildPieceItem item) {
            stack.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item.clientItemFor(material)));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MATERIAL_COMPONENT_KEY, material.name()));
    }

    private static void registerCreativeTabs() {
        CreativeModeTab fortniteTab = FabricCreativeModeTab.builder()
                .title(Component.literal(FortniteInMinecraft.MOD_NAME))
                .icon(() -> new ItemStack(WEAPONS.get(0)))
                .displayItems((parameters, output) -> ALL_ITEMS.forEach(output::accept))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("items"), fortniteTab);

        registerVanillaCreativeTabEntries();
    }

    private static void registerVanillaCreativeTabEntries() {
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("building_blocks")).register(
                output -> acceptAll(output, BUILD_PIECES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("combat")).register(output -> {
            acceptAll(output, WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, PROJECTILE_WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, EXPLOSIVE_WEAPONS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, THROWABLES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, UTILITY_ITEMS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptAll(output, LOOT_CONTAINER_ITEMS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("food_and_drinks")).register(
                output -> acceptAll(output, CONSUMABLES, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
        CreativeModeTabEvents.modifyOutputEvent(creativeTabKey("ingredients")).register(output -> {
            acceptAll(output, PICKUPS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
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

    private static List<BuildPieceItem> registerBuildPieces(List<ItemCatalog.BuildPieceEntry> entries) {
        ArrayList<BuildPieceItem> items = new ArrayList<>(entries.size());
        for (ItemCatalog.BuildPieceEntry entry : entries) {
            items.add(registerBuildPiece(entry));
        }
        return List.copyOf(items);
    }

    private static List<WeaponItem> registerHitscanWeapons(List<ItemCatalog.WeaponEntry> entries) {
        ArrayList<WeaponItem> items = new ArrayList<>();
        for (ItemCatalog.WeaponEntry entry : entries) {
            if (entry.kind() == ItemCatalog.WeaponKind.HITSCAN) {
                items.add(registerWeapon(entry.definition(), clientItem(entry.clientItem())));
            }
        }
        return List.copyOf(items);
    }

    private static List<ProjectileWeaponItem> registerProjectileWeapons(List<ItemCatalog.WeaponEntry> entries) {
        ArrayList<ProjectileWeaponItem> items = new ArrayList<>();
        for (ItemCatalog.WeaponEntry entry : entries) {
            if (entry.kind() == ItemCatalog.WeaponKind.PROJECTILE) {
                items.add(registerProjectileWeapon(entry));
            }
        }
        return List.copyOf(items);
    }

    private static List<ExplosiveProjectileWeaponItem> registerExplosiveWeapons(List<ItemCatalog.WeaponEntry> entries) {
        ArrayList<ExplosiveProjectileWeaponItem> items = new ArrayList<>();
        for (ItemCatalog.WeaponEntry entry : entries) {
            if (entry.kind() == ItemCatalog.WeaponKind.EXPLOSIVE) {
                items.add(registerExplosiveWeapon(entry));
            }
        }
        return List.copyOf(items);
    }

    private static List<ConsumableItem> registerConsumables(List<ItemCatalog.ConsumableEntry> entries) {
        ArrayList<ConsumableItem> items = new ArrayList<>(entries.size());
        for (ItemCatalog.ConsumableEntry entry : entries) {
            items.add(registerConsumable(entry.definition(), clientItem(entry.clientItem())));
        }
        return List.copyOf(items);
    }

    private static List<PickupItem> registerPickups(List<ItemCatalog.PickupEntry> entries) {
        ArrayList<PickupItem> items = new ArrayList<>(entries.size());
        for (ItemCatalog.PickupEntry entry : entries) {
            items.add(registerPickup(entry));
        }
        return List.copyOf(items);
    }
    private static List<LootContainerBlock> registerLootContainers(List<ItemCatalog.LootContainerEntry> entries) {
        ArrayList<LootContainerBlock> blocks = new ArrayList<>(entries.size());
        for (ItemCatalog.LootContainerEntry entry : entries) {
            blocks.add(registerLootContainer(entry));
        }
        return List.copyOf(blocks);
    }


    private static List<ThrowableImpulseItem> registerThrowableImpulses(List<ItemCatalog.ThrowableImpulseEntry> entries) {
        ArrayList<ThrowableImpulseItem> items = new ArrayList<>(entries.size());
        for (ItemCatalog.ThrowableImpulseEntry entry : entries) {
            items.add(registerThrowableImpulse(entry));
        }
        return List.copyOf(items);
    }

    private static List<Item> registerUtilityItems(List<ItemCatalog.UtilityEntry> entries) {
        ArrayList<Item> items = new ArrayList<>(entries.size());
        for (ItemCatalog.UtilityEntry entry : entries) {
            items.add(registerUtilityItem(entry));
        }
        return List.copyOf(items);
    }

    private static BuildPieceItem registerBuildPiece(ItemCatalog.BuildPieceEntry entry) {
        ResourceKey<Item> key = itemKey(entry.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new BuildPieceItem(
                        entry.pieceType(),
                        singleStackProperties(key),
                        clientItem(entry.clientItems().wood()),
                        clientItem(entry.clientItems().stone()),
                        clientItem(entry.clientItems().metal())
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

    private static ProjectileWeaponItem registerProjectileWeapon(ItemCatalog.WeaponEntry entry) {
        ItemCatalog.ProjectileEntry projectile = Objects.requireNonNull(entry.projectile(), entry.path() + ".projectile");
        WeaponDefinition definition = entry.definition();
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ProjectileWeaponItem(
                        definition,
                        projectileWeaponProperties(key, definition),
                        clientItem(entry.clientItem()),
                        projectile.projectileSpeed(),
                        projectile.inaccuracy()
                )
        );
    }

    private static ExplosiveProjectileWeaponItem registerExplosiveWeapon(ItemCatalog.WeaponEntry entry) {
        ItemCatalog.ExplosiveEntry explosive = Objects.requireNonNull(entry.explosive(), entry.path() + ".explosive");
        ExplosiveProjectileWeaponItem.Definition definition = new ExplosiveProjectileWeaponItem.Definition(
                entry.definition(),
                explosive.environmentDamage(),
                explosive.explosionRadiusBlocks(),
                explosive.fuseTicks(),
                explosive.armingDelayTicks(),
                explosive.explodeOnImpact(),
                explosive.proximityTriggered(),
                explosive.proximityRadiusBlocks(),
                explosive.impulseHorizontalStrength(),
                explosive.impulseVerticalStrength(),
                explosive.resetsFallDistance(),
                explosive.projectileSpeed(),
                explosive.inaccuracy(),
                explosive.impactExplosionDelayTicks(),
                explosive.gravityFreeProjectile(),
                explosive.evidenceNote()
        );
        ResourceKey<Item> key = itemKey(definition.weapon().path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ExplosiveProjectileWeaponItem(definition, explosiveWeaponProperties(key, definition), clientItem(entry.clientItem()))
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

    private static PickupItem registerPickup(ItemCatalog.PickupEntry entry) {
        ResourceKey<Item> key = itemKey(entry.path());
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new PickupItem(entry.displayName(), pickupPayload(entry.payload()), stackedProperties(key, entry.stackSize()), clientItem(entry.clientItem()))
        );
    }
    private static LootContainerBlock registerLootContainer(ItemCatalog.LootContainerEntry entry) {
        ResourceKey<Block> blockKey = blockKey(entry.path());
        Item clientItem = clientItem(entry.clientItem());
        LootDropTable drops = new LootDropTable(entry, WEAPONS, PROJECTILE_WEAPONS, EXPLOSIVE_WEAPONS, CONSUMABLES, PICKUPS);
        LootContainerBlock block = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new LootContainerBlock(entry, drops, lootContainerBlockProperties(blockKey))
        );
        ResourceKey<Item> itemKey = itemKey(entry.path());
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new LootContainerItem(block, entry.displayName(), lootContainerItemProperties(itemKey, clientItem))
        );
        return block;
    }
    private static BlockEntityType<LootContainerBlockEntity> registerLootContainerBlockEntity() {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("loot_container"),
                new BlockEntityType<>(LootContainerBlockEntity::new, Set.copyOf(LOOT_CONTAINERS))
        );
    }



    private static ThrowableImpulseItem registerThrowableImpulse(ItemCatalog.ThrowableImpulseEntry entry) {
        ResourceKey<Item> key = itemKey(entry.path());
        ThrowableImpulseItem.Definition definition = new ThrowableImpulseItem.Definition(
                entry.path(),
                entry.displayName(),
                entry.radius(),
                entry.horizontalStrength(),
                entry.verticalStrength(),
                entry.fuseTicks(),
                entry.cooldownTicks(),
                entry.resetsFallDistance(),
                entry.explosionPitch(),
                ChatFormatting.valueOf(entry.textColor())
        );
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ThrowableImpulseItem(definition, throwableProperties(key, definition), clientItem(entry.clientItem()))
        );
    }

    private static Item registerUtilityItem(ItemCatalog.UtilityEntry entry) {
        Item clientItem = clientItem(entry.clientItem());
        return switch (entry.kind()) {
            case PICKAXE -> registerPickaxe(entry.path(), clientItem);
            case GRAPPLER -> registerGrappler(new GrapplerItem.Definition(
                    entry.path(),
                    entry.displayName(),
                    entry.rangeBlocks(),
                    entry.pullSpeed(),
                    entry.upwardBoost(),
                    entry.cooldownTicks(),
                    requireSourceItemId(entry)
            ), clientItem);
            case LAUNCH_PAD -> registerLaunchPad(entry.path(), entry.displayName(), entry.cooldownTicks(), entry.redeployTicks(), clientItem);
            case EXPLOSIVE_THROWABLE -> registerExplosiveThrowable(new ExplosiveThrowableItem.Definition(
                    entry.path(),
                    entry.displayName(),
                    requireRarity(entry),
                    requireSourceItemId(entry),
                    entry.damage(),
                    entry.environmentDamage(),
                    entry.explosionRadiusBlocks(),
                    entry.fuseTicks(),
                    entry.stickDelayTicks(),
                    entry.cooldownTicks(),
                    entry.throwPower(),
                    entry.throwInaccuracy()
            ), clientItem);
            case BOUNCER -> registerBouncer(new BouncerItem.Definition(
                    entry.path(),
                    entry.displayName(),
                    requireRarity(entry),
                    requireSourceItemId(entry),
                    entry.cooldownTicks(),
                    entry.redeployTicks()
            ), clientItem);
            case RIFT_TO_GO -> registerRiftToGo(new RiftToGoItem.Definition(
                    entry.path(),
                    entry.displayName(),
                    requireRarity(entry),
                    requireSourceItemId(entry),
                    entry.cooldownTicks(),
                    entry.redeployTicks(),
                    entry.verticalTeleportBlocks(),
                    entry.horizontalLaunchSpeed(),
                    entry.verticalLaunchSpeed()
            ), clientItem);
            case PORT_A_FORT -> registerPortAFort(new PortAFortItem.Definition(
                    entry.path(),
                    entry.displayName(),
                    requireRarity(entry),
                    requireSourceItemId(entry),
                    entry.cooldownTicks(),
                    entry.radius(),
                    entry.height()
            ), clientItem);
        };
    }

    private static PickaxeItem registerPickaxe(String path, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(BuiltInRegistries.ITEM, key, new PickaxeItem(singleStackProperties(key), clientItem));
    }

    private static GrapplerItem registerGrappler(GrapplerItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(BuiltInRegistries.ITEM, key, new GrapplerItem(definition, singleStackProperties(key), clientItem));
    }

    private static LaunchPadItem registerLaunchPad(String path, String displayName, int cooldownTicks, long redeployTicks, Item clientItem) {
        ResourceKey<Item> key = itemKey(path);
        return Registry.register(BuiltInRegistries.ITEM, key, new LaunchPadItem(displayName, cooldownTicks, redeployTicks, singleStackProperties(key), clientItem));
    }

    private static ExplosiveThrowableItem registerExplosiveThrowable(ExplosiveThrowableItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(BuiltInRegistries.ITEM, key, new ExplosiveThrowableItem(definition, explosiveThrowableProperties(key, definition), clientItem));
    }

    private static BouncerItem registerBouncer(BouncerItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(BuiltInRegistries.ITEM, key, new BouncerItem(definition, bouncerProperties(key, definition), clientItem));
    }

    private static RiftToGoItem registerRiftToGo(RiftToGoItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(BuiltInRegistries.ITEM, key, new RiftToGoItem(definition, riftToGoProperties(key, definition), clientItem));
    }

    private static PortAFortItem registerPortAFort(PortAFortItem.Definition definition, Item clientItem) {
        ResourceKey<Item> key = itemKey(definition.path());
        return Registry.register(BuiltInRegistries.ITEM, key, new PortAFortItem(definition, portAFortProperties(key, definition), clientItem));
    }

    private static List<Item> combatItems() {
        ArrayList<Item> items = new ArrayList<>(WEAPONS.size() + PROJECTILE_WEAPONS.size() + EXPLOSIVE_WEAPONS.size() + THROWABLES.size() + UTILITY_ITEMS.size() + CONSUMABLES.size() + LOOT_CONTAINER_ITEMS.size());
        items.addAll(WEAPONS);
        items.addAll(PROJECTILE_WEAPONS);
        items.addAll(EXPLOSIVE_WEAPONS);
        items.addAll(THROWABLES);
        items.addAll(UTILITY_ITEMS);
        items.addAll(CONSUMABLES);
        items.addAll(LOOT_CONTAINER_ITEMS);
        return List.copyOf(items);
    }

    private static List<Item> allItems() {
        ArrayList<Item> items = new ArrayList<>(BUILD_PIECES.size() + COMBAT_ITEMS.size() + PICKUPS.size());
        items.addAll(BUILD_PIECES);
        items.addAll(COMBAT_ITEMS);
        items.addAll(PICKUPS);
        return List.copyOf(items);
    }

    private static PickupPayload pickupPayload(ItemCatalog.PickupPayloadEntry payload) {
        return switch (payload.kind()) {
            case MATERIAL -> PickupPayload.material(Objects.requireNonNull(payload.material(), "material"), payload.amount());
            case AMMO -> PickupPayload.ammo(Objects.requireNonNull(payload.ammoType(), "ammoType"), payload.amount());
            case GOLD -> PickupPayload.gold(payload.amount());
        };
    }

    private static FortniteRarity requireRarity(ItemCatalog.UtilityEntry entry) {
        return Objects.requireNonNull(entry.rarity(), entry.path() + ".rarity");
    }

    private static String requireSourceItemId(ItemCatalog.UtilityEntry entry) {
        return Objects.requireNonNull(entry.sourceItemId(), entry.path() + ".sourceItemId");
    }

    private static <T extends Item> T utilityItem(String path, Class<T> type) {
        for (Item item : UTILITY_ITEMS) {
            if (BuiltInRegistries.ITEM.getKey(item).getPath().equals(path)) {
                return type.cast(item);
            }
        }
        throw new IllegalStateException("Missing utility item " + path);
    }

    private static BuildPieceItem buildPiece(String path) {
        for (BuildPieceItem item : BUILD_PIECES) {
            if (BuiltInRegistries.ITEM.getKey(item).getPath().equals(path)) {
                return item;
            }
        }
        throw new IllegalStateException("Missing build piece " + path);
    }
    private static LootContainerBlock lootContainer(String path) {
        for (LootContainerBlock block : LOOT_CONTAINERS) {
            if (BuiltInRegistries.BLOCK.getKey(block).getPath().equals(path)) {
                return block;
            }
        }
        throw new IllegalStateException("Missing loot container " + path);
    }

    private static List<Item> lootContainerItems() {
        ArrayList<Item> items = new ArrayList<>(LOOT_CONTAINERS.size());
        for (LootContainerBlock block : LOOT_CONTAINERS) {
            items.add(block.asItem());
        }
        return List.copyOf(items);
    }


    private static Item clientItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid client item id: " + id);
        }
        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        if (item == null) {
            throw new IllegalArgumentException("Unknown client item id: " + id);
        }
        return item;
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

    private static Item.Properties explosiveWeaponProperties(ResourceKey<Item> key, ExplosiveProjectileWeaponItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.USE_COOLDOWN, ExplosiveProjectileWeaponItem.cooldownComponent(definition));
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

    private static Item.Properties explosiveThrowableProperties(ResourceKey<Item> key, ExplosiveThrowableItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(6)
                .component(DataComponents.USE_COOLDOWN, ExplosiveThrowableItem.cooldownComponent(definition));
    }

    private static Item.Properties bouncerProperties(ResourceKey<Item> key, BouncerItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(3)
                .component(DataComponents.USE_COOLDOWN, BouncerItem.cooldownComponent(definition));
    }

    private static Item.Properties riftToGoProperties(ResourceKey<Item> key, RiftToGoItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(2)
                .component(DataComponents.USE_COOLDOWN, RiftToGoItem.cooldownComponent(definition));
    }

    private static Item.Properties portAFortProperties(ResourceKey<Item> key, PortAFortItem.Definition definition) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(2)
                .component(DataComponents.USE_COOLDOWN, PortAFortItem.cooldownComponent(definition));
    }

    private static BlockBehaviour.Properties lootContainerBlockProperties(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noLootTable()
                .setId(key);
    }

    private static Item.Properties lootContainerItemProperties(ResourceKey<Item> key, Item clientItem) {
        return singleStackProperties(key)
                .component(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(clientItem));
    }

    private static Item.Properties singleStackProperties(ResourceKey<Item> key) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(1);
    }

    private static Item.Properties stackedProperties(ResourceKey<Item> key, int stackSize) {
        return new Item.Properties()
                .setId(key)
                .stacksTo(stackSize);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), id(path));
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }
}
