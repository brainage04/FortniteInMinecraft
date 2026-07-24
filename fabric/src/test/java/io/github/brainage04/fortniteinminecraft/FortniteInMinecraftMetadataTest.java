package io.github.brainage04.fortniteinminecraft;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FortniteInMinecraftMetadataTest {
    @Test
    void fabricLoaderBootsInServerModeForTests() {
        assertEquals(EnvType.SERVER, FabricLoader.getInstance().getEnvironmentType());
    }

    @Test
    void fabricLoaderCanResolveTheServerCoreMetadata() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer(FortniteInMinecraft.MOD_ID)
                .orElseThrow(() -> new AssertionError("Expected the mod to be loaded for tests."));
        ModMetadata metadata = mod.getMetadata();

        assertAll(
                () -> assertEquals(FortniteInMinecraft.MOD_ID, metadata.getId()),
                () -> assertEquals(FortniteInMinecraft.MOD_NAME, metadata.getName()),
                () -> assertTrue(metadata.getLicense().contains("MIT")),
                () -> assertTrue(mod.findPath("fabric.mod.json").isPresent())
        );
    }
}
