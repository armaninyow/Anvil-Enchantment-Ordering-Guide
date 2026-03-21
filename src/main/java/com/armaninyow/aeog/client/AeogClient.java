package com.armaninyow.aeog.client;

import com.armaninyow.aeog.client.screen.AeogPanelHost;
import com.armaninyow.aeog.network.AeogPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;

@Environment(EnvType.CLIENT)
public class AeogClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// S2C: engine result — forward to the open AnvilScreen's panel
		ClientPlayNetworking.registerGlobalReceiver(AeogPackets.EngineResultPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				MinecraftClient mc = context.client();
				if (mc.currentScreen instanceof AnvilScreen
					&& mc.currentScreen instanceof AeogPanelHost host) {
					host.aeog$receiveEngineResult(payload);
				}
			});
		});
	}
}