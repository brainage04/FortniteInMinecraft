package com.github.brainage04.fortniteinminecraft.item.building;

import com.github.brainage04.fortniteinminecraft.data.marker.MarkerData;
import com.github.brainage04.fortniteinminecraft.data.marker.MarkerEntityHelper;
import com.github.brainage04.fortniteinminecraft.util.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PieceItem extends Item {
    private static final int COOLDOWN = 1;

    private final PieceType pieceType;
    private final Block material;

    public PieceItem(Settings settings, PieceType pieceType, Block material) {
        super(settings);
        this.pieceType = pieceType;
        this.material = material;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        MinecraftClient.getInstance().itemUseCooldown = COOLDOWN;

        HitResult hit = player.raycast(2, 1, false);
        BlockPos pos = new BlockPos(
                (int) Math.round(hit.getPos().x),
                (int) Math.round(hit.getPos().y),
                (int) Math.round(hit.getPos().z)
        );

        return placePieceItem(
                pos,
                player
        );
    }

    private ActionResult placePieceItem(BlockPos origin, PlayerEntity player) {
        // for direction-based pieces - walls and stairs
        Direction facing = player.getHorizontalFacing();
        boolean facingX = facing == Direction.WEST || facing == Direction.EAST;
        boolean facingNegative = facing == Direction.NORTH || facing == Direction.WEST;

        // pre-alignment offsets
        BlockPos offset = new BlockPos(-1, -1, -1);
        if (this.pieceType == PieceType.FLOOR) offset = offset.up();
        if (this.pieceType == PieceType.STAIR) offset = offset.down();
        origin = origin.add(offset);

        // alignment (centered around every 4th block on all axes)
        origin = new BlockPos(
                Math.round((float) (origin.getX()) / 4) * 4 + 1,
                Math.round((float) (origin.getY()) / 4) * 4 + 1,
                Math.round((float) (origin.getZ()) / 4) * 4 + 1
        );

        // post-alignment offsets
        if (this.pieceType == PieceType.WALL) {
            origin = origin.offset(facing, 2);
        } else if (this.pieceType == PieceType.FLOOR) {
            origin = origin.down(2);
        }

        World world = player.getWorld();

        // detect if piece has already been placed here (do not place this one if so)
        MarkerData nearbyMarker = EntityUtils.getNearbyMarker(world, origin);

        if (nearbyMarker != null && nearbyMarker.getBlockPos().equals(origin)) {
            //player.sendMessage(Text.literal("There is already a %s at %s!".formatted(nearbyMarker.getPieceType(), origin.toShortString())).formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        } /* else {
            player.sendMessage(Text.literal("Placed a %s at %s".formatted(pieceType.name(), origin.toShortString())).formatted(Formatting.GREEN), false);
        } */

        // place marker for piece
        MarkerEntity marker = new MarkerEntity(EntityType.MARKER, world);
        MarkerEntityHelper.addCustomData(marker, origin, pieceType);
        world.spawnEntity(marker);

        List<BlockPos> blocksToPlace = getBlocksToPlace(origin, facingX, facingNegative);
        for (BlockPos pos : blocksToPlace) world.setBlockState(pos, material.getDefaultState());

        world.playSound(player, origin, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.MASTER);

        return ActionResult.SUCCESS;
    }

    private List<BlockPos> getBlocksToPlace(BlockPos origin, boolean facingX, boolean facingNegative) {
        List<BlockPos> blocksToPlace = new ArrayList<>();

        switch (pieceType) {
            case WALL -> {
                for (int side = -2; side <= 2; side++) {
                    for (int y = -2; y <= 2; y++) {
                        blocksToPlace.add(facingX ? origin.add(0, y, side) : origin.add(side, y, 0));
                    }
                }
            }
            case FLOOR -> {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        blocksToPlace.add(origin.add(x, 0, z));
                    }
                }
            }
            case STAIR -> {
                // reversing logic
                int step = facingNegative ? -1 : 1;

                int forwardStart = facingNegative ? 2 : -2;
                int forwardEnd = facingNegative ? -2 : 2;

                for (int forward = forwardStart;
                     facingNegative ? forward >= forwardEnd : forward <= forwardEnd;
                     forward += step) {
                    for (int y = -2; y <= 2; y++) {
                        for (int side = -2; side <= 2; side++) {
                            if (forward * (facingNegative ? -1 : 1) != y) continue;

                            blocksToPlace.add(facingX ? origin.add(forward, y, side) : origin.add(side, y, forward));
                        }
                    }
                }
            }
            case CONE -> {
                int radius = 2;
                int y = -1;

                while (radius >= 0) {
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (Math.abs(x) != radius && Math.abs(z) != radius) continue;

                            blocksToPlace.add(origin.add(x, y, z));
                        }
                    }

                    radius--;
                    y++;
                }
            }
        }

        return blocksToPlace;
    }
}
