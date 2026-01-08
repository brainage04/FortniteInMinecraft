package com.github.brainage04.fortnite_in_minecraft.item.building;

import com.github.brainage04.fortnite_in_minecraft.data.marker.MarkerData;
import com.github.brainage04.fortnite_in_minecraft.data.marker.MarkerEntityHelper;
import com.github.brainage04.fortnite_in_minecraft.util.EntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PieceItem extends SimplePolymerItem {
    private static final int COOLDOWN = 1;

    private final PieceType pieceType;
    private final Material material;
    public static final int MINIMUM_SUPPORTED_BLOCKS = 3;

    public PieceItem(Settings settings, PieceType pieceType, Material material) {
        super(settings);
        this.pieceType = pieceType;
        this.material = material;
    }

    public PieceType getPieceType() {
        return pieceType;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return material.getPiece(pieceType).asItem();
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Registries.ITEM.getId(getPolymerItem(stack, context));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) return ActionResult.FAIL;

        return use(
                context.getWorld(),
                context.getPlayer(),
                context.getHand()
        );
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        //MinecraftClient.getInstance().itemUseCooldown = COOLDOWN;
        player.getItemCooldownManager().set(player.getStackInHand(hand), COOLDOWN);

        BlockPos pos = getDestinationPos(player);

        return placePieceItem(
                pos,
                player
        );
    }

    public @NotNull BlockPos getDestinationPos(PlayerEntity player) {
        HitResult hit = player.raycast(3, 1, false);

        BlockPos origin = new BlockPos(
                (int) Math.round(hit.getPos().x),
                (int) Math.round(hit.getPos().y),
                (int) Math.round(hit.getPos().z)
        );

        // pre-alignment offsets
        Direction facing = player.getHorizontalFacing();

        // if it ain't broke don't fix it
        BlockPos offset = new BlockPos(-1, -1, -1);

        if (pieceType == PieceType.FLOOR) offset = offset.up();
        if (pieceType == PieceType.STAIR) offset = offset.down();

        // IF IT AIN'T BROKE DON'T FIX IT
        if (facing == Direction.SOUTH || facing == Direction.EAST) offset = offset.offset(facing.getOpposite(), 2);

        origin = origin.add(offset);

        // alignment (centered around every 4th block on all axes)
        origin = new BlockPos(
                Math.round((float) (origin.getX()) / 4) * 4 + 1,
                Math.round((float) (origin.getY()) / 4) * 4 + 1,
                Math.round((float) (origin.getZ()) / 4) * 4 + 1
        );

        // post-alignment offsets
        // walls are offset by 2 blocks horizontally
        // floors are offset by 2 blocks vertically
        if (pieceType == PieceType.WALL) {
            origin = origin.offset(facing, 2);
        } else if (pieceType == PieceType.FLOOR) {
            origin = origin.down(2);
        }

        return origin;
    }

    private ActionResult placePieceItem(BlockPos origin, PlayerEntity player) {
        // for direction-based pieces - walls and stairs
        Direction facing = player.getHorizontalFacing();
        boolean facingX = facing == Direction.WEST || facing == Direction.EAST;
        boolean facingNegative = facing == Direction.NORTH || facing == Direction.WEST;

        // check if piece has enough support to be placed
        World world = player.getWorld();
        List<BlockPos> toBePlaced = getBlocksToPlace(origin, facingX, facingNegative);
        Set<BlockPos> supportedBlocks = getSupportedBlocks(world, toBePlaced);
        if (supportedBlocks.size() < MINIMUM_SUPPORTED_BLOCKS) return ActionResult.FAIL;

        // detect if piece has already been placed here (do not place this one if so)
        MarkerData nearbyMarker = EntityUtils.getNearbyMarker(world, origin);

        if (nearbyMarker != null && nearbyMarker.blockPos().equals(origin)) {
            //player.sendMessage(Text.literal("There is already a %s at %s!".formatted(nearbyMarker.getPieceType(), origin.toShortString())).formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        } /* else {
            player.sendMessage(Text.literal("Placed a %s at %s".formatted(pieceType.name(), origin.toShortString())).formatted(Formatting.GREEN), false);
        } */

        // place marker for piece
        MarkerEntity marker = new MarkerEntity(EntityType.MARKER, world);
        MarkerEntityHelper.addCustomData(marker, origin, pieceType);
        world.spawnEntity(marker);

        BlockPos feet = player.getBlockPos();
        BlockPos head = feet.offset(Direction.UP);
        boolean feetBlocked = isSolidBlock(world, feet);
        boolean headBlocked = isSolidBlock(world, head);

        for (BlockPos pos : toBePlaced) world.setBlockState(pos, material.base.getDefaultState());

        // if feet are blocked but weren't before, blip up 1 block
        // else if head is blocked but wasn't before blip up 2 blocks
        if (!feetBlocked && isSolidBlock(world, feet)) {
            player.setPos(
                    player.getPos().x,
                    player.getPos().y + 1,
                    player.getPos().z
            );
        } else if (!headBlocked && isSolidBlock(world, head)) {
            player.setPos(
                    player.getPos().x,
                    player.getPos().y + 2,
                    player.getPos().z
            );
        }

        world.playSound(
                null,
                origin,
                SoundEvents.BLOCK_WOOD_PLACE,
                SoundCategory.PLAYERS
        );

        return ActionResult.SUCCESS;
    }

    private static boolean isSolidBlock(World world, BlockPos feet) {
        return world.getBlockState(feet).isSolidBlock(world, feet);
    }

    public List<BlockPos> getBlocksToPlace(BlockPos origin, boolean facingX, boolean facingNegative) {
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

    public Set<BlockPos> getSupportedBlocks(World world, List<BlockPos> toBePlaced) {
        Set<BlockPos> supportedBlock = new HashSet<>();
        Set<BlockPos> toBePlacedSet = new HashSet<>(toBePlaced);

        for (BlockPos pos : toBePlaced) {
            if (isSolidBlock(world, pos)) supportedBlock.add(pos);

            for (Direction direction : Direction.values()) {
                BlockPos offset = pos.offset(direction, 1);

                if (toBePlacedSet.contains(offset)) continue;
                if (isSolidBlock(world, offset)) supportedBlock.add(offset);
            }
        }

        return supportedBlock;
    }
}
