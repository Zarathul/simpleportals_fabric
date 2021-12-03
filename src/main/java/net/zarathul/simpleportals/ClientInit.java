package net.zarathul.simpleportals;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.zarathul.simpleportals.commands.ConfigCommandMode;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.gui.ConfigGui;
import net.zarathul.simpleportals.configuration.gui.ListCommandGui;
import net.zarathul.simpleportals.configuration.gui.PortalInfo;
import net.zarathul.simpleportals.registration.Address;

import java.util.ArrayList;
import java.util.List;

public class ClientInit implements ClientModInitializer
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
					.executes(context -> {
						FriendlyByteBuf sendBuffer = PacketByteBufs.create();
						sendBuffer.writeEnum(ConfigCommandMode.GetServerSettings);

						ClientPlayNetworking.send(SimplePortals.CONFIG_COMMAND_PACKET_ID, sendBuffer);

						return 1;
					})
					.build();

			ArrayList<String> commandNodePath = new ArrayList<>();
			commandNodePath.add("sportals");
			CommandNode<CommandSourceStack> commandRoot = dispatcher.findNode(commandNodePath);

			commandRoot.addChild(configCommand);

			// Receiver for server side settings if a config command was issued.
			ClientPlayNetworking.registerGlobalReceiver(SimplePortals.CONFIG_COMMAND_PACKET_ID, (minecraft, packetListener, receiveBuffer, sender) -> {
				Config.readServerSettings(Settings.class, receiveBuffer, minecraft.player);

				minecraft.execute(() -> minecraft.setScreen(new ConfigGui(new TextComponent("§nSimplePortals"), Settings.class, SimplePortals.MOD_ID, minecraft.player, player -> {
					FriendlyByteBuf sendBuffer = PacketByteBufs.create();
					sendBuffer.writeEnum(ConfigCommandMode.SetServerSettings);
					Config.writeServerSettings(Settings.class, sendBuffer, player);
					ClientPlayNetworking.send(SimplePortals.CONFIG_COMMAND_PACKET_ID, sendBuffer);
				})));
			});

			// list command
			LiteralCommandNode<CommandSourceStack> listCommand = Commands
					.literal("list")		// sportals list
					.requires(commandSource -> commandSource.hasPermission(4))
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
				portal.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buffer.readResourceLocation());	// dimension
				portal.location = buffer.readBlockPos();															// location
				Address address = new Address();																	// address
				address.deserializeNBT(buffer.readNbt());
				portal.address = address;
				portal.power = buffer.readInt();																	// power

				return portal;
			});

			minecraft.execute(() -> minecraft.setScreen(new ListCommandGui(portals)));
		});
	}
}
