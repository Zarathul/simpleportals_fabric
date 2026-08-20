package net.zarathul.simpleportals.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.network.payloads.DeactivatePortalPayload;
import net.zarathul.simpleportals.network.payloads.ListCommandPayload;
import net.zarathul.simpleportals.network.payloads.SetPortalPowerPayload;
import net.zarathul.simpleportals.network.payloads.TpdCommandPayload;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.List;
import java.util.Set;

public final class ServerPacketHandlers
{
	public static void registerPayloads()
	{
		SimplePortals.LOG.info("Registering custom packets.");

		PayloadTypeRegistry.serverboundPlay().register(TpdCommandPayload.TYPE, TpdCommandPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetPortalPowerPayload.TYPE, SetPortalPowerPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DeactivatePortalPayload.TYPE, DeactivatePortalPayload.STREAM_CODEC);

		PayloadTypeRegistry.clientboundPlay().register(ListCommandPayload.TYPE, ListCommandPayload.STREAM_CODEC);
	}

	public static void register()
	{
		SimplePortals.LOG.info("Registering server side custom packet handlers.");

		ServerPlayNetworking.registerGlobalReceiver(TpdCommandPayload.TYPE, ServerPacketHandlers::onServerTpdCommandPayloadReceived);
		ServerPlayNetworking.registerGlobalReceiver(SetPortalPowerPayload.TYPE, ServerPacketHandlers::onServerSetPortalPowerPayloadReceived);
		ServerPlayNetworking.registerGlobalReceiver(DeactivatePortalPayload.TYPE, ServerPacketHandlers::onServerDeactivatePortalPayloadReceived);
	}

	// Handle "teleport" request from the client. Triggered by clicking the teleport to portal button in the PortalListScreen.
	// Responsible for actually teleporting the client around.
	private static void onServerTpdCommandPayloadReceived(TpdCommandPayload payload, ServerPlayNetworking.Context context)
	{
		var player = context.player();
		var I18N = Language.getInstance();

		if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
		{
			player.sendSystemMessage(Component.translatable("error.missing_permission"));
			return;
		}

		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension());
		BlockPos location = payload.location();
		ServerLevel destinationLevel = context.server().getLevel(dimension);

		if (destinationLevel == null)
		{
			var localizedMessage = String.format(I18N.getOrDefault("error.dimension_missing"), dimension.identifier());
			player.sendSystemMessage(Component.literal(localizedMessage));
			return;
		}

		List<Portal> portals = SimplePortals.PORTAL_REGISTRY.getPortalsAt(location, dimension);

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
	}

	// Handle "set portal power" request from the client. Triggered by clicking the set power button in the PortalListScreen.
	private static void onServerSetPortalPowerPayloadReceived(SetPortalPowerPayload payload, ServerPlayNetworking.Context context)
	{
		var player = context.player();
		var I18N = Language.getInstance();

		if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
		{
			player.sendSystemMessage(Component.translatable("error.missing_permission"));
			return;
		}

		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension());
		BlockPos location = payload.location();
		List<Portal> portals = SimplePortals.PORTAL_REGISTRY.getPortalsAt(location, dimension);

		if (portals.isEmpty())
		{
			var localizedMessage = String.format(I18N.getOrDefault("error.portal_missing"), dimension.identifier(), location);
			player.sendSystemMessage(Component.literal(localizedMessage));
			return;
		}

		Portal targetPortal = portals.getFirst();
		SimplePortals.PORTAL_REGISTRY.setPower(targetPortal, payload.value());
		SimplePortals.PORTAL_REGISTRY.updatePowerGauges(context.server().getLevel(dimension), targetPortal);

		ListCommandPayload.send(player, payload.portalListSettings());
	}

	// Handle "deactivate portal" request from the client. Triggered by clicking the deactivate portal button in the PortalListScreen.
	private static void onServerDeactivatePortalPayloadReceived(DeactivatePortalPayload payload, ServerPlayNetworking.Context context)
	{
		var player = context.player();
		var I18N = Language.getInstance();

		if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
		{
			player.sendSystemMessage(Component.translatable("error.missing_permission"));
			return;
		}

		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, payload.dimension());
		BlockPos location = payload.location();
		ServerLevel destinationLevel = context.server().getLevel(dimension);

		if (destinationLevel == null)
		{
			var localizedMessage = String.format(I18N.getOrDefault("error.dimension_missing"), dimension.identifier());
			player.sendSystemMessage(Component.literal(localizedMessage));
			return;
		}

		List<Portal> portals = SimplePortals.PORTAL_REGISTRY.getPortalsAt(location, dimension);

		if (portals.isEmpty())
		{
			var localizedMessage = String.format(I18N.getOrDefault("error.portal_missing"), dimension.identifier(), location);
			player.sendSystemMessage(Component.literal(localizedMessage));
			return;
		}

		SimplePortals.PORTAL_REGISTRY.deactivatePortal(destinationLevel, payload.location());
		ListCommandPayload.send(player, payload.portalListSettings());
	}
}