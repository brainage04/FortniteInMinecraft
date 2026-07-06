package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

final class LootDropTable {
    private final ItemCatalog.LootContainerEntry definition;
    private final EnumMap<FortniteRarity, List<Item>> gunsByRarity = new EnumMap<>(FortniteRarity.class);
    private final EnumMap<AmmoType, PickupItem> ammoPickupsByType = new EnumMap<>(AmmoType.class);
    private final List<ConsumableItem> consumables = new ArrayList<>();

    LootDropTable(
            ItemCatalog.LootContainerEntry definition,
            List<WeaponItem> hitscanWeapons,
            List<ProjectileWeaponItem> projectileWeapons,
            List<ExplosiveProjectileWeaponItem> explosiveWeapons,
            List<ConsumableItem> catalogConsumables,
            List<PickupItem> catalogPickups
    ) {
        this.definition = Objects.requireNonNull(definition, "definition");
        addWeapons(hitscanWeapons);
        addWeapons(projectileWeapons);
        addWeapons(explosiveWeapons);
        for (ItemCatalog.LootPathEntry entry : definition.loot().consumables()) {
            ConsumableItem item = consumable(entry.path(), catalogConsumables);
            if (entry.weight() > 0.0D) {
                consumables.add(item);
            }
        }
        for (PickupItem item : catalogPickups) {
            PickupPayload payload = item.payload();
            if (payload.ammoType() != null) {
                ammoPickupsByType.put(payload.ammoType(), item);
            }
        }
    }

    List<ItemStack> roll(ItemCatalog.LootContainerKind kind, RandomSource random) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(random, "random");
        if (kind == ItemCatalog.LootContainerKind.AMMO_BOX) {
            return List.of(ammoStack(selectAmmoType(random)));
        }

        Item gun = selectGun(random);
        List<ItemStack> stacks = new ArrayList<>(3);
        stacks.add(new ItemStack(gun));
        stacks.add(ammoStack(ammoTypeFor(gun)));
        if (!consumables.isEmpty()) {
            stacks.add(new ItemStack(selectConsumable(random)));
        }
        return List.copyOf(stacks);
    }

    private Item selectGun(RandomSource random) {
        FortniteRarity rarity = selectRarity(random);
        List<Item> pool = gunsByRarity.get(rarity);
        if (pool == null || pool.isEmpty()) {
            ArrayList<Item> fallback = new ArrayList<>();
            for (List<Item> rarityPool : gunsByRarity.values()) {
                fallback.addAll(rarityPool);
            }
            if (fallback.isEmpty()) {
                throw new IllegalStateException("Loot container " + definition.path() + " has no guns in the current catalog");
            }
            return fallback.get(random.nextInt(fallback.size()));
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private FortniteRarity selectRarity(RandomSource random) {
        FortniteRarity fallback = null;
        double total = 0.0D;
        for (ItemCatalog.LootRarityWeight weight : definition.loot().weaponRarityWeights()) {
            if (weight.weight() <= 0.0D) {
                continue;
            }
            if (fallback == null) {
                fallback = weight.rarity();
            }
            total += weight.weight();
        }
        if (fallback == null || total <= 0.0D) {
            throw new IllegalStateException("Loot container " + definition.path() + " has no positive weapon rarity weights");
        }
        double choice = random.nextDouble() * total;
        for (ItemCatalog.LootRarityWeight weight : definition.loot().weaponRarityWeights()) {
            if (weight.weight() <= 0.0D) {
                continue;
            }
            choice -= weight.weight();
            if (choice <= 0.0D) {
                return weight.rarity();
            }
        }
        return fallback;
    }

    private ConsumableItem selectConsumable(RandomSource random) {
        ItemCatalog.LootPathEntry fallback = null;
        double total = 0.0D;
        for (ItemCatalog.LootPathEntry entry : definition.loot().consumables()) {
            if (entry.weight() <= 0.0D) {
                continue;
            }
            if (fallback == null) {
                fallback = entry;
            }
            total += entry.weight();
        }
        double choice = random.nextDouble() * total;
        for (ItemCatalog.LootPathEntry entry : definition.loot().consumables()) {
            if (entry.weight() <= 0.0D) {
                continue;
            }
            choice -= entry.weight();
            if (choice <= 0.0D) {
                return consumable(entry.path(), consumables);
            }
        }
        return consumable(Objects.requireNonNull(fallback, "fallback").path(), consumables);
    }

    private AmmoType selectAmmoType(RandomSource random) {
        AmmoType fallback = null;
        double total = 0.0D;
        for (ItemCatalog.LootAmmoWeight weight : definition.loot().ammoTypeWeights()) {
            if (weight.weight() <= 0.0D || !ammoPickupsByType.containsKey(weight.ammoType())) {
                continue;
            }
            if (fallback == null) {
                fallback = weight.ammoType();
            }
            total += weight.weight();
        }
        if (fallback == null || total <= 0.0D) {
            throw new IllegalStateException("Loot container " + definition.path() + " has no positive ammo weights backed by catalog pickups");
        }
        double choice = random.nextDouble() * total;
        for (ItemCatalog.LootAmmoWeight weight : definition.loot().ammoTypeWeights()) {
            if (weight.weight() <= 0.0D || !ammoPickupsByType.containsKey(weight.ammoType())) {
                continue;
            }
            choice -= weight.weight();
            if (choice <= 0.0D) {
                return weight.ammoType();
            }
        }
        return fallback;
    }

    private ItemStack ammoStack(AmmoType type) {
        PickupItem item = ammoPickupsByType.get(type);
        if (item == null) {
            throw new IllegalStateException("No catalog ammo pickup for " + type);
        }
        return new ItemStack(item);
    }

    private AmmoType ammoTypeFor(Item gun) {
        WeaponDefinition definition = weaponDefinition(gun);
        return switch (definition.category()) {
            case ASSAULT_RIFLE -> AmmoType.MEDIUM;
            case SHOTGUN -> AmmoType.SHELLS;
            case PISTOL, SMG -> AmmoType.LIGHT;
            case SNIPER -> AmmoType.HEAVY;
            case EXPLOSIVE -> AmmoType.ROCKETS;
        };
    }

    private WeaponDefinition weaponDefinition(Item gun) {
        if (gun instanceof WeaponItem item) {
            return item.definition();
        }
        if (gun instanceof ProjectileWeaponItem item) {
            return item.definition();
        }
        if (gun instanceof ExplosiveProjectileWeaponItem item) {
            return item.definition();
        }
        throw new IllegalArgumentException("Item is not a gun: " + gun);
    }

    private <T extends Item> void addWeapons(List<T> weapons) {
        for (Item item : weapons) {
            WeaponDefinition definition = weaponDefinition(item);
            gunsByRarity.computeIfAbsent(definition.rarity(), ignored -> new ArrayList<>()).add(item);
        }
    }

    private static ConsumableItem consumable(String path, List<ConsumableItem> items) {
        for (ConsumableItem item : items) {
            if (BuiltInRegistries.ITEM.getKey(item).getPath().equals(path)) {
                return item;
            }
        }
        throw new IllegalStateException("Missing catalog consumable " + path);
    }
}
