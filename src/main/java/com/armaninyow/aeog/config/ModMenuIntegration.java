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

		return builder.build();
	}
}