package com.armaninyow.aeog.mixin;

import com.armaninyow.aeog.AnvilEnchantmentOrderingGuide;
import com.armaninyow.aeog.client.screen.AeogOverlayScreen;
import com.armaninyow.aeog.client.screen.AeogPanelHost;
import com.armaninyow.aeog.network.AeogPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the AEOG panel into the vanilla Anvil GUI.
 *
 * Centering strategy: when the panel opens, we permanently shift HandledScreen.x
 * and all TextFieldWidget children by +CENTRE_SHIFT. This means ALL of vanilla's
 * rendering, input handling, and slot hit detection automatically use the shifted
 * position — no per-frame restore needed. When the panel closes we shift back.
 */
@Environment(EnvType.CLIENT)
@Mixin(ForgingScreen.class)
public abstract class AnvilScreenMixin implements AeogPanelHost {

	@Unique private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;

	@Unique private static final Identifier GUIDE_BTN =
		Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_disabled.png");
	@Unique private static final Identifier GUIDE_BTN_H =
		Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_disabled_highlighted.png");
	@Unique private static final Identifier GUIDE_BTN_ACTIVE =
		Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_enabled.png");
	@Unique private static final Identifier GUIDE_BTN_ACTIVE_H =
		Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "textures/gui/anvil/recipe_enabled_highlighted.png");

	@Unique private static final int BTN_W     = 20;
	@Unique private static final int BTN_H     = 18;
	@Unique private static final int BTN_REL_X = 4;
	@Unique private static final int BTN_REL_Y = 46;

	// panel(194) + gap(2) + anvil(176) = 372. Shift right by (194+2)/2 = 98.
	@Unique private static final int CENTRE_SHIFT = (AeogOverlayScreen.P_W + 2) / 2;

	@Unique private boolean aeog$panelOpen    = false;
	@Unique private boolean aeog$wasDown      = false;
	@Unique private boolean aeog$justOpened   = false;
	@Unique private boolean aeog$shifted      = false; // tracks whether x has been shifted
	@Unique private AeogOverlayScreen aeog$panel = null;

	// ── init: restore panel state when anvil screen opens ────────────────────

	@Inject(at = @At("TAIL"), method = "init")
	private void aeog$onInit(CallbackInfo ci) {
		if (!((Object)this instanceof AnvilScreen)) return;
		if (AeogOverlayScreen.s_panelWasOpen) {
			aeog$openPanel();
		}
	}

	// ── removed: save state when anvil screen closes ──────────────────────────

	@Inject(at = @At("HEAD"), method = "removed")
	private void aeog$onRemoved(CallbackInfo ci) {
		if (!((Object)this instanceof AnvilScreen)) return;
		if (aeog$panel != null) aeog$panel.saveState();
		// Don't change s_panelWasOpen here — preserve whatever it was last set to
	}

	// ── drawBackground: draw guide button ────────────────────────────────────

	@Inject(at = @At("TAIL"), method = "drawBackground")
	private void aeog$drawBtn(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (!((Object)this instanceof AnvilScreen)) return;

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
		ctx.drawTexture(PIPE, tex, btnX, btnY, 0f, 0f, BTN_W, BTN_H, BTN_W, BTN_H);

		boolean leftDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
			MinecraftClient.getInstance().getWindow().getHandle(),
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

	// ── render: draw full panel ───────────────────────────────────────────────

	@Inject(at = @At("TAIL"), method = "render")
	private void aeog$render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!((Object)this instanceof AnvilScreen)) return;

		HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;

		// Setting 1: auto-detect item in anvil target slot — works whether panel is open or not
		net.minecraft.item.ItemStack targetStack = net.minecraft.item.ItemStack.EMPTY;
		if (((net.minecraft.client.gui.screen.ingame.HandledScreen<?>)(Object)this)
				.getScreenHandler() instanceof net.minecraft.screen.ForgingScreenHandler fsh) {
			targetStack = fsh.getSlot(0).getStack();
		}

		// If panel is closed but auto-detect fires, open it first
		if (!aeog$panelOpen && com.armaninyow.dibs.config.AeogConfig.autoDetectItem
				&& !targetStack.isEmpty()) {
			aeog$openPanel();
		}

		if (!aeog$panelOpen || aeog$panel == null) return;

		// Tick auto-detect
		aeog$panel.tickAutoDetect(targetStack);

		aeog$panel.setJustOpened(aeog$justOpened);
		aeog$panel.render(ctx, acc.aeog$getX(), acc.aeog$getY(), mouseX, mouseY,
			MinecraftClient.getInstance().textRenderer);
		aeog$justOpened = false;
	}

	// ── Open / close helpers that apply the permanent shift ───────────────────

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

	/** Permanently shifts HandledScreen.x and all TextFieldWidgets by delta. */
	@Unique private void applyShift(int delta) {
		if (!((Object)this instanceof AnvilScreen)) return;
		HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;
		acc.aeog$setX(acc.aeog$getX() + delta);
		net.minecraft.client.gui.screen.Screen screen =
			(net.minecraft.client.gui.screen.Screen)(Object)this;
		for (net.minecraft.client.gui.Element child : screen.children()) {
			if (child instanceof net.minecraft.client.gui.widget.TextFieldWidget tf) {
				tf.setX(tf.getX() + delta);
			}
		}
	}

	// ── AeogPanelHost ─────────────────────────────────────────────────────────

	@Override public void aeog$openOverlay()  { aeog$openPanel(); }
	@Override public void aeog$onOverlayClosed() {
		if (aeog$panel != null) aeog$panel.saveState();
		aeog$panelOpen = false;
		aeog$panel     = null;
		applyShift(-CENTRE_SHIFT);
	}
	@Override public void aeog$receiveEngineResult(AeogPackets.EngineResultPayload payload) {
		if (aeog$panel != null) aeog$panel.receiveEngineResult(payload);
	}

	@Unique private void playAeogClick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.getSoundManager() == null) return;
		net.minecraft.util.Identifier id = net.minecraft.util.Identifier.of("aeog", "click_stereo");
		net.minecraft.sound.SoundEvent ev = net.minecraft.registry.Registries.SOUND_EVENT.get(id);
		if (ev == null) ev = net.minecraft.sound.SoundEvent.of(id);
		mc.getSoundManager().play(new net.minecraft.client.sound.PositionedSoundInstance(
			ev.id(),
			net.minecraft.sound.SoundCategory.MASTER,
			0.25f, 1.0f, net.minecraft.util.math.random.Random.create(),
			false, 0,
			net.minecraft.client.sound.SoundInstance.AttenuationType.NONE,
			0.0, 0.0, 0.0, true));
	}
}