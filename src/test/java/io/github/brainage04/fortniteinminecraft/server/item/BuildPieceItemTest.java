package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class BuildPieceItemTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void clientItemAppearanceFollowsSharedMaterialPalette() {
        BuildPieceItem item = new BuildPieceItem(
                PieceType.STAIR,
                properties("test_build_stair"),
                Items.OAK_STAIRS,
                Items.COBBLESTONE_STAIRS,
                Items.CUT_COPPER_STAIRS.weathering().unaffected()
        );

        assertSame(Items.OAK_STAIRS, item.clientItemFor(MaterialType.WOOD));
        assertSame(Items.COBBLESTONE_STAIRS, item.clientItemFor(MaterialType.STONE));
        assertSame(Items.CUT_COPPER_STAIRS.weathering().unaffected(), item.clientItemFor(MaterialType.METAL));
    }

    private static Item.Properties properties(String path) {
        ResourceKey<Item> key = ResourceKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath("fortniteinminecraft_test", path)
        );
        return new Item.Properties().setId(key).stacksTo(1);
    }
}
