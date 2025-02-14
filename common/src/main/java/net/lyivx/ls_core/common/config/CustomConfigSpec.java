package net.lyivx.ls_core.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomConfigSpec {
    private final Gson gson;
    private JsonObject config;
    private final Path configPath;

    public CustomConfigSpec(Path configDirectory, String fileName) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = new JsonObject();
        this.configPath = configDirectory.resolve(fileName + ".json");
        System.out.println("Config will be saved to: " + this.configPath.toAbsolutePath());
        loadOrCreateConfig();  // Load config at initialization
    }

    private void loadOrCreateConfig() {
        if (Files.exists(configPath)) {
            loadConfig();
        } else {
            createFurnitureModDefaultConfig();
            createChiseldBlocksDefaultConfig();
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

    private void createFurnitureModDefaultConfig() {
        JsonObject furniture_mod = new JsonObject();
        furniture_mod.addProperty("sort_recipes", true);
        furniture_mod.addProperty("preview", true);
        furniture_mod.addProperty("search_bar_mode", Configs.SearchMode.AUTOMATIC.name());
        furniture_mod.addProperty("search_bar_threshold", 32);

        if (!config.has("furniture_mod")) {
            config.add("furniture_mod", furniture_mod);
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());  // Ensure the directory exists
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject getOrCreateFurnitureMod() {
        if (!config.has("furniture_mod") || config.get("furniture_mod").isJsonNull()) {
            JsonObject furniture_mod = new JsonObject();
            config.add("furniture_mod", furniture_mod);
            return furniture_mod;
        }
        return config.getAsJsonObject("furniture_mod");
    }

    public boolean getSortRecipes() {
        JsonObject furniture_mod = config.getAsJsonObject("furniture_mod");
        if (!furniture_mod.has("sort_recipes")) {
            furniture_mod.addProperty("sort_recipes", true);
            saveConfig();
        }
        return furniture_mod.get("sort_recipes").getAsBoolean();
    }

    public boolean getPreview() {
        JsonObject furniture_mod = config.getAsJsonObject("furniture_mod");
        if (!furniture_mod.has("preview")) {
            furniture_mod.addProperty("preview", true);
            saveConfig();
        }
        return furniture_mod.get("preview").getAsBoolean();
    }

    public Configs.SearchMode getSearchMode() {
        JsonObject furniture_mod = config.getAsJsonObject("furniture_mod");
        if (!furniture_mod.has("search_bar_mode")) {
            furniture_mod.addProperty("search_bar_mode", Configs.SearchMode.AUTOMATIC.name());
            saveConfig();
        }
        String mode = furniture_mod.get("search_bar_mode").getAsString();
        return Configs.SearchMode.valueOf(mode);
    }

    public int getSearchBarThreshold() {
        JsonObject furniture_mod = config.getAsJsonObject("furniture_mod");
        if (!furniture_mod.has("search_bar_threshold")) {
            furniture_mod.addProperty("search_bar_threshold", 32);
            saveConfig();
        }
        return furniture_mod.get("search_bar_threshold").getAsInt();
    }

    public void setSortRecipes(boolean value) {
        JsonObject furniture_mod = getOrCreateFurnitureMod();
        furniture_mod.addProperty("sort_recipes", value);
        saveConfig();
    }

    public void setPreview(boolean value) {
        JsonObject furniture_mod = getOrCreateFurnitureMod();
        furniture_mod.addProperty("preview", value);
        saveConfig();
    }

    public void setSearchMode(Configs.SearchMode mode) {
        JsonObject furniture_mod = getOrCreateFurnitureMod();
        furniture_mod.addProperty("search_bar_mode", mode.name());
        saveConfig();
    }

    public void setSearchBarThreshold(int threshold) {
        JsonObject furniture_mod = getOrCreateFurnitureMod();
        furniture_mod.addProperty("search_bar_threshold", threshold);
        saveConfig();
    }


    private void createChiseldBlocksDefaultConfig() {
        JsonObject chiseld_blocks = new JsonObject();
        chiseld_blocks.addProperty("connected", true);

        if (!config.has("chiseld_blocks")) {
            config.add("chiseld_blocks", chiseld_blocks);
        }
    }

    private JsonObject getOrCreateChiseldBlocks() {
        if (!config.has("chiseld_blocks") || config.get("chiseld_blocks").isJsonNull()) {
            JsonObject chiseld_blocks = new JsonObject();
            config.add("chiseld_blocks", chiseld_blocks);
            return chiseld_blocks;
        }
        return config.getAsJsonObject("chiseld_blocks");
    }

    public boolean getConnected() {
        JsonObject chiseld_blocks = config.getAsJsonObject("chiseld_blocks");
        if (!chiseld_blocks.has("chiseld_blocks")) {
            chiseld_blocks.addProperty("connected", true);
            saveConfig();
        }
        return chiseld_blocks.get("connected").getAsBoolean();
    }

    public void setConnected(boolean value) {
        JsonObject chiseld_blocks = getOrCreateChiseldBlocks();
        chiseld_blocks.addProperty("connected", value);
        saveConfig();
    }
}
