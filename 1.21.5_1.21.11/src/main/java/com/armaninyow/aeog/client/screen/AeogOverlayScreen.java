package com.armaninyow.aeog.client.screen;

import com.armaninyow.aeog.AnvilEnchantmentOrderingGuide;
import com.armaninyow.aeog.engine.EnchantData;
import com.armaninyow.aeog.engine.MergeInstruction;
import com.armaninyow.aeog.engine.OptimizationEngine;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * The three-phase AEOG panel renderer.
 * Not a Screen — drawn directly by AnvilScreenMixin inside drawBackground,
 * so the vanilla anvil GUI is never disrupted.
 *
 * Phase 1 — Item selection grid
 * Phase 2 — Enchant/level configuration + scroll
 * Phase 3 — Draggable binary merge tree (correct structure)
 */
@Environment(EnvType.CLIENT)
public class AeogOverlayScreen {

	// ── Render pipeline ───────────────────────────────────────────────────────

	private static final RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;

	// ── Textures ──────────────────────────────────────────────────────────────

	private static final String TEX   = "textures/gui/phases/";
	private static final String ITEMS = "textures/items/";

	private static final Identifier P1_CONTAINER  = id(TEX + "phase_1_container.png");
	private static final Identifier P2_CONTAINER  = id(TEX + "phase_2_container.png");
	private static final Identifier P3_CONTAINER  = id(TEX + "phase_3_container.png");
	private static final Identifier BACKGROUND    = id(TEX + "background.png");

	private static final Identifier BTN_ITEM      = id(TEX + "button.png");
	private static final Identifier BTN_ITEM_H    = id(TEX + "button_highlighted.png");
	private static final Identifier BTN_OPT       = id(TEX + "optimize_button.png");
	private static final Identifier BTN_OPT_H     = id(TEX + "optimize_button_highlighted.png");
	private static final Identifier BTN_CALC      = id(TEX + "calculate_button.png");
	private static final Identifier BTN_CALC_H    = id(TEX + "calculate_button_highlighted.png");
	private static final Identifier BTN_CALC_DIS  = id(TEX + "calculate_button_disabled.png");
	private static final Identifier BTN_BACK      = id(TEX + "phase_backward.png");
	private static final Identifier BTN_BACK_H    = id(TEX + "phase_backward_highlighted.png");

	private static final Identifier ENCH_SLOT     = id(TEX + "enchantment_slot.png");
	private static final Identifier ENCH_SLOT_H   = id(TEX + "enchantment_slot_highlighted.png");
	private static final Identifier ENCH_SLOT_DIS = id(TEX + "enchantment_slot_disabled.png");
	private static final Identifier ENCH_PREV     = id(TEX + "enchantment_prev.png");
	private static final Identifier ENCH_PREV_H   = id(TEX + "enchantment_prev_highlighted.png");
	private static final Identifier ENCH_NEXT     = id(TEX + "enchantment_next.png");
	private static final Identifier ENCH_NEXT_H   = id(TEX + "enchantment_next_highlighted.png");
	private static final Identifier LVL_SLOT      = id(TEX + "level_slot.png");
	private static final Identifier LVL_SLOT_DIS  = id(TEX + "level_slot_disabled.png");
	private static final Identifier LVL_SLOT_SEL  = id(TEX + "level_slot_selected.png");
	private static final Identifier SCROLLER      = id(TEX + "scroller.png");

	private static final Identifier FRAME_INIT_O  = id(TEX + "initial_frame_obtained.png");
	private static final Identifier FRAME_INIT_U  = id(TEX + "initial_frame_unobtained.png");
	private static final Identifier FRAME_PROG_O  = id(TEX + "progress_frame_obtained.png");
	private static final Identifier FRAME_PROG_U  = id(TEX + "progress_frame_unobtained.png");
	private static final Identifier FRAME_FINAL_O = id(TEX + "final_frame_obtained.png");
	private static final Identifier FRAME_FINAL_U = id(TEX + "final_frame_unobtained.png");
	private static final Identifier P3_CONTAINER_2   = id(TEX + "phase_3_container_2.png");
	private static final Identifier LIST_FRAME_O  = id(TEX + "list_frame_obtained.png");
	private static final Identifier LIST_FRAME_U  = id(TEX + "list_frame_unobtained.png");

	/** Custom item icons — 16×16 PNGs from textures/items/ — built lazily on first use. */
	private static Map<String, Identifier> ITEM_ICONS = null;

	private static Map<String, Identifier> getItemIcons() {
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

	private static Identifier id(String path) {
		return Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, path);
	}

