package com.armaninyow.aeog.client.screen;

import com.armaninyow.aeog.AnvilEnchantmentOrderingGuide;
import com.armaninyow.aeog.engine.EnchantData;
import com.armaninyow.aeog.engine.MergeInstruction;
import com.armaninyow.aeog.engine.OptimizationEngine;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.resources.language.I18n;

import java.util.*;

@Environment(EnvType.CLIENT)
public class AeogOverlayScreen {

	private static final RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;

	private static final String TEX   = "textures/gui/phases/";
	private static final String ITEMS = "textures/items/";

	private static final net.minecraft.resources.Identifier P1_CONTAINER  = id(TEX + "phase_1_container.png");
	private static final net.minecraft.resources.Identifier P2_CONTAINER  = id(TEX + "phase_2_container.png");
	private static final net.minecraft.resources.Identifier P3_CONTAINER  = id(TEX + "phase_3_container.png");
	private static final net.minecraft.resources.Identifier BACKGROUND    = id(TEX + "background.png");

	private static final net.minecraft.resources.Identifier BTN_ITEM      = id(TEX + "button.png");
	private static final net.minecraft.resources.Identifier BTN_ITEM_H    = id(TEX + "button_highlighted.png");
	private static final net.minecraft.resources.Identifier BTN_OPT       = id(TEX + "optimize_button.png");
	private static final net.minecraft.resources.Identifier BTN_OPT_H     = id(TEX + "optimize_button_highlighted.png");
	private static final net.minecraft.resources.Identifier BTN_CALC      = id(TEX + "calculate_button.png");
	private static final net.minecraft.resources.Identifier BTN_CALC_H    = id(TEX + "calculate_button_highlighted.png");
	private static final net.minecraft.resources.Identifier BTN_CALC_DIS  = id(TEX + "calculate_button_disabled.png");
	private static final net.minecraft.resources.Identifier BTN_BACK      = id(TEX + "phase_backward.png");
	private static final net.minecraft.resources.Identifier BTN_BACK_H    = id(TEX + "phase_backward_highlighted.png");

	private static final net.minecraft.resources.Identifier ENCH_SLOT     = id(TEX + "enchantment_slot.png");
	private static final net.minecraft.resources.Identifier ENCH_SLOT_H   = id(TEX + "enchantment_slot_highlighted.png");
	private static final net.minecraft.resources.Identifier ENCH_SLOT_DIS = id(TEX + "enchantment_slot_disabled.png");
	private static final net.minecraft.resources.Identifier ENCH_PREV     = id(TEX + "enchantment_prev.png");
	private static final net.minecraft.resources.Identifier ENCH_PREV_H   = id(TEX + "enchantment_prev_highlighted.png");
	private static final net.minecraft.resources.Identifier ENCH_NEXT     = id(TEX + "enchantment_next.png");
	private static final net.minecraft.resources.Identifier ENCH_NEXT_H   = id(TEX + "enchantment_next_highlighted.png");
	private static final net.minecraft.resources.Identifier LVL_SLOT      = id(TEX + "level_slot.png");
	private static final net.minecraft.resources.Identifier LVL_SLOT_DIS  = id(TEX + "level_slot_disabled.png");
	private static final net.minecraft.resources.Identifier LVL_SLOT_SEL  = id(TEX + "level_slot_selected.png");
	private static final net.minecraft.resources.Identifier SCROLLER      = id(TEX + "scroller.png");

	private static final net.minecraft.resources.Identifier FRAME_INIT_O  = id(TEX + "initial_frame_obtained.png");
	private static final net.minecraft.resources.Identifier FRAME_INIT_U  = id(TEX + "initial_frame_unobtained.png");
	private static final net.minecraft.resources.Identifier FRAME_PROG_O  = id(TEX + "progress_frame_obtained.png");
	private static final net.minecraft.resources.Identifier FRAME_PROG_U  = id(TEX + "progress_frame_unobtained.png");
	private static final net.minecraft.resources.Identifier FRAME_FINAL_O = id(TEX + "final_frame_obtained.png");
	private static final net.minecraft.resources.Identifier FRAME_FINAL_U = id(TEX + "final_frame_unobtained.png");
	private static final net.minecraft.resources.Identifier FRAME_INIT_OH  = id(TEX + "initial_frame_obtained_highlighted.png");
	private static final net.minecraft.resources.Identifier FRAME_PROG_OH  = id(TEX + "progress_frame_obtained_highlighted.png");
	private static final net.minecraft.resources.Identifier SLOT_HL_BACK  = id(TEX + "slot_highlight_back.png");
	private static final net.minecraft.resources.Identifier SLOT_HL_FRONT = id(TEX + "slot_highlight_front.png");
	private static final net.minecraft.resources.Identifier P3_CONTAINER_2   = id(TEX + "phase_3_container_2.png");
	private static final net.minecraft.resources.Identifier LIST_FRAME_O  = id(TEX + "list_frame_obtained.png");
	private static final net.minecraft.resources.Identifier LIST_FRAME_U  = id(TEX + "list_frame_unobtained.png");

	private static final net.minecraft.resources.Identifier BTN_MOD   = id(TEX + "mod_button_disabled.png");
	private static final net.minecraft.resources.Identifier BTN_MOD_H = id(TEX + "mod_button_enabled.png");
	private static final int MOD_BTN_X = 179, MOD_BTN_Y = 6, MOD_BTN_SIZE = 8;

	private static Map<String, net.minecraft.resources.Identifier> ITEM_ICONS = null;

	public record ModdedCategory(
		String id,
		List<net.minecraft.world.item.Item> items,
		List<String> enchants
	) {}

	private static List<ModdedCategory> s_moddedCategories = null;

	private static final Map<String, String> s_moddedEnchantNames = new LinkedHashMap<>();

	private static final Set<String> s_loggedMissingTranslations = new HashSet<>();

	public static boolean isModdedId(String id) {
		return id != null && id.startsWith("modded:");
	}

	private static List<ModdedCategory> buildModdedCategories() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return List.of();

		Set<String> vanillaIds = new HashSet<>();
		for (net.minecraft.world.item.Item item : List.of(
			Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET,
			Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET, Items.TURTLE_HELMET,
			Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE, Items.IRON_CHESTPLATE,
			Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE,
			Items.ELYTRA,
			Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS, Items.IRON_LEGGINGS,
			Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS,
			Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS, Items.IRON_BOOTS,
			Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS,
			Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.IRON_SWORD,
			Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD,
			Items.DIAMOND_AXE, Items.NETHERITE_AXE, Items.IRON_AXE,
			Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE,
			Items.MACE,
			Items.TRIDENT, Items.SHIELD, Items.BOW, Items.CROSSBOW,
			Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.IRON_PICKAXE,
			Items.GOLDEN_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE,
			Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL, Items.IRON_SHOVEL,
			Items.GOLDEN_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL,
			Items.DIAMOND_HOE, Items.NETHERITE_HOE, Items.IRON_HOE,
			Items.GOLDEN_HOE, Items.STONE_HOE, Items.WOODEN_HOE,
			Items.BRUSH, Items.SHEARS, Items.FLINT_AND_STEEL, Items.FISHING_ROD,
			Items.CARROT_ON_A_STICK, Items.WARPED_FUNGUS_ON_A_STICK,
			Items.ENCHANTED_BOOK, Items.CARVED_PUMPKIN
		)) {
			vanillaIds.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
		}
		for (String type : List.of("helmet","chestplate","leggings","boots","sword","axe","pickaxe","shovel","hoe")) {
			vanillaIds.add("minecraft:copper_" + type);
		}

		var enchantReg = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
		var itemReg = net.minecraft.core.registries.BuiltInRegistries.ITEM;

		Map<String, List<net.minecraft.world.item.Item>> byEnchantSet = new LinkedHashMap<>();
		Map<String, List<String>> enchantSetKeys = new LinkedHashMap<>();

		for (net.minecraft.world.item.Item item : itemReg) {
			Identifier itemId = BuiltInRegistries.ITEM.getKey(item) != null ? BuiltInRegistries.ITEM.getKey(item) : null;
			if (itemId == null) continue;
			if (vanillaIds.contains(itemId.toString())) continue;
			if (itemId.getNamespace().equals("minecraft")) continue;

			List<String> applicableEnchants = new ArrayList<>();
			for (var enchEntry : enchantReg.listElements().toList()) {
				var ench = enchEntry.value();
				net.minecraft.world.item.ItemStack testStack = new net.minecraft.world.item.ItemStack(item);
				if (ench.isSupportedItem(testStack)) {
					net.minecraft.resources.Identifier enchId = enchEntry.key().identifier();
					String internalKey = toInternalEnchantKey(enchId);
					String storedKey = internalKey != null ? internalKey : enchId.toString();
					applicableEnchants.add(storedKey);

					if (!s_moddedEnchantNames.containsKey(storedKey) && internalKey == null) {
						String name = ench.description().getString();
						s_moddedEnchantNames.put(storedKey, name);
					}
				}
			}

			if (applicableEnchants.isEmpty()) continue;

			Collections.sort(applicableEnchants);
			String setKey = String.join(",", applicableEnchants);
			byEnchantSet.computeIfAbsent(setKey, k -> new ArrayList<>()).add(item);
			enchantSetKeys.put(setKey, applicableEnchants);
		}

