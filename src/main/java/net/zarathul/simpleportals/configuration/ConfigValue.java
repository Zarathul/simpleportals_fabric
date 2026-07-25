package net.zarathul.simpleportals.configuration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ConfigValue(Type type, Object value)
{
	public enum Type
	{
		Int,
		Float,
		Boolean,
		Enum,
		String,
		Complex
	}

	public static final StreamCodec<FriendlyByteBuf, ConfigValue> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, ConfigValue>()
	{
		@Override
		public ConfigValue decode(FriendlyByteBuf input)
		{
			Type type = input.readEnum(Type.class);
			Object value = null;

			switch (type)
			{
				case Int, Enum ->
				{
					value = input.readInt();
				}
				case Float ->
				{
					value = input.readFloat();
				}
				case Boolean ->
				{
					value = input.readBoolean();
				}
				case String, Complex ->
				{
					value = input.readUtf();
				}
			}

			return new ConfigValue(type, value);
		}

		@Override
		public void encode(FriendlyByteBuf output, ConfigValue configValue)
		{
			output.writeEnum(configValue.type);

			switch (configValue.type)
			{
				case Int, Enum ->
				{
					output.writeInt((int)configValue.value);
				}
				case Float ->
				{
					output.writeFloat((float)configValue.value);
				}
				case Boolean ->
				{
					output.writeBoolean((boolean)configValue.value);
				}
				case String, Complex ->
				{
					output.writeUtf((String)configValue.value);
				}
			}
		}
	};
}
