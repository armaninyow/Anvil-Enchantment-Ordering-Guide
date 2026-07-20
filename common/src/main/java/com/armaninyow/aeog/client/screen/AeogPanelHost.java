package com.armaninyow.aeog.client.screen;

import com.armaninyow.aeog.engine.MergeInstruction;
import java.util.List;

public interface AeogPanelHost {

	void aeog$openOverlay();

	void aeog$onOverlayClosed();

	void aeog$receiveEngineResult(List<MergeInstruction> result);
}