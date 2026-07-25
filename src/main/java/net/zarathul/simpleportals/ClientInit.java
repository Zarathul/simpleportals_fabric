package net.zarathul.simpleportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.zarathul.simpleportals.SimplePortals.ConfigCommandPayload;
import net.zarathul.simpleportals.commands.ConfigCommandMode;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.ConfigValue;
import net.zarathul.simpleportals.configuration.gui.ConfigGui;
import net.zarathul.simpleportals.configuration.gui.ListCommandGui;

import java.util.ArrayList;
import java.util.List;

public class ClientInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		//BlockRenderLayerMap.INSTANCE.putBlock(SimplePortals.blockPortal, RenderType.translucent());

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) -> {
			if (stack.getItem() == SimplePortals.itemPortalFrame)
			{
				SimplePortals.itemPortalFrame.addTooltip(stack, tooltipContext, tooltipFlag, lines);
			}
			else if (stack.getItem() == SimplePortals.itemPowerGauge)
			{
				SimplePortals.itemPowerGauge.addTooltip(stack, tooltipContext, tooltipFlag, lines);
			}
			else if (stack.getItem() == SimplePortals.itemPortalActivator)
			{
				SimplePortals.itemPortalActivator.addTooltip(stack, tooltipContext, tooltipFlag, lines);
			}
		});

		// Register client side only commands. As of 1.18.2 the command tree here has to mirror the tree in CommandPortals, otherwise those commands won't work.
		// As of 1.19.2 this seems no longer necessary.
		// TODO: Remove if no longer needed.
//		ClientCommandManager.getActiveDispatcher().register(
//			ClientCommandManager.literal("sportals")
//			.then(
//				ClientCommandManager.literal("config")	// sportals config
//				.executes(context -> {
//					FriendlyByteBuf sendBuffer = PacketByteBufs.create();
//					sendBuffer.writeEnum(ConfigCommandMode.GetServerSettings);
//
//					ClientPlayNetworking.send(SimplePortals.CONFIG_COMMAND_PACKET_ID, sendBuffer);
//
//					return 1;
//				})
//			)
//			.then(
//				ClientCommandManager.literal("list")
//				.requires(commandSource -> commandSource.hasPermission(4))
//				.executes(context -> {
//					ClientPlayNetworking.send(SimplePortals.LIST_COMMAND_PACKET_ID, PacketByteBufs.empty());
//
//					return 1;
//				})
//			)
//			.then(
//				ClientCommandManager.literal("deactivate")
//				.then(
//					ClientCommandManager.argument("address1", BlockArgument.block())
//					.then(
//						ClientCommandManager.argument("address2", BlockArgument.block())
//						.then(
//							ClientCommandManager.argument("address3", BlockArgument.block())
//							.then(
//								ClientCommandManager.argument("address4", BlockArgument.block())
//								.then(
//									ClientCommandManager.argument("dimension", DimensionArgument.dimension())		// sportals deactivate <addressBlockId> <addressBlockId> <addressBlockId> <addressBlockId> [dimension]
//								)
//							)
//						)
//					)
//				)
//				.then(
//					ClientCommandManager.argument("position", BlockPosArgument.blockPos())
//					.then(
//						ClientCommandManager.argument("dimension", DimensionArgument.dimension())		// sportals deactivate <x> <y> <z> [dimension]
//					)
//				)
//			)
//			.then(
//				ClientCommandManager.literal("power")
//				.then(
//					ClientCommandManager.literal("add")
//					.then(
//						ClientCommandManager.argument("amount", IntegerArgumentType.integer(1))
//						.then(
//							ClientCommandManager.argument("position", BlockPosArgument.blockPos())
//							.then(
//								ClientCommandManager.argument("dimension", DimensionArgument.dimension())		// sportals power add <amount> <x> <y> <z> [dimension]
//							)
//						)
//					)
//				)
//				.then(
//					ClientCommandManager.literal("remove")
//					.then(
//						ClientCommandManager.argument("amount", IntegerArgumentType.integer(1))
//						.then(
//							ClientCommandManager.argument("position", BlockPosArgument.blockPos())
//							.then(
//								ClientCommandManager.argument("dimension", DimensionArgument.dimension())		// sportals power remove <amount> <x> <y> <z> [dimension]
//							)
//						)
//					)
//				)
//				.then(
//					ClientCommandManager.literal("get")
//					.then(
//						ClientCommandManager.argument("position", BlockPosArgument.blockPos())
//						.then(
//							ClientCommandManager.argument("dimension", DimensionArgument.dimension())		// sportals power get <x> <y> <z> [dimension]
//						)
//					)
//				)
//				.then(
//					ClientCommandManager.literal("items")
//				)0
//			)
//			.then(
//				ClientCommandManager.literal("cooldown")
//				.then(
//					ClientCommandManager.argument("player", EntityArgument.player())		// sportals cooldown <player>
//				)
//			)
//			.then(
//				ClientCommandManager.literal("clear")
//				.then(
//					ClientCommandManager.literal("confirmed")		// sportals clear confirmed
//				)
//			)
//		);

		// Receiver for server side settings if a config command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.ConfigCommandPayload.TYPE, (payload, ctx) -> {
			Config.readServerSettings(Settings.class, payload.values(), ctx.player());

			var client = ctx.client();
			client.execute(() -> client.gui.setScreen(new ConfigGui(Component.literal("§nSimplePortals"), Settings.class, SimplePortals.MOD_ID, client.player, player -> {
				List<ConfigValue> configValues = new ArrayList<>();
				Config.writeServerSettings(Settings.class, configValues, player);

				ConfigCommandPayload outgoingPayload = new ConfigCommandPayload(ConfigCommandMode.SetServerSettings, configValues);
				ClientPlayNetworking.send(outgoingPayload);
			})));
		});

		// Receiver for portal data from the server if a list command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.ListCommandPayload.TYPE, (payload, ctx) -> {
			var client = ctx.client();
			client.execute(() -> client.gui.setScreen(new ListCommandGui(payload.portals())));
		});
	}
}
