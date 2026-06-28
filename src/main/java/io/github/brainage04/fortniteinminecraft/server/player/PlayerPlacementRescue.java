package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

public final class PlayerPlacementRescue {
    private static final double COLLISION_EPSILON = 1.0E-6D;
    private static final double UPWARD_SCAN_STEP_BLOCKS = 0.25D;

    private PlayerPlacementRescue() {
    }

    public static boolean rescueAfterPlacement(
            ServerPlayer player,
            ServerLevel level,
            BuildRules rules,
            WorldBuildMaterializer materializer,
            PieceFootprint footprint
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(footprint, "footprint");

        List<BlockPos> blocks = materializer.blockPositions(footprint);
        AABB playerBox = player.getBoundingBox().deflate(COLLISION_EPSILON);
        if (!intersectsBody(playerBox, blocks)) {
            return false;
        }

        double offset = upwardOffset(playerBox, blocks);
        double maxOffset = Math.max(offset, rules.wallHeightBlocks() + 2.0D);
        while (offset <= maxOffset) {
            AABB candidateBox = playerBox.move(0.0D, offset, 0.0D).deflate(COLLISION_EPSILON);
            if (level.noCollision(player, candidateBox)) {
                teleportUp(player, offset);
                return true;
            }
            offset += UPWARD_SCAN_STEP_BLOCKS;
        }
        return false;
    }

    static boolean intersectsBody(AABB playerBox, List<BlockPos> blocks) {
        Objects.requireNonNull(playerBox, "playerBox");
        Objects.requireNonNull(blocks, "blocks");
        if (blocks.isEmpty()) {
            return false;
        }

        double midY = (playerBox.minY + playerBox.maxY) * 0.5D;
        AABB lowerHalf = playerBox.setMaxY(midY);
        AABB upperHalf = playerBox.setMinY(midY);
        for (BlockPos block : blocks) {
            AABB blockBox = new AABB(block);
            if (blockBox.intersects(lowerHalf) || blockBox.intersects(upperHalf)) {
                return true;
            }
        }
        return false;
    }

    static double upwardOffset(AABB playerBox, List<BlockPos> blocks) {
        Objects.requireNonNull(playerBox, "playerBox");
        Objects.requireNonNull(blocks, "blocks");
        double highestTop = playerBox.minY;
        for (BlockPos block : blocks) {
            AABB blockBox = new AABB(block);
            if (blockBox.intersects(playerBox)) {
                highestTop = Math.max(highestTop, blockBox.maxY);
            }
        }
        return Math.max(0.0D, highestTop - playerBox.minY + COLLISION_EPSILON);
    }

    private static void teleportUp(ServerPlayer player, double offset) {
        player.teleportTo(player.getX(), player.getY() + offset, player.getZ());
        player.resetFallDistance();
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x(), Math.max(0.0D, velocity.y()), velocity.z());
        player.setOnGround(false);
    }
}
