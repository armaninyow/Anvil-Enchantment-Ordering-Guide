package com.armaninyow.dibs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

/**
 * Holds all AEOG mod settings, with JSON persistence in the config folder.
 */
public class AeogConfig {

	// ── Setting 1: Auto-detect item from anvil slot ───────────────────────────
	public static boolean autoDetectItem = false;

	// ── Setting 2: Auto-fill Phase 2 levels ──────────────────────────────────
	public enum AutoFillMode { OFF, MAX_LEVELS, FROM_INVENTORY }
	public static AutoFillMode autoFillMode = AutoFillMode.OFF;

	// ── Setting 3: Allow incompatible enchantments ────────────────────────────
	public static boolean allowIncompatible = false;

	// ── Persistence ───────────────────────────────────────────────────────────

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH =
		FabricLoader.getInstance().getConfigDir().resolve("aeog.json");

	/** Data class mirroring the static fields for JSON serialization. */
	private static class Data {
		boolean autoDetectItem   = false;
		String  autoFillMode     = "OFF";
		boolean allowIncompatible = false;
	}

	public static void load() {
		File file = CONFIG_PATH.toFile();
		if (!file.exists()) { save(); return; }
		try (Reader r = new FileReader(file)) {
			Data d = GSON.fromJson(r, Data.class);
			if (d == null) return;
			autoDetectItem    = d.autoDetectItem;
			allowIncompatible = d.allowIncompatible;
			try { autoFillMode = AutoFillMode.valueOf(d.autoFillMode); }
			catch (IllegalArgumentException ignored) { autoFillMode = AutoFillMode.OFF; }
		} catch (Exception e) {
			save(); // write defaults if file is corrupt
		}
	}

	public static void save() {
		try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
			Data d = new Data();
			d.autoDetectItem    = autoDetectItem;
			d.autoFillMode      = autoFillMode.name();
			d.allowIncompatible = allowIncompatible;
			GSON.toJson(d, w);
		} catch (Exception ignored) {}
	}

	private AeogConfig() {}
}