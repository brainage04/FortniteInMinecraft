package com.github.brainage04.fortnite_in_minecraft.effect;

import com.github.brainage04.fortnite_in_minecraft.FortniteInMinecraft;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> BOOGIE = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(FortniteInMinecraft.MOD_ID, "boogie"),
            new BoogieEffect()
    );

    public static void initialize() {

    }
}
