package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
    private static final ItemCatalog.Catalog CATALOG = ItemCatalog.load();
    private static final Map<String, LootContainerBlock> LOOT_CONTAINERS_BY_PATH = new HashMap<>();

    public static final List<LootContainerBlock> LOOT_CONTAINERS = registerLootContainers(CATALOG.lootContainers());
    public static final LootContainerBlock LOOT_CHEST = lootContainer("loot_chest");
    public static final LootContainerBlock AMMO_BOX = lootContainer("ammo_box");

    private ModBlocks() {
    }

    public static void bootstrap() {
        if (LOOT_CONTAINERS.size() != CATALOG.lootContainers().size()) {
            throw new IllegalStateException("Not all loot container blocks were registered");
        }
    }

    private static List<LootContainerBlock> registerLootContainers(List<ItemCatalog.LootContainerEntry> entries) {
        ArrayList<LootContainerBlock> blocks = new ArrayList<>(entries.size());
        for (ItemCatalog.LootContainerEntry entry : entries) {
            ResourceKey<Block> key = blockKey(entry.path());
            LootContainerBlock block = FortniteInMinecraft.platform().registerBlock(key, new LootContainerBlock(
                    entry,
                    () -> ModItems.lootDropTable(entry.path()),
                    lootContainerBlockProperties(key)
            ));
            if (LOOT_CONTAINERS_BY_PATH.putIfAbsent(entry.path(), block) != null) {
                throw new IllegalStateException("Duplicate loot container path " + entry.path());
            }
            blocks.add(block);
        }
        return List.copyOf(blocks);
    }

    private static LootContainerBlock lootContainer(String path) {
        LootContainerBlock block = LOOT_CONTAINERS_BY_PATH.get(path);
        if (block == null) {
            throw new IllegalStateException("Missing loot container " + path);
        }
        return block;
    }

    private static BlockBehaviour.Properties lootContainerBlockProperties(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noLootTable()
                .setId(key);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path));
    }
}
