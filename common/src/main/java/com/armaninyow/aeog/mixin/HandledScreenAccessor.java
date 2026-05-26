package com.armaninyow.aeog.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected x/y GUI origin fields from HandledScreen
 * so that AnvilScreenMixin (which targets ForgingScreen) can read them
 * without a @Shadow on the wrong class.
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
	@Accessor("leftPos") int aeog$getX();
	@Accessor("topPos") int aeog$getY();
	@Accessor("leftPos") void aeog$setX(int x);
	@Accessor("topPos") void aeog$setY(int y);
}