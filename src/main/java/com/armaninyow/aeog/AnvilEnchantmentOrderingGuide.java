package com.armaninyow.aeog;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilEnchantmentOrderingGuide implements ModInitializer {
	public static final String MOD_ID = "aeog";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		com.armaninyow.dibs.config.AeogConfig.load();
		LOGGER.info("[AEOG] Anvil Enchantment Ordering Guide initialized.");
	}
}