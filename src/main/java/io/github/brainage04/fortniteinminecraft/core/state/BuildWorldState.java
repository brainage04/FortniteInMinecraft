package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;

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

    public boolean addIfNotConflicting(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        if (conflicts(piece.slot())) {
            return false;
        }
        piecesBySlot.put(piece.slot(), piece);
        return true;
    }

    public BuildPieceState remove(BuildSlot slot) {
        return piecesBySlot.remove(Objects.requireNonNull(slot, "slot"));
    }

    public boolean conflicts(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (piecesBySlot.containsKey(slot)) {
            return true;
        }
        if (slot.pieceType() != PieceType.STAIR) {
            return false;
        }
        for (BuildSlot existing : piecesBySlot.keySet()) {
            if (existing.pieceType() == PieceType.STAIR && existing.gridPos().equals(slot.gridPos())) {
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
