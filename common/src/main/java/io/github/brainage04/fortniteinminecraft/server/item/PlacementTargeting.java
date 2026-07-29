package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.placement.BuildTargeting;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

final class PlacementTargeting {
    static final double TARGET_RANGE_BLOCKS = BuildTargeting.TARGET_RANGE_BLOCKS;

    private PlacementTargeting() {
    }

    static BuildGridPos destinationGrid(ServerLevel level, BuildRules rules, PieceType pieceType, Direction facing, Vec3 hitLocation) {
        Objects.requireNonNull(level, "level");
        return destinationGrid(level.dimension().identifier().toString(), rules, pieceType, facing, hitLocation);
    }

    static BuildGridPos destinationGrid(String dimension, BuildRules rules, PieceType pieceType, Direction facing, Vec3 hitLocation) {
        return BuildTargeting.destinationGrid(dimension, rules, pieceType, facing, hitLocation);
    }
}