		List<ModdedCategory> result = new ArrayList<>();
		for (Map.Entry<String, List<net.minecraft.world.item.Item>> entry : byEnchantSet.entrySet()) {
			List<net.minecraft.world.item.Item> items = entry.getValue();
			List<String> enchants = enchantSetKeys.get(entry.getKey());
			Identifier firstId = BuiltInRegistries.ITEM.getKey(items.get(0));
			String catId = "modded:" + (firstId != null ? firstId.toString() : entry.getKey());
			result.add(new ModdedCategory(catId, items, enchants));
		}
		return result;
	}

	private ItemStack vanillaStackForCategory(String category) {
		net.minecraft.world.item.Item item = switch (category) {
			case "helmet"     -> Items.IRON_HELMET;
			case "chestplate" -> Items.IRON_CHESTPLATE;
			case "leggings"   -> Items.IRON_LEGGINGS;
			case "boots"      -> Items.IRON_BOOTS;
			case "sword"      -> Items.IRON_SWORD;
			case "axe"        -> Items.IRON_AXE;
			case "pickaxe"    -> Items.IRON_PICKAXE;
			case "shovel"     -> Items.IRON_SHOVEL;
			case "hoe"        -> Items.IRON_HOE;
			case "bow"        -> Items.BOW;
			case "crossbow"   -> Items.CROSSBOW;
			case "trident"    -> Items.TRIDENT;
			case "shield"     -> Items.SHIELD;
			case "elytra"     -> Items.ELYTRA;
			case "mace"       -> Items.MACE;
			case "fishing_rod"-> Items.FISHING_ROD;
			case "book"       -> Items.ENCHANTED_BOOK;
			default           -> null;
		};
		return item != null ? new ItemStack(item) : null;
	}

	private static String toInternalEnchantKey(net.minecraft.resources.Identifier enchId) {
		if (!enchId.getNamespace().equals("minecraft")) return null;
		String path = enchId.getPath();
		return path.equals("sweeping_edge") ? "sweeping" : path;
	}

	private int getModdedEnchantMaxLevel(String enchantKey) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return 1;
		net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> reg = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
		String mcId = toMinecraftEnchantId(enchantKey);
		net.minecraft.resources.Identifier enchId = mcId.contains(":")
			? net.minecraft.resources.Identifier.parse(mcId)
			: net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", mcId);
		var entry = reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchId));
		return entry.map(e -> e.value().getMaxLevel()).orElse(1);
	}

	private int enchantMaxLevel(String enchantKey) {
		EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchantKey);
		if (def != null) return def.levelMax();
		return getModdedEnchantMaxLevel(enchantKey);
	}

	private boolean modMode = false;
	private static boolean s_modMode = false;
	private boolean showModdedEnchants = true;
	private static boolean s_showModdedEnchants = true;

	private boolean isShowingModdedEnchants() {
		return com.armaninyow.dibs.config.AeogConfig.showModButtonPhase2 && showModdedEnchants;
	}
	private long lastIconCycleTick = 0;

	private static Map<String, net.minecraft.resources.Identifier> getItemIcons() {
		if (ITEM_ICONS == null) {
			ITEM_ICONS = new LinkedHashMap<>();
			for (String name : EnchantData.getPhase1Items()) {
				String file = name.equals("crossbow") ? "crossbow_standby" : name;
				ITEM_ICONS.put(name, id(ITEMS + file + ".png"));
			}
			ITEM_ICONS.put("book", id(ITEMS + "book.png"));
		}
		return ITEM_ICONS;
	}

	private static net.minecraft.resources.Identifier id(String path) {
		return net.minecraft.resources.Identifier.fromNamespaceAndPath(AnvilEnchantmentOrderingGuide.MOD_ID, path);
	}

	private void playClick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager() == null) return;
		mc.getSoundManager().play(
			net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.25f));
	}

	public static final int P_W = 194, P_H = 166;
	private static final int GAP = 2;

	private static final int GRID_COLS = 8;
	private static final int GRID_X = 25, GRID_Y = 35;
	private static final int BTN_SIZE = 18, ICON_SIZE = 16;

	private static final int OPT_X = 25, OPT_Y = 141, OPT_W = 144, OPT_H = 18;

	private static final int LIST_X = 7,  LIST_Y = 17;
	private static final int LIST_W = 161, LIST_H = 108;
	private static final int ROW_H  = 18;

	private static final int PREV_W = 18, NEXT_W = 18, SLOT_W = 126;

	private static final int CALC_X = 61, CALC_Y = 141, CALC_W = 54, CALC_H = 18;

	private static final int SCROLL_X = 174, SCROLL_Y = 18;
	private static final int SCROLL_TRACK_H = 140;
	private static final int SCROLLER_W = 12, SCROLLER_H = 15;

	private static final int BACK_W = 18, BACK_H = 10;

	private static final int P3_TREE_X = 8,   P3_TREE_Y = 18;
	private static final int P3_TREE_W = 177,  P3_TREE_H = 139;

	private static final int P3L_LIST_X = 7,  P3L_LIST_Y = 17;
	private static final int P3L_LIST_W = 161, P3L_LIST_H = 124;

	private static final int P3L_TOTAL_X = 7,  P3L_TOTAL_Y = 141;
	private static final int P3L_TOTAL_W = 161, P3L_TOTAL_H = 17;

	private static final int LIST_FRAME_SIZE = 16;

	private static final int COLOR_ITEM_TEXT  = 0xFF535353;
	private static final int COLOR_COST_TEXT  = 0xFF6b6b6b;
	private static final int COLOR_TOTAL_TEXT = 0xFF3C3C3C;

	private static final int NODE_SIZE = 26;
	private static final int NODE_ICON = 16;

	private static final int H_GAP = 1;
	private static final int V_GAP = 30;

	private enum Phase { ONE, TWO, THREE }
	private Phase phase = Phase.ONE;

	private String selectedItem = null;
	private boolean modeLevels  = true;

	private List<String> itemEnchants         = new ArrayList<>();
	private List<List<String>> incompatGroups = new ArrayList<>();
	private List<Integer> groupVisible        = new ArrayList<>();
	private final Map<String, Integer> selectedLevels = new LinkedHashMap<>();
	private int scrollOffset   = 0;
	private int totalContentH  = 0;
	private boolean scrollDragging    = false;
	private int scrollDragStartY      = 0;
	private int scrollDragStartOffset = 0;

	private int expandedGroupIdx = -1;

	private int   listScrollOffset = 0;
	private int   listTotalH       = 0;
	private boolean listScrollDragging    = false;
	private int   listScrollDragStartY    = 0;
	private int   listScrollDragStartOff  = 0;

	private List<MergeInstruction> instructions = new ArrayList<>();
	private float treeOffX = 0, treeOffY = 0;
	private boolean loading = false;
	private long    loadingStartMs = 0;
	private boolean treeDragging = false;
	private int treeDragStartX   = 0, treeDragStartY = 0;
	private float treeDragOffX   = 0, treeDragOffY   = 0;

	private static Phase         s_phase         = Phase.ONE;
	private static String        s_selectedItem  = null;
	private static boolean       s_modeLevels    = true;
	private static List<String>  s_itemEnchants  = new ArrayList<>();
	private static List<List<String>> s_incompatGroups = new ArrayList<>();
	private static List<Integer> s_groupVisible  = new ArrayList<>();
	private static Map<String, Integer> s_selectedLevels = new LinkedHashMap<>();
	private static int           s_scrollOffset  = 0;
	private static List<MergeInstruction> s_instructions = new ArrayList<>();
	private static float         s_treeOffX      = 0, s_treeOffY = 0;
	private static int           s_listScrollOffset = 0;
	public  static boolean       s_panelWasOpen  = false;

	public void saveState() {
		loading = false;
		s_phase        = phase;
		s_selectedItem = selectedItem;
		s_modeLevels   = modeLevels;
		s_modMode      = modMode;
		s_showModdedEnchants = showModdedEnchants;
		s_itemEnchants = new ArrayList<>(itemEnchants);
		s_incompatGroups = incompatGroups.stream()
			.map(ArrayList::new).collect(java.util.stream.Collectors.toList());
		s_groupVisible  = new ArrayList<>(groupVisible);
		s_selectedLevels = new LinkedHashMap<>(selectedLevels);
		s_scrollOffset  = scrollOffset;
		s_instructions  = new ArrayList<>(instructions);
		s_treeOffX      = treeOffX;
		s_treeOffY      = treeOffY;
		s_listScrollOffset = listScrollOffset;
	}

	private void restoreState() {
		phase        = s_phase;
		selectedItem = s_selectedItem;
		modeLevels   = s_modeLevels;
		modMode      = s_modMode;
		showModdedEnchants = s_showModdedEnchants;
		itemEnchants = new ArrayList<>(s_itemEnchants);
		incompatGroups = s_incompatGroups.stream()
			.map(ArrayList::new).collect(java.util.stream.Collectors.toList());
		groupVisible  = new ArrayList<>(s_groupVisible);
		selectedLevels.clear();
		selectedLevels.putAll(s_selectedLevels);
		scrollOffset  = s_scrollOffset;
		instructions  = new ArrayList<>(s_instructions);
		treeOffX      = s_treeOffX;
		treeOffY      = s_treeOffY;
		listScrollOffset = s_listScrollOffset;

		if (phase == Phase.THREE && !instructions.isEmpty()) buildTreeLayout();
	}

	private record TreeNode(float x, float y, MergeInstruction.NodeItem data,
	                        boolean isLeaf, boolean isFinal, int instrIdx) {

	}
	private List<TreeNode> treeNodes = new ArrayList<>();

	private Map<Integer, Boolean> treeNodeIsLeft = new HashMap<>();

	private record Connector(int fromIdx, int toIdx) {}
	private List<Connector> connectors = new ArrayList<>();

	private TreeNode hoveredNode = null;
	private boolean hoveredNodeObtained = false;
	private net.minecraft.world.item.ItemStack hoveredMatchedStack = null;

	private boolean wasLeftDown  = false;
	private boolean justOpened   = false;

	public void setJustOpened(boolean val) { justOpened = val; }

	public AeogOverlayScreen() {
		restoreState();
	}

	public void render(GuiGraphicsExtractor ctx, int anvilX, int anvilY,
	                   int mouseX, int mouseY, Font tr) {
		int px = anvilX - P_W - GAP;
		int py = anvilY;
		pollMouse(px, py, mouseX, mouseY);
		switch (phase) {
			case ONE   -> renderPhaseOne(ctx, px, py, mouseX, mouseY, tr);
			case TWO   -> renderPhaseTwo(ctx, px, py, mouseX, mouseY, tr);
			case THREE -> renderPhaseThree(ctx, px, py, mouseX, mouseY, tr);
		}
	}

	private void renderPhaseOne(GuiGraphicsExtractor ctx, int px, int py,
	                             int mx, int my, Font tr) {
		ctx.blit(PIPE, P1_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		if (com.armaninyow.dibs.config.AeogConfig.showModButtonPhase1) {
			int modBtnAbsX = px + MOD_BTN_X, modBtnAbsY = py + MOD_BTN_Y;
			ctx.blit(PIPE, modMode ? BTN_MOD_H : BTN_MOD,
				modBtnAbsX, modBtnAbsY, 0f, 0f, MOD_BTN_SIZE, MOD_BTN_SIZE, MOD_BTN_SIZE, MOD_BTN_SIZE);
			if (inBounds(mx, my, modBtnAbsX, modBtnAbsY, MOD_BTN_SIZE, MOD_BTN_SIZE)) {
				ctx.setTooltipForNextFrame(tr, Component.literal(modMode ? "Switch to Vanilla Items" : "Switch to Modded Items"), mx, my);
			}
		}
		if (modMode) {
			renderPhaseOneModded(ctx, px, py, mx, my, tr);
		} else {
			renderPhaseOneVanilla(ctx, px, py, mx, my, tr);
		}

		int optX = px + OPT_X, optY = py + OPT_Y;
		boolean optHov = inBounds(mx, my, optX, optY, OPT_W, OPT_H);
		ctx.blit(PIPE, optHov ? BTN_OPT_H : BTN_OPT,
			optX, optY, 0f, 0f, OPT_W, OPT_H, OPT_W, OPT_H);

		String modeText = modeLevels ? "Least XP/Levels" : "Least Prior Work Penalty";
		int modeW = tr.width(modeText);
		ctx.text(tr, Component.literal(modeText),
			optX + OPT_W / 2 - modeW / 2, optY + (OPT_H - 8) / 2, 0xFFFFFFFF, true);
	}

	private void renderPhaseOneVanilla(GuiGraphicsExtractor ctx, int px, int py,
	                                    int mx, int my, Font tr) {
		List<String> items = EnchantData.getPhase1Items();
		for (int i = 0; i < items.size(); i++) {
			int bx = px + GRID_X + (i % GRID_COLS) * BTN_SIZE;
			int by = py + GRID_Y + (i / GRID_COLS) * BTN_SIZE;
			String item     = items.get(i);
			boolean hov     = inBounds(mx, my, bx, by, BTN_SIZE, BTN_SIZE);
			boolean sel     = item.equals(selectedItem);
			ctx.blit(PIPE, (hov || sel) ? BTN_ITEM_H : BTN_ITEM,
				bx, by, 0f, 0f, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
			drawIcon(ctx, item, bx + 1, by + 1);
			if (hov) ctx.setTooltipForNextFrame(tr, Component.literal(item.equals("book") ? "Book" : formatName(item)), mx, my);
		}
	}

	private void renderPhaseOneModded(GuiGraphicsExtractor ctx, int px, int py,
	                                   int mx, int my, Font tr) {

		if (s_moddedCategories == null) {
			s_moddedCategories = buildModdedCategories();
		}
		List<ModdedCategory> cats = s_moddedCategories;
		if (cats.isEmpty()) {
			String msg = "No modded enchantable items found.";
			int w = tr.width(msg);
			ctx.text(tr, Component.literal(msg),
				px + P_W / 2 - w / 2, py + GRID_Y + 20, 0xFF685E4A, false);
			return;
		}

		long now = System.currentTimeMillis();
		int iconSlot = (int)((now / 1000) % 1000);

		for (int i = 0; i < cats.size(); i++) {
			int bx = px + GRID_X + (i % GRID_COLS) * BTN_SIZE;
			int by = py + GRID_Y + (i / GRID_COLS) * BTN_SIZE;
			ModdedCategory cat = cats.get(i);
			boolean hov = inBounds(mx, my, bx, by, BTN_SIZE, BTN_SIZE);
			boolean sel = cat.id().equals(selectedItem);
			ctx.blit(PIPE, (hov || sel) ? BTN_ITEM_H : BTN_ITEM,
				bx, by, 0f, 0f, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);

			int iconIdx = iconSlot % cat.items().size();
			net.minecraft.world.item.ItemStack iconStack = new net.minecraft.world.item.ItemStack(cat.items().get(iconIdx));
			ctx.item(iconStack, bx + 1, by + 1);
			if (hov) {

				int tipIdx = iconSlot % cat.items().size();
				net.minecraft.resources.Identifier tipId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cat.items().get(tipIdx));
				String tipName = tipId != null ? formatName("modded:" + tipId) : cat.items().get(tipIdx).toString();
				ctx.setTooltipForNextFrame(tr, Component.literal(tipName), mx, my);
			}
		}
	}

	private void renderPhaseTwo(GuiGraphicsExtractor ctx, int px, int py,
	                             int mx, int my, Font tr) {
		ctx.blit(PIPE, P2_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		renderBack(ctx, px, py, mx, my);

		if (com.armaninyow.dibs.config.AeogConfig.showModButtonPhase2) {
			int modBtnAbsX = px + MOD_BTN_X, modBtnAbsY = py + MOD_BTN_Y;
			ctx.blit(PIPE, isShowingModdedEnchants() ? BTN_MOD_H : BTN_MOD,
				modBtnAbsX, modBtnAbsY, 0f, 0f, MOD_BTN_SIZE, MOD_BTN_SIZE, MOD_BTN_SIZE, MOD_BTN_SIZE);
			if (inBounds(mx, my, modBtnAbsX, modBtnAbsY, MOD_BTN_SIZE, MOD_BTN_SIZE)) {
				ctx.setTooltipForNextFrame(tr, Component.literal(isShowingModdedEnchants() ? "Hide Modded Enchants" : "Show Modded Enchants"), mx, my);
			}
		}

		int calcX = px + CALC_X, calcY = py + CALC_Y;
		boolean hasSelection = !selectedLevels.isEmpty();
		boolean calcHov = hasSelection && inBounds(mx, my, calcX, calcY, CALC_W, CALC_H);
		Identifier calcTex = !hasSelection ? BTN_CALC_DIS : (calcHov ? BTN_CALC_H : BTN_CALC);
		ctx.blit(PIPE, calcTex, calcX, calcY, 0f, 0f, CALC_W, CALC_H, CALC_W, CALC_H);

		int lax = px + LIST_X, lay = py + LIST_Y;
		ctx.enableScissor(lax, lay, lax + LIST_W, lay + LIST_H);
		renderPhase2List(ctx, lax, lay, mx, my, tr);
		ctx.disableScissor();

		renderScrollBar(ctx, px, py);

		String calcLabel = "Calculate";
		int calcLabelW = tr.width(calcLabel);
		ctx.text(tr, Component.literal(calcLabel),
			calcX + CALC_W / 2 - calcLabelW / 2, calcY + (CALC_H - 8) / 2,
			hasSelection ? 0xFFFFFFFF : 0xFF555555, true);
	}

	private void renderPhase2List(GuiGraphicsExtractor ctx, int absX, int absY,
	                               int mx, int my, Font tr) {
		int y = absY - scrollOffset;

		int yTex = y;
		for (int gi = 0; gi < incompatGroups.size(); gi++) {
			List<String> group = incompatGroups.get(gi);
			String enchant     = group.get(groupVisible.get(gi));
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
			boolean disabled   = isEnchantDisabledByTrident(enchant);
			boolean expanded   = (expandedGroupIdx == gi);
			boolean isModded   = !EnchantData.ENCHANTS.containsKey(enchant);
			if (isModded && !isShowingModdedEnchants()) { continue; }

			if (expanded) {

				int totalSlots = enchantMaxLevel(enchant) + 1;
				int totalW     = totalSlots * ROW_H;

				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = absX + (rowW - totalW) / 2;
				for (int lv = 0; lv <= enchantMaxLevel(enchant); lv++) {
					int lvX = startX + lv * ROW_H;
					Integer sel = selectedLevels.get(enchant);
					boolean isSel = (lv > 0) && sel != null && sel == lv;
					Identifier lvTex;
					if (disabled) lvTex = LVL_SLOT_DIS;
					else if (isSel || inBounds(mx, my, lvX, yTex, ROW_H, ROW_H)) lvTex = LVL_SLOT_SEL;
					else lvTex = LVL_SLOT;
					ctx.blit(PIPE, lvTex, lvX, yTex, 0f, 0f, ROW_H, ROW_H, ROW_H, ROW_H);
				}
			} else {

				if (group.size() > 1) {
					boolean ph = inBounds(mx, my, absX, yTex, PREV_W, ROW_H);
					ctx.blit(PIPE, ph ? ENCH_PREV_H : ENCH_PREV,
						absX, yTex, 0f, 0f, PREV_W, ROW_H, PREV_W, ROW_H);
				}
				int slotX = absX + PREV_W;

				boolean slotHov = !disabled && inBounds(mx, my, slotX, yTex, SLOT_W, ROW_H);
				Identifier slotTex = disabled ? ENCH_SLOT_DIS : slotHov ? ENCH_SLOT_H : ENCH_SLOT;
				ctx.blit(PIPE, slotTex, slotX, yTex, 0f, 0f, SLOT_W, ROW_H, SLOT_W, ROW_H);
				if (group.size() > 1) {
					int nx = absX + PREV_W + SLOT_W;
					boolean nh = inBounds(mx, my, nx, yTex, NEXT_W, ROW_H);
					ctx.blit(PIPE, nh ? ENCH_NEXT_H : ENCH_NEXT,
						nx, yTex, 0f, 0f, NEXT_W, ROW_H, NEXT_W, ROW_H);
				}
			}
			yTex += ROW_H;
		}
		totalContentH = yTex - (absY - scrollOffset);

		int yTxt = y;
		for (int gi = 0; gi < incompatGroups.size(); gi++) {
			List<String> group = incompatGroups.get(gi);
			String enchant     = group.get(groupVisible.get(gi));
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
			boolean disabled   = isEnchantDisabledByTrident(enchant);
			boolean expanded   = (expandedGroupIdx == gi);
			int defaultColor   = disabled ? 0xFF342F25 : 0xFF685E4A;
			boolean enchSelected = selectedLevels.containsKey(enchant);
			boolean isCurse    = enchant.equals("binding_curse") || enchant.equals("vanishing_curse");
			boolean isModded   = !EnchantData.ENCHANTS.containsKey(enchant);
			if (isModded && !isShowingModdedEnchants()) { continue; }

			if (expanded) {

				int totalSlots = enchantMaxLevel(enchant) + 1;
				int totalW     = totalSlots * ROW_H;
				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = absX + (rowW - totalW) / 2;
				for (int lv = 0; lv <= enchantMaxLevel(enchant); lv++) {
					int lvX    = startX + lv * ROW_H;
					String lvStr = String.valueOf(lv);
					int lvStrW = tr.width(lvStr);
					Integer sel = selectedLevels.get(enchant);
					boolean isSel = (lv > 0) && sel != null && sel == lv;
					int lvColor = disabled ? defaultColor
						: (isSel && isCurse) ? 0xFFFC5454
						: (isSel && isModded) ? 0xFF54FCFC
						: isSel ? 0xFFFFFFFF
						: defaultColor;
					ctx.text(tr, Component.literal(lvStr),
						lvX + ROW_H / 2 - lvStrW / 2, yTxt + (ROW_H - 8) / 2, lvColor, false);
				}
			} else {

				int slotX    = absX + PREV_W;
				int enchColor = disabled ? defaultColor
					: (enchSelected && isCurse) ? 0xFFFC5454
					: (enchSelected && isModded) ? 0xFF54FCFC
					: enchSelected ? 0xFFFFFFFF
					: defaultColor;
				String enchLabel;
				Integer selLevel = selectedLevels.get(enchant);
				if (!disabled && selLevel != null) {
					boolean showNumeral = enchantMaxLevel(enchant) > 1;
					enchLabel = showNumeral
						? formatName(enchant) + " " + toRoman(selLevel)
						: formatName(enchant);
				} else {
					enchLabel = formatName(enchant);
				}
				int enchLabelW = tr.width(enchLabel);
				ctx.text(tr, Component.literal(enchLabel),
					slotX + SLOT_W / 2 - enchLabelW / 2, yTxt + (ROW_H - 8) / 2, enchColor, false);
			}
			yTxt += ROW_H;
		}
	}

	private void renderScrollBar(GuiGraphicsExtractor ctx, int px, int py) {
		if (totalContentH <= LIST_H) return;
		int trackAbsX = px + SCROLL_X;
		int trackAbsY = py + SCROLL_Y;
		float frac    = (float) scrollOffset / Math.max(1, totalContentH - LIST_H);
		int thumbY    = trackAbsY + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
		ctx.blit(PIPE, SCROLLER,
			trackAbsX, thumbY, 0f, 0f, SCROLLER_W, SCROLLER_H, SCROLLER_W, SCROLLER_H);
	}

	private void renderPhaseThree(GuiGraphicsExtractor ctx, int px, int py,
	                               int mx, int my, Font tr) {
		if (com.armaninyow.dibs.config.AeogConfig.listViewPhase3) {
			renderPhaseThreeList(ctx, px, py, mx, my, tr);
		} else {
			renderPhaseThreeTree(ctx, px, py, mx, my, tr);
		}
	}

	private void renderPhaseThreeTree(GuiGraphicsExtractor ctx, int px, int py,
	                                   int mx, int my, Font tr) {
		int treeAbsX = px + P3_TREE_X;
		int treeAbsY = py + P3_TREE_Y;

		ctx.enableScissor(treeAbsX, treeAbsY, treeAbsX + P3_TREE_W, treeAbsY + P3_TREE_H);
		renderTiledBg(ctx, treeAbsX, treeAbsY);
		renderConnectors(ctx, treeAbsX, treeAbsY);

		hoveredNode = null;
		for (TreeNode node : treeNodes) {
			int nx = treeAbsX + (int)(node.x() + treeOffX);
			int ny = treeAbsY + (int)(node.y() + treeOffY);
			ItemStack match = findMatchingStack(node.data(), node);
			boolean obtained = match != null;
			boolean hovered = inBounds(mx, my, nx, ny, NODE_SIZE, NODE_SIZE);
			Identifier frame = frameFor(node, obtained, hovered);
			ctx.blit(PIPE, frame, nx, ny, 0f, 0f, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);

			if (obtained && match != null && !match.isEmpty()) {
				ctx.item(match, nx + 5, ny + 5);
			} else {
				drawIcon(ctx, node.data().id(), nx + 5, ny + 5);
			}

			if (hovered) { hoveredNode = node; hoveredNodeObtained = obtained; hoveredMatchedStack = match; }
		}
		ctx.disableScissor();

		ctx.blit(PIPE, P3_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		if (hoveredNode != null) {
			renderNodeTooltip(ctx, hoveredNode, hoveredNodeObtained, hoveredMatchedStack, mx, my, tr);
		}
		renderBack(ctx, px, py, mx, my);

		if (loading) {
			renderLoadingOverlay(ctx, px, py, tr);
		}
	}

	private void renderPhaseThreeList(GuiGraphicsExtractor ctx, int px, int py,
	                                   int mx, int my, Font tr) {

		ctx.blit(PIPE, P3_CONTAINER_2, px, py, 0f, 0f, P_W, P_H, P_W, P_H);
		renderBack(ctx, px, py, mx, my);

		if (loading) {
			renderListLoadingOverlay(ctx, px, py, tr);
			return;
		}

		int listAbsX = px + P3L_LIST_X;
		int listAbsY = py + P3L_LIST_Y;

		List<ListRow> rows = buildListRows(tr);
		listTotalH = rows.stream().mapToInt(r -> r.height).sum();
		int maxScroll = Math.max(0, listTotalH - P3L_LIST_H);
		listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset));

		int clipTop    = listAbsY;
		int clipBottom = listAbsY + P3L_LIST_H;
		ctx.enableScissor(listAbsX, clipTop, listAbsX + P3L_LIST_W, clipBottom);
		renderListRows(ctx, tr, rows, listAbsX, listAbsY - listScrollOffset, clipTop, clipBottom, mx, my);
		ctx.disableScissor();

		renderListScrollBar(ctx, px, py, maxScroll);

		int totalCost = instructions.stream().mapToInt(MergeInstruction::mergeCost).sum();
		String totalStr = "Total cost: " + totalCost + " levels";
		int totalStrW = tr.width(totalStr);
		int totalBarMidX = px + P3L_TOTAL_X + P3L_TOTAL_W / 2;
		int totalBarMidY = py + P3L_TOTAL_Y + P3L_TOTAL_H / 2 - 1;
		ctx.text(tr, Component.literal(totalStr), totalBarMidX - totalStrW / 2, totalBarMidY, COLOR_TOTAL_TEXT, false);
	}

	private void renderListLoadingOverlay(GuiGraphicsExtractor ctx, int px, int py, Font tr) {

		int cx = px + P3L_LIST_X + P3L_LIST_W / 2;
		int cy = py + P3L_LIST_Y + P3L_LIST_H / 2;

		float progress = (float)(System.currentTimeMillis() - loadingStartMs) / expectedMs();
		progress = Math.min(progress, 0.9f);

		String line1 = "Calculating...";
		String line2 = "Please keep the anvil open.";
		int line1W = tr.width(line1);
		int line2W = tr.width(line2);

		int totalH = 8 + 2 + 8 + 4 + LOADING_BAR_H;
		int line1Y = cy - totalH / 2;
		int line2Y = line1Y + 8 + 2;
		int barY   = line2Y + 8 + 4;
		int barX   = cx - LOADING_BAR_W / 2;

		ctx.text(tr, Component.literal(line1),
			cx - line1W / 2 + 1, line1Y + 1, 0xFF3F3F3F, false);
		ctx.text(tr, Component.literal(line1),
			cx - line1W / 2, line1Y, 0xFFFFFFFF, false);
		ctx.text(tr, Component.literal(line2),
			cx - line2W / 2 + 1, line2Y + 1, 0xFF3F3F3F, false);
		ctx.text(tr, Component.literal(line2),
			cx - line2W / 2, line2Y, 0xFFFFFFFF, false);
		ctx.fill(barX, barY, barX + LOADING_BAR_W, barY + LOADING_BAR_H, 0xFF000000);
		int fillW = (int)(LOADING_BAR_W * progress);
		if (fillW > 0)
			ctx.fill(barX, barY, barX + fillW, barY + LOADING_BAR_H, 0xFF00FF00);
	}

	private record ListRow(int stepNumber, MergeInstruction instr, int height,
	                       List<String> enchLines,
	                       List<String> costLines,
	                       boolean leftObtained, boolean rightObtained) {}

	private List<ListRow> buildListRows(Font tr) {
		List<ListRow> rows = new ArrayList<>();
		int lineH = tr.lineHeight;
		int iconRowH = Math.max(LIST_FRAME_SIZE, tr.lineHeight);

		int contW = P3L_LIST_W - LIST_FRAME_SIZE;
		for (int i = 0; i < instructions.size(); i++) {
			MergeInstruction instr = instructions.get(i);
			String leftEnch  = enchantSummary(instr.left());
			String rightEnch = enchantSummary(instr.right());
			String enchLine;
			if (leftEnch.isEmpty() && rightEnch.isEmpty()) {
				enchLine = "";
			} else if (leftEnch.isEmpty()) {
				enchLine = "(" + rightEnch + ")";
			} else if (rightEnch.isEmpty()) {
				enchLine = "(" + leftEnch + ")";
			} else {
				enchLine = "(" + leftEnch + ") + (" + rightEnch + ")";
			}
			List<String> enchLines = enchLine.isEmpty()
				? new ArrayList<>()
				: wrapText(tr, enchLine, contW, contW);
			String costStr = "Cost: " + instr.mergeCost() + " levels, PWP: " + instr.priorWorkPenalty() + " levels";
			List<String> costLines = wrapText(tr, costStr, contW, contW);
			int rowH = iconRowH + 1 + lineH * enchLines.size() + lineH * costLines.size() + 3;
			boolean lo = findMatchingStack(instr.left(), null) != null;
			boolean ro = findMatchingStack(instr.right(), null) != null;
			rows.add(new ListRow(i + 1, instr, rowH, enchLines, costLines, lo, ro));
		}
		return rows;
	}

	private List<String> wrapText(Font tr, String text, int firstLineWidth, int contWidth) {
		List<String> result = new ArrayList<>();
		if (text.isEmpty()) { result.add(""); return result; }

		if (firstLineWidth < tr.width("W")) firstLineWidth = contWidth;
		String remaining = text;
		int availW = firstLineWidth;
		while (!remaining.isEmpty()) {
			if (tr.width(remaining) <= availW) {
				result.add(remaining);
				break;
			}

			int cut = remaining.length() - 1;
			while (cut > 0 && tr.width(remaining.substring(0, cut)) > availW) cut--;

			int space = (cut > 0) ? remaining.lastIndexOf(' ', cut - 1) : -1;
			if (space >= 0) {
				result.add(remaining.substring(0, space).stripTrailing());
				remaining = remaining.substring(space).stripLeading();
			} else {

				int hardCut = Math.max(1, cut);
				result.add(remaining.substring(0, hardCut));
				remaining = remaining.substring(hardCut);
			}
			availW = contWidth;
		}
		if (result.isEmpty()) result.add("");
		return result;
	}

	private String enchantSummary(MergeInstruction.NodeItem node) {
		List<String[]> enchants = node.enchants();
		if (enchants.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < enchants.size(); i++) {
			if (i > 0) sb.append(", ");
			String[] e = enchants.get(i);
			int lvl;
			try { lvl = Integer.parseInt(e[1]); } catch (NumberFormatException ex) { lvl = 1; }
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(e[0]);
			boolean showLevel = enchantMaxLevel(e[0]) > 1;
			sb.append(formatName(e[0]));
			if (showLevel) sb.append(" ").append(toRoman(lvl));
		}
		return sb.toString();
	}

	private void renderListRows(GuiGraphicsExtractor ctx, Font tr,
	                             List<ListRow> rows, int absX, int startY,
	                             int clipTop, int clipBottom, int mx, int my) {
		int lineH = tr.lineHeight;
		int iconRowH = Math.max(LIST_FRAME_SIZE, tr.lineHeight);
		int plusW = tr.width(" + ");
		int y = startY;
		for (ListRow row : rows) {
			int rowBottom = y + row.height;
			if (rowBottom < clipTop) { y = rowBottom; continue; }
			if (y >= clipBottom)     { break; }

			boolean iconRowVisible = y < clipBottom && (y + iconRowH) > clipTop;
			if (iconRowVisible) {
				int x = absX;
				int textY = y + iconRowH / 2 - tr.lineHeight / 2;
				boolean obtained = row.leftObtained && row.rightObtained;
				ctx.blit(PIPE, obtained ? LIST_FRAME_O : LIST_FRAME_U,
					x, y, 0f, 0f, LIST_FRAME_SIZE, LIST_FRAME_SIZE, LIST_FRAME_SIZE, LIST_FRAME_SIZE);
				String numStr = String.valueOf(row.stepNumber);
				int numW = tr.width(numStr);
				ctx.text(tr, Component.literal(numStr),
					x + LIST_FRAME_SIZE / 2 - numW / 2, y + LIST_FRAME_SIZE / 2 - tr.lineHeight / 2,
					obtained ? 0xFFFFFFFF : 0xFF6b6b6b, false);
				x += LIST_FRAME_SIZE;

				boolean leftHovered = row.leftObtained && inBounds(mx, my, x, y, LIST_FRAME_SIZE, LIST_FRAME_SIZE)
					&& y < clipBottom && y + LIST_FRAME_SIZE > clipTop;
				if (leftHovered)
					ctx.blit(PIPE, SLOT_HL_BACK, x - 4, y - 4, 0f, 0f, 24, 24, 24, 24);
				if (y >= clipTop && (y + LIST_FRAME_SIZE) <= clipBottom)
					drawListIcon(ctx, tr, row.instr.left(), row.leftObtained, x, y);
				if (leftHovered)
					ctx.blit(PIPE, SLOT_HL_FRONT, x - 4, y - 4, 0f, 0f, 24, 24, 24, 24);
				x += LIST_FRAME_SIZE;

				ctx.text(tr, Component.literal(" + "), x, textY, COLOR_ITEM_TEXT, false);
				x += plusW;

				boolean rightHovered = row.rightObtained && inBounds(mx, my, x, y, LIST_FRAME_SIZE, LIST_FRAME_SIZE)
					&& y < clipBottom && y + LIST_FRAME_SIZE > clipTop;
				if (rightHovered)
					ctx.blit(PIPE, SLOT_HL_BACK, x - 4, y - 4, 0f, 0f, 24, 24, 24, 24);
				if (y >= clipTop && (y + LIST_FRAME_SIZE) <= clipBottom)
					drawListIcon(ctx, tr, row.instr.right(), row.rightObtained, x, y);
				if (rightHovered)
					ctx.blit(PIPE, SLOT_HL_FRONT, x - 4, y - 4, 0f, 0f, 24, 24, 24, 24);
			}

			y += iconRowH + 1;

			for (String seg : row.enchLines) {
				if (y >= clipTop && y < clipBottom && !seg.isEmpty())
					ctx.text(tr, Component.literal(seg), absX + LIST_FRAME_SIZE, y, COLOR_ITEM_TEXT, false);
				y += lineH;
			}

			for (String seg : row.costLines) {
				if (y >= clipTop && y < clipBottom && !seg.isEmpty())
					ctx.text(tr, Component.literal(seg), absX + LIST_FRAME_SIZE, y, COLOR_COST_TEXT, false);
				y += lineH;
			}

			y += 3;
		}
	}

	private void drawListIcon(GuiGraphicsExtractor ctx, Font tr,
	                           MergeInstruction.NodeItem node, boolean obtained, int x, int y) {
		if (obtained) {
			ItemStack match = findMatchingStack(node, null);
			if (match != null && !match.isEmpty()) {
				ctx.item(match, x, y);
				return;
			}
		}
		drawIcon(ctx, node.id(), x, y);
	}

	private void renderListScrollBar(GuiGraphicsExtractor ctx, int px, int py, int maxScroll) {
		if (maxScroll <= 0) return;
		int trackAbsX = px + SCROLL_X;
		int trackAbsY = py + SCROLL_Y;
		float frac    = (float) listScrollOffset / Math.max(1, maxScroll);
		int thumbY    = trackAbsY + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
		ctx.blit(PIPE, SCROLLER,
			trackAbsX, thumbY, 0f, 0f, SCROLLER_W, SCROLLER_H, SCROLLER_W, SCROLLER_H);
	}

	private static final int LOADING_BAR_W = 150;
	private static final int LOADING_BAR_H = 2;

	private long expectedMs() {
		int n = selectedLevels.size();
		return (long)(50 * Math.pow(4.0, n - 9)) + 1000;
	}

	private void renderLoadingOverlay(GuiGraphicsExtractor ctx, int px, int py, Font tr) {
		int vpCX = px + P3_TREE_X + P3_TREE_W / 2;
		int vpCY = py + P3_TREE_Y + P3_TREE_H / 2;

		float progress = (float)(System.currentTimeMillis() - loadingStartMs) / expectedMs();
		progress = Math.min(progress, 0.9f);

		String line1 = "Calculating...";
		String line2 = "Please keep the anvil open.";
		int line1W = tr.width(line1);
		int line2W = tr.width(line2);

		int totalH = 8 + 2 + 8 + 4 + LOADING_BAR_H;
		int line1Y = vpCY - totalH / 2;
		int line2Y = line1Y + 8 + 2;
		int barY   = line2Y + 8 + 4;
		int barX   = vpCX - LOADING_BAR_W / 2;

		ctx.text(tr, Component.literal(line1),
			vpCX - line1W / 2 + 1, line1Y + 1, 0xFF3F3F3F, false);
		ctx.text(tr, Component.literal(line1),
			vpCX - line1W / 2, line1Y, 0xFFFFFFFF, false);

		ctx.text(tr, Component.literal(line2),
			vpCX - line2W / 2 + 1, line2Y + 1, 0xFF3F3F3F, false);
		ctx.text(tr, Component.literal(line2),
			vpCX - line2W / 2, line2Y, 0xFFFFFFFF, false);

		ctx.fill(barX, barY, barX + LOADING_BAR_W, barY + LOADING_BAR_H, 0xFF000000);

		int fillW = (int)(LOADING_BAR_W * progress);
		if (fillW > 0)
			ctx.fill(barX, barY, barX + fillW, barY + LOADING_BAR_H, 0xFF00FF00);
	}

	private void renderTiledBg(GuiGraphicsExtractor ctx, int x, int y) {
		int bgW = 16, bgH = 16;
		int offX = ((int)treeOffX % bgW + bgW) % bgW;
		int offY = ((int)treeOffY % bgH + bgH) % bgH;
		for (int ty = -bgH + offY; ty < P3_TREE_H; ty += bgH)
			for (int tx = -bgW + offX; tx < P3_TREE_W; tx += bgW)
				ctx.blit(PIPE, BACKGROUND, x + tx, y + ty, 0f, 0f, bgW, bgH, bgW, bgH);
	}

	private void renderConnectors(GuiGraphicsExtractor ctx, int treeAbsX, int treeAbsY) {
		Map<Integer, List<Integer>> byDest = new LinkedHashMap<>();
		for (Connector c : connectors) {
			byDest.computeIfAbsent(c.toIdx(), k -> new ArrayList<>()).add(c.fromIdx());
		}

		for (Map.Entry<Integer, List<Integer>> entry : byDest.entrySet()) {
			int destIdx = entry.getKey();
			List<Integer> srcIdxs = entry.getValue();

			TreeNode dest = treeNodes.get(destIdx);
			int destX    = treeAbsX + (int)(dest.x() + treeOffX + NODE_SIZE / 2f);
			int destY    = treeAbsY + (int)(dest.y() + treeOffY);
			int junctionY = destY - V_GAP / 2;

			int minSrcX = Integer.MAX_VALUE, maxSrcX = Integer.MIN_VALUE;

			for (int srcIdx : srcIdxs) {
				TreeNode src = treeNodes.get(srcIdx);
				int srcX   = treeAbsX + (int)(src.x() + treeOffX + NODE_SIZE / 2f);
				int srcBot = treeAbsY + (int)(src.y() + treeOffY + NODE_SIZE);

				ctx.fill(srcX, srcBot, srcX + 1, junctionY, 0xFFFFFFFF);
				minSrcX = Math.min(minSrcX, srcX);
				maxSrcX = Math.max(maxSrcX, srcX);
			}

			if (srcIdxs.size() > 1) {
				ctx.fill(minSrcX, junctionY, maxSrcX + 1, junctionY + 1, 0xFFFFFFFF);
			}

			ctx.fill(destX, junctionY, destX + 1, destY, 0xFFFFFFFF);
		}
	}

	private net.minecraft.resources.Identifier frameFor(TreeNode n, boolean obtained, boolean hovered) {
		if (n.isFinal()) return obtained ? FRAME_FINAL_O : FRAME_FINAL_U;
		boolean clickable = obtained && !n.isFinal();
		if (n.isLeaf())  return (clickable && hovered) ? FRAME_INIT_OH  : (obtained ? FRAME_INIT_O  : FRAME_INIT_U);
		return                 (clickable && hovered) ? FRAME_PROG_OH  : (obtained ? FRAME_PROG_O  : FRAME_PROG_U);
	}

	private void buildTreeLayout() {
		treeNodes.clear();
		connectors.clear();
		treeNodeIsLeft.clear();
		if (instructions.isEmpty()) return;

		int steps = instructions.size();
		float rowH = NODE_SIZE + V_GAP;

		int itemStepIdx = -1;
		boolean itemIsLeft = true;
		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			boolean lItem = ins.left().work() == 0
				&& (ins.left().id().equals(selectedItem != null ? selectedItem : "")
					|| ins.left().id().equals("item"));
			boolean rItem = ins.right().work() == 0
				&& (ins.right().id().equals(selectedItem != null ? selectedItem : "")
					|| ins.right().id().equals("item"));
			if (lItem || rItem) {
				itemStepIdx = i;
				itemIsLeft  = lItem;
				break;
			}
		}

		int[] leftSrc  = new int[steps];
		int[] rightSrc = new int[steps];
		Arrays.fill(leftSrc,  -1);
		Arrays.fill(rightSrc, -1);
		boolean[] claimed = new boolean[steps];

		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work() > 0) {

				for (int j = i - 1; j >= 0; j--) {
					if (!claimed[j] && buildResult(instructions.get(j), false).work() == ins.left().work()) {
						leftSrc[i] = j;
						claimed[j] = true;
						break;
					}
				}
			}
			if (ins.right().work() > 0) {
				for (int j = i - 1; j >= 0; j--) {
					if (!claimed[j] && buildResult(instructions.get(j), false).work() == ins.right().work()) {
						rightSrc[i] = j;
						claimed[j] = true;
						break;
					}
				}
			}
		}

		int[] consumedBy = new int[steps];
		Arrays.fill(consumedBy, -1);
		for (int i = 0; i < steps; i++) {
			if (leftSrc[i]  >= 0) consumedBy[leftSrc[i]]  = i;
			if (rightSrc[i] >= 0) consumedBy[rightSrc[i]] = i;
		}

		int[] resultRow = new int[steps];
		resultRow[steps - 1] = steps;
		for (int i = steps - 1; i >= 0; i--) {
			int inputRow = resultRow[i] - 1;
			if (inputRow < 1) inputRow = 1;
			if (leftSrc[i]  >= 0 && resultRow[leftSrc[i]]  < inputRow) resultRow[leftSrc[i]]  = inputRow;
			if (rightSrc[i] >= 0 && resultRow[rightSrc[i]] < inputRow) resultRow[rightSrc[i]] = inputRow;
		}

		for (int i = 0; i < steps; i++) if (resultRow[i] == 0) resultRow[i] = 1;

		int[] sorted = resultRow.clone();
		Arrays.sort(sorted);
		Map<Integer, Integer> rowRemap = new LinkedHashMap<>();
		int seq = 0;
		for (int r : sorted) if (!rowRemap.containsKey(r)) rowRemap.put(r, ++seq);
		for (int i = 0; i < steps; i++) resultRow[i] = rowRemap.get(resultRow[i]);

		List<Integer> stepsWithLeaves = new ArrayList<>();
		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work() == 0 || ins.right().work() == 0) {
				stepsWithLeaves.add(i);
			}
		}
		stepsWithLeaves.sort((a, b) -> {
			if (resultRow[a] != resultRow[b]) return Integer.compare(resultRow[a], resultRow[b]);
			return Integer.compare(a, b);
		});

		List<String> leafKeyOrder = new ArrayList<>();

		if (itemStepIdx >= 0) {
			String itemKey = itemIsLeft ? "L" + itemStepIdx : "R" + itemStepIdx;
			String pairKey = itemIsLeft ? "R" + itemStepIdx : "L" + itemStepIdx;
			MergeInstruction itemIns = instructions.get(itemStepIdx);
			if ((itemIsLeft  ? itemIns.left()  : itemIns.right()).work() == 0) leafKeyOrder.add(itemKey);
			if ((itemIsLeft  ? itemIns.right() : itemIns.left()).work()  == 0) leafKeyOrder.add(pairKey);
		}

		for (int s : stepsWithLeaves) {
			if (s == itemStepIdx) continue;
			MergeInstruction ins = instructions.get(s);
			if (ins.left().work()  == 0 && !leafKeyOrder.contains("L" + s)) leafKeyOrder.add("L" + s);
			if (ins.right().work() == 0 && !leafKeyOrder.contains("R" + s)) leafKeyOrder.add("R" + s);
		}

		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work()  == 0 && !leafKeyOrder.contains("L" + i)) leafKeyOrder.add("L" + i);
			if (ins.right().work() == 0 && !leafKeyOrder.contains("R" + i)) leafKeyOrder.add("R" + i);
		}

		List<MergeInstruction.NodeItem> leaves = new ArrayList<>();
		Map<String, Integer> leafNodeIdx = new LinkedHashMap<>();
		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work()  == 0) leafNodeIdx.put("L" + i, -1);
			if (ins.right().work() == 0) leafNodeIdx.put("R" + i, -1);
		}
		Map<String, MergeInstruction.NodeItem> keyToLeaf = new LinkedHashMap<>();
		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work()  == 0) keyToLeaf.put("L" + i, ins.left());
			if (ins.right().work() == 0) keyToLeaf.put("R" + i, ins.right());
		}
		for (String key : leafKeyOrder) {
			if (keyToLeaf.containsKey(key)) leaves.add(keyToLeaf.get(key));
		}

		float leafSpacing = NODE_SIZE + H_GAP;
		float groupGap = 0;

		Map<String, Float> leafX = new LinkedHashMap<>();
		float xCursor = 0;
		for (int i = 0; i < leafKeyOrder.size(); i++) {
			String key = leafKeyOrder.get(i);
			if (!keyToLeaf.containsKey(key)) continue;

			if (i > 0 && i % 2 == 0) xCursor += groupGap;
			leafX.put(key, xCursor);
			xCursor += leafSpacing;
		}

		float leafTotalW = xCursor - (NODE_SIZE + H_GAP);
		float leafStartX = -leafTotalW / 2.0f;

		for (int i = 0; i < leafKeyOrder.size(); i++) {
			String key = leafKeyOrder.get(i);
			if (!keyToLeaf.containsKey(key)) continue;
			int idx = treeNodes.size();
			float x = leafStartX + leafX.get(key);
			treeNodes.add(new TreeNode(x, 0, keyToLeaf.get(key), true, false, -1));
			leafNodeIdx.put(key, idx);
		}

		int[] instrResultNodeIdx = new int[steps];
		Arrays.fill(instrResultNodeIdx, -1);

		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			boolean isFinal = (i == steps - 1);

			int lIdx;
			if (leftSrc[i] >= 0) {
				lIdx = instrResultNodeIdx[leftSrc[i]];
			} else {
				lIdx = leafNodeIdx.getOrDefault("L" + i, -1);
			}

			int rIdx;
			if (rightSrc[i] >= 0) {
				rIdx = instrResultNodeIdx[rightSrc[i]];
			} else {
				rIdx = leafNodeIdx.getOrDefault("R" + i, -1);
			}

			float lCx = lIdx >= 0 ? treeNodes.get(lIdx).x() + NODE_SIZE / 2.0f : 0;
			float rCx = rIdx >= 0 ? treeNodes.get(rIdx).x() + NODE_SIZE / 2.0f : 0;
			float resX = (lCx + rCx) / 2.0f - NODE_SIZE / 2.0f;
			float resY = resultRow[i] * rowH;

			MergeInstruction.NodeItem result = buildResult(ins, isFinal);
			int resIdx = treeNodes.size();
			treeNodes.add(new TreeNode(resX, resY, result, false, isFinal, i));
			instrResultNodeIdx[i] = resIdx;

			if (lIdx >= 0) connectors.add(new Connector(lIdx, resIdx));
			if (rIdx >= 0) connectors.add(new Connector(rIdx, resIdx));

			if (lIdx >= 0) treeNodeIsLeft.put(lIdx, true);
			if (rIdx >= 0) treeNodeIsLeft.put(rIdx, false);
		}

		float minSep = NODE_SIZE + H_GAP;
		for (int pass = 0; pass < 10; pass++) {
			float[] xs = new float[treeNodes.size()];
			for (int i = 0; i < treeNodes.size(); i++) xs[i] = treeNodes.get(i).x();

			Map<Integer, List<Integer>> byRow = new LinkedHashMap<>();
			for (int i = 0; i < treeNodes.size(); i++) {
				int row = Math.round(treeNodes.get(i).y() / rowH);
				byRow.computeIfAbsent(row, k -> new ArrayList<>()).add(i);
			}

			boolean anyChanged = false;

			for (List<Integer> rowNodes : byRow.values()) {
				rowNodes.sort((a, b) -> Float.compare(xs[a], xs[b]));
				for (int k = 1; k < rowNodes.size(); k++) {
					int prev = rowNodes.get(k - 1);
					int cur  = rowNodes.get(k);
					float gap = xs[cur] - xs[prev];
					if (gap < minSep) {
						float push = (minSep - gap) / 2.0f;
						xs[prev] -= push;
						xs[cur]  += push;
						anyChanged = true;
					}
				}
			}

			for (int i = 0; i < steps; i++) {
				int resIdx = instrResultNodeIdx[i];
				if (resIdx < 0) continue;
				float sumCx = 0; int count = 0;
				for (Connector c : connectors) {
					if (c.toIdx() == resIdx) {
						sumCx += xs[c.fromIdx()] + NODE_SIZE / 2.0f;
						count++;
					}
				}
				if (count > 0) {
					float newX = sumCx / count - NODE_SIZE / 2.0f;
					if (Math.abs(newX - xs[resIdx]) > 0.01f) {
						xs[resIdx] = newX;
						anyChanged = true;
					}
				}
			}

			List<TreeNode> updated = new ArrayList<>();
			for (int i = 0; i < treeNodes.size(); i++) {
				TreeNode n = treeNodes.get(i);
				updated.add(new TreeNode(xs[i], n.y(), n.data(), n.isLeaf(), n.isFinal(), n.instrIdx()));
			}
			treeNodes = updated;

			if (!anyChanged) break;
		}

		float minX = treeNodes.stream().map(TreeNode::x).min(Float::compareTo).orElse(0f);
		float maxX = treeNodes.stream().map(n -> n.x() + NODE_SIZE).max(Float::compareTo).orElse(0f);
		float shiftX = -(minX + (maxX - minX) / 2.0f);
		float shiftY = V_GAP;

		List<TreeNode> shifted = new ArrayList<>();
		for (TreeNode n : treeNodes)
			shifted.add(new TreeNode(n.x() + shiftX, n.y() + shiftY, n.data(), n.isLeaf(), n.isFinal(), n.instrIdx()));
		treeNodes = shifted;

		treeOffX = P3_TREE_W / 2.0f - NODE_SIZE / 2.0f;
		treeOffY = 0;
	}

	private int resolveInputIdx(MergeInstruction.NodeItem input, int stepIdx,
	                             boolean isLeft,
	                             int[] instrResultNodeIdx,
	                             Map<String, Integer> leafNodeIdx,
	                             Set<Integer> usedResults) {
		if (input.work() > 0) {

			if (isLeft) {
				for (int prev = 0; prev < stepIdx; prev++) {
					if (instrResultNodeIdx[prev] < 0) continue;
					if (usedResults.contains(instrResultNodeIdx[prev])) continue;
					TreeNode prevNode = treeNodes.get(instrResultNodeIdx[prev]);
					if (prevNode.data().work() == input.work()) {
						usedResults.add(instrResultNodeIdx[prev]);
						return instrResultNodeIdx[prev];
					}
				}
			} else {
				for (int prev = stepIdx - 1; prev >= 0; prev--) {
					if (instrResultNodeIdx[prev] < 0) continue;
					if (usedResults.contains(instrResultNodeIdx[prev])) continue;
					TreeNode prevNode = treeNodes.get(instrResultNodeIdx[prev]);
					if (prevNode.data().work() == input.work()) {
						usedResults.add(instrResultNodeIdx[prev]);
						return instrResultNodeIdx[prev];
					}
				}
			}

			for (int prev = 0; prev < stepIdx; prev++) {
				if (instrResultNodeIdx[prev] >= 0 && !usedResults.contains(instrResultNodeIdx[prev])) {
					usedResults.add(instrResultNodeIdx[prev]);
					return instrResultNodeIdx[prev];
				}
			}
			return -1;
		}

		String key = (isLeft ? "L" : "R") + stepIdx;
		return leafNodeIdx.getOrDefault(key, -1);
	}

	private boolean enchantListsMatch(MergeInstruction.NodeItem a, MergeInstruction.NodeItem b) {
		if (a.enchants().size() != b.enchants().size()) return false;
		Set<String> aSet = new LinkedHashSet<>();
		for (String[] e : a.enchants()) aSet.add(e[0] + e[1]);
		for (String[] e : b.enchants()) if (!aSet.contains(e[0] + e[1])) return false;
		return true;
	}

	private void addLeaf(List<MergeInstruction.NodeItem> list, Set<String> seen,
	                      MergeInstruction.NodeItem item) {
		String k = nodeKey(item);
		if (!seen.contains(k)) { list.add(item); seen.add(k); }
	}

	private String nodeKey(MergeInstruction.NodeItem node) {
		List<String> enc = new ArrayList<>();
		for (String[] e : node.enchants()) enc.add(e[0] + e[1]);
		Collections.sort(enc);
		return node.id() + ":" + enc;
	}

	private MergeInstruction.NodeItem buildResult(MergeInstruction instr, boolean isFinal) {
		List<String[]> enchants = new ArrayList<>(instr.left().enchants());
		enchants.addAll(instr.right().enchants());
		boolean leftIsBook  = instr.left().id().equals("book");
		boolean rightIsBook = instr.right().id().equals("book");
		String id = (leftIsBook && rightIsBook) ? "book"
			: (selectedItem != null ? selectedItem : instr.left().id());
		int work = Math.max(instr.left().work(), instr.right().work()) + 1;
		return new MergeInstruction.NodeItem(id, enchants, work,
			instr.left().level() + instr.right().level());
	}

	private String animatedNodeName(String id) {
		String resolvedId = (id.equals("item") && isModdedId(selectedItem)) ? selectedItem : id;
		if (isModdedId(resolvedId) && s_moddedCategories != null) {
			for (ModdedCategory cat : s_moddedCategories) {
				if (cat.id().equals(resolvedId) && !cat.items().isEmpty()) {
					int idx = (int)((System.currentTimeMillis() / 1000) % cat.items().size());
					net.minecraft.resources.Identifier itemId =
						net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cat.items().get(idx));
					return itemId != null ? formatName("modded:" + itemId) : cat.items().get(idx).toString();
				}
			}
		}
		return formatName(id);
	}

	private void renderNodeTooltip(GuiGraphicsExtractor ctx, TreeNode node, boolean obtained, net.minecraft.world.item.ItemStack matchedStack, int mx, int my, Font tr) {
		List<Component> lines = new ArrayList<>();

		Style nameStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0x54FCFC));
		Style enchStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0xA8A8A8));
		Style curseStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFC5454));
		Style moddedStyle = Style.EMPTY.withColor(TextColor.fromRgb(0x54FCFC));
		Style infoStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0x545454));

		List<String[]> enchants = collectEnchantsfromNode(node);

		String nodeName;
		if (obtained && matchedStack != null && !matchedStack.isEmpty()) {
			nodeName = matchedStack.getHoverName().getString();
		} else {
			nodeName = animatedNodeName(node.data().id());
		}
		if (!enchants.isEmpty()) {
			lines.add(Component.literal(nodeName).withStyle(nameStyle));
		} else {
			lines.add(Component.literal(nodeName));
		}

		for (String[] e : enchants) {
			int lvl;
			try { lvl = Integer.parseInt(e[1]); } catch (NumberFormatException ex) { lvl = 1; }
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(e[0]);
			boolean showLevel = enchantMaxLevel(e[0]) > 1;
			String label = showLevel ? formatName(e[0]) + " " + toRoman(lvl) : formatName(e[0]);
			boolean isCurse = e[0].equals("binding_curse") || e[0].equals("vanishing_curse");
			boolean isModdedEnch = !EnchantData.ENCHANTS.containsKey(e[0]);
			Style style = isCurse ? curseStyle : isModdedEnch ? moddedStyle : enchStyle;
			lines.add(Component.literal(label).withStyle(style));
		}

		if (node.instrIdx() >= 0) {
			MergeInstruction instr = instructions.get(node.instrIdx());
			lines.add(Component.literal("Merge cost: " + instr.mergeCost() + " levels").withStyle(infoStyle));
			lines.add(Component.literal("Prior work penalty: " + instr.priorWorkPenalty() + " levels").withStyle(infoStyle));
			if (node.isFinal()) {
				int total = instructions.stream().mapToInt(MergeInstruction::mergeCost).sum();
				lines.add(Component.literal("Total cost: " + total + " levels").withStyle(infoStyle));
			}
		} else if (node.isLeaf()) {
			int pwp = (1 << node.data().work()) - 1;
			if (pwp > 0) lines.add(Component.literal("Prior work penalty: " + pwp + " levels").withStyle(infoStyle));
		}

		ctx.setComponentTooltipForNextFrame(tr, lines, mx, my);
	}

	private List<String[]> collectEnchantsfromNode(TreeNode node) {

		if (!node.data().enchants().isEmpty()) {
			return node.data().enchants();
		}

		if (node.isLeaf() && EnchantData.ENCHANTS.containsKey(node.data().id())) {

			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(node.data().id());
			int weight = def.weight();
			int lvl = weight > 0 ? node.data().level() / weight : 1;
			if (lvl <= 0) lvl = 1;
			return List.<String[]>of(new String[]{node.data().id(), String.valueOf(lvl)});
		}

		if (node.isLeaf() && node.data().id().contains(":") && !isModdedId(node.data().id())) {
			int lvl = node.data().level();
			if (lvl <= 0) lvl = 1;

			int weight = CLIENT_ENGINE.getWeightFor(node.data().id());
			if (weight > 0 && lvl >= weight) lvl = node.data().level() / weight;
			if (lvl <= 0) lvl = 1;
			return List.<String[]>of(new String[]{node.data().id(), String.valueOf(lvl)});
		}

		int nodeIdx = treeNodes.indexOf(node);
		if (nodeIdx < 0) return List.of();

		List<String[]> result = new ArrayList<>();
		for (Connector c : connectors) {
			if (c.toIdx() == nodeIdx) {
				TreeNode src = treeNodes.get(c.fromIdx());
				result.addAll(collectEnchantsfromNode(src));
			}
		}
		return result;
	}

	private static String toRoman(int n) {
		return switch (n) {
			case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
			case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
			case 9 -> "IX"; case 10 -> "X"; default -> String.valueOf(n);
		};
	}

	private void renderBack(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
		int bx = px + 7, by = py + 5;
		boolean hov = inBounds(mx, my, bx, by, BACK_W, BACK_H);
		ctx.blit(PIPE, hov ? BTN_BACK_H : BTN_BACK,
			bx, by, 0f, 0f, BACK_W, BACK_H, BACK_W, BACK_H);
	}

	private void pollMouse(int px, int py, int mx, int my) {
		boolean down = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
			Minecraft.getInstance().getWindow().handle(),
			org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

		if (down && !wasLeftDown && !justOpened) handleClick(px, py, mx, my);
		if (!down) { treeDragging = false; scrollDragging = false; listScrollDragging = false; }
		if (down && wasLeftDown) {
			if (treeDragging) {
				treeOffX = treeDragOffX + (mx - treeDragStartX);
				treeOffY = treeDragOffY + (my - treeDragStartY);
				clampTree();
			}
			if (scrollDragging && phase == Phase.TWO && totalContentH > LIST_H) {
				int maxScroll = totalContentH - LIST_H;

				float frac    = (float)(my - scrollDragStartY) / Math.max(1, SCROLL_TRACK_H - SCROLLER_H);
				int raw       = scrollDragStartOffset + (int)(frac * maxScroll);

				int snapped   = Math.round((float) raw / ROW_H) * ROW_H;
				scrollOffset  = Math.max(0, Math.min(maxScroll, snapped));
			}
			if (listScrollDragging && phase == Phase.THREE
					&& com.armaninyow.dibs.config.AeogConfig.listViewPhase3 && listTotalH > P3L_LIST_H) {
				int maxScroll = listTotalH - P3L_LIST_H;
				float frac    = (float)(my - listScrollDragStartY) / Math.max(1, SCROLL_TRACK_H - SCROLLER_H);
				listScrollOffset = Math.max(0, Math.min(maxScroll,
					listScrollDragStartOff + (int)(frac * maxScroll)));
			}
		}
		wasLeftDown = down;

		double scroll = com.armaninyow.aeog.client.AeogScrollState.consume();
		if (scroll != 0 && phase == Phase.TWO) {
			int maxScroll = Math.max(0, totalContentH - LIST_H);
			int delta     = -(int)Math.signum(scroll) * ROW_H;
			scrollOffset  = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
		}
		if (scroll != 0 && phase == Phase.THREE && com.armaninyow.dibs.config.AeogConfig.listViewPhase3) {
			int maxScroll = Math.max(0, listTotalH - P3L_LIST_H);
			listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int)Math.signum(scroll) * 7));
		}
	}

	private void handleClick(int px, int py, int mx, int my) {
		switch (phase) {
			case ONE   -> clickPhase1(px, py, mx, my);
			case TWO   -> clickPhase2(px, py, mx, my);
			case THREE -> clickPhase3(px, py, mx, my);
		}
	}

	private void clickPhase1(int px, int py, int mx, int my) {

		if (com.armaninyow.dibs.config.AeogConfig.showModButtonPhase1
				&& inBounds(mx, my, px + MOD_BTN_X, py + MOD_BTN_Y, MOD_BTN_SIZE, MOD_BTN_SIZE)) {
			modMode = !modMode;
			if (modMode && s_moddedCategories == null) {
				s_moddedCategories = buildModdedCategories();
			}
			playClick();
			return;
		}

		if (modMode) {
			clickPhase1Modded(px, py, mx, my);
		} else {
			clickPhase1Vanilla(px, py, mx, my);
		}

		if (inBounds(mx, my, px + OPT_X, py + OPT_Y, OPT_W, OPT_H)) {
			modeLevels = !modeLevels;
			playClick();
		}
	}

	private void clickPhase1Vanilla(int px, int py, int mx, int my) {
		List<String> items = EnchantData.getPhase1Items();
		for (int i = 0; i < items.size(); i++) {
			int bx = px + GRID_X + (i % GRID_COLS) * BTN_SIZE;
			int by = py + GRID_Y + (i / GRID_COLS) * BTN_SIZE;
			if (inBounds(mx, my, bx, by, BTN_SIZE, BTN_SIZE)) {
				selectedItem = items.get(i);
				buildPhase2Data();
				phase = Phase.TWO;
				playClick();
				return;
			}
		}
	}

	private void clickPhase1Modded(int px, int py, int mx, int my) {
		if (s_moddedCategories == null) return;
		List<ModdedCategory> cats = s_moddedCategories;
		for (int i = 0; i < cats.size(); i++) {
			int bx = px + GRID_X + (i % GRID_COLS) * BTN_SIZE;
			int by = py + GRID_Y + (i / GRID_COLS) * BTN_SIZE;
			if (inBounds(mx, my, bx, by, BTN_SIZE, BTN_SIZE)) {
				selectedItem = cats.get(i).id();
				buildPhase2Data();
				phase = Phase.TWO;
				playClick();
				return;
			}
		}
	}

	private void clickPhase2(int px, int py, int mx, int my) {

		if (com.armaninyow.dibs.config.AeogConfig.showModButtonPhase2
				&& inBounds(mx, my, px + MOD_BTN_X, py + MOD_BTN_Y, MOD_BTN_SIZE, MOD_BTN_SIZE)) {
			showModdedEnchants = !showModdedEnchants;
			playClick();
			return;
		}
		if (inBounds(mx, my, px + 7, py + 5, BACK_W, BACK_H)) {
			selectedItem = null;
			expandedGroupIdx = -1;
			phase = Phase.ONE;
			playClick();
			return;
		}
		if (!selectedLevels.isEmpty() && inBounds(mx, my, px + CALC_X, py + CALC_Y, CALC_W, CALC_H)) {
			treeNodes.clear();
			connectors.clear();
			loading = true;
			loadingStartMs = System.currentTimeMillis();
			expandedGroupIdx = -1;
			phase = Phase.THREE;
			sendCalculationRequest();
			playClick(); return;
		}

		if (totalContentH > LIST_H) {
			int tAx = px + SCROLL_X, tAy = py + SCROLL_Y;
			float frac = (float) scrollOffset / Math.max(1, totalContentH - LIST_H);
			int thumbY = tAy + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
			if (inBounds(mx, my, tAx, thumbY, SCROLLER_W, SCROLLER_H)) {
				scrollDragging = true; scrollDragStartY = my; scrollDragStartOffset = scrollOffset;
				return;
			}
		}
		int lax = px + LIST_X;
		int listTop = py + LIST_Y, listBottom = listTop + LIST_H;
		int y = listTop - scrollOffset;
		for (int gi = 0; gi < incompatGroups.size(); gi++) {
			List<String> group = incompatGroups.get(gi);
			int vi = groupVisible.get(gi);
			String enchant = group.get(vi);
			boolean disabled = isEnchantDisabledByTrident(enchant);
			boolean expanded = (expandedGroupIdx == gi);
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
			boolean isModded = !EnchantData.ENCHANTS.containsKey(enchant);

			if (isModded && !isShowingModdedEnchants()) { continue; }

			if (y + ROW_H <= listTop || y >= listBottom) { y += ROW_H; continue; }

			if (expanded) {

				int totalSlots = enchantMaxLevel(enchant) + 1;
				int totalW     = totalSlots * ROW_H;
				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = lax + (rowW - totalW) / 2;
				for (int lv = 0; lv <= enchantMaxLevel(enchant); lv++) {
					int lvX = startX + lv * ROW_H;
					if (inBounds(mx, my, lvX, y, ROW_H, ROW_H)) {
						if (lv == 0) {
							selectedLevels.remove(enchant);
						} else {
							selectedLevels.put(enchant, lv);
							enforceTrident(enchant);
							if (!com.armaninyow.dibs.config.AeogConfig.allowIncompatible) {
								for (String other : group) {
									if (!other.equals(enchant)) selectedLevels.remove(other);
								}
							}
						}
						expandedGroupIdx = -1;
						playClick();
						return;
					}
				}

				expandedGroupIdx = -1;

			} else {

				if (group.size() > 1 && inBounds(mx, my, lax, y, PREV_W, ROW_H)) {
					groupVisible.set(gi, (vi - 1 + group.size()) % group.size()); playClick(); return;
				}
				if (group.size() > 1 && inBounds(mx, my, lax + PREV_W + SLOT_W, y, NEXT_W, ROW_H)) {
					groupVisible.set(gi, (vi + 1) % group.size()); playClick(); return;
				}

				if (!disabled && inBounds(mx, my, lax + PREV_W, y, SLOT_W, ROW_H)) {
					expandedGroupIdx = gi;
					playClick();
					return;
				}
			}
			y += ROW_H;
		}
	}

	private void clickPhase3(int px, int py, int mx, int my) {
		if (inBounds(mx, my, px + 7, py + 5, BACK_W, BACK_H)) {
			loading = false;
			phase = Phase.TWO; playClick(); return;
		}
		if (com.armaninyow.dibs.config.AeogConfig.listViewPhase3) {

			if (listTotalH > P3L_LIST_H) {
				int tAx = px + SCROLL_X, tAy = py + SCROLL_Y;
				int maxScroll = listTotalH - P3L_LIST_H;
				float frac = (float) listScrollOffset / Math.max(1, maxScroll);
				int thumbY = tAy + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
				if (inBounds(mx, my, tAx, thumbY, SCROLLER_W, SCROLLER_H)) {
					listScrollDragging = true;
					listScrollDragStartY = my;
					listScrollDragStartOff = listScrollOffset;
					return;
				}
			}

			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				List<ListRow> rows = buildListRows(mc.font);
				int listAbsX = px + P3L_LIST_X;
				int listAbsY = py + P3L_LIST_Y;
				int clipBottom = listAbsY + P3L_LIST_H;
				int y = listAbsY - listScrollOffset;
				for (ListRow row : rows) {
					int rowBottom = y + row.height;
					if (rowBottom < listAbsY) { y = rowBottom; continue; }
					if (y >= clipBottom)      { break; }

					int iconRowH = Math.max(LIST_FRAME_SIZE, mc.font.lineHeight);

					int plusW     = mc.font.width(" + ");
					int frameX    = listAbsX;
					int leftIconX = frameX + LIST_FRAME_SIZE;
					int rightIconX = leftIconX + LIST_FRAME_SIZE + plusW;

					if (row.leftObtained
							&& inBounds(mx, my, leftIconX, y, LIST_FRAME_SIZE, iconRowH)
							&& y < clipBottom && y + iconRowH > listAbsY) {
						ItemStack match = findMatchingStack(row.instr.left(), null);
						if (match != null) { sendToAnvilSlot(match, 0); playClick(); return; }
					}

					if (row.rightObtained
							&& inBounds(mx, my, rightIconX, y, LIST_FRAME_SIZE, iconRowH)
							&& y < clipBottom && y + iconRowH > listAbsY) {
						ItemStack match = findMatchingStack(row.instr.right(), null);
						if (match != null) { sendToAnvilSlot(match, 1); playClick(); return; }
					}

					y = rowBottom;
				}
			}
			return;
		}

		int tax = px + P3_TREE_X, tay = py + P3_TREE_Y;
		if (!inBounds(mx, my, tax, tay, P3_TREE_W, P3_TREE_H)) return;

		for (int i = 0; i < treeNodes.size(); i++) {
			TreeNode node = treeNodes.get(i);
			if (node.isFinal()) continue;
			int nx = tax + (int)(node.x() + treeOffX);
			int ny = tay + (int)(node.y() + treeOffY);
			if (!inBounds(mx, my, nx, ny, NODE_SIZE, NODE_SIZE)) continue;

			ItemStack match = findMatchingStack(node.data(), node);
			if (match == null) return;

			int slot = 0;
			outer:
			for (Connector con : connectors) {
				if (con.fromIdx() != i) continue;
				int parentIdx = con.toIdx();

				for (Connector con2 : connectors) {
					if (con2.toIdx() == parentIdx && con2.fromIdx() != i) {
						float myX  = treeNodes.get(i).x();
						float sibX = treeNodes.get(con2.fromIdx()).x();
						slot = (myX <= sibX) ? 0 : 1;
						break outer;
					}
				}
			}

			sendToAnvilSlot(match, slot);
			playClick();
			return;
		}

		treeDragging = true;
		treeDragStartX = mx; treeDragStartY = my;
		treeDragOffX = treeOffX; treeDragOffY = treeOffY;
	}

	private void clampTree() {
		if (treeNodes.isEmpty()) return;
		float minX = treeNodes.stream().map(TreeNode::x).min(Float::compareTo).orElse(0f);
		float maxX = treeNodes.stream().map(n -> n.x() + NODE_SIZE).max(Float::compareTo).orElse(0f);
		float minY = treeNodes.stream().map(TreeNode::y).min(Float::compareTo).orElse(0f);
		float maxY = treeNodes.stream().map(n -> n.y() + NODE_SIZE).max(Float::compareTo).orElse(0f);
		float treeW = maxX - minX;
		float treeH = maxY - minY;
		treeOffX = Math.max(-(minX + treeW - V_GAP), Math.min(-minX + P3_TREE_W - V_GAP, treeOffX));
		treeOffY = Math.max(-(minY + treeH - V_GAP), Math.min(-minY + P3_TREE_H - V_GAP, treeOffY));
	}

	private void buildPhase2Data() {
		selectedLevels.clear(); scrollOffset = 0; expandedGroupIdx = -1;

		if (isModdedId(selectedItem)) {

			itemEnchants = new ArrayList<>();
			if (s_moddedCategories != null) {
				for (ModdedCategory cat : s_moddedCategories) {
					if (cat.id().equals(selectedItem)) {
						itemEnchants = new ArrayList<>(cat.enchants());
						break;
					}
				}
			}

			incompatGroups.clear(); groupVisible.clear();
			Set<String> placed = new HashSet<>();
			for (String enchant : itemEnchants) {
				if (placed.contains(enchant)) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
				if (def == null) {

					incompatGroups.add(new ArrayList<>(List.of(enchant)));
					groupVisible.add(0);
					placed.add(enchant);
				} else if (com.armaninyow.dibs.config.AeogConfig.allowIncompatible) {
					incompatGroups.add(new ArrayList<>(List.of(enchant)));
					groupVisible.add(0);
					placed.add(enchant);
				} else {
					List<String> group = new ArrayList<>();
					group.add(enchant); placed.add(enchant);
					for (String ic : def.incompatible()) {
						if (!placed.contains(ic) && itemEnchants.contains(ic)) {
							group.add(ic); placed.add(ic);
						}
					}
					incompatGroups.add(group); groupVisible.add(0);
				}
			}
			autoFillLevels();
			return;
		}

		itemEnchants = new ArrayList<>(EnchantData.enchantsForItem(selectedItem));

		Minecraft mc2 = Minecraft.getInstance();
		if (mc2.level != null) {
			var enchantReg2 = mc2.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

			ItemStack vanillaStack = vanillaStackForCategory(selectedItem);
			if (vanillaStack != null && !vanillaStack.isEmpty()) {
				for (var enchEntry : enchantReg2.listElements().toList()) {
					net.minecraft.resources.Identifier enchId = enchEntry.key().identifier();
					if (enchId.getNamespace().equals("minecraft")) continue;
					if (!enchEntry.value().isSupportedItem(vanillaStack)) continue;
					String internalKey = toInternalEnchantKey(enchId);
					String storedKey = internalKey != null ? internalKey : enchId.toString();
					if (!itemEnchants.contains(storedKey)) {
						itemEnchants.add(storedKey);

						if (!s_moddedEnchantNames.containsKey(storedKey)) {
							s_moddedEnchantNames.put(storedKey, enchEntry.value().description().getString());
						}
					}
				}
			}
		}
		incompatGroups.clear(); groupVisible.clear();

		if (com.armaninyow.dibs.config.AeogConfig.allowIncompatible) {
			Set<String> placed = new HashSet<>();

			boolean needsTridentSplit = selectedItem != null
				&& (selectedItem.equals("trident") || selectedItem.equals("book"));
			if (needsTridentSplit) {
				for (String enchant : List.of("channeling", "loyalty", "riptide")) {
					if (itemEnchants.contains(enchant) && !placed.contains(enchant)) {
						incompatGroups.add(new ArrayList<>(List.of(enchant)));
						groupVisible.add(0);
						placed.add(enchant);
					}
				}
			}
			for (String enchant : itemEnchants) {
				if (placed.contains(enchant)) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);

				incompatGroups.add(new ArrayList<>(List.of(enchant)));
				groupVisible.add(0);
				placed.add(enchant);

				for (String ic : def.incompatible()) {
					if (!placed.contains(ic) && itemEnchants.contains(ic)) {
						incompatGroups.add(new ArrayList<>(List.of(ic)));
						groupVisible.add(0);
						placed.add(ic);
					}
				}
			}
		} else {
			Set<String> placed = new HashSet<>();

			boolean needsTridentSplit = selectedItem != null
				&& (selectedItem.equals("trident") || selectedItem.equals("book"));
			if (needsTridentSplit) {
				List<String> tridentOrder = List.of("channeling", "loyalty", "riptide");
				for (String enchant : tridentOrder) {
					if (itemEnchants.contains(enchant) && !placed.contains(enchant)) {
						incompatGroups.add(new ArrayList<>(List.of(enchant)));
						groupVisible.add(0);
						placed.add(enchant);
					}
				}
			}

			for (String enchant : itemEnchants) {
				if (placed.contains(enchant)) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
				List<String> group = new ArrayList<>();
				group.add(enchant); placed.add(enchant);
				if (def != null) for (String ic : def.incompatible()) {
					if (!placed.contains(ic) && itemEnchants.contains(ic)) {
						group.add(ic); placed.add(ic);
					}
				}
				incompatGroups.add(group); groupVisible.add(0);
			}
		}

		Set<String> alreadyPlaced = new HashSet<>();
		for (List<String> g : incompatGroups) alreadyPlaced.addAll(g);
		for (String enchant : itemEnchants) {
			if (alreadyPlaced.contains(enchant)) continue;
			if (EnchantData.ENCHANTS.containsKey(enchant)) continue;
			incompatGroups.add(new ArrayList<>(List.of(enchant)));
			groupVisible.add(0);
		}

		autoFillLevels();
	}

	private void autoFillLevels() {
		com.armaninyow.dibs.config.AeogConfig.AutoFillMode mode =
			com.armaninyow.dibs.config.AeogConfig.autoFillMode;
		if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.OFF) return;

		if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.MAX_LEVELS) {
			for (String enchant : itemEnchants) {
				if (enchant.equals("binding_curse") || enchant.equals("vanishing_curse")) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
				if (def == null) {

					int maxLvl = getModdedEnchantMaxLevel(enchant);
					if (maxLvl > 0) selectedLevels.put(enchant, maxLvl);
				} else {
					selectedLevels.put(enchant, enchantMaxLevel(enchant));
				}
			}
		} else if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.FROM_INVENTORY) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			Inventory inv = mc.player.getInventory();
			if (mc.level == null) return;
			net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> reg = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (s.isEmpty() || !s.is(Items.ENCHANTED_BOOK)) continue;
				var stored = s.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
				if (stored == null) continue;
				for (String enchant : itemEnchants) {
					if (enchant.equals("binding_curse") || enchant.equals("vanishing_curse")) continue;
					String mcId = toMinecraftEnchantId(enchant);
					net.minecraft.resources.Identifier enchIdentifier = mcId.contains(":")
						? net.minecraft.resources.Identifier.parse(mcId)
						: net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", mcId);
					var entry = reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchIdentifier));
					if (entry.isEmpty()) continue;
					int lvl = stored.getLevel(entry.get());
					if (lvl > 0) {
						int cur = selectedLevels.getOrDefault(enchant, 0);
						if (lvl > cur) selectedLevels.put(enchant, lvl);
					}
				}
			}
		}

		if (!com.armaninyow.dibs.config.AeogConfig.allowIncompatible) {
			for (int gi = 0; gi < incompatGroups.size(); gi++) {
				List<String> group = incompatGroups.get(gi);
				if (group.size() <= 1) continue;
				String best = null; int bestLvl = 0;
				for (String e : group) {
					int lvl = selectedLevels.getOrDefault(e, 0);
					if (lvl > bestLvl) { bestLvl = lvl; best = e; }
				}
				for (int j = 0; j < group.size(); j++) {
					String e = group.get(j);
					if (!e.equals(best)) selectedLevels.remove(e);
					else if (best != null) groupVisible.set(gi, j);
				}
			}
		}
	}

	public void tickAutoDetect(net.minecraft.world.item.ItemStack targetSlotStack) {
		if (!com.armaninyow.dibs.config.AeogConfig.autoDetectItem) return;
		if (phase != Phase.ONE) return;
		if (targetSlotStack == null || targetSlotStack.isEmpty()) return;

		String detected = detectItemId(targetSlotStack);
		if (detected == null) return;

		selectedItem = detected;
		buildPhase2Data();
		phase = Phase.TWO;
		playClick();
	}

	private String detectItemId(ItemStack s) {
		for (String id : EnchantData.getPhase1Items()) {
			if (id.equals("book")) continue;
			if (itemMatchesId(s, id)) return id;
		}

		if (s_moddedCategories != null) {
			for (ModdedCategory cat : s_moddedCategories) {
				for (net.minecraft.world.item.Item item : cat.items()) {
					if (s.is(item)) return cat.id();
				}
			}
		}
		return null;
	}

	private boolean isEnchantDisabledByTrident(String e) {
		if (com.armaninyow.dibs.config.AeogConfig.allowIncompatible) return false;
		boolean r = selectedLevels.containsKey("riptide");
		boolean l = selectedLevels.containsKey("loyalty");
		boolean c = selectedLevels.containsKey("channeling");
		if (e.equals("riptide") && (l || c)) return true;
		if ((e.equals("loyalty") || e.equals("channeling")) && r) return true;
		return false;
	}

	private void enforceTrident(String just) {
		if (com.armaninyow.dibs.config.AeogConfig.allowIncompatible) return;
		if (just.equals("riptide")) { selectedLevels.remove("loyalty"); selectedLevels.remove("channeling"); }
		else if (just.equals("loyalty") || just.equals("channeling")) selectedLevels.remove("riptide");
	}

	private static final OptimizationEngine CLIENT_ENGINE = new OptimizationEngine();

	private void sendCalculationRequest() {
		if (selectedItem == null) return;

		List<int[]> enchants = new ArrayList<>();
		Minecraft mcRef = Minecraft.getInstance();
		for (String e : itemEnchants) {
			if (!selectedLevels.containsKey(e)) continue;
			int id;
			if (EnchantData.ENCHANTS.containsKey(e)) {

				id = CLIENT_ENGINE.registerExtraEnchant(e, EnchantData.ENCHANTS.get(e).weight());
			} else {

				int weight = 1;
				if (mcRef.level != null) {
					net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> reg = mcRef.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
					String mcId = toMinecraftEnchantId(e);
					net.minecraft.resources.Identifier enchId = mcId.contains(":")
						? net.minecraft.resources.Identifier.parse(mcId)
						: net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", mcId);
					var entry = reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchId));
					if (entry.isPresent()) weight = Math.max(1, entry.get().value().getAnvilCost() / 2);
				}
				id = CLIENT_ENGINE.registerExtraEnchant(e, weight);
			}
			enchants.add(new int[]{id, selectedLevels.get(e)});
		}

		if (enchants.isEmpty()) return;

		final String itemSnapshot = isModdedId(selectedItem) ? "item" : selectedItem;
		final List<int[]> enchantsSnapshot = enchants;
		final OptimizationEngine.Mode modeSnapshot = modeLevels
			? OptimizationEngine.Mode.LEVELS
			: OptimizationEngine.Mode.WORK;
		Thread.ofVirtual().name("aeog-engine").start(() -> {
			try {
				List<MergeInstruction> result = CLIENT_ENGINE.process(itemSnapshot, enchantsSnapshot, modeSnapshot);
				net.minecraft.client.Minecraft.getInstance().execute(() -> receiveEngineResult(result));
			} catch (Exception ex) {
				AnvilEnchantmentOrderingGuide.LOGGER.error("[AEOG] Engine error: {}", ex.getMessage(), ex);
				net.minecraft.client.Minecraft.getInstance().execute(() -> loading = false);
			}
		});
	}

	public void receiveEngineResult(List<MergeInstruction> result) {
		instructions = result;
		treeOffX = 0; treeOffY = 0;
		buildTreeLayout();
		loading = false;
		phase = Phase.THREE;
	}

	private void sendToAnvilSlot(ItemStack stack, int anvilSlot) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null) return;
		net.minecraft.client.gui.screens.Screen screen = mc.gui.screen();
		if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) return;

		net.minecraft.world.inventory.AbstractContainerMenu menu =
			((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) screen).getMenu();
		int containerId = menu.containerId;

		if (!menu.getSlot(anvilSlot).getItem().isEmpty()) {
			mc.gameMode.handleContainerInput(
				containerId, anvilSlot,
				0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
		}

		net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
		int invSlot = -1;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i) == stack) { invSlot = i; break; }
		}
		if (invSlot < 0) return;

		int containerSlot;
		if (invSlot < 9) {
			containerSlot = 30 + invSlot;
		} else {
			containerSlot = invSlot - 6;
		}

		mc.gameMode.handleContainerInput(
			containerId, containerSlot,
			0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);

		mc.gameMode.handleContainerInput(
			containerId, anvilSlot,
			0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
	}

	private boolean playerHasItem(MergeInstruction.NodeItem data) {
		return findMatchingStack(data, null) != null;
	}

	private ItemStack findMatchingStack(MergeInstruction.NodeItem data, TreeNode node) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		Inventory inv = mc.player.getInventory();
		int expectedPwp = (1 << data.work()) - 1;

		List<String[]> enchants = node != null ? collectEnchantsfromNode(node) : data.enchants();

		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty() || !itemMatchesId(s, data.id())) continue;

			int rc = s.has(DataComponents.REPAIR_COST)
				? s.get(net.minecraft.core.component.DataComponents.REPAIR_COST) : 0;
			if (rc != expectedPwp) continue;

			var enchComp = data.id().equals("book")
				? s.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
				: s.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
			int enchSize = enchComp != null ? enchComp.size() : 0;

			if (!enchants.isEmpty() && enchSize != enchants.size()) continue;

			if (enchComp != null && !enchants.isEmpty()) {
				boolean allMatch = true;
				for (String[] e : enchants) {
					int level;
					try { level = Integer.parseInt(e[1]); } catch (NumberFormatException ex) { level = 1; }
					if (mc.level == null) { allMatch = false; break; }
					var reg      = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
					String mcId  = toMinecraftEnchantId(e[0]);

					net.minecraft.resources.Identifier enchIdentifier = mcId.contains(":")
						? net.minecraft.resources.Identifier.parse(mcId)
						: net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", mcId);
					var entryOpt = reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchIdentifier));
					if (entryOpt.isEmpty()) { allMatch = false; break; }
					if (enchComp.getLevel(entryOpt.get()) != level) { allMatch = false; break; }
				}
				if (!allMatch) continue;
			}

			return s;
		}
		return null;
	}

	private boolean itemMatchesId(ItemStack s, String id) {

		if (id.equals("item") && isModdedId(selectedItem)) {
			return itemMatchesId(s, selectedItem);
		}
		if (isModdedId(id)) {

			if (s_moddedCategories != null) {
				for (ModdedCategory cat : s_moddedCategories) {
					if (cat.id().equals(id)) {
						for (net.minecraft.world.item.Item item : cat.items()) {
							if (s.is(item)) return true;
						}
						return false;
					}
				}
			}
			return false;
		}
		return switch (id) {
			case "book"                     -> s.is(Items.ENCHANTED_BOOK);
			case "helmet"                   -> s.is(Items.DIAMOND_HELMET)||s.is(Items.NETHERITE_HELMET)
				||s.is(Items.IRON_HELMET)||s.is(Items.GOLDEN_HELMET)||s.is(Items.CHAINMAIL_HELMET)
				||s.is(Items.LEATHER_HELMET)||s.is(Items.TURTLE_HELMET);
			case "chestplate"               -> s.is(Items.DIAMOND_CHESTPLATE)||s.is(Items.NETHERITE_CHESTPLATE)
				||s.is(Items.IRON_CHESTPLATE)||s.is(Items.GOLDEN_CHESTPLATE)
				||s.is(Items.CHAINMAIL_CHESTPLATE)||s.is(Items.LEATHER_CHESTPLATE);
			case "leggings"                 -> s.is(Items.DIAMOND_LEGGINGS)||s.is(Items.NETHERITE_LEGGINGS)
				||s.is(Items.IRON_LEGGINGS)||s.is(Items.GOLDEN_LEGGINGS)
				||s.is(Items.CHAINMAIL_LEGGINGS)||s.is(Items.LEATHER_LEGGINGS);
			case "boots"                    -> s.is(Items.DIAMOND_BOOTS)||s.is(Items.NETHERITE_BOOTS)
				||s.is(Items.IRON_BOOTS)||s.is(Items.GOLDEN_BOOTS)
				||s.is(Items.CHAINMAIL_BOOTS)||s.is(Items.LEATHER_BOOTS);
			case "sword"                    -> s.is(Items.DIAMOND_SWORD)||s.is(Items.NETHERITE_SWORD)
				||s.is(Items.IRON_SWORD)||s.is(Items.GOLDEN_SWORD)||s.is(Items.STONE_SWORD)
				||s.is(Items.WOODEN_SWORD);
			case "pickaxe"                  -> s.is(Items.DIAMOND_PICKAXE)||s.is(Items.NETHERITE_PICKAXE)
				||s.is(Items.IRON_PICKAXE)||s.is(Items.GOLDEN_PICKAXE)||s.is(Items.STONE_PICKAXE)
				||s.is(Items.WOODEN_PICKAXE);
			case "axe"                      -> s.is(Items.DIAMOND_AXE)||s.is(Items.NETHERITE_AXE)
				||s.is(Items.IRON_AXE)||s.is(Items.GOLDEN_AXE)||s.is(Items.STONE_AXE)
				||s.is(Items.WOODEN_AXE);
			case "shovel"                   -> s.is(Items.DIAMOND_SHOVEL)||s.is(Items.NETHERITE_SHOVEL)
				||s.is(Items.IRON_SHOVEL)||s.is(Items.GOLDEN_SHOVEL)||s.is(Items.STONE_SHOVEL)
				||s.is(Items.WOODEN_SHOVEL);
			case "hoe"                      -> s.is(Items.DIAMOND_HOE)||s.is(Items.NETHERITE_HOE)
				||s.is(Items.IRON_HOE)||s.is(Items.GOLDEN_HOE)||s.is(Items.STONE_HOE)
				||s.is(Items.WOODEN_HOE);
			case "bow"                      -> s.is(Items.BOW);
			case "crossbow"                 -> s.is(Items.CROSSBOW);
			case "trident"                  -> s.is(Items.TRIDENT);
			case "elytra"                   -> s.is(Items.ELYTRA);
			case "shield"                   -> s.is(Items.SHIELD);
			case "fishing_rod"              -> s.is(Items.FISHING_ROD);
			case "flint_and_steel"          -> s.is(Items.FLINT_AND_STEEL);
			case "shears"                   -> s.is(Items.SHEARS);
			case "brush"                    -> s.is(Items.BRUSH);
			case "carrot_on_a_stick"        -> s.is(Items.CARROT_ON_A_STICK);
			case "warped_fungus_on_a_stick" -> s.is(Items.WARPED_FUNGUS_ON_A_STICK);
			case "pumpkin"                  -> s.is(Items.CARVED_PUMPKIN);
			case "mace"                     -> s.is(Items.MACE);
			case "spear"                    -> {

				String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
				yield path.endsWith("_spear") || path.equals("spear");
			}
			default                         -> false;
		};
	}

	private void drawIcon(GuiGraphicsExtractor ctx, String itemId, int x, int y) {

		if (itemId.equals("item") && isModdedId(selectedItem)) {
			drawIcon(ctx, selectedItem, x, y);
			return;
		}
		if (isModdedId(itemId)) {

			if (s_moddedCategories != null) {
				for (ModdedCategory cat : s_moddedCategories) {
					if (cat.id().equals(itemId) && !cat.items().isEmpty()) {
						int iconIdx = (int)((System.currentTimeMillis() / 1000) % cat.items().size());
						ctx.item(new net.minecraft.world.item.ItemStack(cat.items().get(iconIdx)), x, y);
						return;
					}
				}
			}
			return;
		}
		net.minecraft.resources.Identifier icon = getItemIcons().get(itemId);
		if (icon != null)
			ctx.blit(PIPE, icon, x, y, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	private String formatName(String id) {
		if (isModdedId(id)) {

			String raw = id.substring("modded:".length());
			int colon = raw.indexOf(':');
			if (colon >= 0) {
				String namespace = raw.substring(0, colon);
				String path = raw.substring(colon + 1);
				String translationKey = "item." + namespace + "." + path.replace('/', '.');
				String translated = net.minecraft.client.resources.language.I18n.get(translationKey);
				if (!translated.equals(translationKey)) return translated;

				return Arrays.stream(path.replace('/', ' ').split("[_ ]"))
					.map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
					.reduce((a, b) -> a + " " + b).orElse(path);
			}
			return raw;
		}

		if (id.contains(":")) {

			String cached = s_moddedEnchantNames.get(id);
			if (cached != null && !cached.isEmpty()) return cached;
			String namespace = id.substring(0, id.indexOf(':'));
			String path = id.substring(id.indexOf(':') + 1);

			String translationKey = "enchantment." + namespace + "." + path.replace('/', '.');
			String translated = net.minecraft.client.resources.language.I18n.get(translationKey);
			if (!translated.equals(translationKey)) {

				return translated;
			}

			String leaf = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
			String formatted = Arrays.stream(leaf.split("_"))
				.map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
				.reduce((a, b) -> a + " " + b).orElse(leaf);
			if (s_loggedMissingTranslations.add(id)) {
				AnvilEnchantmentOrderingGuide.LOGGER.debug("[AEOG] no translation for key='{}', formatted='{}'", translationKey, formatted);
			}
			return formatted;
		}
		if (id.equals("book"))                      return "Enchanted Book";
		if (id.equals("pumpkin"))                   return "Carved Pumpkin";
		if (id.equals("flint_and_steel"))           return "Flint and Steel";
		if (id.equals("carrot_on_a_stick"))         return "Carrot on a Stick";
		if (id.equals("warped_fungus_on_a_stick"))  return "Warped Fungus on a Stick";
		if (id.equals("binding_curse"))             return "Curse of Binding";
		if (id.equals("vanishing_curse"))           return "Curse of Vanishing";
		if (id.equals("sweeping"))                  return "Sweeping Edge";
		String result = Arrays.stream(id.split("_"))
			.map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
			.reduce((a, b) -> a + " " + b).orElse(id);
		return result;
	}

	private String toMinecraftEnchantId(String internalKey) {
		return switch (internalKey) {
			case "sweeping" -> "sweeping_edge";
			default         -> internalKey;
		};
	}

	private static boolean inBounds(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}