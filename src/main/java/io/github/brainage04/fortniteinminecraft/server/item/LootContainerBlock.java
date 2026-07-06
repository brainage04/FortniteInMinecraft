package io.github.brainage04.fortniteinminecraft.server.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Objects;

public final class LootContainerBlock extends Block implements EntityBlock {
    private final ItemCatalog.LootContainerEntry definition;
    private final LootDropTable drops;

    public LootContainerBlock(ItemCatalog.LootContainerEntry definition, LootDropTable drops, BlockBehaviour.Properties settings) {
        super(settings);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.drops = Objects.requireNonNull(drops, "drops");
    }

    ItemCatalog.LootContainerEntry definition() {
        return definition;
    }

    int openTicks() {
        return definition.openTicks();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootContainerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return beginOpening(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return beginOpening(level, pos, player);
    }

    void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (level.getBlockState(pos).getBlock() != this) {
            return;
        }
        List<ItemStack> stacks = drops.roll(definition.kind(), level.getRandom());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack stack : stacks) {
            Block.popResource(level, pos.above(), stack);
        }
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 1.0F);
        player.sendSystemMessage(Component.literal(definition.displayName() + " opened."), true);
    }

    private InteractionResult beginOpening(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        LootContainerInteractions.beginOpening(serverPlayer, serverLevel, pos, this);
        return InteractionResult.SUCCESS_SERVER;
    }
}
