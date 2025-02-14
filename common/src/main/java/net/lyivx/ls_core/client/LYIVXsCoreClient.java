package net.lyivx.ls_core.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.lyivx.ls_core.common.keybinds.ModConfigKeybind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class LYIVXsCoreClient {

    public static void init() {
        ModConfigKeybind.registerKeybinding();
        ClientTickEvent.CLIENT_POST.register(client -> ModConfigKeybind.checkKeybinding());
    }

    @ExpectPlatform
    public static void setRenderType(Block block, RenderType type) {
        throw new NotImplementedException();
    }

    @ExpectPlatform
    public static BakedModel getModel(BlockRenderDispatcher dispatcher, @NotNull ResourceLocation model) {
        throw new NotImplementedException();
    }

    private static boolean hasManyRecipes = false;

    public static void onTagsUpdated() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            CraftingContainer dummy = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                public ItemStack quickMoveStack(Player player, int index) {
                    return ItemStack.EMPTY;
                }

                public boolean stillValid(Player player) {
                    return false;
                }
            }, 1, 1);
            dummy.setItem(0, Items.OAK_LOG.getDefaultInstance());

        }
    }

    public static boolean hasManyRecipes() {
        return hasManyRecipes;
    }
}
