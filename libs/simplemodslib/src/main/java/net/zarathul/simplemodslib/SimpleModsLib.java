package net.zarathul.simplemodslib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simplemodslib.api.configuration.ConfigSetting;

public class SimpleModsLib implements ModInitializer
{
	public static final String MOD_ID = "simplemodslib";

	@Override
	public void onInitialize()
	{
//		Config.registerServerSideNetworking();
//
//		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
//			Config.initialize(MOD_ID, "Simple Mods Lib", server.isDedicatedServer(), () -> {
//				Config.addBool(Identifier.fromNamespaceAndPath(MOD_ID, "test_bool"), false, "A boolean test value", true, 0, false);
//				Config.addInt(Identifier.fromNamespaceAndPath(MOD_ID, "test_int"), 41, ConfigSetting.INT_BETWEEN(0, 41), "A integer test value", "thecategory", false, 0, true);
//				Config.addString(Identifier.fromNamespaceAndPath(MOD_ID, "test_string"), "The quick brown fox jumps over the lazy dog", "This is a string!", "theothercategory", false, 4, true);
//			});
//		});
//
//		CommandRegistrationCallback.EVENT.register(Config::registerCommand);
	}
}
