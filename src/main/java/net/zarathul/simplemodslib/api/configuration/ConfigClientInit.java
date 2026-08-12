package net.zarathul.simplemodslib.api.configuration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ConfigClientInit
{
	private ConfigClientInit() {}

	@Environment(EnvType.CLIENT)
	public static void registerClientSideNetworking()
	{
		// Receiver for server side settings if a config command was issued.
		ClientPlayNetworking.registerGlobalReceiver(Config.ConfigCommandPayload.TYPE, (payload, ctx) -> {
			var client = ctx.client();
			boolean fromRemoteServer = payload.fromDedicatedServer() || !Minecraft.getInstance().hasSingleplayerServer();
			List<ConfigSetting> settings;

			// If the incoming settings come from a remote server, meaning a dedicated or an integrated server opened by someone else,
			// read them and display the merged resultset of those and local clientOnly settings in the config gui. Otherwise, display
			// all local settings, clientOnly or not.
			if (fromRemoteServer)
			{
				Config.readServerSettings(fromRemoteServer, payload.values(), ctx.player());
				settings = Config.getMergedSettings(payload.modId(), payload.values());
			}
			else
			{
				settings = Config.getSettings(payload.modId());
			}

			Config.ModInfo modInfo = Config.getModInfo(payload.modId());
			if (modInfo == null) return;

			client.execute(() -> client.gui.setScreen(new ConfigScreen(settings, Component.literal(modInfo.configGuiTitle()), payload.modId(), client.player, player -> {
				// Send the potentially edited settings back to the server. But only if they came from a server in the first place.
				if (fromRemoteServer)
				{
					List<Config.ConfigValue> configValues = new ArrayList<>();
					Config.writeServerSettings(payload.modId(), fromRemoteServer, configValues, player);

					if (!configValues.isEmpty())
					{
						Config.ConfigCommandPayload outgoingPayload = new Config.ConfigCommandPayload(payload.modId(), configValues, false);
						ClientPlayNetworking.send(outgoingPayload);
					}
				}
			})));
		});
	}
}
