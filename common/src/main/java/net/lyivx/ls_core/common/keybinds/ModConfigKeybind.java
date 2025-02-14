package net.lyivx.ls_core.common.keybinds;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.lyivx.ls_core.client.screens.ModConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ModConfigKeybind {
    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.ls_core.openconfig",
            GLFW.GLFW_KEY_M,
            "key.ls_core.categories.ls_core"
    );

    public static void registerKeybinding() {
        KeyMappingRegistry.register(OPEN_CONFIG_KEY);
    }

    public static void checkKeybinding() {
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ModConfigScreen(
                    Minecraft.getInstance().screen,
                    Minecraft.getInstance().options,
                    ModConfigScreen.CategoryType.GENERAL
            ));
        }
    }
}

