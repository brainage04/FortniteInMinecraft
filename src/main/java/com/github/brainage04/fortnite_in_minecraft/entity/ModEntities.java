package com.github.brainage04.fortnite_in_minecraft.entity;

import com.github.brainage04.fortnite_in_minecraft.FortniteInMinecraft;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.BoogieBombEntity;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.ImpulseGrenadeEntity;
import com.github.brainage04.fortnite_in_minecraft.entity.misc.projectile.grenade.ShockwaveGrenadeEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<ImpulseGrenadeEntity> IMPULSE_GRENADE;
    public static final EntityType<ShockwaveGrenadeEntity> SHOCKWAVE_GRENADE;
    public static final EntityType<BoogieBombEntity> BOOGIE_BOMB;

    static {
        RegistryKey<EntityType<?>> impulseGrenadeEntity = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                Identifier.of(FortniteInMinecraft.MOD_ID, "impulse_grenade"));
        IMPULSE_GRENADE = (EntityType) Registry.register(
                Registries.ENTITY_TYPE,
                impulseGrenadeEntity,
                EntityType.Builder.create(ImpulseGrenadeEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f).build(impulseGrenadeEntity)
        );

        RegistryKey<EntityType<?>> shockwaveGrenadeEntity = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                Identifier.of(FortniteInMinecraft.MOD_ID, "shockwave_grenade"));
        SHOCKWAVE_GRENADE = (EntityType) Registry.register(
                Registries.ENTITY_TYPE,
                shockwaveGrenadeEntity,
                EntityType.Builder.create(ShockwaveGrenadeEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f).build(shockwaveGrenadeEntity)
        );

        RegistryKey<EntityType<?>> boogieBombEntity = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
                Identifier.of(FortniteInMinecraft.MOD_ID, "boogie_bomb"));
        BOOGIE_BOMB = (EntityType) Registry.register(
                Registries.ENTITY_TYPE,
                boogieBombEntity,
                EntityType.Builder.create(BoogieBombEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f).build(boogieBombEntity)
        );
    }

    public static void initialize() {
        PolymerEntityUtils.registerType(IMPULSE_GRENADE);
        PolymerEntityUtils.registerType(SHOCKWAVE_GRENADE);
        PolymerEntityUtils.registerType(BOOGIE_BOMB);
    }
}
