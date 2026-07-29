package io.github.brainage04.fortniteinminecraft.core.session;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildSessionManager {
    private final Map<UUID, PlayerBuildSession> sessionsByPlayer = new ConcurrentHashMap<>();

    public PlayerBuildSession getOrCreate(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return sessionsByPlayer.computeIfAbsent(playerId, ignored -> new PlayerBuildSession());
    }

    public PlayerBuildSession get(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return sessionsByPlayer.get(playerId);
    }

    public PlayerBuildSession reset(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerBuildSession session = new PlayerBuildSession();
        sessionsByPlayer.put(playerId, session);
        return session;
    }

    public boolean remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return sessionsByPlayer.remove(playerId) != null;
    }

    public int size() {
        return sessionsByPlayer.size();
    }

    public void clear() {
        sessionsByPlayer.clear();
    }
}
