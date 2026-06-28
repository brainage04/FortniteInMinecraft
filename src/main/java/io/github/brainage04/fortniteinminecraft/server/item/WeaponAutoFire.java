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
    private static boolean registered;

    private WeaponAutoFire() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(WeaponAutoFire::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE_INPUTS.remove(handler.player.getUUID()));
        registered = true;
    }

    static void rememberInput(ServerPlayer player, InteractionHand hand, WeaponItem item, long tick) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(item, "item");
        ACTIVE_INPUTS.put(player.getUUID(), new ActiveInput(hand, item.definition().path(), tick));
    }

    private static void tickLevel(ServerLevel level) {
        long tick = level.getGameTime();
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
    }

    private record ActiveInput(InteractionHand hand, String weaponPath, long lastInputTick) {
        private ActiveInput {
            Objects.requireNonNull(hand, "hand");
            Objects.requireNonNull(weaponPath, "weaponPath");
        }
    }
}
