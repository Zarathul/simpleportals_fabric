package net.zarathul.simpleportals.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.zarathul.simpleportals.SimplePortals;
import org.jspecify.annotations.NonNull;

public record TpdCommandPayload(Identifier dimension, BlockPos location) implements CustomPacketPayload
{
	public static final Identifier ID = SimplePortals.modId("tpd_command");
	public static final Type<TpdCommandPayload> TYPE = new Type<>(ID);
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
