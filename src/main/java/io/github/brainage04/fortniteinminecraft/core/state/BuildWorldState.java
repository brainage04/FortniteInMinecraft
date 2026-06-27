package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuildWorldState {
    private final Map<BuildSlot, BuildPieceState> piecesBySlot = new HashMap<>();

    public BuildPieceState get(BuildSlot slot) {
        return piecesBySlot.get(Objects.requireNonNull(slot, "slot"));
    }

    public boolean contains(BuildSlot slot) {
        return piecesBySlot.containsKey(Objects.requireNonNull(slot, "slot"));
    }

    public boolean addIfAbsent(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        return piecesBySlot.putIfAbsent(piece.slot(), piece) == null;
    }

    public BuildPieceState remove(BuildSlot slot) {
        return piecesBySlot.remove(Objects.requireNonNull(slot, "slot"));
    }

    public boolean hasAnyAt(BuildGridPos gridPos) {
        Objects.requireNonNull(gridPos, "gridPos");
        for (BuildSlot slot : piecesBySlot.keySet()) {
            if (slot.gridPos().equals(gridPos)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return piecesBySlot.size();
    }

    public Collection<BuildPieceState> pieces() {
        return List.copyOf(piecesBySlot.values());
    }
}
