package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public final class BuildTargeting {
    public static final double TARGET_RANGE_BLOCKS = 3.0D;

    private BuildTargeting() {
    }

    public static BuildGridPos destinationGrid(
            String dimension,
            BuildRules rules,
            PieceType pieceType,
            Direction facing,
            Vec3 hitLocation
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(pieceType, "pieceType");
        Objects.requireNonNull(facing, "facing");
        Objects.requireNonNull(hitLocation, "hitLocation");

        int x = Math.round((float) hitLocation.x);
        int y = Math.round((float) hitLocation.y);
        int z = Math.round((float) hitLocation.z);

        int offsetX = -1;
        int offsetY = -1;
        int offsetZ = -1;
        if (pieceType == PieceType.FLOOR) {
            offsetY += 1;
        } else if (pieceType.usesStairConeGrid()) {
            offsetY -= 1;
        }
        if (facing == Direction.SOUTH || facing == Direction.EAST) {
            Direction opposite = facing.getOpposite();
            offsetX += opposite.getStepX() * 2;
            offsetZ += opposite.getStepZ() * 2;
        }

        x = align(x + offsetX, rules);
        y = align(y + offsetY, rules);
        z = align(z + offsetZ, rules);

        if (pieceType == PieceType.WALL) {
            x += facing.getStepX() * rules.footprintRadiusBlocks();
            z += facing.getStepZ() * rules.footprintRadiusBlocks();
        } else if (pieceType == PieceType.FLOOR) {
            y -= rules.footprintRadiusBlocks();
        }

        return new SnapGrid(rules).snap(dimension, x, y, z);
    }

    private static int align(int coordinate, BuildRules rules) {
        int stride = rules.gridStrideBlocks();
        return Math.round((float) coordinate / stride) * stride + rules.gridCenterOffsetBlocks();
    }
}
