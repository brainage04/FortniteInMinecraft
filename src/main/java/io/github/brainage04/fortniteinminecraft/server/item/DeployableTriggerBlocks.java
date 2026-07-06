package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public final class DeployableTriggerBlocks {
    public static final TrapTriggerBlock TRAP_TRIGGER = new TrapTriggerBlock(triggerProperties(blockKey("deployable_trap_trigger")));

    private static boolean registered;

    private DeployableTriggerBlocks() {
    }

    public static void initialize() {
        if (registered) {
            return;
        }
        Registry.register(BuiltInRegistries.BLOCK, blockKey("deployable_trap_trigger"), TRAP_TRIGGER);
        registered = true;
    }

    static BlockState triggerState(Direction facing) {
        return TRAP_TRIGGER.defaultBlockState().setValue(TrapTriggerBlock.FACING, Objects.requireNonNull(facing, "facing"));
    }

    static BlockPos supportPosFor(BlockState state, BlockPos triggerPos) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(triggerPos, "triggerPos");
        if (!state.is(TRAP_TRIGGER) || !state.hasProperty(TrapTriggerBlock.FACING)) {
            return triggerPos;
        }
        return triggerPos.relative(state.getValue(TrapTriggerBlock.FACING).getOpposite());
    }

    private static BlockBehaviour.Properties triggerProperties(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .sound(SoundType.METAL)
                .noCollision()
                .noOcclusion()
                .noLootTable()
                .setId(key);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path));
    }
}
