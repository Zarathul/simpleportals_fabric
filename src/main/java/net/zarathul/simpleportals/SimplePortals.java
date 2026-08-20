package net.zarathul.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simpleportals.blocks.ModBlocks;
import net.zarathul.simpleportals.commands.CommandPortals;
import net.zarathul.simpleportals.commands.CommandTeleport;
import net.zarathul.simpleportals.commands.arguments.BlockArgument;
import net.zarathul.simpleportals.items.ModItems;
import net.zarathul.simpleportals.network.ServerPacketHandlers;
import net.zarathul.simpleportals.registration.PortalRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimplePortals implements ModInitializer
{
	public static final String MOD_ID = "simpleportals";
	public static final String CONFIG_GUI_TITLE = "§nSimplePortals";
	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	public static PortalRegistry PORTAL_REGISTRY;

	public static Identifier modId(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize()
	{
		// Config
		Config.registerServerSideNetworking();
		// Load or create config file. Doing this at the start of onInitialize() would be preferable, but that leads to the validator of Settings.powerSource() failing.
		// This happens because registries are not fully set up at that time, which the validator queries.
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			Config.initialize(MOD_ID, CONFIG_GUI_TITLE, server.isDedicatedServer(), Settings::init);
		});

		// Blocks & Items
		ModBlocks.init();
		ModItems.init();

		UseBlockCallback.EVENT.register(ModBlocks::useBlockCallback);

		// Custom Packets
		ServerPacketHandlers.registerPayloads();
		ServerPacketHandlers.register();

		// Custom block argument type for command parser.
		ArgumentTypeRegistry.registerArgumentType(Identifier.fromNamespaceAndPath(MOD_ID, "block_argument"), BlockArgument.class, SingletonArgumentInfo.contextFree(BlockArgument::block));

		// Register Commands.
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			CommandPortals.register(dispatcher);
			CommandTeleport.register(dispatcher);
		});

		// Load portal registry data on overworld load.
		ServerLevelEvents.LOAD.register((server, world) -> {
			if (!world.isClientSide() && (world.dimension() == Level.OVERWORLD))
			{
				PORTAL_REGISTRY = world.getDataStorage().computeIfAbsent(PortalRegistry.TYPE);
			}
		});

		// Cache the power source item tag on data pack reload and server start.
		// The server start hook is necessary because when the config is initially loaded,
		// the Tag list does not exist yet.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResources, success) -> {
			// The validator already does the caching so just use that.
			Settings.powerSourceIsValid(Settings.powerSource());
		});
	}
}