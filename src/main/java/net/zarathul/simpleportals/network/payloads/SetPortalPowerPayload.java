package net.zarathul.simpleportals.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.gui.PortalListSettings;
import org.jspecify.annotations.NonNull;

public record SetPortalPowerPayload(Identifier dimension, BlockPos location, int value,
									PortalListSettings portalListSettings) implements CustomPacketPayload
{
	public static final Identifier ID = SimplePortals.modId("set_portal_power");
	public static final Type<SetPortalPowerPayload> TYPE = new Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, SetPortalPowerPayload> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, SetPortalPowerPayload::dimension,
		BlockPos.STREAM_CODEC, SetPortalPowerPayload::location,
		ByteBufCodecs.INT, SetPortalPowerPayload::value,
		PortalListSettings.STREAM_CODEC, SetPortalPowerPayload::portalListSettings,
		SetPortalPowerPayload::new
	);

	public @NonNull Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
