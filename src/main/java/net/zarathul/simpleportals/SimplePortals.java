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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zarathul.simplemods.api.configuration.Config;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.blocks.BlockPortalFrame;
import net.zarathul.simpleportals.blocks.BlockPowerGauge;
import net.zarathul.simpleportals.commands.CommandPortals;
import net.zarathul.simpleportals.commands.CommandTeleport;
import net.zarathul.simpleportals.commands.arguments.BlockArgument;
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.gui.PortalInfo;
import net.zarathul.simpleportals.gui.PortalListSettings;
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
import java.util.stream.Collectors;

public class SimplePortals implements ModInitializer
{
	// ids and titles
	public static final String MOD_ID = "simpleportals";
	public static final String SIMPLE_MODS_ID = "simplemods";
	public static final String CONFIG_GUI_TITLE = "§nSimplePortals";

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

		// Register packet handlers.
		registerCustomPackerHandlers();

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
			Config.initialize(MOD_ID, CONFIG_GUI_TITLE, server.isDedicatedServer(), Settings::init);
		});

		// Necessary for dismantling blocks with the portal activator on sneak right-click.
		// Without this BlockPortalFrame.use() is never called when sneaking.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide() || (!player.isShiftKeyDown()) || player.isSpectator())
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
	}

	private static void registerCustomPackerHandlers()
	{
		Config.registerServerSideNetworking();

		PayloadTypeRegistry.serverboundPlay().register(TpdCommandPayload.TYPE, TpdCommandPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetPortalPowerPayload.TYPE, SetPortalPowerPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DeactivatePortalPayload.TYPE, DeactivatePortalPayload.STREAM_CODEC);

		PayloadTypeRegistry.clientboundPlay().register(ListCommandPayload.TYPE, ListCommandPayload.STREAM_CODEC);

		// Handle "teleport" request from the client. Triggered by clicking the teleport to portal button in the PortalListScreen.
		// Responsible for actually teleporting the client around.
		ServerPlayNetworking.registerGlobalReceiver(TpdCommandPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			var I18N = Language.getInstance();

			if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
			{
				player.sendSystemMessage(Component.translatable("error.missing_permission"));
				return;
			}

			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension);
			BlockPos location = payload.location;
			ServerLevel destinationLevel = ctx.server().getLevel(dimension);

			if (destinationLevel == null)
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.dimension_missing"), dimension.identifier());
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			List<Portal> portals = portalRegistry.getPortalsAt(location, dimension);

			if (portals.isEmpty())
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.portal_missing"), dimension.identifier(), location);
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			Portal destinationPortal = portals.getFirst();
			PortalRegistry.TeleportationDestination destination = PortalRegistry.getTeleportDestination(destinationPortal, destinationLevel, player);

			if (destination == null)
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.portal_blocked"), destinationPortal.asReadableString());
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

		// Handle "set portal power" request from the client. Triggered by clicking the set power button in the PortalListScreen.
		ServerPlayNetworking.registerGlobalReceiver(SetPortalPowerPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			var I18N = Language.getInstance();

			if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
			{
				player.sendSystemMessage(Component.translatable("error.missing_permission"));
				return;
			}

			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension);
			BlockPos location = payload.location;
			List<Portal> portals = portalRegistry.getPortalsAt(location, dimension);

			if (portals.isEmpty())
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.portal_missing"), dimension.identifier(), location);
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			Portal targetPortal = portals.getFirst();
			portalRegistry.setPower(targetPortal, payload.value);
			ListCommandPayload.send(player, payload.portalListSettings);
		});

		// Handle "deactivate portal" request from the client. Triggered by clicking the deactivate portal button in the PortalListScreen.
		ServerPlayNetworking.registerGlobalReceiver(DeactivatePortalPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			var I18N = Language.getInstance();

			if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
			{
				player.sendSystemMessage(Component.translatable("error.missing_permission"));
				return;
			}

			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension);
			BlockPos location = payload.location;
			ServerLevel destinationLevel = ctx.server().getLevel(dimension);

			if (destinationLevel == null)
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.dimension_missing"), dimension.identifier());
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			List<Portal> portals = portalRegistry.getPortalsAt(location, dimension);

			if (portals.isEmpty())
			{
				var localizedMessage = String.format(I18N.getOrDefault("error.portal_missing"), dimension.identifier(), location);
				player.sendSystemMessage(Component.literal(localizedMessage));
				return;
			}

			portalRegistry.deactivatePortal(destinationLevel, payload.location);
			ListCommandPayload.send(player, payload.portalListSettings);
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

	// Custom packets

	public record ListCommandPayload(List<PortalInfo> portals, PortalListSettings portalListSettings) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("list_command");
		public static final CustomPacketPayload.Type<ListCommandPayload> TYPE = new CustomPacketPayload.Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, ListCommandPayload> STREAM_CODEC = StreamCodec.composite(
			PortalInfo.LIST_STREAM_CODEC, ListCommandPayload::portals,
			PortalListSettings.STREAM_CODEC, ListCommandPayload::portalListSettings,
			ListCommandPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}

		public static void send(ServerPlayer player)
		{
			send(player, PortalListSettings.DEFAULT);
		}

		public static void send(ServerPlayer player, PortalListSettings portalListSettings)
		{
			// Generate a PortalInfo for every registered portal.
			// Using getInnerCornerPos() instead of pos() is crucial here, because corners can be shared by other portals.
			// Positions inside the frame cannot. Only by doing this, is the client able to uniquely identify the portal to
			// the server later, when asking for the power to be set or for the portal to be deactivated.
			List<PortalInfo> portals = portalRegistry.getAllPortals().stream()
				.map(portal -> new PortalInfo(
					portal.dimension(),
					portal.corner1().getInnerCornerPos(),
					portal.address(),
					portalRegistry.getPortalPower(portal))
				)
				.collect(Collectors.toList());

			SimplePortals.ListCommandPayload outgoingPayload = new SimplePortals.ListCommandPayload(portals, portalListSettings);
			ServerPlayNetworking.send(player, outgoingPayload);
		}
	}

	public record SetPortalPowerPayload(Identifier dimension, BlockPos location, int value, PortalListSettings portalListSettings) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("set_portal_power");
		public static final CustomPacketPayload.Type<SetPortalPowerPayload> TYPE = new CustomPacketPayload.Type<>(ID);

		public static final StreamCodec<FriendlyByteBuf, SetPortalPowerPayload> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, SetPortalPowerPayload::dimension,
			BlockPos.STREAM_CODEC, SetPortalPowerPayload::location,
			ByteBufCodecs.INT, SetPortalPowerPayload::value,
			PortalListSettings.STREAM_CODEC, SetPortalPowerPayload::portalListSettings,
			SetPortalPowerPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record DeactivatePortalPayload(Identifier dimension, BlockPos location, PortalListSettings portalListSettings) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("deactivate_portal");
		public static final CustomPacketPayload.Type<DeactivatePortalPayload> TYPE = new CustomPacketPayload.Type<>(ID);

		public static final StreamCodec<FriendlyByteBuf, DeactivatePortalPayload> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, DeactivatePortalPayload::dimension,
			BlockPos.STREAM_CODEC, DeactivatePortalPayload::location,
			PortalListSettings.STREAM_CODEC, DeactivatePortalPayload::portalListSettings,
			DeactivatePortalPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record TpdCommandPayload(Identifier dimension, BlockPos location) implements CustomPacketPayload
	{
		public static final Identifier ID = Utils.createModIdentifier("tpd_command");
		public static final CustomPacketPayload.Type<TpdCommandPayload> TYPE = new CustomPacketPayload.Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, TpdCommandPayload> STREAM_CODEC = StreamCodec.composite(
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