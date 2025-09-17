package net.lyivx.ls_core.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomConfigSpec {
    private final Gson gson;
    private JsonObject config;
    private final Path configPath;
    private final JsonObject defaultConfig;

    public CustomConfigSpec(Path configDirectory, String providerModId, JsonObject defaultConfig) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.defaultConfig = defaultConfig != null ? defaultConfig : new JsonObject();
        this.config = new JsonObject();
        this.configPath = configDirectory.resolve(providerModId + ".json");
        System.out.println("Config for [" + providerModId + "] will be saved to: " + this.configPath.toAbsolutePath());
        loadOrCreateConfig();
    }

    private void loadOrCreateConfig() {
        if (Files.exists(configPath)) {
            loadConfig();
            mergeDefaults();
        } else {
            this.config = this.defaultConfig.deepCopy();
            saveConfig();
        }
    }

    private void mergeDefaults() {
        boolean changed = false;
        for (String key : defaultConfig.keySet()) {
            if (!config.has(key)) {
                config.add(key, defaultConfig.get(key));
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
    }

    public void loadConfig() {
        try (Reader reader = Files.newBufferedReader(configPath)) {
            this.config = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonObject getConfigRoot() {
        return config;
    }

    public JsonElement get(String key) {
        return config.get(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (config.has(key) && config.get(key).isJsonPrimitive() && config.get(key).getAsJsonPrimitive().isBoolean()) {
            return config.get(key).getAsBoolean();
        }
        setBoolean(key, defaultValue);
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        if (config.has(key) && config.get(key).isJsonPrimitive() && config.get(key).getAsJsonPrimitive().isNumber()) {
            return config.get(key).getAsInt();
        }
        setInt(key, defaultValue);
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        if (config.has(key) && config.get(key).isJsonPrimitive() && config.get(key).getAsJsonPrimitive().isString()) {
            return config.get(key).getAsString();
        }
        setString(key, defaultValue);
        return defaultValue;
    }

    public <T extends Enum<T>> T getEnum(String key, T defaultValue, Class<T> enumClass) {
        if (config.has(key) && config.get(key).isJsonPrimitive() && config.get(key).getAsJsonPrimitive().isString()) {
            try {
                return Enum.valueOf(enumClass, config.get(key).getAsString());
            } catch (IllegalArgumentException e) {
                // Invalid enum value in config
            }
        }
        setEnum(key, defaultValue);
        return defaultValue;
    }

    public void set(String key, JsonElement value) {
        config.add(key, value);
        saveConfig();
    }

    public void setBoolean(String key, boolean value) {
        config.addProperty(key, value);
        saveConfig();
    }

    public void setInt(String key, int value) {
        config.addProperty(key, value);
        saveConfig();
    }

    public void setString(String key, String value) {
        config.addProperty(key, value);
        saveConfig();
    }

    public <T extends Enum<T>> void setEnum(String key, T value) {
        config.addProperty(key, value.name());
        saveConfig();
    }
}
