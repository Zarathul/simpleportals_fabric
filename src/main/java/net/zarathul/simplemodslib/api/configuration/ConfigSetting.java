package net.zarathul.simplemodslib.api.configuration;

import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Function;

public class ConfigSetting
{
	public Identifier id;
	public Type valueType;
	public Object value;
	public Object defaultValue;
	public Optional<Function<Object, Boolean>> validator;
	public Optional<Function<Object, String>> stringifier;
	public Optional<Function<String, Object>> destringifier;
	public String description;
	public String descriptionKey;
	public String category;
	public boolean needsWorldRestart;
	public int permissionLvl;
	public boolean clientOnly;

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Optional<Function<Object, Boolean>> validator, Optional<Function<Object, String>> stringifier, Optional<Function<String, Object>> destringifier, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		if (isComplex() && (stringifier.isEmpty() || destringifier.isEmpty())) throw new IllegalArgumentException("Complex type requires stringifier and destringifier.");

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

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Optional<Function<Object, String>> stringifier, Optional<Function<String, Object>> destringifier, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, Optional.empty(), stringifier, destringifier, description,descriptionKey,category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, Optional<Function<Object, Boolean>> validator, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, validator, Optional.empty(), Optional.empty(), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public ConfigSetting(Identifier id, Type valueType, Object value, Object defaultValue, String description, String descriptionKey, String category, boolean needsWorldRestart, int permissionLvl, boolean clientOnly)
	{
		this(id, valueType, value, defaultValue, Optional.empty(), Optional.empty(), Optional.empty(), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public void setDefaultValue() { value = defaultValue; }
	public boolean isInt() { return valueType == Type.Int; }
	public boolean isFloat() { return valueType == Type.Float; }
	public boolean isBoolean() { return valueType == Type.Boolean; }
	public boolean isString() { return valueType == Type.String; }
	public boolean isComplex() { return valueType == Type.Complex; }
	public boolean isInvalid() { return (validator.isPresent()) && !validator.get().apply(value); }
	public boolean isValid(Object value) { return (value.getClass() == this.value.getClass()) && (validator.isEmpty() || validator.get().apply(value)); }
	public boolean canStringify() { return stringifier.isPresent(); }
	public boolean canDestringify() { return destringifier.isPresent(); }
	public String stringify() { return (stringifier.isPresent()) ? stringifier.get().apply(value) : ""; }
	public String stringifyDefaultValue() { return (stringifier.isPresent()) ? stringifier.get().apply(defaultValue) : ""; }
	public Object destringify(String text) { return (destringifier.isPresent()) ? destringifier.get().apply(text) : null; }
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

	public static Function<Object, Boolean> INT_BETWEEN(int startInclusive, int endInclusive)
	{
		return o -> {
			int value = (int)o;
			return ((value >= startInclusive) && (value <= endInclusive));
		};
	}

	public static final Function<Object, Boolean> FLOAT_GREATER_THAN_ZERO = o -> {
			float value = (float)o;
			return value > 0;
		};

	public static final Function<Object, Boolean> FLOAT_GREATER_OR_EQUAL_TO_ZERO = o -> {
			float value = (float)o;
			return value >= 0;
		};

	public static Function<Object, Boolean> FLOAT_BETWEEN(float startInclusive, float endInclusive)
	{
		return o -> {
			float value = (float)o;
			return ((value >= startInclusive) && (value <= endInclusive));
		};
	}
}