	private void playClick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.getSoundManager() == null) return;
		mc.getSoundManager().play(new PositionedSoundInstance(
			net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value().id(),
			net.minecraft.sound.SoundCategory.MASTER,
			0.25f, 1.0f, net.minecraft.util.math.random.Random.create(),
			false, 0,
			net.minecraft.client.sound.SoundInstance.AttenuationType.NONE,
			0.0, 0.0, 0.0, true));
	}

	// ── Layout constants ──────────────────────────────────────────────────────

	/** Container size — 194×166 px PNGs */
	public static final int P_W = 194, P_H = 166;
	private static final int GAP = 2;

	// Phase 1 — button grid: x=25 y=35 → x=168 y=88  (3 rows × 8 cols, 18×18 each)
	private static final int GRID_COLS = 8;
	private static final int GRID_X = 25, GRID_Y = 35;
	private static final int BTN_SIZE = 18, ICON_SIZE = 16;
	// Optimize button: x=25, y=141, 144×18
	private static final int OPT_X = 25, OPT_Y = 141, OPT_W = 144, OPT_H = 18;

	// Phase 2 — scrollable list: x=7 y=17 → x=168 y=124  (w=161 h=107)
	private static final int LIST_X = 7,  LIST_Y = 17;
	private static final int LIST_W = 161, LIST_H = 108;
	private static final int ROW_H  = 18;
	// Enchant slot: 126×18, prev/next: 18×18 each  (18+126+18 = 162, fits LIST_W=161 — slot trims by 1)
	private static final int PREV_W = 18, NEXT_W = 18, SLOT_W = 126;
	// Level slot: 18×18
	// Calculate button: x=61, y=141, 54×18
	private static final int CALC_X = 61, CALC_Y = 141, CALC_W = 54, CALC_H = 18;
	// Scroll bar: x=174 y=18 → x=185 y=157  (track height = 139)
	private static final int SCROLL_X = 174, SCROLL_Y = 18;
	private static final int SCROLL_TRACK_H = 140;
	private static final int SCROLLER_W = 12, SCROLLER_H = 15;
	// Back button: 18×10
	private static final int BACK_W = 18, BACK_H = 10;

	// Phase 3 — tree viewport: x=8 y=18 → x=185 y=157  (w=177 h=139)
	private static final int P3_TREE_X = 8,   P3_TREE_Y = 18;
	private static final int P3_TREE_W = 177,  P3_TREE_H = 139;

	// Phase 3 list view — list area x=7 y=17 → x=168 y=140  (w=161 h=123)
	private static final int P3L_LIST_X = 7,  P3L_LIST_Y = 17;
	private static final int P3L_LIST_W = 161, P3L_LIST_H = 124;
	// Total cost bar: x=7 y=141 → x=168 y=158
	private static final int P3L_TOTAL_X = 7,  P3L_TOTAL_Y = 141;
	private static final int P3L_TOTAL_W = 161, P3L_TOTAL_H = 17;
	// List frame size
	private static final int LIST_FRAME_SIZE = 16;
	// Colors
	private static final int COLOR_ITEM_TEXT  = 0xFF535353;
	private static final int COLOR_COST_TEXT  = 0xFF6b6b6b;
	private static final int COLOR_TOTAL_TEXT = 0xFF3C3C3C;

	// Tree node dimensions: 26×26 frames
	private static final int NODE_SIZE = 26;
	private static final int NODE_ICON = 16;
	// Spacing — wide enough that result nodes never land on top of leaf nodes
	private static final int H_GAP = 1;   // minimum gap between node edges — tightly packed
	private static final int V_GAP = 30;  // gap between node bottom and result top

	// ── Phase state ───────────────────────────────────────────────────────────

	private enum Phase { ONE, TWO, THREE }
	private Phase phase = Phase.ONE;

	// Phase 1
	private String selectedItem = null;
	private boolean modeLevels  = true;

	// Phase 2
	private List<String> itemEnchants         = new ArrayList<>();
	private List<List<String>> incompatGroups = new ArrayList<>();
	private List<Integer> groupVisible        = new ArrayList<>();
	private final Map<String, Integer> selectedLevels = new LinkedHashMap<>();
	private int scrollOffset   = 0;
	private int totalContentH  = 0;
	private boolean scrollDragging    = false;
	private int scrollDragStartY      = 0;
	private int scrollDragStartOffset = 0;
	/** Index of the group whose level picker is currently open, or -1 if none. */
	private int expandedGroupIdx = -1;

	// Phase 3
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

	// ── Persistent state — survives anvil close/reopen ────────────────────────
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

	/** Save current instance state to static fields before the panel is destroyed. */
	public void saveState() {
		loading = false; // never persist a loading state across screen close
		s_phase        = phase;
		s_selectedItem = selectedItem;
		s_modeLevels   = modeLevels;
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

	/** Restore state from static fields into the new instance. */
	private void restoreState() {
		phase        = s_phase;
		selectedItem = s_selectedItem;
		modeLevels   = s_modeLevels;
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
		// Rebuild derived state
		if (phase == Phase.THREE && !instructions.isEmpty()) buildTreeLayout();
	}

	/**
	 * A positioned node in the tree.
	 * x, y are in tree-local space (before treeOff is applied).
	 * data holds the enchant info for tooltip/matching.
	 * isLeaf = true for initial single books/items.
	 * isFinal = true for the last result node.
	 */
	private record TreeNode(float x, float y, MergeInstruction.NodeItem data,
	                        boolean isLeaf, boolean isFinal, int instrIdx) {
		// instrIdx >= 0 means this is a result node produced by instructions[instrIdx]
		// instrIdx == -1 means this is a leaf node
	}
	private List<TreeNode> treeNodes = new ArrayList<>();

	/**
	 * One connector: from the bottom-centre of (fromIdx) to the top-centre of (toIdx).
	 */
	private record Connector(int fromIdx, int toIdx) {}
	private List<Connector> connectors = new ArrayList<>();

	private TreeNode hoveredNode = null;

	// Mouse
	private boolean wasLeftDown  = false;
	private boolean justOpened   = false;

	/** Called by the mixin to suppress click processing on the frame the panel opens. */
	public void setJustOpened(boolean val) { justOpened = val; }

	// ── Constructor ───────────────────────────────────────────────────────────

	public AeogOverlayScreen() {
		restoreState();
	}

	// ── Main entry ────────────────────────────────────────────────────────────

	/**
	 * Called every frame from AnvilScreenMixin.
	 * anvilX/anvilY are the anvil screen's actual pixel origin (GUI-scale-correct).
	 */
	public void render(DrawContext ctx, int anvilX, int anvilY,
	                   int mouseX, int mouseY, TextRenderer tr) {
		int px = anvilX - P_W - GAP;
		int py = anvilY;
		pollMouse(px, py, mouseX, mouseY);
		switch (phase) {
			case ONE   -> renderPhaseOne(ctx, px, py, mouseX, mouseY, tr);
			case TWO   -> renderPhaseTwo(ctx, px, py, mouseX, mouseY, tr);
			case THREE -> renderPhaseThree(ctx, px, py, mouseX, mouseY, tr);
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PHASE 1
	// ─────────────────────────────────────────────────────────────────────────

	private void renderPhaseOne(DrawContext ctx, int px, int py,
	                             int mx, int my, TextRenderer tr) {
		ctx.drawTexture(PIPE, P1_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		List<String> items = EnchantData.getPhase1Items();
		for (int i = 0; i < items.size(); i++) {
			int bx = px + GRID_X + (i % GRID_COLS) * BTN_SIZE;
			int by = py + GRID_Y + (i / GRID_COLS) * BTN_SIZE;
			String item     = items.get(i);
			boolean hov     = inBounds(mx, my, bx, by, BTN_SIZE, BTN_SIZE);
			boolean sel     = item.equals(selectedItem);
			ctx.drawTexture(PIPE, (hov || sel) ? BTN_ITEM_H : BTN_ITEM,
				bx, by, 0f, 0f, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
			drawIcon(ctx, item, bx + 1, by + 1);
			if (hov) ctx.drawTooltip(tr, Text.literal(item.equals("book") ? "Book" : formatName(item)), mx, my);
		}

		// Optimize toggle button
		int optX = px + OPT_X, optY = py + OPT_Y;
		boolean optHov = inBounds(mx, my, optX, optY, OPT_W, OPT_H);
		ctx.drawTexture(PIPE, optHov ? BTN_OPT_H : BTN_OPT,
			optX, optY, 0f, 0f, OPT_W, OPT_H, OPT_W, OPT_H);

		// Text drawn after textures so it renders on top
		String modeText = modeLevels ? "Least XP/Levels" : "Least Prior Work Penalty";
		int modeW = tr.getWidth(modeText);
		ctx.drawText(tr, Text.literal(modeText),
			optX + OPT_W / 2 - modeW / 2, optY + (OPT_H - 8) / 2, 0xFFFFFFFF, true);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PHASE 2
	// ─────────────────────────────────────────────────────────────────────────

	private void renderPhaseTwo(DrawContext ctx, int px, int py,
	                             int mx, int my, TextRenderer tr) {
		ctx.drawTexture(PIPE, P2_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		renderBack(ctx, px, py, mx, my);

		// Calculate button texture
		int calcX = px + CALC_X, calcY = py + CALC_Y;
		boolean hasSelection = !selectedLevels.isEmpty();
		boolean calcHov = hasSelection && inBounds(mx, my, calcX, calcY, CALC_W, CALC_H);
		Identifier calcTex = !hasSelection ? BTN_CALC_DIS : (calcHov ? BTN_CALC_H : BTN_CALC);
		ctx.drawTexture(PIPE, calcTex, calcX, calcY, 0f, 0f, CALC_W, CALC_H, CALC_W, CALC_H);

		// Scrollable list (textures + text inside)
		int lax = px + LIST_X, lay = py + LIST_Y;
		ctx.enableScissor(lax, lay, lax + LIST_W, lay + LIST_H);
		renderPhase2List(ctx, lax, lay, mx, my, tr);
		ctx.disableScissor();

		renderScrollBar(ctx, px, py);

		// Calculate button text
		String calcLabel = "Calculate";
		int calcLabelW = tr.getWidth(calcLabel);
		ctx.drawText(tr, Text.literal(calcLabel),
			calcX + CALC_W / 2 - calcLabelW / 2, calcY + (CALC_H - 8) / 2,
			hasSelection ? 0xFFFFFFFF : 0xFF555555, true);
	}

	private void renderPhase2List(DrawContext ctx, int absX, int absY,
	                               int mx, int my, TextRenderer tr) {
		int y = absY - scrollOffset;

		// ── Pass 1: draw all textures ──────────────────────────────────────────
		int yTex = y;
		for (int gi = 0; gi < incompatGroups.size(); gi++) {
			List<String> group = incompatGroups.get(gi);
			String enchant     = group.get(groupVisible.get(gi));
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
			boolean disabled   = isEnchantDisabledByTrident(enchant);
			boolean expanded   = (expandedGroupIdx == gi);

			if (expanded) {
				// Show level slots centered over where the enchant slot would be.
				// Include level 0 (unselect). Total slots = levelMax + 1.
				int totalSlots = def.levelMax() + 1;
				int totalW     = totalSlots * ROW_H;
				// Centre within the full row width (PREV_W + SLOT_W + NEXT_W = 162)
				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = absX + (rowW - totalW) / 2;
				for (int lv = 0; lv <= def.levelMax(); lv++) {
					int lvX = startX + lv * ROW_H;
					Integer sel = selectedLevels.get(enchant);
					boolean isSel = (lv > 0) && sel != null && sel == lv;
					Identifier lvTex;
					if (disabled) lvTex = LVL_SLOT_DIS;
					else if (isSel || inBounds(mx, my, lvX, yTex, ROW_H, ROW_H)) lvTex = LVL_SLOT_SEL;
					else lvTex = LVL_SLOT;
					ctx.drawTexture(PIPE, lvTex, lvX, yTex, 0f, 0f, ROW_H, ROW_H, ROW_H, ROW_H);
				}
			} else {
				// Normal row: [Prev] [Enchant Slot] [Next]
				// Only hide prev/next for THIS group if it is the expanded one (it's not, so always show)
				// Fix 3: only hide prev/next of the expanded group itself — other groups always show theirs
				if (group.size() > 1) {
					boolean ph = inBounds(mx, my, absX, yTex, PREV_W, ROW_H);
					ctx.drawTexture(PIPE, ph ? ENCH_PREV_H : ENCH_PREV,
						absX, yTex, 0f, 0f, PREV_W, ROW_H, PREV_W, ROW_H);
				}
				int slotX = absX + PREV_W;
				// Fix 4: highlight this slot on hover even while another group is expanded
				boolean slotHov = !disabled && inBounds(mx, my, slotX, yTex, SLOT_W, ROW_H);
				Identifier slotTex = disabled ? ENCH_SLOT_DIS : slotHov ? ENCH_SLOT_H : ENCH_SLOT;
				ctx.drawTexture(PIPE, slotTex, slotX, yTex, 0f, 0f, SLOT_W, ROW_H, SLOT_W, ROW_H);
				if (group.size() > 1) {
					int nx = absX + PREV_W + SLOT_W;
					boolean nh = inBounds(mx, my, nx, yTex, NEXT_W, ROW_H);
					ctx.drawTexture(PIPE, nh ? ENCH_NEXT_H : ENCH_NEXT,
						nx, yTex, 0f, 0f, NEXT_W, ROW_H, NEXT_W, ROW_H);
				}
			}
			yTex += ROW_H;
		}
		totalContentH = yTex - (absY - scrollOffset);

		// ── Pass 2: draw all text with Z offset so it appears above textures ───
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

			if (expanded) {
				// Draw level numbers (0 = deselect, 1..max = level)
				int totalSlots = def.levelMax() + 1;
				int totalW     = totalSlots * ROW_H;
				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = absX + (rowW - totalW) / 2;
				for (int lv = 0; lv <= def.levelMax(); lv++) {
					int lvX    = startX + lv * ROW_H;
					String lvStr = String.valueOf(lv); // Arabic numerals in level slots, 0 = deselect
					int lvStrW = tr.getWidth(lvStr);
					Integer sel = selectedLevels.get(enchant);
					boolean isSel = (lv > 0) && sel != null && sel == lv;
					int lvColor = disabled ? defaultColor
						: (isSel && isCurse) ? 0xFFFC5454
						: isSel ? 0xFFFFFFFF
						: defaultColor;
					ctx.drawText(tr, Text.literal(lvStr),
						lvX + ROW_H / 2 - lvStrW / 2, yTxt + (ROW_H - 8) / 2, lvColor, false);
				}
			} else {
				// Fix 2: show "Enchant Name [Roman numeral]" when a level is selected
				int slotX    = absX + PREV_W;
				int enchColor = disabled ? defaultColor
					: (enchSelected && isCurse) ? 0xFFFC5454
					: enchSelected ? 0xFFFFFFFF
					: defaultColor;
				String enchLabel;
				Integer selLevel = selectedLevels.get(enchant);
				if (!disabled && selLevel != null) {
					boolean showNumeral = def.levelMax() > 1;
					enchLabel = showNumeral
						? formatName(enchant) + " " + toRoman(selLevel)
						: formatName(enchant);
				} else {
					enchLabel = formatName(enchant);
				}
				int enchLabelW = tr.getWidth(enchLabel);
				ctx.drawText(tr, Text.literal(enchLabel),
					slotX + SLOT_W / 2 - enchLabelW / 2, yTxt + (ROW_H - 8) / 2, enchColor, false);
			}
			yTxt += ROW_H;
		}
	}

	private void renderScrollBar(DrawContext ctx, int px, int py) {
		if (totalContentH <= LIST_H) return;
		int trackAbsX = px + SCROLL_X;
		int trackAbsY = py + SCROLL_Y;
		float frac    = (float) scrollOffset / Math.max(1, totalContentH - LIST_H);
		int thumbY    = trackAbsY + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
		ctx.drawTexture(PIPE, SCROLLER,
			trackAbsX, thumbY, 0f, 0f, SCROLLER_W, SCROLLER_H, SCROLLER_W, SCROLLER_H);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PHASE 3
	// ─────────────────────────────────────────────────────────────────────────

	private void renderPhaseThree(DrawContext ctx, int px, int py,
	                               int mx, int my, TextRenderer tr) {
		if (com.armaninyow.dibs.config.AeogConfig.listViewPhase3) {
			renderPhaseThreeList(ctx, px, py, mx, my, tr);
		} else {
			renderPhaseThreeTree(ctx, px, py, mx, my, tr);
		}
	}

	private void renderPhaseThreeTree(DrawContext ctx, int px, int py,
	                                   int mx, int my, TextRenderer tr) {
		int treeAbsX = px + P3_TREE_X;
		int treeAbsY = py + P3_TREE_Y;

		// Draw tree content FIRST (behind container)
		ctx.enableScissor(treeAbsX, treeAbsY, treeAbsX + P3_TREE_W, treeAbsY + P3_TREE_H);
		renderTiledBg(ctx, treeAbsX, treeAbsY);
		renderConnectors(ctx, treeAbsX, treeAbsY);

		hoveredNode = null;
		for (TreeNode node : treeNodes) {
			int nx = treeAbsX + (int)(node.x() + treeOffX);
			int ny = treeAbsY + (int)(node.y() + treeOffY);
			ItemStack match = findMatchingStack(node.data(), node);
			boolean obtained = match != null;
			Identifier frame = frameFor(node, obtained);
			ctx.drawTexture(PIPE, frame, nx, ny, 0f, 0f, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);

			if (obtained && match != null && !match.isEmpty()) {
				ctx.drawItem(match, nx + 5, ny + 5);
			} else {
				drawIcon(ctx, node.data().id(), nx + 5, ny + 5);
			}

			if (inBounds(mx, my, nx, ny, NODE_SIZE, NODE_SIZE)) hoveredNode = node;
		}
		ctx.disableScissor();

		// Draw container LAST so it renders on top of tree content
		ctx.drawTexture(PIPE, P3_CONTAINER, px, py, 0f, 0f, P_W, P_H, P_W, P_H);

		// Tooltip and back button drawn after container
		if (hoveredNode != null) {
			renderNodeTooltip(ctx, hoveredNode, mx, my, tr);
		}
		renderBack(ctx, px, py, mx, my);

		// Loading overlay — shown while waiting for the engine result
		if (loading) {
			renderLoadingOverlay(ctx, px, py, tr);
		}
	}

	// ── Phase 3 list view ─────────────────────────────────────────────────────

	private void renderPhaseThreeList(DrawContext ctx, int px, int py,
	                                   int mx, int my, TextRenderer tr) {
		// Container
		ctx.drawTexture(PIPE, P3_CONTAINER_2, px, py, 0f, 0f, P_W, P_H, P_W, P_H);
		renderBack(ctx, px, py, mx, my);

		if (loading) {
			renderListLoadingOverlay(ctx, px, py, tr);
			return;
		}

		int listAbsX = px + P3L_LIST_X;
		int listAbsY = py + P3L_LIST_Y;

		// Build row layout (needed for scroll height and rendering)
		List<ListRow> rows = buildListRows(tr);
		listTotalH = rows.stream().mapToInt(r -> r.height).sum();
		int maxScroll = Math.max(0, listTotalH - P3L_LIST_H);
		listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset));

		// Scrollable content — scissor clips text/textures, visibility check skips drawItem
		int clipTop    = listAbsY;
		int clipBottom = listAbsY + P3L_LIST_H;
		ctx.enableScissor(listAbsX, clipTop, listAbsX + P3L_LIST_W, clipBottom);
		renderListRows(ctx, tr, rows, listAbsX, listAbsY - listScrollOffset, clipTop, clipBottom);
		ctx.disableScissor();

		// Scroll bar
		renderListScrollBar(ctx, px, py, maxScroll);

		// Pinned total cost bar
		int totalCost = instructions.stream().mapToInt(MergeInstruction::mergeCost).sum();
		String totalStr = "Total cost: " + totalCost + " levels";
		int totalStrW = tr.getWidth(totalStr);
		int totalBarMidX = px + P3L_TOTAL_X + P3L_TOTAL_W / 2;
		int totalBarMidY = py + P3L_TOTAL_Y + P3L_TOTAL_H / 2 - 1;
		ctx.drawText(tr, Text.literal(totalStr), totalBarMidX - totalStrW / 2, totalBarMidY, COLOR_TOTAL_TEXT, false);
	}

	private void renderListLoadingOverlay(DrawContext ctx, int px, int py, TextRenderer tr) {
		// Center inside the scrollable list area: x=7 y=17 w=161 h=123
		int cx = px + P3L_LIST_X + P3L_LIST_W / 2;
		int cy = py + P3L_LIST_Y + P3L_LIST_H / 2;

		float progress = (float)(System.currentTimeMillis() - loadingStartMs) / expectedMs();
		progress = Math.min(progress, 0.9f);

		String line1 = "Calculating...";
		String line2 = "Please keep the anvil open.";
		int line1W = tr.getWidth(line1);
		int line2W = tr.getWidth(line2);

		int totalH = 8 + 2 + 8 + 4 + LOADING_BAR_H;
		int line1Y = cy - totalH / 2;
		int line2Y = line1Y + 8 + 2;
		int barY   = line2Y + 8 + 4;
		int barX   = cx - LOADING_BAR_W / 2;

		ctx.drawText(tr, net.minecraft.text.Text.literal(line1),
			cx - line1W / 2 + 1, line1Y + 1, 0xFF3F3F3F, false);
		ctx.drawText(tr, net.minecraft.text.Text.literal(line1),
			cx - line1W / 2, line1Y, 0xFFFFFFFF, false);
		ctx.drawText(tr, net.minecraft.text.Text.literal(line2),
			cx - line2W / 2 + 1, line2Y + 1, 0xFF3F3F3F, false);
		ctx.drawText(tr, net.minecraft.text.Text.literal(line2),
			cx - line2W / 2, line2Y, 0xFFFFFFFF, false);
		ctx.fill(barX, barY, barX + LOADING_BAR_W, barY + LOADING_BAR_H, 0xFF000000);
		int fillW = (int)(LOADING_BAR_W * progress);
		if (fillW > 0)
			ctx.fill(barX, barY, barX + fillW, barY + LOADING_BAR_H, 0xFF00FF00);
	}

	/** One rendered "row" in the list view. */
	private record ListRow(int stepNumber, MergeInstruction instr, int height,
	                       List<String> enchLines, // wrapped enchant description lines (below icons)
	                       List<String> costLines, // wrapped cost/PWP lines
	                       boolean leftObtained, boolean rightObtained) {}

	private List<ListRow> buildListRows(TextRenderer tr) {
		List<ListRow> rows = new ArrayList<>();
		int lineH = tr.fontHeight; // no gap between text lines
		int iconRowH = Math.max(LIST_FRAME_SIZE, tr.fontHeight);
		// Icon row is always: [frame16][leftIcon16][" + "][rightIcon16]
		// ALL enchant text goes below the icon row, indented by LIST_FRAME_SIZE.
		// This avoids any overflow from text trying to fit beside the icons.
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

	/** Wraps text into lines. firstLineWidth is the usable width for the first segment,
	 *  contWidth for continuation lines. Returns list of segments (never empty). */
	private List<String> wrapText(TextRenderer tr, String text, int firstLineWidth, int contWidth) {
		List<String> result = new ArrayList<>();
		if (text.isEmpty()) { result.add(""); return result; }
		// Guard: if available width is too small to fit even one character, treat as contWidth
		if (firstLineWidth < tr.getWidth("W")) firstLineWidth = contWidth;
		String remaining = text;
		int availW = firstLineWidth;
		while (!remaining.isEmpty()) {
			if (tr.getWidth(remaining) <= availW) {
				result.add(remaining);
				break;
			}
			// Walk down from full length until the prefix strictly fits
			// Start at length-1 since we already know the full string doesn't fit
			int cut = remaining.length() - 1;
			while (cut > 0 && tr.getWidth(remaining.substring(0, cut)) > availW) cut--;
			// cut is now the longest prefix that fits, but may still be 0 if nothing fits
			// Try to break at the last space at or before cut
			int space = (cut > 0) ? remaining.lastIndexOf(' ', cut - 1) : -1;
			if (space >= 0) {
				result.add(remaining.substring(0, space).stripTrailing());
				remaining = remaining.substring(space).stripLeading();
			} else {
				// No space — hard cut; ensure at least 1 char to avoid infinite loop
				int hardCut = Math.max(1, cut);
				result.add(remaining.substring(0, hardCut));
				remaining = remaining.substring(hardCut);
			}
			availW = contWidth;
		}
		if (result.isEmpty()) result.add("");
		return result;
	}

	/** Returns a comma-separated enchant summary for a NodeItem, e.g. "Fortune III, Efficiency V". */
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
			boolean showLevel = def == null || def.levelMax() > 1;
			sb.append(formatName(e[0]));
			if (showLevel) sb.append(" ").append(toRoman(lvl));
		}
		return sb.toString();
	}

	private void renderListRows(DrawContext ctx, TextRenderer tr,
	                             List<ListRow> rows, int absX, int startY,
	                             int clipTop, int clipBottom) {
		int lineH = tr.fontHeight; // no gap between text lines
		int iconRowH = Math.max(LIST_FRAME_SIZE, tr.fontHeight);
		int plusW = tr.getWidth(" + ");
		int y = startY;
		for (ListRow row : rows) {
			int rowBottom = y + row.height;
			if (rowBottom < clipTop) { y = rowBottom; continue; }
			if (y >= clipBottom)     { break; }

			// ── Icon row ──────────────────────────────────────────────────────
			boolean iconRowVisible = y < clipBottom && (y + iconRowH) > clipTop;
			if (iconRowVisible) {
				int x = absX;
				int textY = y + iconRowH / 2 - tr.fontHeight / 2;
				boolean obtained = row.leftObtained && row.rightObtained;
				ctx.drawTexture(PIPE, obtained ? LIST_FRAME_O : LIST_FRAME_U,
					x, y, 0f, 0f, LIST_FRAME_SIZE, LIST_FRAME_SIZE, LIST_FRAME_SIZE, LIST_FRAME_SIZE);
				String numStr = String.valueOf(row.stepNumber);
				int numW = tr.getWidth(numStr);
				ctx.drawText(tr, Text.literal(numStr),
					x + LIST_FRAME_SIZE / 2 - numW / 2, y + LIST_FRAME_SIZE / 2 - tr.fontHeight / 2,
					obtained ? 0xFFFFFFFF : 0xFF6b6b6b, false);
				x += LIST_FRAME_SIZE;
				if (y >= clipTop && (y + LIST_FRAME_SIZE) <= clipBottom)
					drawListIcon(ctx, tr, row.instr.left(), row.leftObtained, x, y);
				x += LIST_FRAME_SIZE;
				ctx.drawText(tr, Text.literal(" + "), x, textY, COLOR_ITEM_TEXT, false);
				x += plusW;
				if (y >= clipTop && (y + LIST_FRAME_SIZE) <= clipBottom)
					drawListIcon(ctx, tr, row.instr.right(), row.rightObtained, x, y);
			}

			y += iconRowH + 1; // past icon row + 1px gap

			// ── Enchant description lines (indented, below icons) ─────────────
			for (String seg : row.enchLines) {
				if (y >= clipTop && y < clipBottom && !seg.isEmpty())
					ctx.drawText(tr, Text.literal(seg), absX + LIST_FRAME_SIZE, y, COLOR_ITEM_TEXT, false);
				y += lineH;
			}

			// ── Cost/PWP lines (indented) ─────────────────────────────────────
			for (String seg : row.costLines) {
				if (y >= clipTop && y < clipBottom && !seg.isEmpty())
					ctx.drawText(tr, Text.literal(seg), absX + LIST_FRAME_SIZE, y, COLOR_COST_TEXT, false);
				y += lineH;
			}

			y += 3; // gap between rows
		}
	}

	/** Draws a 16x16 item icon in list view: vanilla item if obtained, mod PNG if not. */
	private void drawListIcon(DrawContext ctx, TextRenderer tr,
	                           MergeInstruction.NodeItem node, boolean obtained, int x, int y) {
		if (obtained) {
			ItemStack match = findMatchingStack(node, null);
			if (match != null && !match.isEmpty()) {
				ctx.drawItem(match, x, y);
				return;
			}
		}
		drawIcon(ctx, node.id(), x, y);
	}

	private void renderListScrollBar(DrawContext ctx, int px, int py, int maxScroll) {
		if (maxScroll <= 0) return;
		int trackAbsX = px + SCROLL_X;
		int trackAbsY = py + SCROLL_Y;
		float frac    = (float) listScrollOffset / Math.max(1, maxScroll);
		int thumbY    = trackAbsY + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
		ctx.drawTexture(PIPE, SCROLLER,
			trackAbsX, thumbY, 0f, 0f, SCROLLER_W, SCROLLER_H, SCROLLER_W, SCROLLER_H);
	}

	private static final int LOADING_BAR_W = 150;
	private static final int LOADING_BAR_H = 2;

	/** Expected calculation time in ms: 50 * 4^(n-9) + 1000, for n >= 10. */
	private long expectedMs() {
		int n = selectedLevels.size();
		return (long)(50 * Math.pow(4.0, n - 9)) + 1000;
	}

	private void renderLoadingOverlay(DrawContext ctx, int px, int py, TextRenderer tr) {
		int vpCX = px + P3_TREE_X + P3_TREE_W / 2;
		int vpCY = py + P3_TREE_Y + P3_TREE_H / 2;

		// Progress: clamp to 0.9 so bar never falsely completes before result arrives
		float progress = (float)(System.currentTimeMillis() - loadingStartMs) / expectedMs();
		progress = Math.min(progress, 0.9f);

		String line1 = "Calculating...";
		String line2 = "Please keep the anvil open.";
		int line1W = tr.getWidth(line1);
		int line2W = tr.getWidth(line2);

		// Layout: line1 (8px) + 2px gap + line2 (8px) + 4px gap + bar (2px)
		int totalH = 8 + 2 + 8 + 4 + LOADING_BAR_H;
		int line1Y = vpCY - totalH / 2;
		int line2Y = line1Y + 8 + 2;
		int barY   = line2Y + 8 + 4;
		int barX   = vpCX - LOADING_BAR_W / 2;

		// Line 1 shadow + text
		ctx.drawText(tr, net.minecraft.text.Text.literal(line1),
			vpCX - line1W / 2 + 1, line1Y + 1, 0xFF3F3F3F, false);
		ctx.drawText(tr, net.minecraft.text.Text.literal(line1),
			vpCX - line1W / 2, line1Y, 0xFFFFFFFF, false);

		// Line 2 shadow + text
		ctx.drawText(tr, net.minecraft.text.Text.literal(line2),
			vpCX - line2W / 2 + 1, line2Y + 1, 0xFF3F3F3F, false);
		ctx.drawText(tr, net.minecraft.text.Text.literal(line2),
			vpCX - line2W / 2, line2Y, 0xFFFFFFFF, false);

		// Bar background (black)
		ctx.fill(barX, barY, barX + LOADING_BAR_W, barY + LOADING_BAR_H, 0xFF000000);
		// Bar fill (green)
		int fillW = (int)(LOADING_BAR_W * progress);
		if (fillW > 0)
			ctx.fill(barX, barY, barX + fillW, barY + LOADING_BAR_H, 0xFF00FF00);
	}

	private void renderTiledBg(DrawContext ctx, int x, int y) {
		int bgW = 16, bgH = 16;
		int offX = ((int)treeOffX % bgW + bgW) % bgW;
		int offY = ((int)treeOffY % bgH + bgH) % bgH;
		for (int ty = -bgH + offY; ty < P3_TREE_H; ty += bgH)
			for (int tx = -bgW + offX; tx < P3_TREE_W; tx += bgW)
				ctx.drawTexture(PIPE, BACKGROUND, x + tx, y + ty, 0f, 0f, bgW, bgH, bgW, bgH);
	}

	private void renderConnectors(DrawContext ctx, int treeAbsX, int treeAbsY) {
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
				// Vertical from src bottom down to junctionY
				ctx.fill(srcX, srcBot, srcX + 1, junctionY, 0xFFFFFFFF);
				minSrcX = Math.min(minSrcX, srcX);
				maxSrcX = Math.max(maxSrcX, srcX);
			}

			// Horizontal bar at junctionY (only if two sources)
			if (srcIdxs.size() > 1) {
				ctx.fill(minSrcX, junctionY, maxSrcX + 1, junctionY + 1, 0xFFFFFFFF);
			}
			// Vertical from junctionY down to result top
			ctx.fill(destX, junctionY, destX + 1, destY, 0xFFFFFFFF);
		}
	}

	private Identifier frameFor(TreeNode n, boolean obtained) {
		if (n.isFinal()) return obtained ? FRAME_FINAL_O : FRAME_FINAL_U;
		if (n.isLeaf())  return obtained ? FRAME_INIT_O  : FRAME_INIT_U;
		return                 obtained ? FRAME_PROG_O  : FRAME_PROG_U;
	}

	// ── Tree layout ───────────────────────────────────────────────────────────

	/**
	 * Option 2 layout: all leaf nodes in row 0, results at their production rows,
	 * long straight connectors. No viewport width cap — the tree is as wide as needed
	 * and the player drags to explore.
	 *
	 * Layout rules:
	 *  - Leaves (work==0) → row 0, spread with H_GAP spacing
	 *  - Each instruction produces a result node at a specific row
	 *  - A "book pair" step (both inputs work==0, neither is the item) produces a result
	 *    that sits at the same row as the chain step that consumes it as right input
	 *  - A "chain" step consumes the previous chain result as left input; its result is
	 *    one row below
	 *  - Result X = midpoint between its two inputs' centres
	 *  - Connectors: straight vertical from each input bottom to junction, horizontal bar,
	 *    then vertical down to result top
	 */
	private void buildTreeLayout() {
		treeNodes.clear();
		connectors.clear();
		if (instructions.isEmpty()) return;

		int steps = instructions.size();
		float rowH = NODE_SIZE + V_GAP;

		// ── The engine always produces instructions in this pattern: ──────────
		// - The very first instruction merges the base item with the most-expensive book.
		//   Its LEFT input is the base item (work=0, id=selectedItem or book).
		// - Each subsequent instruction is EITHER:
		//   (a) a book-pair merge (both inputs work=0, neither is the item), OR
		//   (b) a chain-merge (left input is a previous chain result, work>0).
		// - The results of book-pairs feed into chain-merges as RIGHT inputs.
		// - The results of chain-merges feed into the next chain-merge as LEFT inputs.
		//
		// We classify by: chain step = left.work > 0, OR (left.work==0 AND left is the item).
		// Everything else is a book pair.

		// ── Identify the item step and classify chain vs book-pair steps ──────
		// The "item step" is the one whose left or right input is the actual item
		// (work==0, id==selectedItem or "item"). It may be any step, not necessarily
		// step 0 — the engine builds the full book tree first when there are many
		// enchants, then merges the item in near the end.
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

		// ── Assign Y rows ─────────────────────────────────────────────────────
		// Each chain step's result goes one row lower than the previous chain result.
		// The chain step at index i has result row = (chain sequence number).
		// A book-pair result sits at the SAME row as the chain result it feeds into
		// as the right input — meaning it is at the same Y level as the chain step's
		// two inputs (one row above the chain result).
		// So: book-pair result row = (chain seq of the chain step that uses it) - 1 + 1
		//   = same as the chain step's result row - 0... let me be precise:
		//
		// chain step 0 result → row 1
		// chain step 1 result → row 2  (book pair feeding it sits at row 1)
		// chain step 2 result → row 3  (book pair feeding it sits at row 2)
		// ...
		// The book pair result and the previous chain result are BOTH at the "input row"
		// of the next chain step, which equals the chain step's result row minus 1.
		// But we want them at the same ROW as each other. Since the previous chain result
		// is already placed at chain_seq - 1, the book pair result should also be at chain_seq - 1.

		// ── Assign Y rows ─────────────────────────────────────────────────────
		// Build an explicit map: for each step i, which step consumes its result
		// (consumedBy[i]), and which result node is its left/right input
		// (leftSrc[i], rightSrc[i] = step index, or -1 for leaf).
		// Then propagate top-down: final step gets the highest row, and each step's
		// row = its consumer's row - 1. Both inputs to any step always land on the
		// same row by construction.

		// Step 1: resolve which prior step produced each non-leaf input.
		// Use a claimed[] array so each result is only matched once.
		int[] leftSrc  = new int[steps]; // step index that produced left input, or -1
		int[] rightSrc = new int[steps];
		Arrays.fill(leftSrc,  -1);
		Arrays.fill(rightSrc, -1);
		boolean[] claimed = new boolean[steps];

		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work() > 0) {
				// Find most recent unclaimed prior step whose result work matches
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

		// Step 2: build consumedBy[] — which step i is an input to
		int[] consumedBy = new int[steps];
		Arrays.fill(consumedBy, -1);
		for (int i = 0; i < steps; i++) {
			if (leftSrc[i]  >= 0) consumedBy[leftSrc[i]]  = i;
			if (rightSrc[i] >= 0) consumedBy[rightSrc[i]] = i;
		}

		// Step 3: assign rows top-down.
		// Final step (steps-1) gets row = steps. Walk each step's inputs and set
		// their row = this step's row - 1.
		int[] resultRow = new int[steps];
		resultRow[steps - 1] = steps;
		for (int i = steps - 1; i >= 0; i--) {
			int inputRow = resultRow[i] - 1;
			if (inputRow < 1) inputRow = 1;
			if (leftSrc[i]  >= 0 && resultRow[leftSrc[i]]  < inputRow) resultRow[leftSrc[i]]  = inputRow;
			if (rightSrc[i] >= 0 && resultRow[rightSrc[i]] < inputRow) resultRow[rightSrc[i]] = inputRow;
		}
		// Any steps not yet assigned (disconnected leaf-only steps) get row 1
		for (int i = 0; i < steps; i++) if (resultRow[i] == 0) resultRow[i] = 1;

		// Step 4: normalise — compress to contiguous rows starting from 1
		int[] sorted = resultRow.clone();
		Arrays.sort(sorted);
		Map<Integer, Integer> rowRemap = new LinkedHashMap<>();
		int seq = 0;
		for (int r : sorted) if (!rowRemap.containsKey(r)) rowRemap.put(r, ++seq);
		for (int i = 0; i < steps; i++) resultRow[i] = rowRemap.get(resultRow[i]);

		// ── Collect and order leaf nodes ──────────────────────────────────────
		// The item leaf always goes first (leftmost). Then all book-pair groups
		// follow, each pair's two leaves adjacent, sorted shallowest first.

		// ── Collect and order leaf nodes ──────────────────────────────────────
		// Sort ALL steps that have leaf inputs by resultRow, then emit their leaf
		// keys in that order — item step always first. This handles chain steps
		// with a leaf input (like step 4 above with R4) just as well as pure
		// book-pair steps, since we no longer filter by isChain.
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
		// Item step always first
		if (itemStepIdx >= 0) {
			String itemKey = itemIsLeft ? "L" + itemStepIdx : "R" + itemStepIdx;
			String pairKey = itemIsLeft ? "R" + itemStepIdx : "L" + itemStepIdx;
			MergeInstruction itemIns = instructions.get(itemStepIdx);
			if ((itemIsLeft  ? itemIns.left()  : itemIns.right()).work() == 0) leafKeyOrder.add(itemKey);
			if ((itemIsLeft  ? itemIns.right() : itemIns.left()).work()  == 0) leafKeyOrder.add(pairKey);
		}
		// All other steps with leaves, sorted by resultRow
		for (int s : stepsWithLeaves) {
			if (s == itemStepIdx) continue;
			MergeInstruction ins = instructions.get(s);
			if (ins.left().work()  == 0 && !leafKeyOrder.contains("L" + s)) leafKeyOrder.add("L" + s);
			if (ins.right().work() == 0 && !leafKeyOrder.contains("R" + s)) leafKeyOrder.add("R" + s);
		}
		// Safety fallback
		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			if (ins.left().work()  == 0 && !leafKeyOrder.contains("L" + i)) leafKeyOrder.add("L" + i);
			if (ins.right().work() == 0 && !leafKeyOrder.contains("R" + i)) leafKeyOrder.add("R" + i);
		}

		// Build the actual leaf list and leafNodeIdx map in display order.
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

		// ── Place leaf nodes at row 0 with group gaps ────────────────────────────
		// Add an extra gap between the sword+first-book pair and each subsequent book pair.
		float leafSpacing = NODE_SIZE + H_GAP;
		float groupGap = 0; // no extra gap between groups — same spacing throughout

		// Compute X positions per leaf key, inserting group gaps
		Map<String, Float> leafX = new LinkedHashMap<>();
		float xCursor = 0;
		for (int i = 0; i < leafKeyOrder.size(); i++) {
			String key = leafKeyOrder.get(i);
			if (!keyToLeaf.containsKey(key)) continue;
			// Insert a group gap before the start of each book-pair group (every 2 leaves after pos 1)
			// Group 0: indices 0,1 (sword+looting)
			// Group 1: indices 2,3 (first book pair)
			// Group 2: indices 4,5 (second book pair)
			// ...
			if (i > 0 && i % 2 == 0) xCursor += groupGap;
			leafX.put(key, xCursor);
			xCursor += leafSpacing;
		}

		// Centre the whole leaf row around 0
		float leafTotalW = xCursor - (NODE_SIZE + H_GAP); // remove trailing slot
		float leafStartX = -leafTotalW / 2.0f;

		for (int i = 0; i < leafKeyOrder.size(); i++) {
			String key = leafKeyOrder.get(i);
			if (!keyToLeaf.containsKey(key)) continue;
			int idx = treeNodes.size();
			float x = leafStartX + leafX.get(key);
			treeNodes.add(new TreeNode(x, 0, keyToLeaf.get(key), true, false, -1));
			leafNodeIdx.put(key, idx);
		}

		// ── Add result nodes in instruction order ─────────────────────────────
		int[] instrResultNodeIdx = new int[steps];
		Arrays.fill(instrResultNodeIdx, -1);

		for (int i = 0; i < steps; i++) {
			MergeInstruction ins = instructions.get(i);
			boolean isFinal = (i == steps - 1);

			// Resolve left input: if it came from a prior result, use leftSrc[i];
			// otherwise it's a leaf, look up by positional key.
			int lIdx;
			if (leftSrc[i] >= 0) {
				lIdx = instrResultNodeIdx[leftSrc[i]];
			} else {
				lIdx = leafNodeIdx.getOrDefault("L" + i, -1);
			}

			// Resolve right input similarly
			int rIdx;
			if (rightSrc[i] >= 0) {
				rIdx = instrResultNodeIdx[rightSrc[i]];
			} else {
				rIdx = leafNodeIdx.getOrDefault("R" + i, -1);
			}

			// X = midpoint of left and right input centres
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
		}

		// ── Separate overlapping nodes at the same Y row ──────────────────────
		// When many enchants are selected, multiple result nodes at the same row can
		// end up with X midpoints close enough that their 26px frames overlap. This
		// post-pass iteratively pushes apart any overlapping same-row pair (splitting
		// the overlap equally left and right), then re-centres each parent result node
		// over its (now-shifted) children. Repeats up to 10 times until stable.
		float minSep = NODE_SIZE + H_GAP;
		for (int pass = 0; pass < 10; pass++) {
			float[] xs = new float[treeNodes.size()];
			for (int i = 0; i < treeNodes.size(); i++) xs[i] = treeNodes.get(i).x();

			// Group node indices by Y row
			Map<Integer, List<Integer>> byRow = new LinkedHashMap<>();
			for (int i = 0; i < treeNodes.size(); i++) {
				int row = Math.round(treeNodes.get(i).y() / rowH);
				byRow.computeIfAbsent(row, k -> new ArrayList<>()).add(i);
			}

			boolean anyChanged = false;
			// Push overlapping pairs apart within each row
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

			// Re-centre each result node over its (possibly shifted) children
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

			// Write updated X values back
			List<TreeNode> updated = new ArrayList<>();
			for (int i = 0; i < treeNodes.size(); i++) {
				TreeNode n = treeNodes.get(i);
				updated.add(new TreeNode(xs[i], n.y(), n.data(), n.isLeaf(), n.isFinal(), n.instrIdx()));
			}
			treeNodes = updated;

			if (!anyChanged) break;
		}

		// ── Centre horizontally around 0, pad top ─────────────────────────────
		float minX = treeNodes.stream().map(TreeNode::x).min(Float::compareTo).orElse(0f);
		float maxX = treeNodes.stream().map(n -> n.x() + NODE_SIZE).max(Float::compareTo).orElse(0f);
		float shiftX = -(minX + (maxX - minX) / 2.0f);
		float shiftY = V_GAP;

		List<TreeNode> shifted = new ArrayList<>();
		for (TreeNode n : treeNodes)
			shifted.add(new TreeNode(n.x() + shiftX, n.y() + shiftY, n.data(), n.isLeaf(), n.isFinal(), n.instrIdx()));
		treeNodes = shifted;

		// Initial pan: centre the tree horizontally in the viewport
		treeOffX = P3_TREE_W / 2.0f - NODE_SIZE / 2.0f;
		treeOffY = 0;
	}

	/**
	 * Resolves an input node to its treeNode index.
	 * For work>0 inputs: search previous results by work value.
	 * usedResults tracks which result indices have already been claimed for this step.
	 */
	private int resolveInputIdx(MergeInstruction.NodeItem input, int stepIdx,
	                             boolean isLeft,
	                             int[] instrResultNodeIdx,
	                             Map<String, Integer> leafNodeIdx,
	                             Set<Integer> usedResults) {
		if (input.work() > 0) {
			// LEFT input: pick the earliest produced result with matching work (not yet used)
			// RIGHT input: pick the most recent produced result with matching work (not yet used)
			// This matches instruction ordering: the left chain comes from earlier steps.
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
			// Fallback: any unused result
			for (int prev = 0; prev < stepIdx; prev++) {
				if (instrResultNodeIdx[prev] >= 0 && !usedResults.contains(instrResultNodeIdx[prev])) {
					usedResults.add(instrResultNodeIdx[prev]);
					return instrResultNodeIdx[prev];
				}
			}
			return -1;
		}
		// work == 0: leaf — use positional key
		String key = (isLeft ? "L" : "R") + stepIdx;
		return leafNodeIdx.getOrDefault(key, -1);
	}

	/** Checks if two NodeItems carry the same enchantments (used for result matching). */
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

	/** Unique key for a node based on its id and enchant list. */
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

	// ── Node tooltip ──────────────────────────────────────────────────────────

	private void renderNodeTooltip(DrawContext ctx, TreeNode node, int mx, int my, TextRenderer tr) {
		List<Text> lines = new ArrayList<>();

		// Colors per spec
		Style nameStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0x54FCFC));
		Style enchStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0xA8A8A8));
		Style curseStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFC5454));
		Style infoStyle  = Style.EMPTY.withColor(TextColor.fromRgb(0x545454));

		List<String[]> enchants = collectEnchantsfromNode(node);

		// Item name
		if (!enchants.isEmpty()) {
			lines.add(Text.literal(formatName(node.data().id())).setStyle(nameStyle));
		} else {
			lines.add(Text.literal(formatName(node.data().id())));
		}

		for (String[] e : enchants) {
			int lvl;
			try { lvl = Integer.parseInt(e[1]); } catch (NumberFormatException ex) { lvl = 1; }
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(e[0]);
			boolean showLevel = def == null || def.levelMax() > 1;
			String label = showLevel ? formatName(e[0]) + " " + toRoman(lvl) : formatName(e[0]);
			boolean isCurse = e[0].equals("binding_curse") || e[0].equals("vanishing_curse");
			lines.add(Text.literal(label).setStyle(isCurse ? curseStyle : enchStyle));
		}

		if (node.instrIdx() >= 0) {
			MergeInstruction instr = instructions.get(node.instrIdx());
			lines.add(Text.literal("Merge cost: " + instr.mergeCost() + " levels").setStyle(infoStyle));
			lines.add(Text.literal("Prior work penalty: " + instr.priorWorkPenalty() + " levels").setStyle(infoStyle));
			if (node.isFinal()) {
				int total = instructions.stream().mapToInt(MergeInstruction::mergeCost).sum();
				lines.add(Text.literal("Total cost: " + total + " levels").setStyle(infoStyle));
			}
		} else if (node.isLeaf()) {
			int pwp = (1 << node.data().work()) - 1;
			if (pwp > 0) lines.add(Text.literal("Prior work penalty: " + pwp + " levels").setStyle(infoStyle));
		}

		ctx.drawTooltip(tr, lines, mx, my);
	}

	/**
	 * Walks the connector tree backwards from a node to collect all enchants.
	 * Leaf nodes have their single enchant populated by the engine.
	 * For result nodes, we recurse into their source nodes via connectors.
	 * This bypasses the empty enchant list problem on intermediate nodes.
	 */
	private List<String[]> collectEnchantsfromNode(TreeNode node) {
		// If node has enchants directly, use them
		if (!node.data().enchants().isEmpty()) {
			return node.data().enchants();
		}

		// Leaf node with enchant name as id (e.g. id="sharpness", enchants=[])
		// This happens when the engine sends enchant name as id but empty list
		if (node.isLeaf() && EnchantData.ENCHANTS.containsKey(node.data().id())) {
			// The id IS the enchant name — synthesize the enchant entry
			// Level must come from somewhere: use work=0 means level is in the leaf's level field
			// For a single-enchant book: level = node.data().level() / weight
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(node.data().id());
			int weight = def.weight();
			int lvl = weight > 0 ? node.data().level() / weight : 1;
			if (lvl <= 0) lvl = 1;
			return List.<String[]>of(new String[]{node.data().id(), String.valueOf(lvl)});
		}

		// Find this node's index in treeNodes
		int nodeIdx = treeNodes.indexOf(node);
		if (nodeIdx < 0) return List.of();

		// Recurse into source nodes via connectors
		List<String[]> result = new ArrayList<>();
		for (Connector c : connectors) {
			if (c.toIdx() == nodeIdx) {
				TreeNode src = treeNodes.get(c.fromIdx());
				result.addAll(collectEnchantsfromNode(src));
			}
		}
		return result;
	}

	/** Converts an integer to Roman numerals (1–10). */
	private static String toRoman(int n) {
		return switch (n) {
			case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
			case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
			case 9 -> "IX"; case 10 -> "X"; default -> String.valueOf(n);
		};
	}

	// ─────────────────────────────────────────────────────────────────────────
	// SHARED HELPER — back button
	// ─────────────────────────────────────────────────────────────────────────

	private void renderBack(DrawContext ctx, int px, int py, int mx, int my) {
		int bx = px + 7, by = py + 5;
		boolean hov = inBounds(mx, my, bx, by, BACK_W, BACK_H);
		ctx.drawTexture(PIPE, hov ? BTN_BACK_H : BTN_BACK,
			bx, by, 0f, 0f, BACK_W, BACK_H, BACK_W, BACK_H);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// MOUSE INPUT
	// ─────────────────────────────────────────────────────────────────────────

	private void pollMouse(int px, int py, int mx, int my) {
		boolean down = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
			MinecraftClient.getInstance().getWindow().getHandle(),
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
				// Map drag pixel distance to scroll offset, then snap to ROW_H increments
				float frac    = (float)(my - scrollDragStartY) / Math.max(1, SCROLL_TRACK_H - SCROLLER_H);
				int raw       = scrollDragStartOffset + (int)(frac * maxScroll);
				// Snap to nearest ROW_H increment
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

		// Consume scroll accumulated by HandledScreenScrollMixin via AeogScrollState
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
		if (inBounds(mx, my, px + OPT_X, py + OPT_Y, OPT_W, OPT_H)) {
			modeLevels = !modeLevels;
			playClick();
		}
	}

	private void clickPhase2(int px, int py, int mx, int my) {
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
		// Scroll thumb drag
		if (totalContentH > LIST_H) {
			int tAx = px + SCROLL_X, tAy = py + SCROLL_Y;
			float frac = (float) scrollOffset / Math.max(1, totalContentH - LIST_H);
			int thumbY = tAy + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
			if (inBounds(mx, my, tAx, thumbY, SCROLLER_W, SCROLLER_H)) {
				scrollDragging = true; scrollDragStartY = my; scrollDragStartOffset = scrollOffset;
				return;
			}
		}
		int lax = px + LIST_X, lay = py + LIST_Y;
		int y = lay - scrollOffset;
		for (int gi = 0; gi < incompatGroups.size(); gi++) {
			List<String> group = incompatGroups.get(gi);
			int vi = groupVisible.get(gi);
			String enchant = group.get(vi);
			boolean disabled = isEnchantDisabledByTrident(enchant);
			boolean expanded = (expandedGroupIdx == gi);
			EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);

			if (expanded) {
				// Click on a level slot (0 = deselect, 1..max = select)
				int totalSlots = def.levelMax() + 1;
				int totalW     = totalSlots * ROW_H;
				int rowW       = PREV_W + SLOT_W + NEXT_W;
				int startX     = lax + (rowW - totalW) / 2;
				for (int lv = 0; lv <= def.levelMax(); lv++) {
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
				// Fix 5: click was outside this group's level slots — collapse and fall through
				// so the click can register on whatever row the user actually clicked.
				expandedGroupIdx = -1;
				// Don't return — continue iterating so the click lands on the correct row below.
			} else {
				// Fix 3: prev/next are always available on non-expanded groups
				if (group.size() > 1 && inBounds(mx, my, lax, y, PREV_W, ROW_H)) {
					groupVisible.set(gi, (vi - 1 + group.size()) % group.size()); playClick(); return;
				}
				if (group.size() > 1 && inBounds(mx, my, lax + PREV_W + SLOT_W, y, NEXT_W, ROW_H)) {
					groupVisible.set(gi, (vi + 1) % group.size()); playClick(); return;
				}
				// Click on enchant slot — expand it (show level picker)
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
			// List view: scroll thumb drag
			if (listTotalH > P3L_LIST_H) {
				int tAx = px + SCROLL_X, tAy = py + SCROLL_Y;
				int maxScroll = listTotalH - P3L_LIST_H;
				float frac = (float) listScrollOffset / Math.max(1, maxScroll);
				int thumbY = tAy + (int)(frac * (SCROLL_TRACK_H - SCROLLER_H));
				if (inBounds(mx, my, tAx, thumbY, SCROLLER_W, SCROLLER_H)) {
					listScrollDragging = true;
					listScrollDragStartY = my;
					listScrollDragStartOff = listScrollOffset;
				}
			}
			return;
		}
		int tax = px + P3_TREE_X, tay = py + P3_TREE_Y;
		if (inBounds(mx, my, tax, tay, P3_TREE_W, P3_TREE_H)) {
			treeDragging = true;
			treeDragStartX = mx; treeDragStartY = my;
			treeDragOffX = treeOffX; treeDragOffY = treeOffY;
		}
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

	// ─────────────────────────────────────────────────────────────────────────
	// PHASE 2 DATA
	// ─────────────────────────────────────────────────────────────────────────

	private void buildPhase2Data() {
		selectedLevels.clear(); scrollOffset = 0; expandedGroupIdx = -1;
		itemEnchants = EnchantData.enchantsForItem(selectedItem);
		incompatGroups.clear(); groupVisible.clear();

		// Setting 3: when allowIncompatible is on, preserve the original grouping ORDER
		// (so incompatible enchants still appear next to each other) but each enchant
		// gets its own independent row — no prev/next, no disabling.
		if (com.armaninyow.dibs.config.AeogConfig.allowIncompatible) {
			Set<String> placed = new HashSet<>();
			// Use the same grouping logic to determine adjacency order,
			// then add each enchant individually so they stay neighbours.
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
				// Add this enchant as its own row first
				incompatGroups.add(new ArrayList<>(List.of(enchant)));
				groupVisible.add(0);
				placed.add(enchant);
				// Then add each incompatible partner right after (also as own row)
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

			// For trident and book: force channeling → loyalty → riptide in that order
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

			// All other enchants — group mutually exclusive ones via prev/next
			for (String enchant : itemEnchants) {
				if (placed.contains(enchant)) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
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

		// Setting 2: auto-fill levels after groups are built
		autoFillLevels();
	}

	/** Setting 2: auto-select levels based on the configured mode. */
	private void autoFillLevels() {
		com.armaninyow.dibs.config.AeogConfig.AutoFillMode mode =
			com.armaninyow.dibs.config.AeogConfig.autoFillMode;
		if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.OFF) return;

		if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.MAX_LEVELS) {
			for (String enchant : itemEnchants) {
				if (enchant.equals("binding_curse") || enchant.equals("vanishing_curse")) continue;
				EnchantData.EnchantDef def = EnchantData.ENCHANTS.get(enchant);
				selectedLevels.put(enchant, def.levelMax());
			}
		} else if (mode == com.armaninyow.dibs.config.AeogConfig.AutoFillMode.FROM_INVENTORY) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player == null) return;
			PlayerInventory inv = mc.player.getInventory();
			if (mc.world == null) return;
			var reg = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
			for (int i = 0; i < inv.size(); i++) {
				ItemStack s = inv.getStack(i);
				if (s.isEmpty() || !s.isOf(Items.ENCHANTED_BOOK)) continue;
				var stored = s.get(DataComponentTypes.STORED_ENCHANTMENTS);
				if (stored == null) continue;
				for (String enchant : itemEnchants) {
					if (enchant.equals("binding_curse") || enchant.equals("vanishing_curse")) continue;
					String mcId = toMinecraftEnchantId(enchant);
					var entry = reg.getEntry(Identifier.of("minecraft", mcId));
					if (entry.isEmpty()) continue;
					int lvl = stored.getLevel(entry.get());
					if (lvl > 0) {
						int cur = selectedLevels.getOrDefault(enchant, 0);
						if (lvl > cur) selectedLevels.put(enchant, lvl);
					}
				}
			}
		}

		// Enforce incompatibility groups — keep only highest-level enchant per group
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

	/**
	 * Setting 1: check if the anvil target slot has an item and auto-advance to Phase 2.
	 * Called each render frame from render().
	 */
	public void tickAutoDetect(net.minecraft.item.ItemStack targetSlotStack) {
		if (!com.armaninyow.dibs.config.AeogConfig.autoDetectItem) return;
		if (phase != Phase.ONE) return;
		if (targetSlotStack == null || targetSlotStack.isEmpty()) return;

		// Map the item in the slot to our item ID
		String detected = detectItemId(targetSlotStack);
		if (detected == null) return;

		selectedItem = detected;
		buildPhase2Data();
		phase = Phase.TWO;
		playClick();
	}

	/** Maps a vanilla ItemStack to our internal item ID string, or null if not supported. */
	private String detectItemId(ItemStack s) {
		for (String id : EnchantData.getPhase1Items()) {
			if (id.equals("book")) continue; // book is selected manually
			if (itemMatchesId(s, id)) return id;
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

	// ─────────────────────────────────────────────────────────────────────────
	// NETWORK
	// ─────────────────────────────────────────────────────────────────────────

	private static final OptimizationEngine CLIENT_ENGINE = new OptimizationEngine();

	private void sendCalculationRequest() {
		if (selectedItem == null) return;
		int idC = 0;
		Map<String, Integer> ids = new LinkedHashMap<>();
		for (String k : EnchantData.ENCHANTS.keySet()) ids.put(k, idC++);
		List<int[]> enchants = new ArrayList<>();
		for (String e : itemEnchants) {
			if (!selectedLevels.containsKey(e)) continue;
			enchants.add(new int[]{ids.get(e), selectedLevels.get(e)});
		}
		if (enchants.isEmpty()) return;
		OptimizationEngine.Mode mode = modeLevels
			? OptimizationEngine.Mode.LEVELS
			: OptimizationEngine.Mode.WORK;
		final String itemSnapshot = selectedItem;
		final List<int[]> enchantsSnapshot = enchants;
		final OptimizationEngine.Mode modeSnapshot = mode;
		Thread.ofVirtual().name("aeog-engine").start(() -> {
			try {
				List<MergeInstruction> result = CLIENT_ENGINE.process(itemSnapshot, enchantsSnapshot, modeSnapshot);
				net.minecraft.client.MinecraftClient.getInstance().execute(() -> receiveEngineResult(result));
			} catch (Exception ex) {
				AnvilEnchantmentOrderingGuide.LOGGER.error("[AEOG] Engine error: {}", ex.getMessage(), ex);
				net.minecraft.client.MinecraftClient.getInstance().execute(() -> loading = false);
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

	// ─────────────────────────────────────────────────────────────────────────
	// INVENTORY MATCHING
	// ─────────────────────────────────────────────────────────────────────────

	private boolean playerHasItem(MergeInstruction.NodeItem data) {
		return findMatchingStack(data, null) != null;
	}

	/** When true, findMatchingStack logs its checks (set by hover tooltip). */

	private ItemStack findMatchingStack(MergeInstruction.NodeItem data, TreeNode node) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return null;
		PlayerInventory inv = mc.player.getInventory();
		int expectedPwp = (1 << data.work()) - 1;

		List<String[]> enchants = node != null ? collectEnchantsfromNode(node) : data.enchants();

		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (s.isEmpty() || !itemMatchesId(s, data.id())) continue;

			int rc = s.contains(DataComponentTypes.REPAIR_COST)
				? s.get(DataComponentTypes.REPAIR_COST) : 0;
			if (rc != expectedPwp) continue;

			var enchComp = data.id().equals("book")
				? s.get(DataComponentTypes.STORED_ENCHANTMENTS)
				: s.get(DataComponentTypes.ENCHANTMENTS);
			int enchSize = enchComp != null ? enchComp.getSize() : 0;

			if (!enchants.isEmpty() && enchSize != enchants.size()) continue;

			if (enchComp != null && !enchants.isEmpty()) {
				boolean allMatch = true;
				for (String[] e : enchants) {
					int level;
					try { level = Integer.parseInt(e[1]); } catch (NumberFormatException ex) { level = 1; }
					if (mc.world == null) { allMatch = false; break; }
					var reg      = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
					String mcId  = toMinecraftEnchantId(e[0]);
					var entryOpt = reg.getEntry(Identifier.of("minecraft", mcId));
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
		return switch (id) {
			case "book"                     -> s.isOf(Items.ENCHANTED_BOOK);
			case "helmet"                   -> s.isOf(Items.DIAMOND_HELMET)||s.isOf(Items.NETHERITE_HELMET)
				||s.isOf(Items.IRON_HELMET)||s.isOf(Items.GOLDEN_HELMET)||s.isOf(Items.CHAINMAIL_HELMET)
				||s.isOf(Items.LEATHER_HELMET)||s.isOf(Items.TURTLE_HELMET);
			case "chestplate"               -> s.isOf(Items.DIAMOND_CHESTPLATE)||s.isOf(Items.NETHERITE_CHESTPLATE)
				||s.isOf(Items.IRON_CHESTPLATE)||s.isOf(Items.GOLDEN_CHESTPLATE)
				||s.isOf(Items.CHAINMAIL_CHESTPLATE)||s.isOf(Items.LEATHER_CHESTPLATE);
			case "leggings"                 -> s.isOf(Items.DIAMOND_LEGGINGS)||s.isOf(Items.NETHERITE_LEGGINGS)
				||s.isOf(Items.IRON_LEGGINGS)||s.isOf(Items.GOLDEN_LEGGINGS)
				||s.isOf(Items.CHAINMAIL_LEGGINGS)||s.isOf(Items.LEATHER_LEGGINGS);
			case "boots"                    -> s.isOf(Items.DIAMOND_BOOTS)||s.isOf(Items.NETHERITE_BOOTS)
				||s.isOf(Items.IRON_BOOTS)||s.isOf(Items.GOLDEN_BOOTS)
				||s.isOf(Items.CHAINMAIL_BOOTS)||s.isOf(Items.LEATHER_BOOTS);
			case "sword"                    -> s.isOf(Items.DIAMOND_SWORD)||s.isOf(Items.NETHERITE_SWORD)
				||s.isOf(Items.IRON_SWORD)||s.isOf(Items.GOLDEN_SWORD)||s.isOf(Items.STONE_SWORD)
				||s.isOf(Items.WOODEN_SWORD);
			case "pickaxe"                  -> s.isOf(Items.DIAMOND_PICKAXE)||s.isOf(Items.NETHERITE_PICKAXE)
				||s.isOf(Items.IRON_PICKAXE)||s.isOf(Items.GOLDEN_PICKAXE)||s.isOf(Items.STONE_PICKAXE)
				||s.isOf(Items.WOODEN_PICKAXE);
			case "axe"                      -> s.isOf(Items.DIAMOND_AXE)||s.isOf(Items.NETHERITE_AXE)
				||s.isOf(Items.IRON_AXE)||s.isOf(Items.GOLDEN_AXE)||s.isOf(Items.STONE_AXE)
				||s.isOf(Items.WOODEN_AXE);
			case "shovel"                   -> s.isOf(Items.DIAMOND_SHOVEL)||s.isOf(Items.NETHERITE_SHOVEL)
				||s.isOf(Items.IRON_SHOVEL)||s.isOf(Items.GOLDEN_SHOVEL)||s.isOf(Items.STONE_SHOVEL)
				||s.isOf(Items.WOODEN_SHOVEL);
			case "hoe"                      -> s.isOf(Items.DIAMOND_HOE)||s.isOf(Items.NETHERITE_HOE)
				||s.isOf(Items.IRON_HOE)||s.isOf(Items.GOLDEN_HOE)||s.isOf(Items.STONE_HOE)
				||s.isOf(Items.WOODEN_HOE);
			case "bow"                      -> s.isOf(Items.BOW);
			case "crossbow"                 -> s.isOf(Items.CROSSBOW);
			case "trident"                  -> s.isOf(Items.TRIDENT);
			case "elytra"                   -> s.isOf(Items.ELYTRA);
			case "shield"                   -> s.isOf(Items.SHIELD);
			case "fishing_rod"              -> s.isOf(Items.FISHING_ROD);
			case "flint_and_steel"          -> s.isOf(Items.FLINT_AND_STEEL);
			case "shears"                   -> s.isOf(Items.SHEARS);
			case "brush"                    -> s.isOf(Items.BRUSH);
			case "carrot_on_a_stick"        -> s.isOf(Items.CARROT_ON_A_STICK);
			case "warped_fungus_on_a_stick" -> s.isOf(Items.WARPED_FUNGUS_ON_A_STICK);
			case "pumpkin"                  -> s.isOf(Items.CARVED_PUMPKIN);
			case "mace"                     -> s.isOf(Items.MACE);
			case "spear"                    -> {
				// Matches all spear tiers: wooden_spear, stone_spear, iron_spear, golden_spear, diamond_spear, netherite_spear
				String path = net.minecraft.registry.Registries.ITEM.getId(s.getItem()).getPath();
				yield path.endsWith("_spear") || path.equals("spear");
			}
			default                         -> false;
		};
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────────────────────────────────

	/** Draws a custom 16×16 PNG icon from assets/aeog/textures/items/. Used in Phase 1 buttons. */
	private void drawIcon(DrawContext ctx, String itemId, int x, int y) {
		Identifier icon = getItemIcons().get(itemId);
		if (icon != null)
			ctx.drawTexture(PIPE, icon, x, y, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	private String formatName(String id) {
		if (id.equals("book"))                      return "Enchanted Book";
		if (id.equals("pumpkin"))                   return "Carved Pumpkin";
		if (id.equals("flint_and_steel"))           return "Flint and Steel";
		if (id.equals("carrot_on_a_stick"))         return "Carrot on a Stick";
		if (id.equals("warped_fungus_on_a_stick"))  return "Warped Fungus on a Stick";
		if (id.equals("binding_curse"))             return "Curse of Binding";
		if (id.equals("vanishing_curse"))           return "Curse of Vanishing";
		if (id.equals("sweeping"))                  return "Sweeping Edge";
		return Arrays.stream(id.split("_"))
			.map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
			.reduce((a, b) -> a + " " + b).orElse(id);
	}

	/**
	 * Translates our internal enchant key to the Minecraft registry ID.
	 * Most keys match directly (e.g. "looting" → "looting"), but a few differ.
	 */
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