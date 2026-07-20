package com.armaninyow.dibs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class AeogConfig {

	public static boolean autoDetectItem = false;

	public enum AutoFillMode { OFF, MAX_LEVELS, FROM_INVENTORY }
	public static AutoFillMode autoFillMode = AutoFillMode.OFF;

	public static boolean allowIncompatible = false;

	public static boolean listViewPhase3 = false;

	public static boolean showModButtonPhase1 = false;

	public static boolean showModButtonPhase2 = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH =
		FabricLoader.getInstance().getConfigDir().resolve("aeog.json");

	private static class Data {
		boolean autoDetectItem    = false;
		String  autoFillMode      = "OFF";
		boolean allowIncompatible = false;
		boolean listViewPhase3    = false;
		boolean showModButtonPhase1 = false;
		boolean showModButtonPhase2 = false;
	}

	public static void load() {
		File file = CONFIG_PATH.toFile();
		if (!file.exists()) { save(); return; }
		try (Reader r = new FileReader(file)) {
			Data d = GSON.fromJson(r, Data.class);
			if (d == null) return;
			autoDetectItem      = d.autoDetectItem;
			allowIncompatible   = d.allowIncompatible;
			listViewPhase3      = d.listViewPhase3;
			showModButtonPhase1 = d.showModButtonPhase1;
			showModButtonPhase2 = d.showModButtonPhase2;
			try { autoFillMode = AutoFillMode.valueOf(d.autoFillMode); }
			catch (IllegalArgumentException ignored) { autoFillMode = AutoFillMode.OFF; }
		} catch (Exception e) {
			save();
		}
	}

	public static void save() {
		try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
			Data d = new Data();
			d.autoDetectItem      = autoDetectItem;
			d.autoFillMode        = autoFillMode.name();
			d.allowIncompatible   = allowIncompatible;
			d.listViewPhase3      = listViewPhase3;
			d.showModButtonPhase1 = showModButtonPhase1;
			d.showModButtonPhase2 = showModButtonPhase2;
			GSON.toJson(d, w);
		} catch (Exception ignored) {}
	}

	private AeogConfig() {}
}