package com.armaninyow.aeog.engine;

import java.util.*;
import java.util.Arrays;

/**
 * Java port of data.js.
 * Provides enchant definitions (levelMax, weight, incompatible, items)
 * and the ordered item list used for Phase 1 button layout.
 */
public final class EnchantData {

	public record EnchantDef(
		int levelMax,
		int weight,
		List<String> incompatible,
		List<String> items
	) {}

	/** Ordered map preserving insertion order (mirrors data.js enchants object). */
	public static final LinkedHashMap<String, EnchantDef> ENCHANTS = new LinkedHashMap<>();

	/** True only on versions that include spear items (1.21.11+). Checked lazily. */
	public static boolean hasSpear() {
		return net.minecraft.registry.Registries.ITEM.containsId(
			net.minecraft.util.Identifier.of("minecraft", "wooden_spear"));
	}

	/**
	 * Ordered item list for Phase 1 buttons.
	 * Spear is included only when present in the item registry.
	 */
	public static List<String> getPhase1Items() {
		ensureSpearEnchants();
		List<String> items = new ArrayList<>(List.of(
			"helmet",
			"chestplate",
			"leggings",
			"boots",
			"elytra",
			"sword",
			"axe",
			"mace"
		));
		if (hasSpear()) items.add("spear");
		items.addAll(List.of(
			"trident",
			"shield",
			"bow",
			"crossbow",
			"pickaxe",
			"shovel",
			"hoe",
			"brush",
			"shears",
			"flint_and_steel",
			"fishing_rod",
			"carrot_on_a_stick",
			"warped_fungus_on_a_stick",
			"book",
			"pumpkin"
		));
		return Collections.unmodifiableList(items);
	}

	private static boolean spearRegistered = false;

	/** Call before reading ENCHANTS if spear entries are needed. */
	private static void ensureSpearEnchants() {
		if (spearRegistered || !hasSpear()) return;
		spearRegistered = true;
		// Add spear to existing enchant item lists
		addItem("bane_of_arthropods", "spear");
		addItem("fire_aspect",        "spear");
		addItem("knockback",          "spear");
		addItem("looting",            "spear");
		addItem("mending",            "spear");
		addItem("sharpness",          "spear");
		addItem("smite",              "spear");
		addItem("unbreaking",         "spear");
		addItem("vanishing_curse",    "spear");
		// Patch lunge items (registered as placeholder in static block)
		addItem("lunge", "spear");
	}

	private static void addItem(String enchant, String item) {
		EnchantDef old = ENCHANTS.get(enchant);
		if (old == null) return;
		List<String> items = new ArrayList<>(old.items());
		items.add(item);
		ENCHANTS.put(enchant, new EnchantDef(old.levelMax(), old.weight(), old.incompatible(), items));
	}

