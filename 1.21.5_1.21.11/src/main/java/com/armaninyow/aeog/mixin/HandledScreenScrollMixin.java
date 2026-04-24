package com.armaninyow.aeog.mixin;

import com.armaninyow.aeog.client.AeogScrollState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures vertical scroll from HandledScreen.mouseScrolled.
 * HandledScreen does override this method, so the inject is valid.
 * The delta is stored in AeogScrollState (a plain class) so that
 * AnvilScreenMixin can read it — Mixin forbids non-private static
 * methods on mixin classes, hence the external state holder.
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public class HandledScreenScrollMixin {

	@Inject(at = @At("HEAD"), method = "mouseScrolled")
	private void aeog$onScroll(double mouseX, double mouseY, double hAmount, double vAmount,
	                            CallbackInfoReturnable<Boolean> cir) {
		AeogScrollState.add(vAmount);
	}
}