package com.armaninyow.aeog.client.screen;

import com.armaninyow.aeog.network.AeogPackets;

/**
 * Implemented by {@link com.armaninyow.aeog.mixin.AnvilScreenMixin} (which targets ForgingScreen).
 * Exposes all mixin-injected panel methods to code that cannot reference the mixin class directly.
 */
public interface AeogPanelHost {

	/** Opens the AEOG panel. */
	void aeog$openOverlay();

	/** Hides the AEOG panel and resets the button texture. */
	void aeog$onOverlayClosed();

	/** Forwards an S2C engine result to the open panel. */
	void aeog$receiveEngineResult(AeogPackets.EngineResultPayload payload);
}