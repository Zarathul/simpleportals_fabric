package net.zarathul.simpleportals.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.gui.PortalListSettings;
import org.jspecify.annotations.NonNull;

public record DeactivatePortalPayload(Identifier dimension, BlockPos location,
									  PortalListSettings portalListSettings) implements CustomPacketPayload
{
	public static final Identifier ID = SimplePortals.modId("deactivate_portal");
	public static final Type<DeactivatePortalPayload> TYPE = new Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, DeactivatePortalPayload> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, DeactivatePortalPayload::dimension,
		BlockPos.STREAM_CODEC, DeactivatePortalPayload::location,
		PortalListSettings.STREAM_CODEC, DeactivatePortalPayload::portalListSettings,
		DeactivatePortalPayload::new
	);

	public @NonNull Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
