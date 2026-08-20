package net.zarathul.simpleportals.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.gui.PortalListScreen;
import net.zarathul.simpleportals.network.payloads.ListCommandPayload;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandlers
{
	public static void register()
	{
		SimplePortals.LOG.info("Registering client side custom packet handlers.");

		ClientPlayNetworking.registerGlobalReceiver(ListCommandPayload.TYPE, ClientPacketHandlers::onClientListCommandPayloadReceived);
	}

	// Receiver for portal data from the server if a list command was issued.
	private static void onClientListCommandPayloadReceived(ListCommandPayload payload, ClientPlayNetworking.Context context)
	{
		var client = context.client();
		client.execute(() -> client.gui.setScreen(new PortalListScreen(payload.portals(), payload.portalListSettings())));
	}
}
