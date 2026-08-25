package net.zarathul.simpleportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simpleportals.items.ModItems;
import net.zarathul.simpleportals.network.ClientPacketHandlers;

public class ClientInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		// Config
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
			// Loading the config earlier can cause the validator of the power_source setting to fail, if a fabric tag is set,
			// because those are loaded later.
			Config.initialize(SimplePortals.MOD_ID, SimplePortals.CONFIG_GUI_TITLE, false, Settings::init);
		});

		Config.registerClientSideNetworking();

		// Custom Packets
		ClientPacketHandlers.register();

		ModItems.registerTooltips();
	}
}
