package io.github.brainage04.fortniteinminecraft.core.session;

import java.util.Objects;
import java.util.UUID;

public record PlayerBuildContext(UUID playerId, boolean creative, ResourceWallet resources) {
    public PlayerBuildContext {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(resources, "resources");
    }

    public static PlayerBuildContext creative(UUID playerId) {
        return new PlayerBuildContext(playerId, true, new ResourceWallet());
    }

    public static PlayerBuildContext survival(UUID playerId, ResourceWallet resources) {
        return new PlayerBuildContext(playerId, false, resources);
    }
}
