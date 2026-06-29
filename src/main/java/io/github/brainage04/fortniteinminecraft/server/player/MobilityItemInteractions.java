package io.github.brainage04.fortniteinminecraft.server.player;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MobilityItemInteractions {
    private static final Map<UUID, GliderState> GLIDERS = new HashMap<>();
    private static boolean registered;

    private MobilityItemInteractions() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(MobilityItemInteractions::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GLIDERS.remove(handler.player.getUUID()));
        registered = true;
    }

    public static void enableRedeploy(ServerPlayer player, long durationTicks) {
        Objects.requireNonNull(player, "player");
        GLIDERS.computeIfAbsent(player.getUUID(), uuid -> new GliderState())
                .enableRedeploy(player.level().getGameTime(), durationTicks);
    }

    public static boolean isGliding(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        GliderState state = GLIDERS.get(player.getUUID());
        return state != null && !player.onGround() && state.canRedeploy(player.level().getGameTime());
    }

    public static void clearAll() {
        GLIDERS.clear();
    }

    private static void tickLevel(ServerLevel level) {
        long tick = level.getGameTime();
        Iterator<Map.Entry<UUID, GliderState>> iterator = GLIDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GliderState> entry = iterator.next();
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer player)
                    || player.onGround()
                    || !entry.getValue().canRedeploy(tick)) {
                iterator.remove();
                continue;
            }
            applyGlide(player, entry.getValue());
        }
    }

    private static void applyGlide(ServerPlayer player, GliderState state) {
        Vec3 velocity = player.getDeltaMovement();
        double y = state.fallSpeed(velocity.y());
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
        Vec3 horizontal = new Vec3(velocity.x(), 0.0D, velocity.z());
        if (horizontalLook.lengthSqr() > 1.0E-9D && horizontal.lengthSqr() < 0.22D) {
            horizontal = horizontal.add(horizontalLook.normalize().scale(0.025D));
        }
        player.setDeltaMovement(horizontal.x(), y, horizontal.z());
        player.hurtMarked = true;
        player.resetFallDistance();
    }
}
