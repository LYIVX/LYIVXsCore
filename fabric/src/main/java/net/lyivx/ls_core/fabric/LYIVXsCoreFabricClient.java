package net.lyivx.ls_core.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.lyivx.ls_core.client.LYIVXsCoreClient;


public class LYIVXsCoreFabricClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        LYIVXsCoreClient.init();


    }
}
