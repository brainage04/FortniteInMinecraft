package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TerrainResourceHarvest {
    public static final int WOOD_HEALTH = 100;
    public static final int STONE_HEALTH = 120;
    public static final int METAL_HEALTH = 150;
    public static final int RESOURCE_REWARD = 10;
    private static final int SURFACE_SCAN_DEPTH = 24;
    private static final int SURFACE_SCAN_ABOVE = 16;
    private static final Map<Key, DamageState> DAMAGED_TERRAIN = new HashMap<>();

    private static final ResourceDefinition WOOD = new ResourceDefinition(MaterialType.WOOD, WOOD_HEALTH, RESOURCE_REWARD);
    private static final ResourceDefinition STONE = new ResourceDefinition(MaterialType.STONE, STONE_HEALTH, RESOURCE_REWARD);
    private static final ResourceDefinition METAL = new ResourceDefinition(MaterialType.METAL, METAL_HEALTH, RESOURCE_REWARD);

    private TerrainResourceHarvest() {
    }

    public static ResourceDefinition resourceFor(BlockState state) {
        Objects.requireNonNull(state, "state");
        if (state.isAir()) {
            return null;
        }
        if (isLogLike(state)) {
            return WOOD;
        }
        if (isCopperLike(state)) {
            return METAL;
        }
        if (isStoneLike(state)) {
            return STONE;
        }
        return null;
    }

    public static boolean isHarvestable(BlockState state) {
        return resourceFor(state) != null;
    }

    public static HarvestResult hit(ServerLevel level, BlockPos pos, int damage) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        String dimension = level.dimension().identifier().toString();
        Key key = new Key(dimension, pos.immutable());
        ResourceDefinition resource = resourceFor(level.getBlockState(pos));
        if (resource == null) {
            DAMAGED_TERRAIN.remove(key);
            return HarvestResult.miss();
        }

        int beforeHealth = remainingHealth(key, resource);
        int remainingHealth = Math.max(0, beforeHealth - damage);
        if (remainingHealth <= 0) {
            DAMAGED_TERRAIN.remove(key);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return new HarvestResult(true, true, resource.material(), resource.resourceReward(), 0, resource.maxHealth());
        }

        DAMAGED_TERRAIN.put(key, new DamageState(resource.material(), remainingHealth, resource.maxHealth()));
        return new HarvestResult(true, false, resource.material(), resource.resourceReward(), remainingHealth, resource.maxHealth());
    }

    public static int remainingHealth(String dimension, BlockPos pos, ResourceDefinition resource) {
        dimension = requireText(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(resource, "resource");
        return remainingHealth(new Key(dimension, pos.immutable()), resource);
    }

    public static Optional<HarvestableBlock> nearest(ServerLevel level, BlockPos origin, int radius) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");
        if (radius < 0) {
            throw new IllegalArgumentException("radius cannot be negative");
        }
        double maxDistanceSqr = (double) radius * radius;
        HarvestableBlock best = null;
        double bestDistanceSqr = Double.POSITIVE_INFINITY;
        for (HarvestableBlock block : harvestablesNear(level, origin, radius, Integer.MAX_VALUE)) {
            double distanceSqr = block.pos().distSqr(origin);
            if (distanceSqr <= maxDistanceSqr && distanceSqr < bestDistanceSqr) {
                best = block;
                bestDistanceSqr = distanceSqr;
            }
        }
        return Optional.ofNullable(best);
    }

    public static List<HarvestableBlock> harvestablesNear(ServerLevel level, BlockPos origin, int radius, int limit) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");
        if (radius < 0) {
            throw new IllegalArgumentException("radius cannot be negative");
        }
        if (limit <= 0) {
            return List.of();
        }

        ArrayList<HarvestableBlock> found = new ArrayList<>();
        int minChunkX = Math.floorDiv(origin.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + radius, 16);
        double maxDistanceSqr = (double) radius * radius;
        String dimension = level.dimension().identifier().toString();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                collectChunkHarvestables(level, level.getChunk(chunkX, chunkZ), dimension, origin, maxDistanceSqr, found, limit);
                if (found.size() >= limit) {
                    found.sort(Comparator.comparingDouble(block -> block.pos().distSqr(origin)));
                    return List.copyOf(found.subList(0, Math.min(found.size(), limit)));
                }
            }
        }
        found.sort(Comparator.comparingDouble(block -> block.pos().distSqr(origin)));
        return List.copyOf(found);
    }

    public static void clearDamage(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        DAMAGED_TERRAIN.remove(new Key(level.dimension().identifier().toString(), pos.immutable()));
    }

    public static void clearAll() {
        DAMAGED_TERRAIN.clear();
    }

    private static void collectChunkHarvestables(
            ServerLevel level,
            LevelChunk chunk,
            String dimension,
            BlockPos origin,
            double maxDistanceSqr,
            ArrayList<HarvestableBlock> found,
            int limit
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int minY = Math.max(level.getMinY(), origin.getY() - (int) Math.ceil(Math.sqrt(maxDistanceSqr)));
        int maxY = Math.min(level.getMaxY() - 1, origin.getY() + (int) Math.ceil(Math.sqrt(maxDistanceSqr)));
        for (int localX = 0; localX < 16; localX++) {
            int x = chunkPos.getBlockX(localX);
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = chunkPos.getBlockZ(localZ);
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                int scanTop = Math.min(maxY, surfaceY + SURFACE_SCAN_ABOVE);
                int scanBottom = Math.max(minY, surfaceY - SURFACE_SCAN_DEPTH);
                for (int y = scanTop; y >= scanBottom; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.distSqr(origin) > maxDistanceSqr) {
                        continue;
                    }
                    ResourceDefinition resource = resourceFor(chunk.getBlockState(pos));
                    if (resource == null) {
                        continue;
                    }
                    found.add(new HarvestableBlock(pos.immutable(), resource.material(), remainingHealth(dimension, pos, resource), resource.maxHealth()));
                    if (found.size() >= limit) {
                        return;
                    }
                }
            }
        }
    }

    private static int remainingHealth(Key key, ResourceDefinition resource) {
        DamageState damage = DAMAGED_TERRAIN.get(key);
        if (damage == null || damage.material() != resource.material() || damage.maxHealth() != resource.maxHealth()) {
            return resource.maxHealth();
        }
        return Math.min(damage.remainingHealth(), resource.maxHealth());
    }

    private static boolean isLogLike(BlockState state) {
        return isTagged(state, BlockTags.LOGS)
                || state.is(Blocks.OAK_LOG)
                || state.is(Blocks.SPRUCE_LOG)
                || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.JUNGLE_LOG)
                || state.is(Blocks.ACACIA_LOG)
                || state.is(Blocks.DARK_OAK_LOG)
                || state.is(Blocks.MANGROVE_LOG)
                || state.is(Blocks.CHERRY_LOG)
                || state.is(Blocks.PALE_OAK_LOG)
                || state.is(Blocks.CRIMSON_STEM)
                || state.is(Blocks.WARPED_STEM);
    }

    private static boolean isCopperLike(BlockState state) {
        return isTagged(state, BlockTags.COPPER)
                || isTagged(state, BlockTags.COPPER_ORES)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.RAW_COPPER_BLOCK)
                || state.is(Blocks.COPPER_BLOCK.weathering().unaffected())
                || state.is(Blocks.COPPER_BLOCK.waxed().unaffected())
                || state.is(Blocks.CUT_COPPER.weathering().unaffected())
                || state.is(Blocks.CUT_COPPER.waxed().unaffected());
    }

    private static boolean isStoneLike(BlockState state) {
        return isTagged(state, BlockTags.BASE_STONE_OVERWORLD)
                || isTagged(state, BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.TUFF_BRICKS);
    }

    private static boolean isTagged(BlockState state, TagKey<Block> tag) {
        try {
            return state.is(tag);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    public record ResourceDefinition(MaterialType material, int maxHealth, int resourceReward) {
        public ResourceDefinition {
            Objects.requireNonNull(material, "material");
            if (maxHealth <= 0) {
                throw new IllegalArgumentException("maxHealth must be positive");
            }
            if (resourceReward < 0) {
                throw new IllegalArgumentException("resourceReward cannot be negative");
            }
        }
    }

    public record HarvestResult(
            boolean hit,
            boolean destroyed,
            MaterialType material,
            int resourceReward,
            int remainingHealth,
            int maxHealth
    ) {
        private static HarvestResult miss() {
            return new HarvestResult(false, false, null, 0, 0, 0);
        }
    }

    public record HarvestableBlock(BlockPos pos, MaterialType material, int health, int maxHealth) {
        public HarvestableBlock {
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(material, "material");
            if (health < 0) {
                throw new IllegalArgumentException("health cannot be negative");
            }
            if (maxHealth <= 0) {
                throw new IllegalArgumentException("maxHealth must be positive");
            }
        }
    }

    private record DamageState(MaterialType material, int remainingHealth, int maxHealth) {
    }

    private record Key(String dimension, BlockPos pos) {
    }
}
