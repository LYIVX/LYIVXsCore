package net.lyivx.ls_core.fabric;

import dev.architectury.event.events.common.LifecycleEvent;
import net.fabricmc.api.ModInitializer;
import net.lyivx.ls_core.LYIVXsCore;

public class LYIVXsCoreFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LYIVXsCore.init();
    }
}