package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<LootContainerBlockEntity> LOOT_CONTAINER =
            FortniteInMinecraft.platform().registerBlockEntityType(
                    Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, "loot_container"),
                    new BlockEntityType<>(LootContainerBlockEntity::new, Set.copyOf(ModBlocks.LOOT_CONTAINERS))
            );

    private ModBlockEntities() {
    }

    public static void bootstrap() {
        if (LOOT_CONTAINER == null) {
            throw new IllegalStateException("Loot container block entity type was not registered");
        }
    }
}
