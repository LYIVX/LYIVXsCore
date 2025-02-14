package net.lyivx.ls_core.client.fabric;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class LYIVXsCoreClientImpl {
    public static void setRenderType(Block block, RenderType type) {
        BlockRenderLayerMap.INSTANCE.putBlock(block, type);
    }
    public static BakedModel getModel(BlockRenderDispatcher dispatcher, ResourceLocation model) {
        return BakedModelManagerHelper.getModel(dispatcher.getBlockModelShaper().getModelManager(), model);
    }
}