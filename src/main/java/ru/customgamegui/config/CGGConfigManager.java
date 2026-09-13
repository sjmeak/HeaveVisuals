package ru.customgamegui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class CGGConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("CustomGameGui");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("customgamegui.json").toFile();

    private static CGGConfig config = new CGGConfig();

    public static CGGConfig getConfig() {
        return config;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            CGGConfig loaded = GSON.fromJson(reader, CGGConfig.class);
            if (loaded != null) {
                config = loaded;
            } else {
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load CustomGameGui configuration, using defaults", e);
            config = new CGGConfig();
            save();
        }
    }

    public static void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save CustomGameGui configuration", e);
        }
    }
}
