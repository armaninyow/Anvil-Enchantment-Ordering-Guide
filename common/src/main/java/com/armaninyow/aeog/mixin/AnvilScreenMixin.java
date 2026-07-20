package com.armaninyow.aeog.mixin;

import com.armaninyow.aeog.AnvilEnchantmentOrderingGuide;
import com.armaninyow.aeog.client.screen.AeogOverlayScreen;
import com.armaninyow.aeog.client.screen.AeogPanelHost;
import com.armaninyow.aeog.engine.MergeInstruction;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;

import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemCombinerScreen.class)
public abstract class AnvilScreenMixin implements AeogPanelHost {

	@Unique private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;

	@Unique private static final net.minecraft.resources.Identifier GUIDE_BTN =
		net.minecraft.resources.Identifier.fromNamespaceAndPath(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_disabled.png");
	@Unique private static final net.minecraft.resources.Identifier GUIDE_BTN_H =
		net.minecraft.resources.Identifier.fromNamespaceAndPath(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_disabled_highlighted.png");
	@Unique private static final net.minecraft.resources.Identifier GUIDE_BTN_ACTIVE =
		net.minecraft.resources.Identifier.fromNamespaceAndPath(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_enabled.png");
	@Unique private static final net.minecraft.resources.Identifier GUIDE_BTN_ACTIVE_H =
		net.minecraft.resources.Identifier.fromNamespaceAndPath(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_enabled_highlighted.png");

	@Unique private static final int BTN_W     = 20;
	@Unique private static final int BTN_H     = 18;
	@Unique private static final int BTN_REL_X = 4;
	@Unique private static final int BTN_REL_Y = 46;

	@Unique private static final int CENTRE_SHIFT = (AeogOverlayScreen.P_W + 2) / 2;

	@Unique private boolean aeog$panelOpen    = false;
	@Unique private boolean aeog$wasDown      = false;
	@Unique private boolean aeog$justOpened   = false;
	@Unique private boolean aeog$shifted      = false;
	@Unique private AeogOverlayScreen aeog$panel = null;

	@Inject(at = @At("TAIL"), method = "init")
	private void aeog$onInit(CallbackInfo ci) {
		if (!((Object)this instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen)) return;
		if (AeogOverlayScreen.s_panelWasOpen) {
			aeog$openPanel();
		}
	}

	@Inject(at = @At("HEAD"), method = "removed")
	private void aeog$onRemoved(CallbackInfo ci) {
		if (!((Object)this instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen)) return;
		if (aeog$panel != null) aeog$panel.saveState();
	}

	@Inject(at = @At("TAIL"), method = "extractBackground")
	private void aeog$drawBtn(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!((Object)this instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen)) return;

		HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;
		int guiX = acc.aeog$getX();
		int guiY = acc.aeog$getY();
		int btnX = guiX + BTN_REL_X;
		int btnY = guiY + BTN_REL_Y;

		boolean hovered = mouseX >= btnX && mouseX < btnX + BTN_W
			&& mouseY >= btnY && mouseY < btnY + BTN_H;
		Identifier tex = aeog$panelOpen
			? (hovered ? GUIDE_BTN_ACTIVE_H : GUIDE_BTN_ACTIVE)
			: (hovered ? GUIDE_BTN_H        : GUIDE_BTN);
		ctx.blit(PIPE, tex, btnX, btnY, 0f, 0f, BTN_W, BTN_H, BTN_W, BTN_H);

		boolean leftDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
			Minecraft.getInstance().getWindow().handle(),
			org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

		if (leftDown && !aeog$wasDown && hovered) {
			if (aeog$panelOpen) {
				aeog$closePanel();
			} else {
				aeog$openPanel();
			}
			playAeogClick();
		}
		aeog$wasDown = leftDown;
	}

	@Inject(at = @At("TAIL"), method = "extractBackground")
	private void aeog$render(GuiGraphicsExtractor ctx, int guiLeft, int guiTop, float delta, CallbackInfo ci) {
		if (!((Object)this instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen)) return;

		HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;
		net.minecraft.client.Minecraft _mc = net.minecraft.client.Minecraft.getInstance();
		int mouseX = (int)(_mc.mouseHandler.xpos() * _mc.getWindow().getGuiScaledWidth() / _mc.getWindow().getScreenWidth());
		int mouseY = (int)(_mc.mouseHandler.ypos() * _mc.getWindow().getGuiScaledHeight() / _mc.getWindow().getScreenHeight());

		net.minecraft.world.item.ItemStack targetStack = net.minecraft.world.item.ItemStack.EMPTY;
		if (((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)(Object)this)
				.getMenu() instanceof net.minecraft.world.inventory.ItemCombinerMenu fsh) {
			targetStack = fsh.getSlot(0).getItem();
		}

		if (!aeog$panelOpen && com.armaninyow.dibs.config.AeogConfig.autoDetectItem
				&& !targetStack.isEmpty()) {
			aeog$openPanel();
		}

		if (!aeog$panelOpen || aeog$panel == null) return;

		aeog$panel.tickAutoDetect(targetStack);

		aeog$panel.setJustOpened(aeog$justOpened);
		aeog$panel.render(ctx, acc.aeog$getX(), acc.aeog$getY(), mouseX, mouseY,
			Minecraft.getInstance().font);
		aeog$justOpened = false;
	}

	@Unique private void aeog$openPanel() {
		if (aeog$panelOpen) return;
		aeog$panelOpen  = true;
		aeog$justOpened = true;
		aeog$panel      = new AeogOverlayScreen();
		AeogOverlayScreen.s_panelWasOpen = true;
		applyShift(CENTRE_SHIFT);
	}

	@Unique private void aeog$closePanel() {
		if (!aeog$panelOpen) return;
		if (aeog$panel != null) aeog$panel.saveState();
		AeogOverlayScreen.s_panelWasOpen = false;
		aeog$panelOpen = false;
		aeog$panel     = null;
		applyShift(-CENTRE_SHIFT);
	}

	@Unique private void applyShift(int delta) {
		if (!((Object)this instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen)) return;
		HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;
		acc.aeog$setX(acc.aeog$getX() + delta);
		net.minecraft.client.gui.screens.Screen screen =
			(net.minecraft.client.gui.screens.Screen)(Object)this;
		for (net.minecraft.client.gui.components.events.GuiEventListener child : screen.children()) {
			if (child instanceof net.minecraft.client.gui.components.EditBox tf) {
				tf.setX(tf.getX() + delta);
			}
		}
	}

	@Override public void aeog$openOverlay()  { aeog$openPanel(); }
	@Override public void aeog$onOverlayClosed() {
		if (aeog$panel != null) aeog$panel.saveState();
		aeog$panelOpen = false;
		aeog$panel     = null;
		applyShift(-CENTRE_SHIFT);
	}
	@Override public void aeog$receiveEngineResult(java.util.List<com.armaninyow.aeog.engine.MergeInstruction> result) {
		if (aeog$panel != null) aeog$panel.receiveEngineResult(result);
	}

	@Unique private void playAeogClick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager() == null) return;
		mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
			net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.25f));
	}
}