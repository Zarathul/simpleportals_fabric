package net.zarathul.simpleportals;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.zarathul.simpleportals.configuration.gui.ConfigGui;
import net.zarathul.simpleportals.configuration.gui.ListCommandGui;
import net.zarathul.simpleportals.configuration.gui.PortalInfo;
import net.zarathul.simpleportals.registration.Address;

import java.util.ArrayList;
import java.util.List;

public class ClienInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		BlockRenderLayerMap.INSTANCE.putBlock(SimplePortals.blockPortal, RenderType.translucent());

		// Register client side only command to show the config in the command tree (/sportals config).
		// TODO: check behavior with dedicated server

		CommandRegistrationCallback.EVENT.register((dispatcher, isDedicatedServer) -> {
			if (isDedicatedServer) return;

			// config command
			LiteralCommandNode<CommandSourceStack> configCommand = Commands
					.literal("config")		// sportals config
					.requires(commandSource -> {
						return commandSource.hasPermission(4);
					})
					.executes(context -> {
						Minecraft client = Minecraft.getInstance();
						if (!client.isSameThread())
						{
							client.execute(() -> {
								client.setScreen(new ConfigGui(new TextComponent("SimplePortals"), null, Settings.class, SimplePortals.MOD_ID));
							});
						}

						return 1;
					})
					.build();

			ArrayList<String> commandNodePath = new ArrayList<>();
			commandNodePath.add("sportals");
			CommandNode<CommandSourceStack> commandRoot = dispatcher.findNode(commandNodePath);

			commandRoot.addChild(configCommand);

			// list command
			LiteralCommandNode<CommandSourceStack> listCommand = Commands
					.literal("list")		// sportals list
					.requires(commandSource -> {
						return commandSource.hasPermission(4);
					})
					.executes(context -> {
						ClientPlayNetworking.send(SimplePortals.LIST_COMMAND_PACKET_ID, PacketByteBufs.empty());

						return 1;
					})
					.build();

			commandRoot.addChild(listCommand);
		});

		// Receiver for portal data from the server if a list command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.LIST_COMMAND_PACKET_ID, (minecraft, packetListener, receiveBuffer, sender) -> {
			List<PortalInfo> portals = receiveBuffer.readCollection(ArrayList::new, buffer -> {
				PortalInfo portal = new PortalInfo();
				portal.dimension = buffer.readResourceLocation();	// dimension
				portal.location = buffer.readBlockPos();			// location
				Address address = new Address();					// address
				address.deserializeNBT(buffer.readNbt());
				portal.address = address;
				portal.power = buffer.readInt();					// power

				return portal;
			});

			minecraft.execute(() -> minecraft.setScreen(new ListCommandGui(portals)));
		});
	}
}
