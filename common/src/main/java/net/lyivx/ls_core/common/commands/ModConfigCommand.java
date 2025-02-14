package net.lyivx.ls_core.common.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.lyivx.ls_core.client.screens.ModConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;


public class ModConfigCommand {
    private static final String COMMAND_NAME = "lyivxs_mods_config";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .executes(ModConfigCommand::openConfigScreen));
    }

    private static int openConfigScreen(CommandContext<CommandSourceStack> context) {
        Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().setScreen(new ModConfigScreen(
                            Minecraft.getInstance().screen,
                            Minecraft.getInstance().options,
                            ModConfigScreen.CategoryType.GENERAL
                            )));

        return Command.SINGLE_SUCCESS;
    }
}