package com.github.brainage04.fortnite_in_minecraft.manager;

import net.minecraft.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// used to nullify fall damage after a player has used a shockwave grenade
public class FallDamageManager {
    private static final Set<UUID> immunePlayers = new HashSet<>();

    public static void grantImmunity(LivingEntity entity) {
        immunePlayers.add(entity.getUuid());
    }

    public static boolean consumeImmunity(LivingEntity entity) {
        return immunePlayers.remove(entity.getUuid());
    }
}