	static {
		reg("protection",           4, 1, List.of("blast_protection","fire_protection","projectile_protection"),
			List.of("helmet","chestplate","leggings","boots","turtle_shell"));
		reg("aqua_affinity",        1, 2, List.of(),
			List.of("helmet","turtle_shell"));
		reg("bane_of_arthropods",   5, 1, List.of("smite","sharpness","density","breach"),
			List.of("sword","axe","mace"));
		reg("blast_protection",     4, 2, List.of("fire_protection","protection","projectile_protection"),
			List.of("helmet","chestplate","leggings","boots","turtle_shell"));
		reg("channeling",           1, 4, List.of("riptide"),
			List.of("trident"));
		reg("depth_strider",        3, 2, List.of("frost_walker"),
			List.of("boots"));
		reg("efficiency",           5, 1, List.of(),
			List.of("pickaxe","shovel","axe","hoe","shears"));
		reg("feather_falling",      4, 1, List.of(),
			List.of("boots"));
		reg("fire_aspect",          2, 2, List.of(),
			List.of("sword","mace"));
		reg("fire_protection",      4, 1, List.of("blast_protection","protection","projectile_protection"),
			List.of("helmet","chestplate","leggings","boots","turtle_shell"));
		reg("flame",                1, 2, List.of(),
			List.of("bow"));
		reg("fortune",              3, 2, List.of("silk_touch"),
			List.of("pickaxe","shovel","axe","hoe"));
		reg("frost_walker",         2, 2, List.of("depth_strider"),
			List.of("boots"));
		reg("impaling",             5, 2, List.of(),
			List.of("trident"));
		reg("infinity",             1, 4, List.of("mending"),
			List.of("bow"));
		reg("knockback",            2, 1, List.of(),
			List.of("sword"));
		reg("looting",              3, 2, List.of(),
			List.of("sword"));
		reg("lunge",                3, 1, List.of(),
			List.of()); // items added lazily by ensureSpearEnchants() on 1.21.11+
		reg("loyalty",              3, 1, List.of("riptide"),
			List.of("trident"));
		reg("luck_of_the_sea",      3, 2, List.of(),
			List.of("fishing_rod"));
		reg("lure",                 3, 2, List.of(),
			List.of("fishing_rod"));
		reg("mending",              1, 2, List.of("infinity"),
			List.of("helmet","chestplate","leggings","boots","pickaxe","shovel","axe","sword","hoe",
				"brush","fishing_rod","bow","shears","flint_and_steel","carrot_on_a_stick",
				"warped_fungus_on_a_stick","shield","elytra","trident","turtle_shell","crossbow","mace"));
		reg("multishot",            1, 2, List.of("piercing"),
			List.of("crossbow"));
		reg("piercing",             4, 1, List.of("multishot"),
			List.of("crossbow"));
		reg("power",                5, 1, List.of(),
			List.of("bow"));
		reg("projectile_protection",4, 1, List.of("protection","blast_protection","fire_protection"),
			List.of("helmet","chestplate","leggings","boots","turtle_shell"));
		reg("punch",                2, 2, List.of(),
			List.of("bow"));
		reg("quick_charge",         3, 1, List.of(),
			List.of("crossbow"));
		reg("respiration",          3, 2, List.of(),
			List.of("helmet","turtle_shell"));
		reg("riptide",              3, 2, List.of("channeling","loyalty"),
			List.of("trident"));
		reg("sharpness",            5, 1, List.of("bane_of_arthropods","smite"),
			List.of("sword","axe"));
		reg("silk_touch",           1, 4, List.of("fortune"),
			List.of("pickaxe","shovel","axe","hoe"));
		reg("smite",                5, 1, List.of("bane_of_arthropods","sharpness","density","breach"),
			List.of("sword","axe","mace"));
		reg("soul_speed",           3, 4, List.of(),
			List.of("boots"));
		reg("sweeping",             3, 2, List.of(),
			List.of("sword"));
		reg("swift_sneak",          3, 4, List.of(),
			List.of("leggings"));
		reg("thorns",               3, 4, List.of(),
			List.of("helmet","chestplate","leggings","boots","turtle_shell"));
		reg("unbreaking",           3, 1, List.of(),
			List.of("helmet","chestplate","leggings","boots","pickaxe","shovel","axe","sword","hoe",
				"brush","fishing_rod","bow","shears","flint_and_steel","carrot_on_a_stick",
				"warped_fungus_on_a_stick","shield","elytra","trident","turtle_shell","crossbow","mace"));
		reg("density",              5, 1, List.of("breach","smite","bane_of_arthropods"),
			List.of("mace"));
		reg("breach",               4, 2, List.of("density","smite","bane_of_arthropods"),
			List.of("mace"));
		reg("wind_burst",           3, 2, List.of(),
			List.of("mace"));
		reg("binding_curse",        1, 4, List.of(),
			List.of("helmet","chestplate","leggings","boots","elytra","pumpkin","turtle_shell"));
		reg("vanishing_curse",      1, 4, List.of(),
			List.of("helmet","chestplate","leggings","boots","pickaxe","shovel","axe","sword","hoe",
				"brush","fishing_rod","bow","shears","flint_and_steel","carrot_on_a_stick",
				"warped_fungus_on_a_stick","shield","elytra","pumpkin","trident","turtle_shell",
				"crossbow","mace"));
	}

	private static void reg(String name, int levelMax, int weight,
	                         List<String> incompatible, List<String> items) {
		ENCHANTS.put(name, new EnchantDef(levelMax, weight, incompatible, items));
	}

	/**
	 * Returns enchants applicable to the given item name.
	 * "helmet" also includes turtle_shell enchants.
	 */
	public static List<String> enchantsForItem(String item) {
		ensureSpearEnchants();
		// "book" can hold any enchantment — skip placeholder enchants with no items
		if (item.equals("book")) {
			return ENCHANTS.entrySet().stream()
				.filter(e -> !e.getValue().items().isEmpty())
				.map(Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		}
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, EnchantDef> entry : ENCHANTS.entrySet()) {
			List<String> items = entry.getValue().items();
			if (items.contains(item) || (item.equals("helmet") && items.contains("turtle_shell"))) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private EnchantData() {}
}