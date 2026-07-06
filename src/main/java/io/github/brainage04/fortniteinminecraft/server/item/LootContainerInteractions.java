package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.LootContainerProgressPayload;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LootContainerInteractions {
    private static final double MAX_OPEN_DISTANCE_SQUARED = 36.0D;
    private static final Map<OpeningKey, Opening> OPENINGS = new HashMap<>();
    private static boolean registered;

    private LootContainerInteractions() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(LootContainerInteractions::tickServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAll());
        registered = true;
    }

    static void beginOpening(ServerPlayer player, ServerLevel level, BlockPos pos, LootContainerBlock block) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(block, "block");

        long tick = level.getGameTime();
        OpeningKey key = new OpeningKey(player.getUUID(), level.dimension(), pos.immutable());
        Opening existing = OPENINGS.get(key);
        if (existing != null && existing.block() == block) {
            return;
        }
        removeOpenings(player.getUUID(), key);
        OPENINGS.put(key, new Opening(block, tick));
        sendProgress(player, block, 0);
    }

    public static void clearAll() {
        OPENINGS.clear();
    }

    public static void cancelOpening(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (removeOpenings(player.getUUID(), null)) {
            sendClear(player);
        }
    }

    private static void tickServer(MinecraftServer server) {
        Iterator<Map.Entry<OpeningKey, Opening>> iterator = OPENINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<OpeningKey, Opening> entry = iterator.next();
            OpeningKey key = entry.getKey();
            Opening opening = entry.getValue();
            ServerLevel level = server.getLevel(key.dimension());
            ServerPlayer player = server.getPlayerList().getPlayer(key.playerId());
            if (level == null || player == null || !canKeepOpening(player, level, key.pos(), opening.block())) {
                if (player != null) {
                    sendClear(player);
                }
                iterator.remove();
                continue;
            }
            long tick = level.getGameTime();
            int elapsedTicks = (int) Math.min(tick - opening.startedTick(), opening.block().openTicks());
            if (elapsedTicks < opening.block().openTicks()) {
                sendProgress(player, opening.block(), elapsedTicks);
                continue;
            }
            opening.block().open(level, key.pos(), player);
            sendClear(player);
            iterator.remove();
        }
    }

    private static boolean canKeepOpening(ServerPlayer player, ServerLevel level, BlockPos pos, LootContainerBlock block) {
        if (player.isSpectator() || player.isDeadOrDying() || player.level() != level) {
            return false;
        }
        if (level.getBlockState(pos).getBlock() != block) {
            return false;
        }
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > MAX_OPEN_DISTANCE_SQUARED) {
            return false;
        }
        HitResult hit = player.pick(5.0D, 0.0F, false);
        return hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos);
    }

    private static boolean removeOpenings(UUID playerId, OpeningKey except) {
        boolean removed = false;
        Iterator<Map.Entry<OpeningKey, Opening>> iterator = OPENINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            OpeningKey key = iterator.next().getKey();
            if (key.playerId().equals(playerId) && !key.equals(except)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static void sendProgress(ServerPlayer player, LootContainerBlock block, int elapsedTicks) {
        if (ServerPlayNetworking.canSend(player, LootContainerProgressPayload.TYPE)) {
            ServerPlayNetworking.send(player, new LootContainerProgressPayload(
                    true,
                    block.definition().displayName(),
                    elapsedTicks,
                    Math.max(1, block.openTicks())
            ));
        }
    }

    private static void sendClear(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, LootContainerProgressPayload.TYPE)) {
            ServerPlayNetworking.send(player, LootContainerProgressPayload.inactive());
        }
    }

    private record OpeningKey(UUID playerId, ResourceKey<Level> dimension, BlockPos pos) {
        private OpeningKey {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(pos, "pos");
        }
    }

    private static final class Opening {
        private final LootContainerBlock block;
        private final long startedTick;
        private Opening(LootContainerBlock block, long startedTick) {
            this.block = Objects.requireNonNull(block, "block");
            this.startedTick = startedTick;
        }

        private LootContainerBlock block() {
            return block;
        }

        private long startedTick() {
            return startedTick;
        }

    }
}
