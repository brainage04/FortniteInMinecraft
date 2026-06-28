package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WeaponItem extends SimplePolymerItem {
    private static final String MAGAZINE_KEY = "magazine";
    private static final String NEXT_FIRE_TICK_KEY = "next_fire_tick";
    private static final double FORTNITE_TO_MINECRAFT_DAMAGE = 0.2D;
    private static final double ENTITY_HITBOX_INFLATE_BLOCKS = 0.3D;

    private final WeaponDefinition definition;
    private final Item clientItem;

    public WeaponItem(WeaponDefinition definition, Item.Properties settings, Item clientItem) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
    }

    public WeaponDefinition definition() {
        return definition;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.rarity().label() + " " + definition.displayName());
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        WeaponStats stats = definition.stats();
        tooltip.add(Component.literal(definition.category().label() + " / " + definition.rarity().label()));
        tooltip.add(Component.literal("Damage: " + format(stats.damage())
                + (stats.pellets() > 1 ? " x" + stats.pellets() : "")));
        tooltip.add(Component.literal("Fire rate: " + format(stats.fireRatePerSecond()) + "/s; magazine: " + stats.magazineSize()));
        tooltip.add(Component.literal("Reload: " + format(stats.reloadSeconds()) + "s; crit: " + format(stats.criticalMultiplier()) + "x"));
        tooltip.add(Component.literal("Source: " + definition.sourceStatRow()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        long tick = serverLevel.getGameTime();
        long nextFireTick = customData(stack).getLongOr(NEXT_FIRE_TICK_KEY, 0L);
        if (tick < nextFireTick) {
            serverPlayer.sendSystemMessage(Component.literal("Weapon cooling down."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        int magazine = magazine(stack);
        if (magazine <= 0) {
            reload(stack, tick);
            serverPlayer.sendSystemMessage(Component.literal("Reloading " + definition.displayName() + "."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        magazine--;
        setGunState(stack, magazine, tick + fireDelayTicks());
        LivingEntity target = firstEntityHit(serverLevel, serverPlayer, definition.stats().rangeBlocks());
        if (target != null) {
            target.hurtServer(serverLevel, serverPlayer.damageSources().playerAttack(serverPlayer), minecraftDamage());
            serverPlayer.sendSystemMessage(Component.literal("Hit " + target.getName().getString()
                    + " (" + magazine + "/" + definition.stats().magazineSize() + ")."), true);
        } else {
            serverPlayer.sendSystemMessage(Component.literal("Fired " + definition.displayName()
                    + " (" + magazine + "/" + definition.stats().magazineSize() + ")."), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private float minecraftDamage() {
        return (float) Math.max(1.0D, definition.stats().damage() * definition.stats().pellets() * FORTNITE_TO_MINECRAFT_DAMAGE);
    }

    private int magazine(ItemStack stack) {
        return customData(stack).getIntOr(MAGAZINE_KEY, definition.stats().magazineSize());
    }

    private void reload(ItemStack stack, long tick) {
        setGunState(stack, definition.stats().magazineSize(), tick + reloadTicks());
    }

    private void setGunState(ItemStack stack, int magazine, long nextFireTick) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(MAGAZINE_KEY, magazine);
            tag.putLong(NEXT_FIRE_TICK_KEY, nextFireTick);
        });
    }

    private CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private long fireDelayTicks() {
        return Math.max(1L, Math.round(20.0D / definition.stats().fireRatePerSecond()));
    }

    private long reloadTicks() {
        return Math.max(1L, Math.round(definition.stats().reloadSeconds() * 20.0D));
    }

    private static LivingEntity firstEntityHit(ServerLevel level, ServerPlayer player, double rangeBlocks) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(rangeBlocks));
        HitResult blockHit = player.pick(rangeBlocks, 0.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity closest = null;
        double closestDistance = start.distanceToSqr(end);
        for (Entity entity : level.getEntities(player, searchBox, WeaponItem::canShoot)) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(ENTITY_HITBOX_INFLATE_BLOCKS).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }

    private static boolean canShoot(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator();
    }

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
