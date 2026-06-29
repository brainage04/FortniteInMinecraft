package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.BuildResourceHarvest;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildMaterializer;
import io.github.brainage04.fortniteinminecraft.server.world.WorldBuildWriteResult;
import io.github.brainage04.fortniteinminecraft.server.world.ResourceNodeRegistry;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.Objects;

public final class PickaxeItem extends SimplePolymerItem {
    public static final int DEFAULT_STRUCTURE_DAMAGE = 50;
    public static final int DEFAULT_RESOURCE_REWARD = 5;
    public static final double DEFAULT_RANGE_BLOCKS = 5.0D;

    private static BuildWorldState buildWorld;
    private static WorldBuildMaterializer materializer;

    private final Item clientItem;
    private final int structureDamage;
    private final int resourceReward;

    public PickaxeItem(Item.Properties settings, Item clientItem) {
        this(settings, clientItem, DEFAULT_STRUCTURE_DAMAGE, DEFAULT_RESOURCE_REWARD);
    }

    public PickaxeItem(Item.Properties settings, Item clientItem, int structureDamage, int resourceReward) {
        super(settings, clientItem);
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
        if (structureDamage < 0) {
            throw new IllegalArgumentException("structureDamage cannot be negative");
        }
        if (resourceReward < 0) {
            throw new IllegalArgumentException("resourceReward cannot be negative");
        }
        this.structureDamage = structureDamage;
        this.resourceReward = resourceReward;
    }

    public static void configureHarvesting(BuildWorldState state, WorldBuildMaterializer worldMaterializer) {
        buildWorld = Objects.requireNonNull(state, "state");
        materializer = Objects.requireNonNull(worldMaterializer, "worldMaterializer");
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Harvesting Tool");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (buildWorld == null || materializer == null) {
            serverPlayer.sendSystemMessage(Component.literal("Harvesting is not configured."), true);
            return InteractionResult.PASS;
        }

        HitResult hit = player.pick(DEFAULT_RANGE_BLOCKS, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResult.PASS;
        }
        String dimension = serverLevel.dimension().identifier().toString();
        BlockPos hitPos = blockHit.getBlockPos();
        BuildSlot slot = materializer.topOwnerAt(dimension, hitPos);
        if (slot == null) {
            return harvestResourceNode(serverLevel, serverPlayer, dimension, hitPos);
        }

        PlayerResourceState resources = PlayerResourceStates.stateFor(serverPlayer);
        BuildResourceHarvest.HarvestResult result = BuildResourceHarvest.harvest(
                buildWorld,
                slot,
                structureDamage,
                resourceReward,
                serverLevel.getGameTime(),
                resources
        );
        if (!result.hit()) {
            return InteractionResult.PASS;
        }
        if (result.destroyed()) {
            WorldBuildWriteResult clearResult = materializer.clear(serverLevel, result.after());
            if (clearResult.success()) {
                buildWorld.remove(slot);
            } else {
                serverPlayer.sendSystemMessage(Component.literal("Build destroyed but world clear failed: " + clearResult.message()), false);
            }
        } else {
            WorldBuildWriteResult refreshResult = materializer.refresh(serverLevel, result.after());
            if (!refreshResult.success()) {
                serverPlayer.sendSystemMessage(Component.literal("Build damaged but world refresh failed: " + refreshResult.message()), false);
            }
        }
        serverPlayer.sendSystemMessage(Component.literal("Harvested " + result.grantedResources() + " "
                + result.material().name().toLowerCase(Locale.ROOT) + "."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult harvestResourceNode(ServerLevel level, ServerPlayer player, String dimension, BlockPos hitPos) {
        ResourceNodeRegistry.ResourceHit hit = ResourceNodeRegistry.hit(dimension, hitPos, structureDamage);
        if (!hit.hit()) {
            return InteractionResult.PASS;
        }
        PlayerResourceState resources = PlayerResourceStates.stateFor(player);
        int accepted = resources.addMaterial(hit.material(), hit.resourceReward());
        if (hit.destroyed()) {
            level.setBlock(hitPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        player.sendSystemMessage(Component.literal("Harvested " + accepted + " "
                + hit.material().name().toLowerCase(Locale.ROOT)
                + (hit.destroyed() ? "." : "; node health " + hit.remainingHealth() + ".")), true);
        return InteractionResult.SUCCESS_SERVER;
    }
}
