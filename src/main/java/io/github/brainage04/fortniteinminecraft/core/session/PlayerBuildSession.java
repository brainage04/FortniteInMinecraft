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
    private static final long BUILD_USE_SWING_SUPPRESSION_TICKS = 4L;

    private PieceType selectedPiece = PieceType.WALL;
    private MaterialType selectedMaterial = MaterialType.WOOD;
    private boolean buildModeActive;
    private int rotationSteps;
    private PlacementCandidate previewCandidate;
    private BuildSlot lastPlacedSlot;
    private long lastPlacementTick = NO_TURBO_PLACEMENT_TICK;
    private long nextTurboPlacementTick = NO_TURBO_PLACEMENT_TICK;
    private long turboPlacementUntilTick = NO_TURBO_PLACEMENT_TICK;
    private long lastMaterialCycleTick = NO_TURBO_PLACEMENT_TICK;
    private long lastBuildUseTick = NO_TURBO_PLACEMENT_TICK;

    public PieceType selectedPiece() {
        return selectedPiece;
    }

    public MaterialType selectedMaterial() {
        return selectedMaterial;
    }

    public boolean buildModeActive() {
        return buildModeActive;
    }

    public int rotationSteps() {
        return rotationSteps;
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

    public long lastMaterialCycleTick() {
        return lastMaterialCycleTick;
    }

    public long lastBuildUseTick() {
        return lastBuildUseTick;
    }

    public void selectPiece(PieceType piece) {
        Objects.requireNonNull(piece, "piece");
        if (selectedPiece != piece) {
            selectedPiece = piece;
            rotationSteps = 0;
            clearTransientPlacementState();
        }
    }

    public void activateBuildMode(PieceType piece) {
        selectPiece(piece);
        buildModeActive = true;
    }

    public void deactivateBuildMode() {
        if (buildModeActive) {
            buildModeActive = false;
            rotationSteps = 0;
            clearTransientPlacementState();
        }
    }

    public void rotatePlacement() {
        rotationSteps = (rotationSteps + 1) & 3;
        clearTransientPlacementState();
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

    public boolean markMaterialCycle(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }
        if (lastMaterialCycleTick == tick) {
            return false;
        }
        lastMaterialCycleTick = tick;
        return true;
    }

    public void markBuildUse(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }
        lastBuildUseTick = tick;
    }

    public boolean shouldIgnoreMaterialSwing(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }
        return lastBuildUseTick != NO_TURBO_PLACEMENT_TICK
                && tick >= lastBuildUseTick
                && tick - lastBuildUseTick <= BUILD_USE_SWING_SUPPRESSION_TICKS;
    }

    public PlacementCandidate candidateAt(BuildGridPos gridPos, Orientation orientation) {
        Objects.requireNonNull(gridPos, "gridPos");
        Objects.requireNonNull(orientation, "orientation");
        return new PlacementCandidate(new BuildSlot(gridPos, selectedPiece, rotated(orientation)), selectedMaterial);
    }

    private Orientation rotated(Orientation orientation) {
        Orientation rotated = orientation;
        for (int i = 0; i < rotationSteps; i++) {
            rotated = rotated.clockwise();
        }
        return rotated;
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
        lastBuildUseTick = NO_TURBO_PLACEMENT_TICK;
    }
}
