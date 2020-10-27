package net.zarathul.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.blocks.BlockPortalFrame;
import net.zarathul.simpleportals.blocks.BlockPowerGauge;
import net.zarathul.simpleportals.commands.CommandPortals;
import net.zarathul.simpleportals.commands.CommandTeleport;
import net.zarathul.simpleportals.commands.arguments.BlockArgument;
import net.zarathul.simpleportals.common.PortalWorldSaveData;
import net.zarathul.simpleportals.common.TeleportTask;
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.items.ItemPortalActivator;
import net.zarathul.simpleportals.items.ItemPortalFrame;
import net.zarathul.simpleportals.items.ItemPowerGauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class SimplePortals implements ModInitializer
{
	// constants
	public static final String MOD_ID = "simpleportals";
	public static final String SIMPLE_MODS_ID = "simplemods";

	// block and item names
	public static final String BLOCK_PORTAL_NAME = "portal";
	public static final String BLOCK_PORTAL_FRAME_NAME = "portal_frame";
	public static final String BLOCK_POWER_GAUGE_NAME = "power_gauge";
	public static final String ITEM_PORTAL_FRAME_NAME = "portal_frame";
	public static final String ITEM_POWER_GAUGE_NAME = "power_gauge";
	public static final String ITEM_PORTAL_ACTIVATOR_NAME = "portal_activator";

	// creative tab
	public static final CreativeModeTab creativeTab = MakeCreativeTab();

	// blocks
	public static final BlockPortal blockPortal = new BlockPortal();
	public static final BlockPortalFrame blockPortalFrame = new BlockPortalFrame();
	public static final BlockPowerGauge blockPowerGauge = new BlockPowerGauge();

	// items
	public static final ItemPortalActivator itemPortalActivator = new ItemPortalActivator();
	public static final BlockItem itemPortalFrame = new ItemPortalFrame();
	public static final BlockItem itemPowerGauge = new ItemPowerGauge();

	// world save data handler
	public static PortalWorldSaveData portalSaveData;

	// logger
	public static final Logger log = LogManager.getLogger(MOD_ID);

	public static LinkedBlockingQueue<TeleportTask> TELEPORT_QUEUE = new LinkedBlockingQueue<>();

	@Override
	public void onInitialize() {
		// Register custom block argument type for command parser.
		ArgumentTypes.register(MOD_ID + ":block_argument", BlockArgument.class, new EmptyArgumentSerializer(BlockArgument::block));

		// Load or create config file.
		Config.loadOrCreate(MOD_ID, Settings.class);

		// Register Blocks & Items.
		Registry.register(Registry.BLOCK, new ResourceLocation(MOD_ID, BLOCK_PORTAL_NAME), blockPortal);

		Registry.register(Registry.BLOCK, new ResourceLocation(MOD_ID, BLOCK_PORTAL_FRAME_NAME), blockPortalFrame);
		Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, ITEM_PORTAL_FRAME_NAME), itemPortalFrame);

		Registry.register(Registry.BLOCK, new ResourceLocation(MOD_ID, BLOCK_POWER_GAUGE_NAME), blockPowerGauge);
		Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, ITEM_POWER_GAUGE_NAME), itemPowerGauge);

		Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, ITEM_PORTAL_ACTIVATOR_NAME), itemPortalActivator);

		// Register Commands.
		CommandRegistrationCallback.EVENT.register((dispatcher, isDedicatedServer) -> {
			CommandPortals.register(dispatcher);
			CommandTeleport.register(dispatcher);
		});

		// Handle teleportation queue in server tick event.
		ServerTickEvents.END_SERVER_TICK.register((server) -> {
			TeleportTask task;
			MinecraftServer mcServer;

			while (true)
			{
				task = SimplePortals.TELEPORT_QUEUE.peek();
				if (task == null) return;

				mcServer = task.player.getServer();

				if (mcServer == null)
				{
					// No point in keeping the task if there's no server. Should never happen but who knows.
					TELEPORT_QUEUE.poll();
				}
				else if (mcServer != server)
				{
					// Wrong server. No idea if this even can happen but let's be sure.
					return;
				}
				else if (mcServer.getTickCount() > (task.creationTickCount + Settings.playerTeleportationDelay))
				{
					// Task is due.
					TELEPORT_QUEUE.poll();
					Utils.teleportTo(task.player, task.dimension, task.pos, task.facing);
				}
				else
				{
					// Task was not due yet, so if there are others they won't be either.
					return;
				}
			}
		});

		// Load portal registry data on overworld load.
		ServerWorldEvents.LOAD.register((server, world) -> {
			if (!world.isClientSide && (world.dimension() == Level.OVERWORLD))
			{
				portalSaveData = PortalWorldSaveData.get(world);
			}
		});

		// Cache the power source item tag on data pack reload and server start.
		// The server start hook is necessary because when the config is initially loaded,
		// the Tag list does not exist yet.

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResources, success) -> {
			// The validator already does the caching so just use that.
			Settings.powerSourceValidator(Settings.powerSource);
		});

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			// The validator already does the caching so just use that.
			Settings.powerSourceValidator(Settings.powerSource);
		});

		// Necessary for dismantling blocks with the portal activator on sneak right-click.
		// Without this BlockPortalFrame.use() is never called when sneaking.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide() || (!player.isShiftKeyDown()) ||
				(((ServerPlayer)player).gameMode.getGameModeForPlayer() == GameType.SPECTATOR))
				return InteractionResult.PASS;

			BlockState blockState = world.getBlockState(hit.getBlockPos());
			Block hitBlock = blockState.getBlock();
			Item usedItem = player.getItemInHand(hand).getItem();

			if ((usedItem == itemPortalActivator) && ((hitBlock == blockPortalFrame)) || (hitBlock == blockPowerGauge))
			{
				InteractionResult result = blockState.use(world, player, hand, hit);
				return result;
			}

			return  InteractionResult.PASS;
		});
	}

	public static CreativeModeTab MakeCreativeTab()
	{
		return Arrays.stream(CreativeModeTab.TABS)
			.filter(tab -> tab.getRecipeFolderName().equals(SIMPLE_MODS_ID))
			.findFirst()
			.orElseGet(() -> FabricItemGroupBuilder.build(
				new ResourceLocation(MOD_ID, SIMPLE_MODS_ID),
				() -> new ItemStack(blockPortalFrame)).setRecipeFolderName(SIMPLE_MODS_ID)
			);
	}
}
