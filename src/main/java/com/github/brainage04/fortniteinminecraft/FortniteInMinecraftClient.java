package com.github.brainage04.fortniteinminecraft;

import com.github.brainage04.fortniteinminecraft.command.core.ModCommands;
import com.github.brainage04.fortniteinminecraft.config.core.ModConfig;
import com.github.brainage04.fortniteinminecraft.event.ModEvents;
import com.github.brainage04.fortniteinminecraft.key.core.ModKeys;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

public class FortniteInMinecraftClient implements ClientModInitializer {
    @Override
	public void onInitializeClient() {
		FortniteInMinecraft.LOGGER.info("{} client initialising...", FortniteInMinecraft.MOD_NAME);

		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ModCommands.initialize();
		ModKeys.initialize();

        ModEvents.initialize();

		FortniteInMinecraft.LOGGER.info("{} client initialised.", FortniteInMinecraft.MOD_NAME);
	}
}