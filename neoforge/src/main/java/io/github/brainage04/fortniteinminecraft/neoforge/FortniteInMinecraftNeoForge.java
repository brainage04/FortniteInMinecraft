package io.github.brainage04.fortniteinminecraft.neoforge;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.neoforge.platform.NeoForgeLoaderPlatform;
import io.github.brainage04.hudrendererlib.HudRendererLib;
import io.github.brainage04.hudrendererlib.neoforge.NeoForgeHudRendererPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(FortniteInMinecraft.MOD_ID)
public final class FortniteInMinecraftNeoForge {
    public FortniteInMinecraftNeoForge(IEventBus modBus) {
        new NeoForgeLoaderPlatform(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientBootstrap.initialize(modBus);
        }
    }

    private static final class ClientBootstrap {
        private ClientBootstrap() {
        }

        private static void initialize(IEventBus modBus) {
            HudRendererLib.initialize(new NeoForgeHudRendererPlatform(modBus));
            io.github.brainage04.fortniteinminecraft.FortniteInMinecraftClient.initialize(
                    new io.github.brainage04.fortniteinminecraft.neoforge.platform.NeoForgeClientPlatform(modBus)
            );
        }
    }
}
