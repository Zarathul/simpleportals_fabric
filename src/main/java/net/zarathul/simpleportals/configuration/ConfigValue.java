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

			ConfigValue decodedValue = new ConfigValue(type, value);

			return decodedValue;
		}

		@Override
		public void encode(FriendlyByteBuf output, ConfigValue value)
		{
			output.writeEnum(value.type);

			switch (value.type)
			{
				case Int ->
				{
					output.writeInt((int)value.value);
				}
				case Float ->
				{
					output.writeFloat((float)value.value);
				}
				case Boolean ->
				{
					output.writeBoolean((boolean)value.value);
				}
				case Enum ->
				{
					output.writeInt(((Enum<?>)value.value).ordinal());
				}
				case String, Complex ->
				{
					output.writeUtf((String)value.value);
				}
			}
		}
	};
}
