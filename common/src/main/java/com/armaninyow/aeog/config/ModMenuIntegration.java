package com.armaninyow.dibs.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

	public enum Phase3ViewMode { TREE, LIST }

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> buildScreen(parent);
	}

	private Screen buildScreen(Screen parent) {
		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("config.aeog.title"))
			.category(ConfigCategory.createBuilder()
				.name(Component.translatable("config.aeog.category.general"))

				// ── Setting 1: Auto-detect item ───────────────────────────────────────
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("config.aeog.autoDetectItem"))
					.description(OptionDescription.of(Component.translatable("config.aeog.autoDetectItem.tooltip")))
					.binding(false, () -> AeogConfig.autoDetectItem, val -> AeogConfig.autoDetectItem = val)
					.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
					.build())

				// ── Setting 2: Auto-fill mode ─────────────────────────────────────────
				.option(Option.<AeogConfig.AutoFillMode>createBuilder()
					.name(Component.translatable("config.aeog.autoFillMode"))
					.description(OptionDescription.of(
						Component.translatable("config.aeog.autoFillMode.tooltip")))
					.binding(AeogConfig.AutoFillMode.OFF, () -> AeogConfig.autoFillMode, val -> AeogConfig.autoFillMode = val)
					.controller(opt -> EnumControllerBuilder.create(opt)
						.enumClass(AeogConfig.AutoFillMode.class)
						.formatValue(e -> Component.translatable("config.aeog.autoFillMode." + e.name().toLowerCase())))
					.build())

				// ── Setting 3: Allow incompatible ─────────────────────────────────────
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("config.aeog.allowIncompatible"))
					.description(OptionDescription.of(Component.translatable("config.aeog.allowIncompatible.tooltip")))
					.binding(false, () -> AeogConfig.allowIncompatible, val -> AeogConfig.allowIncompatible = val)
					.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
					.build())

				// ── Setting 4: Phase 3 view mode ─────────────────────────────────────
				.option(Option.<Phase3ViewMode>createBuilder()
					.name(Component.translatable("config.aeog.phase3ViewMode"))
					.description(OptionDescription.of(Component.translatable("config.aeog.phase3ViewMode.tooltip")))
					.binding(Phase3ViewMode.TREE,
						() -> AeogConfig.listViewPhase3 ? Phase3ViewMode.LIST : Phase3ViewMode.TREE,
						val -> AeogConfig.listViewPhase3 = (val == Phase3ViewMode.LIST))
					.controller(opt -> EnumControllerBuilder.create(opt)
						.enumClass(Phase3ViewMode.class)
						.formatValue(e -> Component.translatable("config.aeog.phase3ViewMode." + e.name().toLowerCase())))
					.build())

				// ── Setting 5: Show mod button in Phase 1 ─────────────────────────────
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("config.aeog.showModButtonPhase1"))
					.description(OptionDescription.of(Component.translatable("config.aeog.showModButtonPhase1.tooltip")))
					.binding(false, () -> AeogConfig.showModButtonPhase1, val -> AeogConfig.showModButtonPhase1 = val)
					.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
					.build())

				// ── Setting 6: Show mod button in Phase 2 ─────────────────────────────
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("config.aeog.showModButtonPhase2"))
					.description(OptionDescription.of(Component.translatable("config.aeog.showModButtonPhase2.tooltip")))
					.binding(false, () -> AeogConfig.showModButtonPhase2, val -> AeogConfig.showModButtonPhase2 = val)
					.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
					.build())

				.build())
			.save(AeogConfig::save)
			.build()
			.generateScreen(parent);
	}
}