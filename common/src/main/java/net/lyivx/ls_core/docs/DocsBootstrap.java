package net.lyivx.ls_core.docs;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * Registers the documentation reload listener with the game.
 */
public final class DocsBootstrap {
    private static boolean registered;

    private DocsBootstrap() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ReloadListenerRegistry.register(PackType.SERVER_DATA, DocsService.get(), ResourceLocation.fromNamespaceAndPath("ls_core", "docs"));
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, DocsService.get(), ResourceLocation.fromNamespaceAndPath("ls_core", "docs_client"));
    }
}


