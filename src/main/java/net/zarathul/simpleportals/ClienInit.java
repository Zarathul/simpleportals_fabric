package net.zarathul.simpleportals;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.zarathul.simpleportals.configuration.gui.ConfigGui;

import java.util.ArrayList;

public class ClienInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		BlockRenderLayerMap.INSTANCE.putBlock(SimplePortals.blockPortal, RenderType.translucent());

		// Register client side only command to show the config.
		CommandRegistrationCallback.EVENT.register((dispatcher, isDedicatedServer) -> {
			if (isDedicatedServer) return;

			LiteralCommandNode<CommandSourceStack> configCommand = Commands
					.literal("config")		// sportals config
					.executes(context -> {
						Minecraft.getInstance().setScreen(new ConfigGui(new TextComponent("SimplePortals"), null, Settings.class, SimplePortals.MOD_ID));
						return 1;
					})
					.build();

			ArrayList<String> commandNodePath = new ArrayList<>();
			commandNodePath.add("sportals");
			dispatcher.findNode(commandNodePath).addChild(configCommand);
		});
	}
}
