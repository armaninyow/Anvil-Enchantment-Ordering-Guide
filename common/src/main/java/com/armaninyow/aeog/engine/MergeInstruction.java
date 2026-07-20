package com.armaninyow.aeog.engine;

import java.util.List;

public record MergeInstruction(
	NodeItem left,
	NodeItem right,
	int mergeCost,
	int mergeCostXp,
	int priorWorkPenalty
) {
	public record NodeItem(
		String id,
		List<String[]> enchants,
		int work,
		int level
	) {}
}