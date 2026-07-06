package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Objects;

public final class BuildVisualBlocks {
    public static final Block HOLOGRAM_WOOD = hologramBlock("build_hologram_wood", DyeColor.LIGHT_BLUE, Blocks.OAK_PLANKS);
    public static final Block HOLOGRAM_STONE = hologramBlock("build_hologram_stone", DyeColor.LIGHT_BLUE, Blocks.STONE_BRICKS);
    public static final Block HOLOGRAM_METAL = hologramBlock("build_hologram_metal", DyeColor.LIGHT_BLUE, Blocks.COPPER_BLOCK.waxed().unaffected());

    public static final Block INVALID_HOLOGRAM_WOOD = hologramBlock("build_hologram_invalid_wood", DyeColor.RED, Blocks.OAK_PLANKS);
    public static final Block INVALID_HOLOGRAM_STONE = hologramBlock("build_hologram_invalid_stone", DyeColor.RED, Blocks.STONE_BRICKS);
    public static final Block INVALID_HOLOGRAM_METAL = hologramBlock("build_hologram_invalid_metal", DyeColor.RED, Blocks.COPPER_BLOCK.waxed().unaffected());

    private static final EnumMap<MaterialType, Block> HOLOGRAMS = new EnumMap<>(MaterialType.class);
    private static final EnumMap<MaterialType, Block> INVALID_HOLOGRAMS = new EnumMap<>(MaterialType.class);
    private static boolean registered;

    static {
        HOLOGRAMS.put(MaterialType.WOOD, HOLOGRAM_WOOD);
        HOLOGRAMS.put(MaterialType.STONE, HOLOGRAM_STONE);
        HOLOGRAMS.put(MaterialType.METAL, HOLOGRAM_METAL);
        INVALID_HOLOGRAMS.put(MaterialType.WOOD, INVALID_HOLOGRAM_WOOD);
        INVALID_HOLOGRAMS.put(MaterialType.STONE, INVALID_HOLOGRAM_STONE);
        INVALID_HOLOGRAMS.put(MaterialType.METAL, INVALID_HOLOGRAM_METAL);
    }

    private BuildVisualBlocks() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }
        register("build_hologram_wood", HOLOGRAM_WOOD);
        register("build_hologram_stone", HOLOGRAM_STONE);
        register("build_hologram_metal", HOLOGRAM_METAL);
        register("build_hologram_invalid_wood", INVALID_HOLOGRAM_WOOD);
        register("build_hologram_invalid_stone", INVALID_HOLOGRAM_STONE);
        register("build_hologram_invalid_metal", INVALID_HOLOGRAM_METAL);
        registered = true;
    }

    public static Block hologramBlock(MaterialType material) {
        return HOLOGRAMS.get(Objects.requireNonNull(material, "material"));
    }

    public static BlockState hologramState(MaterialType material) {
        return hologramBlock(material).defaultBlockState();
    }

    public static BlockState previewState(MaterialType material, boolean valid) {
        EnumMap<MaterialType, Block> blocks = valid ? HOLOGRAMS : INVALID_HOLOGRAMS;
        return blocks.get(Objects.requireNonNull(material, "material")).defaultBlockState();
    }

    private static Block hologramBlock(String path, DyeColor color, Block baseBlock) {
        ResourceKey<Block> key = blockKey(path);
        return new StainedGlassBlock(color, BlockBehaviour.Properties.ofFullCopy(baseBlock)
                .mapColor(color)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .noLootTable()
                .setId(key));
    }

    private static Block register(String path, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, blockKey(path), block);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path));
    }
}
