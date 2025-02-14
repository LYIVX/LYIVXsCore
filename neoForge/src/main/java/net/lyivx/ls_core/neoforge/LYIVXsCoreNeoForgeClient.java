package net.lyivx.ls_core.neoforge;

import net.lyivx.ls_core.LYIVXsCore;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(LYIVXsCore.MOD_ID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LYIVXsCoreNeoForgeClient {
    private static IEventBus modEventBus;

    public LYIVXsCoreNeoForgeClient(IEventBus bus) {
        modEventBus = bus;
        modEventBus.addListener(LYIVXsCoreNeoForgeClient::onClientSetup);

    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LYIVXsCore.init();
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
    }

}