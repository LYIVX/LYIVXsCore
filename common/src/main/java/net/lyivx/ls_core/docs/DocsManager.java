package net.lyivx.ls_core.docs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.lyivx.ls_core.LYIVXsCore;
import net.lyivx.ls_core.docs.model.CategoryJson;
import net.lyivx.ls_core.docs.model.DocJson;
import net.lyivx.ls_core.docs.model.EntryJson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads documentation JSON from data/<namespace>/ls_core/...
 * Implements PreparableReloadListener to automatically reload when Minecraft reloads data packs.
 */
public class DocsManager implements PreparableReloadListener {

    public static final String ROOT_PATH = "ls_core";
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, DocJson> namespaceToDoc = new HashMap<>();
    private final Map<String, Map<String, CategoryJson>> namespaceToCategories = new HashMap<>();
    private final Map<String, Map<String, EntryJson>> namespaceToEntries = new HashMap<>();

    public Optional<DocJson> getDocForNamespace(String namespace) {
        return Optional.ofNullable(namespaceToDoc.get(namespace));
    }

    public Collection<String> getNamespaces() {
        return Collections.unmodifiableSet(namespaceToDoc.keySet());
    }

    public Collection<CategoryJson> getCategories(String namespace) {
        return namespaceToCategories.getOrDefault(namespace, Collections.emptyMap()).values();
    }

    public Collection<EntryJson> getEntries(String namespace) {
        return namespaceToEntries.getOrDefault(namespace, Collections.emptyMap()).values();
    }

    public Optional<CategoryJson> getCategory(String namespace, String id) {
        return Optional.ofNullable(namespaceToCategories.getOrDefault(namespace, Collections.emptyMap()).get(id));
    }

    public Optional<EntryJson> getEntry(String namespace, String id) {
        return Optional.ofNullable(namespaceToEntries.getOrDefault(namespace, Collections.emptyMap()).get(id));
    }

    /**
     * Get a list of all mods that currently have documentation data loaded.
     * Useful for debugging and verifying that all mods are being loaded.
     */
    public List<String> getLoadedMods() {
        return new ArrayList<>(namespaceToDoc.keySet());
    }

    /**
     * Get detailed information about what documentation is loaded from each mod.
     * Useful for debugging and verifying the reload system.
     */
    public Map<String, Map<String, Object>> getDocumentationStats() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        for (String namespace : namespaceToDoc.keySet()) {
            Map<String, Object> modStats = new HashMap<>();
            modStats.put("doc", namespaceToDoc.get(namespace) != null);
            modStats.put("categories", namespaceToCategories.getOrDefault(namespace, Collections.emptyMap()).size());
            modStats.put("entries", namespaceToEntries.getOrDefault(namespace, Collections.emptyMap()).size());
            stats.put(namespace, modStats);
        }
        
