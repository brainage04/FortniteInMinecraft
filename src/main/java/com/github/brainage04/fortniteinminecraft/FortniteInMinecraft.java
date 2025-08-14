package com.github.brainage04.fortniteinminecraft;

import com.github.brainage04.fortniteinminecraft.command.core.ModCommands;
import com.github.brainage04.fortniteinminecraft.config.core.ModConfig;
import com.github.brainage04.fortniteinminecraft.key.core.ModKeys;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FortniteInMinecraft implements ClientModInitializer {
	public static final String MOD_ID = "fortniteinminecraft";
	public static final String MOD_NAME = "FortniteInMinecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} initialising...", MOD_NAME);

		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ModCommands.initialize();
		ModKeys.initialize();

		LOGGER.info("{} initialised.", MOD_NAME);
	}
}