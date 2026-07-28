package net.zarathul.simpleportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.zarathul.simpleportals.SimplePortals.ConfigCommandPayload;
import net.zarathul.simpleportals.commands.ConfigCommandMode;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.gui.ConfigGui;
import net.zarathul.simpleportals.configuration.gui.ListCommandGui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
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

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, builderContext) -> {
			dispatcher.register(
				ClientCommands.literal("sportals")
					.then(
						ClientCommands.literal("config")	// sportals config
							.executes(context -> {
								var payload = new ConfigCommandPayload(ConfigCommandMode.GetServerSettings, Collections.emptyList());
								ClientPlayNetworking.send(payload);

								return 1;
							})
					)
					.then(
						ClientCommands.literal("list")
							.requires(commandSource -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
							.executes(context -> {
								var payload = new SimplePortals.ListCommandPayload(Collections.emptyList());
								ClientPlayNetworking.send(payload);

								return 1;
							})
					)
			);
		});

		// Receiver for server side settings if a config command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.ConfigCommandPayload.TYPE, (payload, ctx) -> {
			Config.readServerSettings(payload.values(), ctx.player());

			var client = ctx.client();
			var settings = Config.getSettings();
			client.execute(() -> client.gui.setScreen(new ConfigGui(settings, Component.literal("§nSimplePortals"), SimplePortals.MOD_ID, client.player, player -> {
				List<Config.ConfigValue> configValues = new ArrayList<>();
				Config.writeServerSettings(configValues, player);

				if (!configValues.isEmpty())
				{
					ConfigCommandPayload outgoingPayload = new ConfigCommandPayload(ConfigCommandMode.SetServerSettings, configValues);
					ClientPlayNetworking.send(outgoingPayload);
				}
			})));
		});

		// Receiver for portal data from the server if a list command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.ListCommandPayload.TYPE, (payload, ctx) -> {
			var client = ctx.client();
			client.execute(() -> client.gui.setScreen(new ListCommandGui(payload.portals())));
		});
	}
}
