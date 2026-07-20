package com.armaninyow.aeog.engine;

import java.util.*;

public final class OptimizationEngine {

	public enum Mode { LEVELS, WORK }

	private static final int MAXIMUM_MERGE_LEVELS = 39;

	private final Map<String, Integer> idList = new LinkedHashMap<>();
	private final List<Integer> enchant2Weight = new ArrayList<>();

	private final Map<String, Map<Integer, ItemObj>> memo = new HashMap<>();

	public OptimizationEngine() {
		int id = 0;
		for (Map.Entry<String, EnchantData.EnchantDef> e : EnchantData.ENCHANTS.entrySet()) {
			idList.put(e.getKey(), id);
			enchant2Weight.add(e.getValue().weight());
			id++;
		}
	}

	public int registerExtraEnchant(String key, int weight) {
		if (idList.containsKey(key)) return idList.get(key);
		int id = enchant2Weight.size();
		idList.put(key, id);
		enchant2Weight.add(weight);
		return id;
	}

	public int getWeightFor(String key) {
		Integer id = idList.get(key);
		if (id == null || id >= enchant2Weight.size()) return 1;
		int w = enchant2Weight.get(id);
		return w > 0 ? w : 1;
	}

	public List<MergeInstruction> process(String itemName, List<int[]> enchants, Mode mode) {
		memo.clear();

		List<ItemObj> enchantObjs = new ArrayList<>();
		for (int[] pair : enchants) {
			int id = pair[0];
			int level = pair[1];
			int value = level * enchant2Weight.get(id);
			ItemObj e = new ItemObj("book", value, new ArrayList<>(List.of(id)));
			enchantObjs.add(e);
		}
		enchantObjs.sort((a, b) -> Integer.compare(b.l, a.l));

		int mostExpensive = indexOfMostExpensive(enchantObjs);

		ItemObj baseItem;
		if (itemName.equals("book")) {
			int id = enchantObjs.get(mostExpensive).e.get(0);
			String enchantName = enchantNameFromId(id);
			baseItem = new ItemObj(enchantName, enchantObjs.get(mostExpensive).l);
			baseItem.e.add(id);
			enchantObjs.remove(mostExpensive);
			mostExpensive = indexOfMostExpensive(enchantObjs);
		} else {
			baseItem = new ItemObj("item");
		}

		if (enchantObjs.isEmpty()) {
			List<MergeInstruction> out = new ArrayList<>();
			addInstructions(baseItem.c, out, itemName);
			return out;
		}

		MergeObj merged = new MergeObj(baseItem, enchantObjs.get(mostExpensive));
		merged.c.L = nodeFromItem(baseItem, itemName);
		enchantObjs.remove(mostExpensive);

		List<ItemObj> allObjs = new ArrayList<>(enchantObjs);
		allObjs.add(merged);

		Map<Integer, ItemObj> cheapestItems = cheapestItemsFromList(allObjs);

		double cheapestCost = Double.MAX_VALUE;
		int cheapestWork = -1;
		for (Map.Entry<Integer, ItemObj> entry : cheapestItems.entrySet()) {
			ItemObj item = entry.getValue();
			double cost = (mode == Mode.LEVELS) ? item.x : item.w;
			if (cost < cheapestCost) {
				cheapestCost = cost;
				cheapestWork = entry.getKey();
			}
		}

		ItemObj best = cheapestItems.get(cheapestWork);
		List<MergeInstruction> instructions = new ArrayList<>();
		addInstructions(best.c, instructions, itemName);
		return instructions;
	}

