package net.zarathul.simpleportals.commands;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum ConfigCommandMode
{
	GetServerSettings,
	SetServerSettings;

	public static final StreamCodec<FriendlyByteBuf, ConfigCommandMode> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, ConfigCommandMode>()
	{
		@Override
		public ConfigCommandMode decode(FriendlyByteBuf input)
		{
			return input.readEnum(ConfigCommandMode.class);
		}

		@Override
		public void encode(FriendlyByteBuf output, ConfigCommandMode mode)
		{
			output.writeEnum(mode);
		}
	};
}
