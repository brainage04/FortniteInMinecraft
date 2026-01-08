package com.github.brainage04.fortnite_in_minecraft.util;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

public class RegistryUtils {
    public static <T> @Nullable Registry<T> getRegistryFromKey(DynamicRegistryManager registryManager, RegistryKey<Registry<T>> registryKey) {
        return registryManager.getOptional(registryKey).orElse(null);
    }
}
