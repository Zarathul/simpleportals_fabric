package net.zarathul.simplemodslib.api.configuration;

import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class ConfigSetting
{
	public Identifier id;
	public Type valueType;
	public Object value;
	public Object defaultValue;
	public Function<Object, Boolean> validator;
	public Function<Object, String> stringifier;
	public Function<String, Object> destringifier;
	public String description;
	public String descriptionKey;
	public String category;
	public boolean needsWorldRestart;
	public int permissionLvl;
	public boolean clientOnly;

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Function<Object, Boolean> validator, Function<Object, String> stringifier, Function<String, Object> destringifier, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		if (isComplex() && (stringifier == null || destringifier == null)) throw new IllegalArgumentException("Complex type requires stringifier and destringifier.");

		this.id = id;
		this.valueType = valueType;
		this.value = value;
		this.defaultValue = defaultValue;
		this.validator = validator;
		this.stringifier = stringifier;
		this.destringifier = destringifier;
		this.description = description;
		this.descriptionKey = descriptionKey;
		this.category = category;
		this.needsWorldRestart = needsWorldRestart;
		this.permissionLvl = permissionLvl;
		this.clientOnly = clientOnly;
	}

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Function<Object, String> stringifier, Function<String, Object> destringifier, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, null, stringifier, destringifier, description,descriptionKey,category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Function<Object, Boolean> validator, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, validator, null, null, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, null, null, null, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public void setDefaultValue() { value = defaultValue; }
	public boolean isInt() { return valueType == Type.Int; }
	public boolean isFloat() { return valueType == Type.Float; }
	public boolean isBoolean() { return valueType == Type.Boolean; }
	public boolean isString() { return valueType == Type.String; }
	public boolean isComplex() { return valueType == Type.Complex; }
	public boolean isInvalid() { return (validator != null) && !validator.apply(value); }
	public boolean isValidValue(Object value) { return (value.getClass() == this.value.getClass()) && (validator == null || validator.apply(value)); }
	public boolean canStringify() { return stringifier != null; }
	public boolean canDestringify() { return destringifier != null; }
	public String stringify() { return (stringifier != null) ? stringifier.apply(value) : ""; }
	public String stringifyDefaultValue() { return (stringifier != null) ? stringifier.apply(defaultValue) : ""; }
	public Object destringify(String text) { return (destringifier != null) ? destringifier.apply(text) : null; }
	public ConfigSetting copy() { return new ConfigSetting(id, valueType, value, defaultValue, validator, stringifier, destringifier, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly); }

	public enum Type
	{
		Int,
		Float,
		Boolean,
		String,
		Complex
	}

	public static final Function<Object, Boolean> INT_GREATER_THAN_ZERO = o -> {
		int value = (int)o;
		return value > 0;
	};

	public static final Function<Object, Boolean> INT_GREATER_OR_EQUAL_TO_ZERO = o -> {
		int value = (int)o;
		return value >= 0;
	};

	public static final Function<Object, Boolean> INT_GREATER_OR_EQUAL_TO_MINUS_ONE = o -> {
		int value = (int)o;
		return value >= -1;
	};

	public static Function<Object, Boolean> INT_BETWEEN(int startInclusive, int endInclusive)
	{
		return o -> {
			int value = (int)o;
			return ((value >= startInclusive) && (value <= endInclusive));
		};
	}

	public static final Function<Object, Boolean> FLOAT_GREATER_THAN_ZERO = o -> {
		float value = (float)o;
		return value > 0f;
	};

	public static final Function<Object, Boolean> FLOAT_GREATER_OR_EQUAL_TO_ZERO = o -> {
		float value = (float)o;
		return value >= 0f;
	};

	public static final Function<Object, Boolean> FLOAT_GREATER_OR_EQUAL_TO_MINUS_ONE = o -> {
		float value = (float)o;
		return value >= -1f;
	};

	public static Function<Object, Boolean> FLOAT_BETWEEN(float startInclusive, float endInclusive)
	{
		return o -> {
			float value = (float)o;
			return ((value >= startInclusive) && (value <= endInclusive));
		};
	}
}