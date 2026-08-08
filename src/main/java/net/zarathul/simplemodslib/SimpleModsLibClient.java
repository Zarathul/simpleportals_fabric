package net.zarathul.simplemodslib;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simplemodslib.api.configuration.ConfigSetting;

public class SimpleModsLibClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
//		Config.initialize(SimpleModsLib.MOD_ID, "Simple Mods Lib", false, () -> {
//			Config.addBool(Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID, "test_bool"), false, "A boolean test value", true, 0, false);
//			Config.addInt(Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID, "test_int"), 41, ConfigSetting.INT_BETWEEN(0, 41), "A integer test value", "thecategory", false, 0, true);
//			Config.addString(Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID, "test_string"), "The quick brown fox jumps over the lazy dog", "This is a string!", "theothercategory", false, 4, true);
//		});
//		Config.registerClientSideNetworking();
	}
}
