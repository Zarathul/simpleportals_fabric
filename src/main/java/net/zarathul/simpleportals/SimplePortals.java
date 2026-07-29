package net.zarathul.simpleportals;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
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
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.gui.PortalInfo;
import net.zarathul.simpleportals.items.ItemPortalActivator;
import net.zarathul.simpleportals.items.ItemPortalFrame;
import net.zarathul.simpleportals.items.ItemPowerGauge;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;

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
	public static final String CREATIVE_MODE_TAB_TITLE = "Simple Mods";
	public static final Identifier CREATIVE_MODE_TAB_ID = Identifier.fromNamespaceAndPath(SIMPLE_MODS_ID, "creative_tab");
	public static final CreativeModeTab creativeTab = MakeCreativeTab();

	// blocks
	public static final BlockPortal blockPortal = new BlockPortal(createBlockKey(BLOCK_PORTAL_NAME));
	public static final BlockPortalFrame blockPortalFrame = new BlockPortalFrame(createBlockKey(BLOCK_PORTAL_FRAME_NAME));
	public static final BlockPowerGauge blockPowerGauge = new BlockPowerGauge(createBlockKey(BLOCK_POWER_GAUGE_NAME));

	// items
	public static final ItemPortalFrame itemPortalFrame = new ItemPortalFrame(createItemKey(ITEM_PORTAL_FRAME_NAME));
	public static final ItemPowerGauge itemPowerGauge = new ItemPowerGauge(createItemKey(ITEM_POWER_GAUGE_NAME));
	public static final ItemPortalActivator itemPortalActivator = new ItemPortalActivator(createItemKey(ITEM_PORTAL_ACTIVATOR_NAME));

	// logger
	public static final Logger log = LogManager.getLogger(MOD_ID);

	// portal registry
	public static PortalRegistry portalRegistry;

	public static boolean onDedicatedServer;

	@Override
	public void onInitialize()
	{
		// Register custom block argument type for command parser.
		ArgumentTypeRegistry.registerArgumentType(Identifier.fromNamespaceAndPath(MOD_ID, "block_argument"), BlockArgument.class, SingletonArgumentInfo.contextFree(BlockArgument::block));

		// Register Blocks & Items.
		Registry.register(BuiltInRegistries.BLOCK, Utils.createModIdentifier(BLOCK_PORTAL_NAME), blockPortal);

		Registry.register(BuiltInRegistries.ITEM, Utils.createModIdentifier(ITEM_PORTAL_FRAME_NAME), itemPortalFrame);
		Registry.register(BuiltInRegistries.BLOCK, Utils.createModIdentifier(BLOCK_PORTAL_FRAME_NAME), blockPortalFrame);

		Registry.register(BuiltInRegistries.ITEM, Utils.createModIdentifier(ITEM_POWER_GAUGE_NAME), itemPowerGauge);
		Registry.register(BuiltInRegistries.BLOCK, Utils.createModIdentifier(BLOCK_POWER_GAUGE_NAME), blockPowerGauge);

		Registry.register(BuiltInRegistries.ITEM, Utils.createModIdentifier(ITEM_PORTAL_ACTIVATOR_NAME), itemPortalActivator);

		// Register creative tab.
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_ID, creativeTab);

		// Register custom network payloads.
		PayloadTypeRegistry.serverboundPlay().register(ListCommandPayload.TYPE, ListCommandPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ListCommandPayload.TYPE, ListCommandPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ConfigCommandPayload.TYPE, ConfigCommandPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfigCommandPayload.TYPE, ConfigCommandPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TpdCommandPayload.TYPE, TpdCommandPayload.CODEC);

		// Register Commands.
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			CommandPortals.register(dispatcher);
			CommandTeleport.register(dispatcher);
		});

		// Load portal registry data on overworld load.
		ServerLevelEvents.LOAD.register((server, world) -> {
			if (!world.isClientSide() && (world.dimension() == Level.OVERWORLD))
			{
				portalRegistry = world.getDataStorage().computeIfAbsent(PortalRegistry.TYPE);
			}
		});

		// Cache the power source item tag on data pack reload and server start.
		// The server start hook is necessary because when the config is initially loaded,
		// the Tag list does not exist yet.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResources, success) -> {
			// The validator already does the caching so just use that.
			Settings.powerSourceIsValid(Settings.powerSource());
		});

		// Load or create config file. Doing this at the start of onInitialize() would be preferable, but that leads to the validator of Settings.powerSource() failing.
		// This happens because registries are not fully set up at that time, which the validator queries.
		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			onDedicatedServer = server.isDedicatedServer();
			Config.reset();
			Settings.init();
			Config.loadOrCreateConfigFile(MOD_ID, onDedicatedServer);
		});

		// Necessary for dismantling blocks with the portal activator on sneak right-click.
		// Without this BlockPortalFrame.use() is never called when sneaking.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide() || (!player.isShiftKeyDown()) ||
				(((ServerPlayer)player).gameMode.getGameModeForPlayer() == GameType.SPECTATOR))
				return InteractionResult.PASS;

			BlockState blockState = world.getBlockState(hit.getBlockPos());
			Block hitBlock = blockState.getBlock();
			ItemStack usedItem = player.getItemInHand(hand);

			if ((usedItem.getItem() == itemPortalActivator) && ((hitBlock == blockPortalFrame) || (hitBlock == blockPowerGauge)))
			{
				InteractionResult result = blockState.useItemOn(usedItem, world, player, hand, hit);
				return result;
			}

			return InteractionResult.PASS;
		});

		// Server side receiver for the config command. Stores the received settings in the config of the server,
		// assuming the player has the required permissions.
		ServerPlayNetworking.registerGlobalReceiver(ConfigCommandPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			Config.readServerSettings(false, payload.values, player);
			Config.save(MOD_ID, true);
		});

		// Server side receiver for clicking on the teleport to portal button in the ListCommandGui.
		// Responsible for actually teleporting the client around.
		ServerPlayNetworking.registerGlobalReceiver(TpdCommandPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			var I18N = Language.getInstance();

			if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
			{
				player.sendSystemMessage(Component.translatable("missing_permission"));
				return;
			}

			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension);
			BlockPos location = payload.location;
			ServerLevel destinationLevel = ctx.server().getLevel(dimension);

			if (destinationLevel == null)
			{
				var localizedMessage = String.format(I18N.getOrDefault("dimension_missing"), dimension.identifier());
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			List<Portal> portals = portalRegistry.getPortalsAt(location, dimension);

			if (portals.isEmpty())
			{
				var localizedMessage = String.format(I18N.getOrDefault("portal_missing"), dimension.identifier(), location);
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			Portal destinationPortal = portals.getFirst();
			PortalRegistry.TeleportationDestination destination = PortalRegistry.getTeleportDestination(destinationPortal, destinationLevel, player);

			if (destination == null)
			{
				var localizedMessage = String.format(I18N.getOrDefault("portal_blocked"), destinationPortal.asReadableString());
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			player.teleportTo(
				destinationLevel,
				destination.pos().getX() + 0.5d,
				destination.pos().getY(),
				destination.pos().getZ() + 0.5d,
				Set.of(),
				destination.facing().toYRot(),
				player.xRotO,
				false
			);
		});
	}

	private static ResourceKey<Block> createBlockKey(String name)
	{
		return ResourceKey.create(Registries.BLOCK, Utils.createModIdentifier(name));
	}

	private static ResourceKey<Item> createItemKey(String name)
	{
		return ResourceKey.create(Registries.ITEM, Utils.createModIdentifier(name));
	}

	public static CreativeModeTab MakeCreativeTab()
	{
		var simpleModsTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(CREATIVE_MODE_TAB_ID);

		if (simpleModsTab.isPresent()) return simpleModsTab.get().value();

		return FabricCreativeModeTab.builder()
			.title(Component.literal(CREATIVE_MODE_TAB_TITLE))
			.icon(() -> new ItemStack(blockPortalFrame))
			.displayItems((parameters, output) -> {
				output.accept(itemPortalFrame);
				output.accept(itemPowerGauge);
				output.accept(itemPortalActivator);
			})
			.build();
	}

	public record ListCommandPayload(List<PortalInfo> portals) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("list_command");
		public static final CustomPacketPayload.Type<ListCommandPayload> TYPE = new CustomPacketPayload.Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, ListCommandPayload> CODEC = StreamCodec.composite(PortalInfo.LIST_STREAM_CODEC, ListCommandPayload::portals, ListCommandPayload::new);

		public @NonNull Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}
	}

	public record ConfigCommandPayload(List<Config.ConfigValue> values, boolean fromDedicatedServer) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("config_command");
		public static final CustomPacketPayload.Type<ConfigCommandPayload> TYPE = new CustomPacketPayload.Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, ConfigCommandPayload> CODEC = StreamCodec.composite(
				Config.LIST_STREAM_CODEC, ConfigCommandPayload::values,
				ByteBufCodecs.BOOL, ConfigCommandPayload::fromDedicatedServer,
				ConfigCommandPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}
	}

	public record TpdCommandPayload(Identifier dimension, BlockPos location) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("tpd_command");
		public static final CustomPacketPayload.Type<TpdCommandPayload> TYPE = new CustomPacketPayload.Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, TpdCommandPayload> CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, TpdCommandPayload::dimension,
				BlockPos.STREAM_CODEC, TpdCommandPayload::location,
				TpdCommandPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}
	}
}