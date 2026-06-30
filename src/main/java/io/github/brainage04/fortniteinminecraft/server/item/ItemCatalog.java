package io.github.brainage04.fortniteinminecraft.server.item;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.brainage04.fortniteinminecraft.core.item.ConsumableDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponCategory;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

final class ItemCatalog {
    private static final String RESOURCE_PATH = "/data/fortniteinminecraft/item_catalog.json";
    private static final Gson GSON = new Gson();

    private ItemCatalog() {
    }

    static Catalog load() {
        try (InputStream stream = ItemCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing item catalog resource: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, Catalog.class);
            }
        } catch (JsonSyntaxException exception) {
            throw new IllegalStateException("Invalid item catalog resource: " + RESOURCE_PATH, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read item catalog resource: " + RESOURCE_PATH, exception);
        }
    }

    record Catalog(
            int version,
            List<String> generatedFrom,
            List<BuildPieceEntry> buildPieces,
            List<WeaponEntry> weapons,
            List<ConsumableEntry> consumables,
            List<PickupEntry> pickups,
            List<ThrowableImpulseEntry> throwableImpulses,
            List<UtilityEntry> utilities
    ) {
        Catalog {
            generatedFrom = copy(generatedFrom, "generatedFrom");
            buildPieces = copy(buildPieces, "buildPieces");
            weapons = copy(weapons, "weapons");
            consumables = copy(consumables, "consumables");
            pickups = copy(pickups, "pickups");
            throwableImpulses = copy(throwableImpulses, "throwableImpulses");
            utilities = copy(utilities, "utilities");
        }
    }

    record BuildPieceEntry(String path, PieceType pieceType, ClientItems clientItems) {
        BuildPieceEntry {
            path = requireText(path, "path");
            Objects.requireNonNull(pieceType, "pieceType");
            Objects.requireNonNull(clientItems, "clientItems");
        }
    }

    record ClientItems(String wood, String stone, String metal) {
        ClientItems {
            wood = requireText(wood, "wood");
            stone = requireText(stone, "stone");
            metal = requireText(metal, "metal");
        }
    }

    enum WeaponKind {
        HITSCAN,
        PROJECTILE,
        EXPLOSIVE
    }

    record WeaponEntry(
            WeaponKind kind,
            String path,
            String displayName,
            WeaponCategory category,
            FortniteRarity rarity,
            WeaponStatsEntry stats,
            String sourceItemId,
            String sourceStatRow,
            String clientItem,
            ProjectileEntry projectile,
            ExplosiveEntry explosive
    ) {
        WeaponEntry {
            Objects.requireNonNull(kind, "kind");
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(rarity, "rarity");
            Objects.requireNonNull(stats, "stats");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            sourceStatRow = requireText(sourceStatRow, "sourceStatRow");
            clientItem = requireText(clientItem, "clientItem");
        }

        WeaponDefinition definition() {
            return new WeaponDefinition(path, displayName, category, rarity, stats.toStats(), sourceItemId, sourceStatRow);
        }
    }

    record WeaponStatsEntry(
            double damage,
            double criticalMultiplier,
            int magazineSize,
            double fireRatePerSecond,
            double reloadSeconds,
            int pellets,
            double rangeBlocks,
            double maxDamagePerShot
    ) {
        WeaponStats toStats() {
            return new WeaponStats(
                    damage,
                    criticalMultiplier,
                    magazineSize,
                    fireRatePerSecond,
                    reloadSeconds,
                    pellets,
                    rangeBlocks,
                    maxDamagePerShot
            );
        }
    }

    record ProjectileEntry(float projectileSpeed, float inaccuracy) {
    }

    record ExplosiveEntry(
            int environmentDamage,
            double explosionRadiusBlocks,
            int fuseTicks,
            int armingDelayTicks,
            boolean explodeOnImpact,
            boolean proximityTriggered,
            double proximityRadiusBlocks,
            double impulseHorizontalStrength,
            double impulseVerticalStrength,
            boolean resetsFallDistance,
            float projectileSpeed,
            float inaccuracy,
            long impactExplosionDelayTicks,
            boolean gravityFreeProjectile,
            String evidenceNote
    ) {
        ExplosiveEntry {
            evidenceNote = Objects.requireNonNullElse(evidenceNote, "");
        }
    }

    record ConsumableEntry(
            String path,
            String displayName,
            FortniteRarity rarity,
            double castSeconds,
            int healthRestore,
            int healthCap,
            int shieldRestore,
            int shieldCap,
            boolean movementLocked,
            String sourceItemId,
            int effectiveRestore,
            String clientItem
    ) {
        ConsumableEntry {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(rarity, "rarity");
            sourceItemId = requireText(sourceItemId, "sourceItemId");
            clientItem = requireText(clientItem, "clientItem");
        }

        ConsumableDefinition definition() {
            return new ConsumableDefinition(
                    path,
                    displayName,
                    rarity,
                    castSeconds,
                    healthRestore,
                    healthCap,
                    shieldRestore,
                    shieldCap,
                    movementLocked,
                    sourceItemId,
                    effectiveRestore
            );
        }
    }

    record PickupEntry(String path, String displayName, PickupPayloadEntry payload, String clientItem, int stackSize) {
        PickupEntry {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(payload, "payload");
            clientItem = requireText(clientItem, "clientItem");
            if (stackSize <= 0) {
                throw new IllegalArgumentException("stackSize must be positive");
            }
        }
    }

    enum PickupPayloadKind {
        MATERIAL,
        AMMO,
        GOLD
    }

    record PickupPayloadEntry(PickupPayloadKind kind, MaterialType material, AmmoType ammoType, int amount) {
        PickupPayloadEntry {
            Objects.requireNonNull(kind, "kind");
        }
    }

    record ThrowableImpulseEntry(
            String path,
            String displayName,
            double radius,
            double horizontalStrength,
            double verticalStrength,
            int fuseTicks,
            int cooldownTicks,
            boolean resetsFallDistance,
            float explosionPitch,
            String textColor,
            String clientItem
    ) {
        ThrowableImpulseEntry {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            textColor = requireText(textColor, "textColor");
            clientItem = requireText(clientItem, "clientItem");
        }
    }

    enum UtilityKind {
        PICKAXE,
        GRAPPLER,
        LAUNCH_PAD,
        EXPLOSIVE_THROWABLE,
        BOUNCER,
        RIFT_TO_GO,
        PORT_A_FORT
    }

    record UtilityEntry(
            UtilityKind kind,
            String path,
            String displayName,
            FortniteRarity rarity,
            String sourceItemId,
            String clientItem,
            int cooldownTicks,
            long redeployTicks,
            double rangeBlocks,
            double pullSpeed,
            double upwardBoost,
            double damage,
            int environmentDamage,
            double explosionRadiusBlocks,
            int fuseTicks,
            int stickDelayTicks,
            float throwPower,
            float throwInaccuracy,
            int radius,
            int height,
            double verticalTeleportBlocks,
            double horizontalLaunchSpeed,
            double verticalLaunchSpeed
    ) {
        UtilityEntry {
            Objects.requireNonNull(kind, "kind");
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            clientItem = requireText(clientItem, "clientItem");
        }
    }

    private static <T> List<T> copy(List<T> entries, String name) {
        return List.copyOf(Objects.requireNonNull(entries, name));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
