package net.zarathul.simpleportals.network.payloads;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.gui.PortalInfo;
import net.zarathul.simpleportals.gui.PortalListSettings;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.stream.Collectors;

public record ListCommandPayload(List<PortalInfo> portals,
								 PortalListSettings portalListSettings) implements CustomPacketPayload
{
	public static final Identifier ID = SimplePortals.modId("list_command");
	public static final Type<ListCommandPayload> TYPE = new Type<>(ID);
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
		List<PortalInfo> portals = SimplePortals.PORTAL_REGISTRY.getAllPortals().stream()
			.map(portal -> new PortalInfo(
				portal.dimension(),
				portal.corner1().getInnerCornerPos(),
				portal.address(),
				SimplePortals.PORTAL_REGISTRY.getPortalPower(portal))
			)
			.collect(Collectors.toList());

		ListCommandPayload outgoingPayload = new ListCommandPayload(portals, portalListSettings);
		ServerPlayNetworking.send(player, outgoingPayload);
	}
}
