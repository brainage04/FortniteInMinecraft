package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LootContainerBlockEntity extends BlockEntity {
    public LootContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModItems.LOOT_CONTAINER_BLOCK_ENTITY_TYPE, pos, state);
    }
}
