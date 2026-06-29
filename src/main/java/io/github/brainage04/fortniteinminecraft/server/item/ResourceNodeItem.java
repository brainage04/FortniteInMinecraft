package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.world.ResourceNodeRegistry;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ResourceNodeItem extends SimplePolymerItem {
    private final Definition definition;
    private final BlockState blockState;
    private final Item clientItem;

    public ResourceNodeItem(Definition definition, BlockState blockState, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.blockState = Objects.requireNonNull(blockState, "blockState");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public Definition definition() {
        return definition;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public void modifyBasePolymerItemStack(
            ItemStack out,
            ItemStack stack,
            PacketContext context,
            HolderLookup.Provider registries
    ) {
        out.set(DataComponents.ITEM_NAME, Component.literal(definition.displayName()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName());
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        tooltip.add(Component.literal("Places a tracked " + definition.material().name().toLowerCase(java.util.Locale.ROOT) + " resource cluster."));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResult.PASS;
        }
        List<BlockPos> positions = positions(blockHit.getBlockPos().relative(anchorFace(blockHit)), definition.footprint());
        for (BlockPos pos : positions) {
            if (!serverLevel.getBlockState(pos).isAir()) {
                serverPlayer.sendSystemMessage(Component.literal(definition.displayName() + " needs empty space."), true);
                return InteractionResult.FAIL;
            }
        }
        for (BlockPos pos : positions) {
            serverLevel.setBlock(pos, blockState, Block.UPDATE_ALL);
        }
        ResourceNodeRegistry.register(serverLevel.dimension().identifier().toString(), positions, definition.material(), definition.blockHealth(), definition.resourceReward());
        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }
        serverLevel.playSound(null, positions.get(0), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.9F, 1.0F);
        serverPlayer.sendSystemMessage(Component.literal("Placed " + definition.displayName() + "."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    static List<BlockPos> positions(BlockPos anchor, List<BlockOffset> footprint) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(footprint, "footprint");
        ArrayList<BlockPos> positions = new ArrayList<>(footprint.size());
        for (BlockOffset offset : footprint) {
            positions.add(anchor.offset(offset.x(), offset.y(), offset.z()));
        }
        return List.copyOf(positions);
    }

    private static Direction anchorFace(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        return direction == Direction.DOWN ? Direction.UP : direction;
    }

    public record Definition(
            String path,
            String displayName,
            MaterialType material,
            int blockHealth,
            int resourceReward,
            List<BlockOffset> footprint
    ) {
        public Definition {
            path = requireText(path, "path");
            displayName = requireText(displayName, "displayName");
            Objects.requireNonNull(material, "material");
            if (blockHealth <= 0) {
                throw new IllegalArgumentException("blockHealth must be positive");
            }
            if (resourceReward < 0) {
                throw new IllegalArgumentException("resourceReward cannot be negative");
            }
            footprint = List.copyOf(Objects.requireNonNull(footprint, "footprint"));
            if (footprint.isEmpty()) {
                throw new IllegalArgumentException("footprint cannot be empty");
            }
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return value;
        }
    }
}
