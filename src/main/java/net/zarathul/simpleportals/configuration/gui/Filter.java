package net.zarathul.simpleportals.configuration.gui;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

public class Filter
{
	public Type type;
	public Condition condition;
	public Object value;

	public static final Filter NONE = new Filter(Type.None, Condition.None, null);
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

	public Filter(Type type, Condition condition, Object value)
	{
		this.type = type;
		this.condition = condition;
		this.value = value;
	}

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
}
