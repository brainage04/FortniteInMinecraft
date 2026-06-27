package io.github.brainage04.fortniteinminecraft.core.session;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.PlacementCandidate;

import java.util.Objects;

public final class PlayerBuildSession {
    public static final long NO_TURBO_PLACEMENT_TICK = -1L;

    private PieceType selectedPiece = PieceType.WALL;
    private MaterialType selectedMaterial = MaterialType.WOOD;
    private PreviewMode previewMode = PreviewMode.PARTICLES;
    private PlacementCandidate previewCandidate;
    private BuildSlot lastPlacedSlot;
    private long lastPlacementTick = NO_TURBO_PLACEMENT_TICK;
    private long nextTurboPlacementTick = NO_TURBO_PLACEMENT_TICK;
    private long turboPlacementUntilTick = NO_TURBO_PLACEMENT_TICK;

    public PieceType selectedPiece() {
        return selectedPiece;
    }

    public MaterialType selectedMaterial() {
        return selectedMaterial;
    }

    public PreviewMode previewMode() {
        return previewMode;
    }

    public PlacementCandidate previewCandidate() {
        return previewCandidate;
    }

    public BuildSlot lastPlacedSlot() {
        return lastPlacedSlot;
    }

    public long lastPlacementTick() {
        return lastPlacementTick;
    }

    public long nextTurboPlacementTick() {
        return nextTurboPlacementTick;
    }

    public long turboPlacementUntilTick() {
        return turboPlacementUntilTick;
    }

    public void selectPiece(PieceType piece) {
        Objects.requireNonNull(piece, "piece");
        if (selectedPiece != piece) {
            selectedPiece = piece;
            clearTransientPlacementState();
        }
    }

    public void selectMaterial(MaterialType material) {
        Objects.requireNonNull(material, "material");
        if (selectedMaterial != material) {
            selectedMaterial = material;
            clearTransientPlacementState();
        }
    }

    public MaterialType cycleMaterial() {
        MaterialType[] materials = MaterialType.values();
        selectMaterial(materials[(selectedMaterial.ordinal() + 1) % materials.length]);
        return selectedMaterial;
    }

    public void selectPreviewMode(PreviewMode previewMode) {
        Objects.requireNonNull(previewMode, "previewMode");
        if (this.previewMode != previewMode) {
            this.previewMode = previewMode;
            clearPreview();
        }
    }

    public PlacementCandidate candidateAt(BuildGridPos gridPos, Orientation orientation) {
        Objects.requireNonNull(gridPos, "gridPos");
        Objects.requireNonNull(orientation, "orientation");
        return new PlacementCandidate(new BuildSlot(gridPos, selectedPiece, orientation), selectedMaterial);
    }

    public void rememberPreview(PlacementCandidate candidate) {
        previewCandidate = Objects.requireNonNull(candidate, "candidate");
    }

    public void rememberPlacement(BuildSlot slot, long placedAtTick, long nextTurboPlacementTick) {
        Objects.requireNonNull(slot, "slot");
        if (placedAtTick < 0) {
            throw new IllegalArgumentException("placedAtTick cannot be negative");
        }
        if (nextTurboPlacementTick < placedAtTick) {
            throw new IllegalArgumentException("nextTurboPlacementTick cannot be earlier than placedAtTick");
        }
        previewCandidate = null;
        lastPlacedSlot = slot;
        lastPlacementTick = placedAtTick;
        this.nextTurboPlacementTick = nextTurboPlacementTick;
    }

    public void extendTurboPlacement(long currentTick, long durationTicks) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("currentTick cannot be negative");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
        turboPlacementUntilTick = Math.max(turboPlacementUntilTick, currentTick + durationTicks);
    }

    public boolean turboPlacementActive(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }
        return turboPlacementUntilTick >= tick;
    }

    public boolean canTurboPlace(BuildSlot slot, long tick) {
        Objects.requireNonNull(slot, "slot");
        return turboPlacementActive(tick) && tick >= nextTurboPlacementTick && !slot.equals(lastPlacedSlot);
    }

    public void stopTurboPlacement() {
        nextTurboPlacementTick = NO_TURBO_PLACEMENT_TICK;
        turboPlacementUntilTick = NO_TURBO_PLACEMENT_TICK;
    }

    public void clearPreview() {
        previewCandidate = null;
    }

    public void clearTransientPlacementState() {
        previewCandidate = null;
        lastPlacedSlot = null;
        lastPlacementTick = NO_TURBO_PLACEMENT_TICK;
        nextTurboPlacementTick = NO_TURBO_PLACEMENT_TICK;
        turboPlacementUntilTick = NO_TURBO_PLACEMENT_TICK;
    }
}
