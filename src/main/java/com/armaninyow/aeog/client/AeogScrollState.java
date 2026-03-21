package com.armaninyow.aeog.client;

/**
 * Simple holder for the mouse scroll delta captured by the scroll mixin.
 * Kept outside the mixin class to avoid Mixin's restriction on non-private static methods.
 */
public final class AeogScrollState {
	private static double pending = 0;

	/** Called by HandledScreenScrollMixin when a scroll event occurs. */
	public static void add(double v) { pending += v; }

	/** Called by AeogOverlayScreen.pollMouse() each frame — reads and resets. */
	public static double consume() {
		double v = pending;
		pending  = 0;
		return v;
	}

	private AeogScrollState() {}
}