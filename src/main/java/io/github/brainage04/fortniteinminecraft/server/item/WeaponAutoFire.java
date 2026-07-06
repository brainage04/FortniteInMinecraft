package io.github.brainage04.fortniteinminecraft.server.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WeaponAutoFire {
    static final long HELD_USE_GRACE_TICKS = 6L;
    private static final Map<UUID, ActiveInput> ACTIVE_INPUTS = new HashMap<>();
    private static final Map<UUID, PendingWeaponBurst> PENDING_WEAPON_BURSTS = new HashMap<>();
    private static final Map<UUID, PendingExplosiveBurst> PENDING_EXPLOSIVE_BURSTS = new HashMap<>();
    private static boolean registered;

    private WeaponAutoFire() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(WeaponAutoFire::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ACTIVE_INPUTS.remove(handler.player.getUUID());
            PENDING_WEAPON_BURSTS.remove(handler.player.getUUID());
            PENDING_EXPLOSIVE_BURSTS.remove(handler.player.getUUID());
        });
        registered = true;
    }

    public static void rememberInput(ServerPlayer player, InteractionHand hand, WeaponItem item, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(item, "item");
        ACTIVE_INPUTS.put(player.getUUID(), new ActiveInput(hand, item.definition().path(), tick));
    }

    public static void scheduleBurstShots(
            ServerPlayer player,
            InteractionHand hand,
            WeaponItem item,
            long dueTick,
            int remainingShots,
            int intervalTicks
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(item, "item");
        if (remainingShots <= 0) {
            return;
        }
        PENDING_WEAPON_BURSTS.put(player.getUUID(), new PendingWeaponBurst(
                hand,
                item.definition().path(),
                dueTick,
                remainingShots,
                intervalTicks
        ));
    }

    public static void scheduleBurstShots(
            ServerPlayer player,
            InteractionHand hand,
            ExplosiveProjectileWeaponItem item,
            long dueTick,
            int remainingShots,
            int intervalTicks
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(item, "item");
        if (remainingShots <= 0) {
            return;
        }
        PENDING_EXPLOSIVE_BURSTS.put(player.getUUID(), new PendingExplosiveBurst(
                hand,
                item.definition().path(),
                dueTick,
                remainingShots,
                intervalTicks
        ));
    }

    public static void forgetInput(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ACTIVE_INPUTS.remove(player.getUUID());
    }

    private static void tickLevel(ServerLevel level) {
        long tick = level.getGameTime();
        tickBurstShots(level, tick);
        Iterator<Map.Entry<UUID, ActiveInput>> iterator = ACTIVE_INPUTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveInput> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.level() != level) {
                continue;
            }

            ActiveInput input = entry.getValue();
            if (tick - input.lastInputTick() > HELD_USE_GRACE_TICKS) {
                iterator.remove();
                continue;
            }

            ItemStack stack = player.getItemInHand(input.hand());
            if (!(stack.getItem() instanceof WeaponItem item) || !item.definition().path().equals(input.weaponPath())) {
                iterator.remove();
                continue;
            }
            item.fireFromHeldItem(level, player, input.hand());
        }
        for (ServerPlayer player : level.players()) {
            WeaponItem.cancelInactiveReloads(player);
            ProjectileWeaponItem.cancelInactiveReloads(player);
            ExplosiveProjectileWeaponItem.cancelInactiveReloads(player);
            if (!WeaponItem.showHeldStatus(player)
                    && !ProjectileWeaponItem.showHeldStatus(player)) {
                ExplosiveProjectileWeaponItem.showHeldStatus(player);
            }
        }

    }

    private static void tickBurstShots(ServerLevel level, long tick) {
        tickWeaponBurstShots(level, tick);
        tickExplosiveBurstShots(level, tick);
    }

    private static void tickWeaponBurstShots(ServerLevel level, long tick) {
        Iterator<Map.Entry<UUID, PendingWeaponBurst>> iterator = PENDING_WEAPON_BURSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingWeaponBurst> entry = iterator.next();
            PendingWeaponBurst burst = entry.getValue();
            if (tick < burst.dueTick()) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.level() != level) {
                continue;
            }

            ItemStack stack = player.getItemInHand(burst.hand());
            if (!(stack.getItem() instanceof WeaponItem item) || !item.definition().path().equals(burst.weaponPath())) {
                iterator.remove();
                continue;
            }
            item.fireBurstShotFromHeldItem(level, player, burst.hand());
            if (burst.remainingShots() <= 1 || !item.canContinueScheduledBurst(player, burst.hand())) {
                iterator.remove();
            } else {
                entry.setValue(new PendingWeaponBurst(
                        burst.hand(),
                        burst.weaponPath(),
                        tick + burst.intervalTicks(),
                        burst.remainingShots() - 1,
                        burst.intervalTicks()
                ));
            }
        }
    }

    private static void tickExplosiveBurstShots(ServerLevel level, long tick) {
        Iterator<Map.Entry<UUID, PendingExplosiveBurst>> iterator = PENDING_EXPLOSIVE_BURSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingExplosiveBurst> entry = iterator.next();
            PendingExplosiveBurst burst = entry.getValue();
            if (tick < burst.dueTick()) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.level() != level) {
                continue;
            }

            ItemStack stack = player.getItemInHand(burst.hand());
            if (!(stack.getItem() instanceof ExplosiveProjectileWeaponItem item) || !item.definition().path().equals(burst.weaponPath())) {
                iterator.remove();
                continue;
            }
            item.fireBurstShotFromHeldItem(level, player, burst.hand());
            if (burst.remainingShots() <= 1 || !item.canContinueScheduledBurst(player, burst.hand())) {
                iterator.remove();
            } else {
                entry.setValue(new PendingExplosiveBurst(
                        burst.hand(),
                        burst.weaponPath(),
                        tick + burst.intervalTicks(),
                        burst.remainingShots() - 1,
                        burst.intervalTicks()
                ));
            }
        }
    }

    private record ActiveInput(InteractionHand hand, String weaponPath, long lastInputTick) {
        private ActiveInput {
            Objects.requireNonNull(hand, "hand");
            Objects.requireNonNull(weaponPath, "weaponPath");
        }
    }

    private record PendingWeaponBurst(InteractionHand hand, String weaponPath, long dueTick, int remainingShots, int intervalTicks) {
        private PendingWeaponBurst {
            Objects.requireNonNull(hand, "hand");
            Objects.requireNonNull(weaponPath, "weaponPath");
            if (remainingShots <= 0) {
                throw new IllegalArgumentException("remainingShots must be positive");
            }
            if (intervalTicks <= 0) {
                throw new IllegalArgumentException("intervalTicks must be positive");
            }
        }
    }

    private record PendingExplosiveBurst(InteractionHand hand, String weaponPath, long dueTick, int remainingShots, int intervalTicks) {
        private PendingExplosiveBurst {
            Objects.requireNonNull(hand, "hand");
            Objects.requireNonNull(weaponPath, "weaponPath");
            if (remainingShots <= 0) {
                throw new IllegalArgumentException("remainingShots must be positive");
            }
            if (intervalTicks <= 0) {
                throw new IllegalArgumentException("intervalTicks must be positive");
            }
        }
    }
}