	private int indexOfMostExpensive(List<ItemObj> list) {
		int maxIdx = 0;
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).l > list.get(maxIdx).l) maxIdx = i;
		}
		return maxIdx;
	}

	private static int experience(int level) {
		if (level == 0) return 0;
		if (level <= 16) return level * level + 6 * level;
		if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
		return (int)(4.5 * level * level - 162.5 * level + 2220);
	}

	private Map<Integer, ItemObj> cheapestItemsFromList(List<ItemObj> items) {
		String key = memoKey(items);
		if (memo.containsKey(key)) return memo.get(key);
		Map<Integer, ItemObj> result = cheapestItemsFromListImpl(items);
		memo.put(key, result);
		return result;
	}

	private String memoKey(List<ItemObj> items) {
		StringBuilder sb = new StringBuilder();
		for (ItemObj item : items) {
			List<Integer> sorted = new ArrayList<>(item.e);
			Collections.sort(sorted);
			sb.append(item.i.charAt(0)).append(sorted).append(item.w).append(item.l).append('|');
		}
		return sb.toString();
	}

	private Map<Integer, ItemObj> cheapestItemsFromListImpl(List<ItemObj> items) {
		switch (items.size()) {
			case 1 -> {
				Map<Integer, ItemObj> m = new LinkedHashMap<>();
				m.put(items.get(0).w, items.get(0));
				return m;
			}
			case 2 -> {
				ItemObj merged = cheapestFromTwo(items.get(0), items.get(1));
				Map<Integer, ItemObj> m = new LinkedHashMap<>();
				m.put(merged.w, merged);
				return m;
			}
			default -> {
				return cheapestFromListN(items, items.size() / 2);
			}
		}
	}

	private ItemObj cheapestFromTwo(ItemObj left, ItemObj right) {
		if (right.i.equals("item")) return safeMerge(right, left);
		if (left.i.equals("item"))  return safeMerge(left, right);

		MergeObj normal = null, reversed = null;
		try { normal   = new MergeObj(left, right);  } catch (TooExpensiveException ignored) {}
		try { reversed = new MergeObj(right, left); } catch (TooExpensiveException ignored) {}

		if (normal == null && reversed == null) throw new TooExpensiveException();
		if (normal == null)   return reversed;
		if (reversed == null) return normal;

		return compareCheapest(normal, reversed);
	}

	private ItemObj safeMerge(ItemObj a, ItemObj b) {
		try { return new MergeObj(a, b); }
		catch (TooExpensiveException e) { return new MergeObj(b, a); }
	}

	private Map<Integer, ItemObj> cheapestFromListN(List<ItemObj> items, int maxSub) {
		Map<Integer, ItemObj> cheapestWork2Item = new LinkedHashMap<>();
		Set<Integer> cheapestWorks = new HashSet<>();

		for (int subcount = 1; subcount <= maxSub; subcount++) {
			for (int[] leftIndices : indexCombinations(items.size(), subcount)) {
				Set<Integer> leftSet = new HashSet<>();
				for (int idx : leftIndices) leftSet.add(idx);

				List<ItemObj> left  = new ArrayList<>();
				List<ItemObj> right = new ArrayList<>();
				for (int i = 0; i < items.size(); i++) {
					if (leftSet.contains(i)) left.add(items.get(i));
					else right.add(items.get(i));
				}

				Map<Integer, ItemObj> leftMap  = cheapestItemsFromList(left);
				Map<Integer, ItemObj> rightMap = cheapestItemsFromList(right);
				Map<Integer, ItemObj> merged   = cheapestFromDicts(leftMap, rightMap);

				for (Map.Entry<Integer, ItemObj> entry : merged.entrySet()) {
					int w = entry.getKey();
					ItemObj item = entry.getValue();
					if (cheapestWorks.contains(w)) {
						cheapestWork2Item.put(w, compareCheapest(cheapestWork2Item.get(w), item));
					} else {
						cheapestWork2Item.put(w, item);
						cheapestWorks.add(w);
					}
				}
			}
		}
		return cheapestWork2Item;
	}

	private List<int[]> indexCombinations(int n, int k) {
		List<int[]> result = new ArrayList<>();
		indexCombHelper(0, n, k, new int[k], 0, result);
		return result;
	}

	private void indexCombHelper(int start, int n, int k, int[] current, int depth, List<int[]> result) {
		if (depth == k) { result.add(current.clone()); return; }
		for (int i = start; i <= n - (k - depth); i++) {
			current[depth] = i;
			indexCombHelper(i + 1, n, k, current, depth + 1, result);
		}
	}

	private Map<Integer, ItemObj> cheapestFromDicts(Map<Integer, ItemObj> leftMap,
	                                                 Map<Integer, ItemObj> rightMap) {
		Map<Integer, ItemObj> result = new LinkedHashMap<>();
		Set<Integer> seen = new HashSet<>();

		for (ItemObj left : leftMap.values()) {
			for (ItemObj right : rightMap.values()) {
				Map<Integer, ItemObj> merged;
				try {
					merged = cheapestItemsFromList(List.of(left, right));
				} catch (TooExpensiveException e) {
					continue;
				}
				for (Map.Entry<Integer, ItemObj> entry : merged.entrySet()) {
					int w = entry.getKey();
					ItemObj item = entry.getValue();
					if (seen.contains(w)) {
						result.put(w, compareCheapest(result.get(w), item));
					} else {
						result.put(w, item);
						seen.add(w);
					}
				}
			}
		}
		return result;
	}

	private ItemObj compareCheapest(ItemObj a, ItemObj b) {
		if (a.w != b.w) return a;
		if (a.l != b.l) return a.l < b.l ? a : b;
		return a.x <= b.x ? a : b;
	}

	private void addInstructions(CNode comb, List<MergeInstruction> out, String itemName) {
		if (comb == null) return;
		if (comb.L != null && comb.L.nested != null) addInstructions(comb.L.nested, out, itemName);
		if (comb.R != null && comb.R.nested != null) addInstructions(comb.R.nested, out, itemName);

		if (comb.L == null || comb.R == null) return;

		int mergeCost;
		int rVal;
		if (comb.R.valueOverride >= 0) {
			rVal = comb.R.valueOverride;
		} else if (comb.R.nested != null) {
			rVal = comb.R.nested.v;
		} else {
			rVal = comb.R.l;
		}
		mergeCost = rVal + (1 << comb.L.w) - 1 + (1 << comb.R.w) - 1;
		int resultWork = Math.max(comb.L.w, comb.R.w) + 1;
		int priorWorkPenalty = (1 << resultWork) - 1;

		MergeInstruction.NodeItem left  = toNodeItem(comb.L, itemName);
		MergeInstruction.NodeItem right = toNodeItem(comb.R, itemName);

		out.add(new MergeInstruction(left, right, mergeCost, experience(mergeCost), priorWorkPenalty));
	}

	private MergeInstruction.NodeItem toNodeItem(CNode.Ref ref, String itemName) {
		String id;
		List<String[]> enchants = new ArrayList<>();

		if (ref.enchantId >= 0) {
			id = "book";
			String enchantName = enchantNameFromId(ref.enchantId);
			int val = ref.valueOverride >= 0 ? ref.valueOverride : ref.l;
			int weight = enchant2Weight.get(ref.enchantId);
			int level  = (weight > 0) ? (val / weight) : 1;
			enchants.add(new String[]{enchantName, String.valueOf(level)});
		} else if (ref.nested != null) {
			id = resolveNodeId(ref.nested, itemName);
			enchants = collectEnchants(ref.nested);
		} else {
			String rawId = ref.itemId != null ? ref.itemId : itemName;
			if (rawId.equals("item")) {
				id = itemName;
			} else if (rawId.equals("book") || idList.containsKey(rawId)) {
				id = "book";
				if (idList.containsKey(rawId)) {
					int eid = idList.get(rawId);
					int val    = ref.valueOverride >= 0 ? ref.valueOverride : ref.l;
					int weight = enchant2Weight.get(eid);
					int level  = (weight > 0) ? (val / weight) : 1;
					enchants.add(new String[]{rawId, String.valueOf(level)});
				}
			} else {
				id = rawId;
			}
			if (ref.enchantsList != null) enchants = ref.enchantsList;
		}

		return new MergeInstruction.NodeItem(id, enchants, ref.w, ref.l);
	}

	private String resolveNodeId(CNode comb, String itemName) {
		if (comb == null) return "book";
		if (hasItemLeaf(comb)) return itemName;
		return "book";
	}

	private boolean hasItemLeaf(CNode comb) {
		if (comb == null) return false;
		if (comb.L != null) {
			if (comb.L.nested != null && hasItemLeaf(comb.L.nested)) return true;
			if (comb.L.itemId != null && comb.L.itemId.equals("item")) return true;
			if (comb.L.enchantId < 0 && comb.L.itemId != null
				&& !idList.containsKey(comb.L.itemId) && !comb.L.itemId.equals("book")) return true;
		}
		if (comb.R != null) {
			if (comb.R.nested != null && hasItemLeaf(comb.R.nested)) return true;
			if (comb.R.itemId != null && comb.R.itemId.equals("item")) return true;
			if (comb.R.enchantId < 0 && comb.R.itemId != null
				&& !idList.containsKey(comb.R.itemId) && !comb.R.itemId.equals("book")) return true;
		}
		return false;
	}

	private List<String[]> collectEnchants(CNode comb) {
		List<String[]> result = new ArrayList<>();
		if (comb == null) return result;
		if (comb.L != null) result.addAll(collectEnchantsFromRef(comb.L));
		if (comb.R != null) result.addAll(collectEnchantsFromRef(comb.R));
		return result;
	}

	private List<String[]> collectEnchantsFromRef(CNode.Ref ref) {
		if (ref == null) return List.of();
		if (ref.nested != null) return collectEnchants(ref.nested);
		if (ref.enchantId >= 0) {
			String name = enchantNameFromId(ref.enchantId);
			int val    = ref.valueOverride >= 0 ? ref.valueOverride : ref.l;
			int weight = enchant2Weight.get(ref.enchantId);
			int level  = (weight > 0) ? (val / weight) : 1;
			return List.<String[]>of(new String[]{name, String.valueOf(level)});
		}
		if (ref.enchantsList != null) return ref.enchantsList;
		return List.of();
	}

	private boolean isItemType(String s) {
		return s.equals("book") || s.equals("item") || idList.containsKey(s);
	}

	private String enchantNameFromId(int id) {
		for (Map.Entry<String, Integer> e : idList.entrySet()) {
			if (e.getValue() == id) return e.getKey();
		}
		return "unknown";
	}

	private CNode.Ref nodeFromItem(ItemObj item, String itemName) {
		CNode.Ref ref = new CNode.Ref();
		if (item.i.equals("item")) {
			ref.itemId = itemName;
		} else if (item.i.equals("book") && item.e.size() == 1) {
			ref.enchantId     = item.e.get(0);
			ref.valueOverride = item.l;
		} else {
			ref.itemId = item.i;
		}
		ref.w = item.w;
		ref.l = item.l;
		return ref;
	}

	static class ItemObj {
		String i;
		List<Integer> e;
		CNode c;
		int w;
		int l;
		double x;

		ItemObj(String name, int value, List<Integer> ids) {
			this.i = name; this.l = value; this.e = new ArrayList<>(ids);
			this.w = 0; this.x = 0; this.c = null;
		}
		ItemObj(String name, int value) { this(name, value, new ArrayList<>()); }
		ItemObj(String name) { this(name, 0, new ArrayList<>()); }
	}

	class MergeObj extends ItemObj {
		MergeObj(ItemObj left, ItemObj right) {
			super(left.i, left.l + right.l);
			int mergeCost = right.l + (1 << left.w) - 1 + (1 << right.w) - 1;
			if (mergeCost > MAXIMUM_MERGE_LEVELS) throw new TooExpensiveException();
			this.e = new ArrayList<>(left.e);
			this.e.addAll(right.e);
			this.w = Math.max(left.w, right.w) + 1;
			this.x = left.x + right.x + experience(mergeCost);

			CNode cn = new CNode();
			cn.L = left.c != null ? left.c.asRef() : nodeFromItem(left, "item");
			cn.R = right.c != null ? right.c.asRef() : rightRefFromItem(right);
			cn.l = mergeCost;
			cn.w = this.w;
			cn.v = this.l;
			this.c = cn;
		}

		private CNode.Ref rightRefFromItem(ItemObj item) {
			if (item.c != null) return item.c.asRef();
			CNode.Ref ref = new CNode.Ref();
			if (item.e.size() == 1) {
				ref.enchantId = item.e.get(0);
				ref.valueOverride = item.l;
			} else {
				ref.itemId = item.i;
			}
			ref.w = item.w;
			ref.l = item.l;
			return ref;
		}
	}

	static class CNode {
		Ref L, R;
		int l, w, v;

		CNode() {}
		CNode(int enchantId, int l, int w) {
			this.L = null; this.R = null;
			this.l = l; this.w = w;
		}

		Ref asRef() {
			Ref ref = new Ref();
			ref.nested = this;
			ref.l = l; ref.w = w;
			return ref;
		}

		static class Ref {
			CNode nested;
			int enchantId = -1;
			String itemId;
			List<String[]> enchantsList;
			int w, l;
			int valueOverride = -1;
		}
	}

	static class TooExpensiveException extends RuntimeException {
		TooExpensiveException() { super("merge levels is above maximum allowed"); }
	}
}