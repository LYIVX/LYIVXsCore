package net.lyivx.ls_core.common.config;

import com.google.gson.JsonObject;

public interface ILyivxConfigProvider {
    /**
     * @return The unique identifier for the mod providing this configuration.
     */
    String getModId();

    /**
     * @return The name of the mod providing this configuration.
     */
    String getModName();

    /**
     * @return The display name for the configuration category in the UI.
     */
    String getConfigCategoryName();

    /**
     * @return A JsonObject representing the default configuration values for this mod.
     */
    JsonObject getDefaultConfig();

    /**
     * Called by LYIVX's Core to register the config spec for this provider.
     * The provider should typically store this spec for later use.
     * @param spec The CustomConfigSpec instance created for this provider.
     */
    void registerConfigSpec(CustomConfigSpec spec);

    /**
     * Allows the provider to define and add its specific options to the config screen.
     * @param screen The ModConfigScreen instance.
     * @param list The CustomOptionsList to add options to.
     */
    void addConfigOptions(net.lyivx.ls_core.client.screens.ModConfigScreen screen, net.lyivx.ls_core.client.screens.widgets.CustomOptionsList list);

    /**
     * Allows the provider to reset its specific config options to their defaults.
     */
    void resetConfigDefaults();

}