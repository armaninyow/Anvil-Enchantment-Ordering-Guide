package com.armaninyow.aeog.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AeogClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// All computation is now done client-side in AeogOverlayScreen.
		// No network registration needed.
	}
}