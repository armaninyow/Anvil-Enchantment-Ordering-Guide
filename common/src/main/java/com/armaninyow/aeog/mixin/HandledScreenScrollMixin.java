package com.armaninyow.aeog.mixin;

import com.armaninyow.aeog.client.AeogScrollState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public class HandledScreenScrollMixin {

	@Inject(at = @At("HEAD"), method = "mouseScrolled")
	private void aeog$onScroll(double mouseX, double mouseY, double hAmount, double vAmount,
	                            CallbackInfoReturnable<Boolean> cir) {
		AeogScrollState.add(vAmount);
	}
}