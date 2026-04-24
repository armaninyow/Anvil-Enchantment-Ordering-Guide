package com.armaninyow.aeog.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected x/y GUI origin fields from HandledScreen
 * so that AnvilScreenMixin (which targets ForgingScreen) can read them
 * without a @Shadow on the wrong class.
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x") int aeog$getX();
	@Accessor("y") int aeog$getY();
	@Accessor("x") void aeog$setX(int x);
	@Accessor("y") void aeog$setY(int y);
}