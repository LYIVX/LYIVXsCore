package net.lyivx.ls_core;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import net.lyivx.ls_core.common.commands.ModConfigCommand;
import net.lyivx.ls_core.common.config.Configs;
import net.lyivx.ls_core.common.config.CustomConfigSpec;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class LYIVXsCore {
    public static final String MOD_ID = "ls_core";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModConfigCommand.register(dispatcher);
        });

        Path configDir = Platform.getConfigFolder().resolve(LYIVXsCore.MOD_ID);
        CustomConfigSpec configSpec = new CustomConfigSpec(configDir, LYIVXsCore.MOD_ID + "_config");
        Configs.setConfigSpec(configSpec);
    }

    public static ResourceLocation createResourceLocation(String location) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, location);
    }

    public static ModelResourceLocation createModelResourceLocation(String location) {
        return ModelResourceLocation.inventory(ResourceLocation.parse(location));
    }
/*
    public static void addCreativeTabContents(ResourceKey<CreativeModeTab> tab, Consumer<ItemLike> consumer) {
        if (tab == FUNCTIONAL_BLOCKS) {
            ModItems.DECO.boundStream().forEach(consumer);
        } else if (tab == INGREDIENTS) {
            ModItems.MISC.boundStream().forEach(consumer);
            ModItems.FOODS.boundStream().forEach(consumer);
            ModItems.INGREDIENTS.boundStream().forEach(consumer);
            ModItems.INGREDIENTS_BURNABLE_100.boundStream().forEach(consumer);
            ModItems.INGREDIENTS_BURNABLE_200.boundStream().forEach(consumer);
        } else if (tab == TOOLS_AND_UTILITIES) {
            ModItems.TOOLS.boundStream().forEach(consumer);
        }
    }*/
}

