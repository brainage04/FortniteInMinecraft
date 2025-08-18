package com.github.brainage04.fortniteinminecraft;

import com.github.brainage04.fortniteinminecraft.block.ModBlocks;
import com.github.brainage04.fortniteinminecraft.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FortniteInMinecraft implements ModInitializer {
    public static final String MOD_ID = "fortniteinminecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_NAME = "FortniteInMinecraft";

    @Override
	public void onInitialize() {
		LOGGER.info("{} initialising...", MOD_NAME);

        ModItems.initialize();
        ModBlocks.initialize();

		LOGGER.info("{} initialised.", MOD_NAME);
	}
}