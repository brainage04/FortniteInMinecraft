package com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.core;

import com.github.brainage04.fortnite_in_minecraft.manager.FallDamageManager;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public abstract class AbstractGrenadeEntity extends ArrowEntity implements PolymerEntity {
    private static final TrackedData<Integer> FUSE;
    private static final int DEFAULT_FUSE = 12;
    public static final String FUSE_NBT_KEY = "fuse";

    private static final TrackedData<Integer> POWER;
    private static final int DEFAULT_POWER = 2;
    public static final String POWER_NBT_KEY = "power";

    static {
        FUSE = DataTracker.registerData(AbstractGrenadeEntity.class, TrackedDataHandlerRegistry.INTEGER);
        POWER = DataTracker.registerData(AbstractGrenadeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    }

    protected abstract float getKnockbackModifier();

    public AbstractGrenadeEntity(EntityType<? extends Entity> type, World world) {
        super((EntityType<? extends ArrowEntity>) type, world);
    }

    public AbstractGrenadeEntity(EntityType<? extends Entity> type, World world, Position pos) {
        super((EntityType<? extends ArrowEntity>) type, world);
        setPosition(
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    public AbstractGrenadeEntity(EntityType<? extends Entity> type, ServerWorld serverWorld, LivingEntity livingEntity, ItemStack itemStack) {
        super((EntityType<? extends ArrowEntity>) type, serverWorld);
        setPosition(livingEntity.getEyePos());
        setOwner(livingEntity);
        setStack(itemStack);
    }

    private int getFuse() {
        return dataTracker.get(FUSE);
    }

    private void setFuse(int fuse) {
        dataTracker.set(FUSE, fuse);
    }

    private int getPower() {
        return dataTracker.get(POWER);
    }

    private void setPower(int power) {
        dataTracker.set(POWER, power);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(FUSE, DEFAULT_FUSE);
        builder.add(POWER, DEFAULT_POWER);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        setFuse(view.getInt(FUSE_NBT_KEY, DEFAULT_FUSE));
        setPower(view.getInt(POWER_NBT_KEY, DEFAULT_POWER));
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt(FUSE_NBT_KEY, getFuse());
        view.putInt(POWER_NBT_KEY, getPower());
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.ARROW;
    }

    protected abstract boolean shouldTakeFallDamage();

    private void applyCustomKnockback(World world, Vec3d explosionPos, double strength) {
        List<Entity> entities = world.getEntitiesByClass(
                Entity.class,
                new Box(
                        explosionPos.x - 5, explosionPos.y - 5, explosionPos.z - 5,
                        explosionPos.x + 5, explosionPos.y + 5, explosionPos.z + 5
                ),
                entity -> true
        );

        for (Entity entity : entities) {
            Vec3d direction = entity.getPos().subtract(explosionPos).normalize();
            Vec3d knockback = direction.multiply(strength);

            entity.addVelocity(knockback.x, Math.max(knockback.y, 0.5), knockback.z);

            if (!shouldTakeFallDamage() && entity instanceof LivingEntity livingEntity) {
                FallDamageManager.grantImmunity(livingEntity);
            }

            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(
                        new EntityVelocityUpdateS2CPacket(serverPlayer)
                );
            }
        }
    }

    private void explode() {
        // todo: only reason I still use this is for damage sources/correct death messages. find simpler way to do this
        getWorld().createExplosion(
                this,
                Explosion.createDamageSource(getWorld(), this),
                null,
                getX(),
                getBodyY(0.0625),
                getZ(),
                0,
                false,
                World.ExplosionSourceType.TNT
        );

        applyCustomKnockback(getWorld(), getPos(), getKnockbackModifier());
    }

    @Override
    public void tick() {
        super.tick();

        if (!isInGround()) return;

        int i = getFuse() - 1;
        setFuse(i);

        if (i <= 0) {
            discard();

            if (!getWorld().isClient) explode();
        }
    }
}
