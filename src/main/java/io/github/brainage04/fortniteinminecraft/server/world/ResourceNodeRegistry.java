package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceNodeRegistry {
    private static final Map<Key, Node> NODES = new HashMap<>();

    private ResourceNodeRegistry() {
    }

    public static void register(String dimension, Collection<BlockPos> positions, MaterialType material, int health, int resourceReward) {
        dimension = requireText(dimension, "dimension");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(material, "material");
        if (health <= 0) {
            throw new IllegalArgumentException("health must be positive");
        }
        if (resourceReward < 0) {
            throw new IllegalArgumentException("resourceReward cannot be negative");
        }
        for (BlockPos pos : positions) {
            NODES.put(new Key(dimension, pos.immutable()), new Node(material, health, resourceReward));
        }
    }

    public static ResourceHit hit(String dimension, BlockPos pos, int damage) {
        dimension = requireText(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        Key key = new Key(dimension, pos.immutable());
        Node node = NODES.get(key);
        if (node == null) {
            return ResourceHit.miss();
        }
        int remaining = Math.max(0, node.health() - damage);
        if (remaining <= 0) {
            NODES.remove(key);
            return new ResourceHit(true, true, node.material(), node.resourceReward(), 0);
        }
        NODES.put(key, new Node(node.material(), remaining, node.resourceReward()));
        return new ResourceHit(true, false, node.material(), node.resourceReward(), remaining);
    }

    public static boolean tracked(String dimension, BlockPos pos) {
        dimension = requireText(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        return NODES.containsKey(new Key(dimension, pos.immutable()));
    }

    public static void remove(String dimension, BlockPos pos) {
        dimension = requireText(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        NODES.remove(new Key(dimension, pos.immutable()));
    }

    public static void clearAll() {
        NODES.clear();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }

    public record ResourceHit(boolean hit, boolean destroyed, MaterialType material, int resourceReward, int remainingHealth) {
        private static ResourceHit miss() {
            return new ResourceHit(false, false, null, 0, 0);
        }
    }

    private record Key(String dimension, BlockPos pos) {
    }

    private record Node(MaterialType material, int health, int resourceReward) {
    }
}
