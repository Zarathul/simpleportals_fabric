package net.zarathul.simpleportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.zarathul.simplemods.api.configuration.Config;
import net.zarathul.simpleportals.gui.PortalListScreen;

public class ClientInit implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		// Loading the config earlier can cause the validator of the power_source setting to fail, if a fabric tag is set,
		// because those are loaded later.
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
			Config.initialize(SimplePortals.MOD_ID, SimplePortals.CONFIG_GUI_TITLE, false, Settings::init);
		});

		Config.registerClientSideNetworking();

		// Set tooltips for items.
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

		// Receiver for portal data from the server if a list command was issued.
		ClientPlayNetworking.registerGlobalReceiver(SimplePortals.ListCommandPayload.TYPE, (payload, ctx) -> {
			var client = ctx.client();
			client.execute(() -> client.gui.setScreen(new PortalListScreen(payload.portals(), payload.portalListSettings())));
		});
	}
}
