package com.github.brainage04.fortnite_in_minecraft.event;

import com.github.brainage04.fortnite_in_minecraft.effect.ModEffects;
import com.github.brainage04.fortnite_in_minecraft.item.ModItems;
import com.github.brainage04.fortnite_in_minecraft.item.building.PencilItem;
import com.github.brainage04.fortnite_in_minecraft.item.building.PieceItem;
import com.github.brainage04.fortnite_in_minecraft.item.building.PieceType;
import com.github.brainage04.fortnite_in_minecraft.item.weapon.core.GunItem;
import com.github.brainage04.fortnite_in_minecraft.manager.FallDamageManager;
import com.github.brainage04.fortnite_in_minecraft.util.ParticleUtils;
import com.github.brainage04.fortnite_in_minecraft.util.RegistryUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModEvents {
    private static @Nullable PieceType prevPiece = null;
    private static @Nullable BlockPos prevOrigin = null;
    private static @Nullable List<BlockPos> toBePlaced;

    private static void registerPencilHandler() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (stack.getItem() == ModItems.PENCIL) {
                //MinecraftClient.getInstance().itemUseCooldown = PencilItem.COOLDOWN;
                player.getItemCooldownManager().set(stack, PencilItem.COOLDOWN);

                BlockState state = world.getBlockState(pos);

                if (!PencilItem.WHITELIST.contains(state.getBlock())) return ActionResult.PASS;

                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.playSound(
                        null,
                        pos,
                        SoundEvents.BLOCK_WOOD_BREAK,
                        SoundCategory.PLAYERS
                );

                if (PencilItem.useMats && !player.isCreative()) {
                    player.giveOrDropStack(new ItemStack(state.getBlock()));
                }

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Item> @Nullable T getHeldItem(
            ServerPlayerEntity player,
            Class<T> itemClass
    ) {
        Item mainHand = player.getMainHandStack().getItem();
        if (itemClass.isInstance(mainHand)) {
            return (T) mainHand;
        }

        Item offHand = player.getOffHandStack().getItem();
        if (itemClass.isInstance(offHand)) {
            return (T) offHand;
        }

        return null;
    }

    private static void piecePlacementPreview(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PieceItem item = getHeldItem(player, PieceItem.class);
            if (item == null) return;

            BlockPos origin = item.getDestinationPos(player);
            PieceType piece = item.getPieceType();
            if (toBePlaced == null
                    || (prevOrigin != null && !prevOrigin.equals(origin))
                    || (prevPiece != null && !prevPiece.equals(piece))) {
                Direction facing = player.getHorizontalFacing();
                boolean facingX = facing == Direction.WEST || facing == Direction.EAST;
                boolean facingNegative = facing == Direction.NORTH || facing == Direction.WEST;
                toBePlaced = item.getBlocksToPlace(origin, facingX, facingNegative);
            }

            // draw particles at center of each block every tick
            for (BlockPos pos : toBePlaced) {
                ServerWorld world = player.getWorld();

                boolean canPlace = item.getSupportedBlocks(world, toBePlaced).size() >= PieceItem.MINIMUM_SUPPORTED_BLOCKS;

                ParticleUtils.createParticles(
                        world,
                        List.of(player),
                        new DustParticleEffect(canPlace ? 0xa0a0ff : 0xffa0a0, 1),
                        pos,
                        1,
                        0
                );

                world.spawnParticles(
                        player,
                        new DustParticleEffect(canPlace ? 0xa0a0ff : 0xffa0a0, 1),
                        true,
                        true,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        1,
                        0,
                        0,
                        0,
                        0
                );
            }

            prevPiece = piece;
            prevOrigin = origin;
        }
    }

    private static void sendGunItemStats(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            GunItem<?> item = getHeldItem(player, GunItem.class);
            if (item == null) return;

            player.sendMessage(Text.literal("Ammo: %d/%d".formatted(
                    item.stats.currentCapacity,
                    item.stats.maxCapacity
            )), true);
        }
    }

    private static @NotNull ActionResult cancelBoogieActions(PlayerEntity player, World world) {
        if (!world.isClient && player.hasStatusEffect(ModEffects.BOOGIE) && !player.isInCreativeMode()) {
            return ActionResult.FAIL;
        } else return ActionResult.PASS;
    }

    private static boolean boogieSneaking = true;

    public static void initialize() {
        registerPencilHandler();

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            piecePlacementPreview(server);
            sendGunItemStats(server);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            Registry<DamageType> damageTypeRegistry = RegistryUtils.getRegistryFromKey(
                    entity.getRegistryManager(),
                    RegistryKeys.DAMAGE_TYPE
            );
            if (damageTypeRegistry == null) return true;

            DamageType fallDamage = damageTypeRegistry.get(DamageTypes.FALL);
            if (source.getType() != fallDamage) return true;

            if (FallDamageManager.consumeImmunity(player)) return false;
            else return true;
        }));

        // disable item/block uses for players under the Boogie effect
        UseItemCallback.EVENT.register((player, world, hand) -> cancelBoogieActions(player, world));
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> cancelBoogieActions(player, world));

        AttackEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof PlayerEntity target)) return ActionResult.PASS;

            target.removeStatusEffect(ModEffects.BOOGIE);

            return ActionResult.PASS;
        }));

        // makes players with the Boogie effect toggle between sneaking and not sneaking every 0.5 seconds
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 == 0) {
                boogieSneaking = !boogieSneaking;

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player.hasStatusEffect(ModEffects.BOOGIE)) {
                        player.setPose(boogieSneaking
                                ? net.minecraft.entity.EntityPose.CROUCHING
                                : net.minecraft.entity.EntityPose.STANDING);

                        // todo: this only updates for other players i think
                        server.getPlayerManager().sendToAll(
                                new EntityTrackerUpdateS2CPacket(player.getId(), player.getDataTracker().getChangedEntries())
                        );

                        player.getWorld().playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(),
                                SoundCategory.PLAYERS
                        );
                    }
                }
            }
        });
    }
}
