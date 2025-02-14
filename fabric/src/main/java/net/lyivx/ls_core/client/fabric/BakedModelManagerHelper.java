package net.lyivx.ls_core.client.fabric;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class BakedModelManagerHelper {

    @Nullable
    public static BakedModel getModel(ModelManager manager, ResourceLocation id) {
        return manager.getModel(id);
    }

    private BakedModelManagerHelper() { }
}