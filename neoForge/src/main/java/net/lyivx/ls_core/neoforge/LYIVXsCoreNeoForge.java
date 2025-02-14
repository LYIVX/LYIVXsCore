package net.lyivx.ls_core.neoforge;

import net.lyivx.ls_core.LYIVXsCore;
import net.lyivx.ls_core.client.LYIVXsCoreClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(LYIVXsCore.MOD_ID)
public class LYIVXsCoreNeoForge {
    public LYIVXsCoreNeoForge(IEventBus bus) {
        LYIVXsCore.init();

        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);

        NeoForge.EVENT_BUS.register(this);

    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LYIVXsCoreClient.init();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {

    }


    @SubscribeEvent
    public void onPlace(PlayerInteractEvent.RightClickBlock event) {

    }

    @SubscribeEvent
    public void onServerStart(ServerAboutToStartEvent event) {

    }


}