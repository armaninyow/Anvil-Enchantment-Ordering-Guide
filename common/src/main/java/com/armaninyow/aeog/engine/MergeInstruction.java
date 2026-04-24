package com.armaninyow.aeog.engine;

import java.util.List;

/**
 * Represents a single merge step produced by the OptimizationEngine.
 * Mirrors one entry of the {@code instructions} array from work.js:
 *   [L, R, merge_cost, merge_xp, prior_work_penalty]
 *
 * Each NodeItem carries:
 *   - id       : enchant name, "item", or a specific item string like "sword"
 *   - enchants : list of enchant names on this intermediate item
 *   - work     : prior-work counter (w)
 *   - level    : value (l) — sum of enchant weights * level
 */
public record MergeInstruction(
	NodeItem left,
	NodeItem right,
	int mergeCost,
	int mergeCostXp,
	int priorWorkPenalty
) {
	public record NodeItem(
		String id,          // enchant name, "book", or item type ("sword", "item")
		List<String[]> enchants, // each: [enchantName, levelString]
		int work,
		int level
	) {}
}