        return stats;
    }

    /**
     * Find all mods that have data/ls_core directories by scanning for doc.json files.
     */
    private Set<String> findModsWithDocumentation(ResourceManager manager) {
        Set<String> modsWithDocs = new HashSet<>();
        
        try {
            // Look for any doc.json files in ls_core directories
            Map<ResourceLocation, Resource> docResources = manager.listResources(ROOT_PATH, 
                loc -> loc.getPath().equals(ROOT_PATH + "/doc.json"));
            
            for (ResourceLocation docLoc : docResources.keySet()) {
                String modNamespace = docLoc.getNamespace();
                modsWithDocs.add(modNamespace);
                LYIVXsCore.LOGGER.debug("Found documentation in mod: {}", modNamespace);
            }
            
        } catch (Exception e) {
            LYIVXsCore.LOGGER.warn("Error scanning for mods with documentation", e);
        }
        
        return modsWithDocs;
    }

    /**
     * Load all documentation data from a specific mod.
     */
    private void loadModDocumentation(ResourceManager manager, String modNamespace, LoadedData data) {
        try {
            // Load doc.json
            ResourceLocation docPath = ResourceLocation.fromNamespaceAndPath(modNamespace, ROOT_PATH + "/doc.json");
            Optional<Resource> docResource = manager.getResource(docPath);
            if (docResource.isPresent()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(docResource.get().open(), StandardCharsets.UTF_8))) {
                    DocJson doc = GSON.fromJson(reader, DocJson.class);
                    doc.sourceNamespace = modNamespace;
                    data.docs.put(modNamespace, doc);
                    LYIVXsCore.LOGGER.debug("Loaded doc.json from {}", modNamespace);
            }
        }

        // Load categories
            Map<ResourceLocation, Resource> categoryResources = manager.listResources(ROOT_PATH + "/categories", 
                loc -> loc.getNamespace().equals(modNamespace) && loc.getPath().endsWith(".json"));
            
        for (Map.Entry<ResourceLocation, Resource> entry : categoryResources.entrySet()) {
            ResourceLocation id = entry.getKey();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                CategoryJson category = GSON.fromJson(reader, CategoryJson.class);
                String simpleId = stripJson(id.getPath().substring((ROOT_PATH + "/categories/").length()));
                category.id = simpleId;
                    data.categories.computeIfAbsent(modNamespace, k -> new HashMap<>()).put(simpleId, category);
            }
        }

        // Load entries
            Map<ResourceLocation, Resource> entryResources = manager.listResources(ROOT_PATH + "/entries", 
                loc -> loc.getNamespace().equals(modNamespace) && loc.getPath().endsWith(".json"));
            
        for (Map.Entry<ResourceLocation, Resource> entry : entryResources.entrySet()) {
            ResourceLocation id = entry.getKey();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                EntryJson ej = GSON.fromJson(reader, EntryJson.class);
                String simpleId = stripJson(id.getPath().substring((ROOT_PATH + "/entries/").length()));
                ej.id = simpleId;
                    data.entries.computeIfAbsent(modNamespace, k -> new HashMap<>()).put(simpleId, ej);
                }
            }
            
            LYIVXsCore.LOGGER.debug("Loaded documentation from {}: {} categories, {} entries", 
                modNamespace,
                data.categories.getOrDefault(modNamespace, Collections.emptyMap()).size(),
                data.entries.getOrDefault(modNamespace, Collections.emptyMap()).size());
                
        } catch (Exception e) {
            LYIVXsCore.LOGGER.error("Error loading documentation from mod: {}", modNamespace, e);
        }
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            LYIVXsCore.LOGGER.info("Starting documentation reload from Minecraft data reload");
            
            // Find all mods with documentation
            Set<String> modsWithDocs = findModsWithDocumentation(manager);
            LYIVXsCore.LOGGER.info("Found mods with documentation: {}", modsWithDocs);
            
            // Load documentation from each mod
            LoadedData data = new LoadedData();
            for (String modNamespace : modsWithDocs) {
                loadModDocumentation(manager, modNamespace, data);
            }
            
            return data;
        }, prepareExecutor).thenCompose(barrier::wait).thenAcceptAsync(data -> {
            apply(data);
            
            LYIVXsCore.LOGGER.info("Documentation reload completed: {} namespaces, {} categories, {} entries",
                    namespaceToDoc.size(),
                    namespaceToCategories.values().stream().mapToInt(Map::size).sum(),
                    namespaceToEntries.values().stream().mapToInt(Map::size).sum());
        }, applyExecutor);
    }

    private void apply(LoadedData data) {
        // Clear existing data
        namespaceToDoc.clear();
        namespaceToCategories.clear();
        namespaceToEntries.clear();

        // Apply new data
        namespaceToDoc.putAll(data.docs);
        namespaceToCategories.putAll(data.categories);
        namespaceToEntries.putAll(data.entries);
    }

    private static String stripJson(String path) {
        if (path.endsWith(".json")) {
            return path.substring(0, path.length() - 5);
        }
        return path;
    }

    private static class LoadedData {
        final Map<String, DocJson> docs = new HashMap<>();
        final Map<String, Map<String, CategoryJson>> categories = new HashMap<>();
        final Map<String, Map<String, EntryJson>> entries = new HashMap<>();
    }
}
