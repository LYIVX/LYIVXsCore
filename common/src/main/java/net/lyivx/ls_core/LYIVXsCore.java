package net.lyivx.ls_core;

import dev.architectury.platform.Platform;
import net.lyivx.ls_core.common.config.CustomConfigSpec;
import net.lyivx.ls_core.common.config.ILyivxConfigProvider;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class LYIVXsCore {
    public static final String MOD_ID = "ls_core";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final List<ILyivxConfigProvider> configProviders = new ArrayList<>();

    public static void init() {
        // Register data-driven docs reload listener
        net.lyivx.ls_core.docs.DocsBootstrap.register();
    }

    public static void setup() {
        LOGGER.info("Looking for configuration providers...");
        ServiceLoader<ILyivxConfigProvider> loader = ServiceLoader.load(ILyivxConfigProvider.class);
        loader.forEach(provider -> {
            configProviders.add(provider);
            LOGGER.info("Found provider: {} (ID: {})", provider.getModName(), provider.getModId());
        });

        loadConfigProviders(configProviders);
        LOGGER.info("Total providers found: {}", configProviders.size());
    }

    public static void loadConfigProviders(List<ILyivxConfigProvider> providers) {
        for (ILyivxConfigProvider provider : providers) {
            try {
                String safeName = provider.getModName()
                        .replaceAll("[^a-zA-Z0-9\\-_ ]", "_")
                        .replace(" ", "_");

                Path providerConfigDir = Platform.getConfigFolder().resolve(safeName);
                Path configFilePath = providerConfigDir.resolve(provider.getModId() + ".json");

                LOGGER.info("Initializing config for mod '{}'", provider.getModId());
                LOGGER.info(" > Config directory: {}", providerConfigDir);
                LOGGER.info(" > Config file path: {}", configFilePath);

                if (Files.exists(configFilePath)) {
                    LOGGER.info(" > Found existing config file. Loading...");
                } else {
                    LOGGER.info(" > No config file found. Creating new config with defaults.");
                }

                CustomConfigSpec spec = new CustomConfigSpec(providerConfigDir, provider.getModId(), provider.getDefaultConfig());
                provider.registerConfigSpec(spec);

                LOGGER.info(" > Config registered successfully for '{}'\n", provider.getModId());

            } catch (Exception e) {
                LOGGER.error("Failed to load config for mod '{}'", provider.getModId(), e);
            }
        }
    }

    public static List<ILyivxConfigProvider> getConfigProviders() {
        return Collections.unmodifiableList(configProviders);
    }

    public static ResourceLocation createResourceLocation(String location) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, location);
    }
}

