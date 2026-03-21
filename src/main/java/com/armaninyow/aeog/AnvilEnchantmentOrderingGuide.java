package com.armaninyow.aeog;

import com.armaninyow.aeog.network.AeogPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilEnchantmentOrderingGuide implements ModInitializer {
	public static final String MOD_ID = "aeog";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AeogPackets.registerServerPackets();
		LOGGER.info("[AEOG] Anvil Enchantment Ordering Guide initialized.");
	}
}