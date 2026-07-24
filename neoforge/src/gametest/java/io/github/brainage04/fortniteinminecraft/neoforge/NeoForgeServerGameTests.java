package io.github.brainage04.fortniteinminecraft.neoforge;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.FortniteServerGameTestSuite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = FortniteInMinecraft.MOD_ID)
public final class NeoForgeServerGameTests {
    private NeoForgeServerGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        for (FortniteServerGameTestSuite.TestCase test : FortniteServerGameTestSuite.tests()) {
            Identifier id = Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, test.path());
            event.register(BuiltInRegistries.TEST_FUNCTION.key(), id, test::function);
        }
    }
}
