package net.zarathul.simpleportals.configuration.gui;

public class Filter
{
	public Type type;
	public Condition condition;
	public Object value;

	public static final Filter NONE = new Filter(Type.None, Condition.None, null);

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
