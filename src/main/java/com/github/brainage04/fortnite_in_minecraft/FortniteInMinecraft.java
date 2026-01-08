package com.github.brainage04.fortnite_in_minecraft;

import com.github.brainage04.fortnite_in_minecraft.effect.ModEffects;
import com.github.brainage04.fortnite_in_minecraft.entity.ModEntities;
import com.github.brainage04.fortnite_in_minecraft.event.ModEvents;
import com.github.brainage04.fortnite_in_minecraft.item.ModItems;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FortniteInMinecraft implements ModInitializer {
    public static final String MOD_ID = "fortnite_in_minecraft";
    public static final String MOD_NAME = "Fortnite In Minecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("{} initializing...", MOD_ID);

        ModItems.initialize();
        ModEvents.initialize();
        ModEntities.initialize();
        ModEffects.initialize();

        if (PolymerResourcePackUtils.addModAssets(MOD_ID)) {
            LOGGER.info("Polymer resource pack added.");
        } else {
            LOGGER.error("Polymer resource pack not added - something went wrong!");
        }

        LOGGER.info("{} initialized.", MOD_ID);
    }
}