package com.armaninyow.dibs.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class ModMenuIntegration implements ModMenuApi {

	public enum Phase3ViewMode { TREE, LIST }

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> buildScreen(parent);
	}

	private Screen buildScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Text.translatable("config.aeog.title"))
			.setSavingRunnable(AeogConfig::save);

		ConfigEntryBuilder eb = builder.entryBuilder();
		ConfigCategory cat = builder.getOrCreateCategory(Text.translatable("config.aeog.category.general"));

		// ── Setting 1: Auto-detect item ───────────────────────────────────────
		cat.addEntry(eb.startBooleanToggle(
				Text.translatable("config.aeog.autoDetectItem"),
				AeogConfig.autoDetectItem)
			.setDefaultValue(false)
			.setTooltip(Text.translatable("config.aeog.autoDetectItem.tooltip"))
			.setSaveConsumer(val -> AeogConfig.autoDetectItem = val)
			.build());

		// ── Setting 2: Auto-fill mode ─────────────────────────────────────────
		cat.addEntry(eb.startEnumSelector(
				Text.translatable("config.aeog.autoFillMode"),
				AeogConfig.AutoFillMode.class,
				AeogConfig.autoFillMode)
			.setDefaultValue(AeogConfig.AutoFillMode.OFF)
			.setTooltip(
				Text.translatable("config.aeog.autoFillMode.tooltip.off"),
				Text.translatable("config.aeog.autoFillMode.tooltip.max"),
				Text.translatable("config.aeog.autoFillMode.tooltip.inv"))
			.setEnumNameProvider(e -> Text.translatable("config.aeog.autoFillMode." + e.name().toLowerCase()))
			.setSaveConsumer(val -> AeogConfig.autoFillMode = val)
			.build());

		// ── Setting 3: Allow incompatible ─────────────────────────────────────
		cat.addEntry(eb.startBooleanToggle(
				Text.translatable("config.aeog.allowIncompatible"),
				AeogConfig.allowIncompatible)
			.setDefaultValue(false)
			.setTooltip(Text.translatable("config.aeog.allowIncompatible.tooltip"))
			.setSaveConsumer(val -> AeogConfig.allowIncompatible = val)
			.build());

		// ── Setting 4: Phase 3 view mode ─────────────────────────────────────
		cat.addEntry(eb.startEnumSelector(
				Text.translatable("config.aeog.phase3ViewMode"),
				Phase3ViewMode.class,
				AeogConfig.listViewPhase3 ? Phase3ViewMode.LIST : Phase3ViewMode.TREE)
			.setDefaultValue(Phase3ViewMode.TREE)
			.setTooltip(Text.translatable("config.aeog.phase3ViewMode.tooltip"))
			.setEnumNameProvider(e -> Text.translatable("config.aeog.phase3ViewMode." + e.name().toLowerCase()))
			.setSaveConsumer(val -> AeogConfig.listViewPhase3 = (val == Phase3ViewMode.LIST))
			.build());

		// ── Setting 5: Show mod button in Phase 1 ─────────────────────────────
		cat.addEntry(eb.startBooleanToggle(
				Text.translatable("config.aeog.showModButtonPhase1"),
				AeogConfig.showModButtonPhase1)
			.setDefaultValue(false)
			.setTooltip(Text.translatable("config.aeog.showModButtonPhase1.tooltip"))
			.setSaveConsumer(val -> AeogConfig.showModButtonPhase1 = val)
			.build());

		// ── Setting 6: Show mod button in Phase 2 ─────────────────────────────
		cat.addEntry(eb.startBooleanToggle(
				Text.translatable("config.aeog.showModButtonPhase2"),
				AeogConfig.showModButtonPhase2)
			.setDefaultValue(false)
			.setTooltip(Text.translatable("config.aeog.showModButtonPhase2.tooltip"))
			.setSaveConsumer(val -> AeogConfig.showModButtonPhase2 = val)
			.build());

		return builder.build();
	}
}