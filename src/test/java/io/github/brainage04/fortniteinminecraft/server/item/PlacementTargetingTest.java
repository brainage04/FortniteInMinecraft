package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacementTargetingTest {
    private static final BuildRules RULES = BuildRules.defaults();

    @Test
    void usesOldModFacingOffsetBeforeSnapping() {
        Vec3 hitLocation = new Vec3(3.0D, 2.0D, 3.0D);

        BuildGridPos north = PlacementTargeting.destinationGrid("overworld", RULES, PieceType.WALL, Direction.NORTH, hitLocation);
        BuildGridPos south = PlacementTargeting.destinationGrid("overworld", RULES, PieceType.WALL, Direction.SOUTH, hitLocation);
        BuildGridPos east = PlacementTargeting.destinationGrid("overworld", RULES, PieceType.WALL, Direction.EAST, hitLocation);

        assertEquals(new BuildGridPos("overworld", 1, 0, 1), north);
        assertEquals(new BuildGridPos("overworld", 1, 0, 0), south);
        assertEquals(new BuildGridPos("overworld", 0, 0, 1), east);
    }

    @Test
    void keepsOldFloorAndStairVerticalPrealignment() {
        Vec3 hitLocation = new Vec3(2.0D, 2.0D, 2.0D);

        BuildGridPos floor = PlacementTargeting.destinationGrid("overworld", RULES, PieceType.FLOOR, Direction.NORTH, hitLocation);
        BuildGridPos stair = PlacementTargeting.destinationGrid("overworld", RULES, PieceType.STAIR, Direction.NORTH, hitLocation);

        assertEquals(new BuildGridPos("overworld", 0, 1, 0), floor);
        assertEquals(new BuildGridPos("overworld", 0, 0, 0), stair);
    }
}
