package net.zarathul.simpleportals.gui;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PortalListSettings(Sorting sorting, Filter filter)
{
	public static final PortalListSettings DEFAULT = new PortalListSettings(Sorting.DEFAULT, Filter.NONE);

	public static StreamCodec<FriendlyByteBuf, PortalListSettings> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, PortalListSettings>()
	{
		@Override
		public PortalListSettings decode(FriendlyByteBuf input)
		{
			return new PortalListSettings(Sorting.STREAM_CODEC.decode(input), Filter.STREAM_CODEC.decode(input));
		}

		@Override
		public void encode(FriendlyByteBuf output, PortalListSettings value)
		{
			Sorting.STREAM_CODEC.encode(output, value.sorting);
			Filter.STREAM_CODEC.encode(output, value.filter);
		}
	};

	public record Filter(Type type, Condition condition, String value)
	{
		public static final Filter NONE = new Filter(Filter.Type.None, Filter.Condition.None, "");
		public static final Multimap<Type, Condition> CONDITIONS_BY_TYPE;

		static
		{
			var conditionsByType = ArrayListMultimap.<Type, Condition>create();
			conditionsByType.put(Type.None, Condition.None);

			conditionsByType.put(Type.Dimension, Condition.Equals);
			conditionsByType.put(Type.Dimension, Condition.Contains);

			conditionsByType.put(Type.Location, Condition.Equals);
			conditionsByType.put(Type.Location, Condition.Contains);

			conditionsByType.put(Type.Address, Condition.Equals);
			conditionsByType.put(Type.Address, Condition.Contains);

			conditionsByType.put(Type.Power, Condition.Equals);
			conditionsByType.put(Type.Power, Condition.GreaterThan);
			conditionsByType.put(Type.Power, Condition.LessThan);

			CONDITIONS_BY_TYPE = ImmutableListMultimap.copyOf(conditionsByType);
		}

		// The ordering of the enum values should reflect the order of columns in the list. Otherwise,
		// the cycle button for setting up the filter will have a different order than the columns.
		// It uses the ordinal values for ordering.
		public enum Type
		{
			None,
			Dimension,
			Location,
			Address,
			Power
		}

		public enum Condition
		{
			None,
			Equals,
			GreaterThan,
			LessThan,
			Contains
		}

		public static final StreamCodec<FriendlyByteBuf, Filter> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, Filter>()
		{
			@Override
			public Filter decode(FriendlyByteBuf input)
			{
				return new Filter(input.readEnum(Type.class), input.readEnum(Condition.class), input.readUtf());
			}

			@Override
			public void encode(FriendlyByteBuf output, Filter value)
			{
				output.writeEnum(value.type);
				output.writeEnum(value.condition);
				output.writeUtf(value.value);
			}
		};
	}

	public record Sorting(Type type, Direction direction)
	{
		public static final Sorting DEFAULT = new Sorting(Type.Address);

		public Sorting(Type type)
		{
			this(type, Direction.Ascending);
		}

		public Sorting invert()
		{
			return new Sorting(type, (direction == Direction.Ascending) ? Direction.Descending : Direction.Ascending);
		}

		public boolean isDescending() { return direction == Direction.Descending; }

		public enum Type
		{
			Dimension,
			Location,
			Address,
			Power
		}

		public enum Direction
		{
			Ascending,
			Descending
		}

		public static final StreamCodec<FriendlyByteBuf, Sorting> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, Sorting>()
		{
			@Override
			public Sorting decode(FriendlyByteBuf input)
			{
				return new Sorting(input.readEnum(Type.class), input.readEnum(Direction.class));
			}

			@Override
			public void encode(FriendlyByteBuf output, Sorting value)
			{
				output.writeEnum(value.type);
				output.writeEnum(value.direction);
			}
		};
	}
}
