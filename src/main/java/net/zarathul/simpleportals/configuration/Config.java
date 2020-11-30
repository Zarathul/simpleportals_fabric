package net.zarathul.simpleportals.configuration;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Provides methods to load/save mod settings into a config file.
 *
 * Public static fields of any class can be settings as long as they conform to the following rules:
 *
 * 1) A second public static final field of the same type must exist, that has the same name as the field
 *    holding the setting with 'Default' appended. (e.g. 'mySetting' and 'mySettingDefault'. This field
 *    holds the default value for the setting.
 *
 * 2) Optionally a public static method may exist, that takes one parameter of the same type as the setting
 *    field and returns a boolean. It has to have the same name as the setting with "Validator" appended
 *    (e.g. 'mySetting' and 'mySettingValidator'). This method is used for input validation.
 *
 * 3) int, float, boolean, enum and string are considered primitive types, everything else is a complex type.
 *
 * 4) For Complex types there must be two additional public static methods. One takes a string as a parameter
 *    and returns a value of the same type as the setting field. It has to have to same name as the field with
 *    'Load' appended (eg. 'mySetting' and 'mySettingLoad').
 *    The other one takes one parameter of the same type as the setting field and returns a string. It has to
 *    have to same name as the setting field with 'Save' appended (eg. 'mySetting' and 'mySettingSave').
 *    (Note: This was originally done using member function names of the setting field type stored in the
 *    ConfigSetting annotation. Unfortunately this only works in a Dev environment. In release all minecraft
 *    types have their original meaningless names restored and the reflection API can't find them anymore).
 *
 * 5) For a setting to be considered valid for use in the UI, it has to have a ConfigSetting annotation with
 *    at least the 'descriptionKey' value set. This is true for both, primitive and complex types.
 *
 *    Note to myself: DO NOT write annotation bases config systems ever again. They suck!
 *
 */
public final class Config
{
	public static final String DEFAULT_CATEGORY = "common";

	public static void loadOrCreate(String configName, Class<?> clazz)
	{
		File configFile = getConfigPath(configName);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());

		if (configFile.exists())
		{
			loadConfig(configFilePath, clazz);
		}
		else
		{
			createConfig(configFilePath, clazz, true);
		}
	}

	public static void save(String configName, Class<?> clazz)
	{
		File configFile = getConfigPath(configName);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());
		createConfig(configFilePath, clazz, false);
	}

	public static boolean isComplexType(Field field)
	{
		Class<?> fieldType = field.getType();

		boolean isComplex = ((fieldType != int.class) &&
							 (fieldType != float.class) &&
							 (fieldType != boolean.class) &&
							 (fieldType != String.class) &&
							 (!fieldType.isEnum()));

		return isComplex;
	}

	public static boolean isValidSetting(Field field)
	{
		return isValidSetting(field, false);
	}

	public static boolean isValidSetting(Field field, boolean forUI)
	{
		Class<?> settingsClass = field.getDeclaringClass();

		// Check default value field.
		String defaultValueName = field.getName() + "Default";

		try
		{
			Field defaultValueField = settingsClass.getField(defaultValueName);
			if (!Modifier.isStatic(defaultValueField.getModifiers()) ||
				!Modifier.isFinal(defaultValueField.getModifiers()) ||
				(defaultValueField.getType() != field.getType()))
				return false;
		}
		catch (NoSuchFieldException ex)
		{
			return false;
		}

		// Check additional UI requirements.
		ConfigSetting annotation = field.getAnnotation(ConfigSetting.class);
		if (forUI && ((annotation == null) || annotation.descriptionKey().trim().isEmpty())) return false;

		// Primitive types done.
		if (!isComplexType(field)) return true;

		// Check if complex type has load and save methods.
		if (!getLoadSave(field).isPresent()) return false;

		return true;
	}

	public static Field[] getSettingFieldsSortedByCategory(Class<?> clazz)
	{
		Field[] fields = clazz.getFields();

		// Sort the fields by category. If a field has no annotation the default category is used.
		// Non-static and final fields are filtered out.
		fields = Arrays.stream(fields)
			.filter((field) -> (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())))
			.sorted((a, b) -> {
				ConfigSetting annotationA = a.getAnnotation(ConfigSetting.class);
				ConfigSetting annotationB = b.getAnnotation(ConfigSetting.class);
				String categoryA = (annotationA != null && !annotationA.category().isEmpty()) ? annotationA.category() : DEFAULT_CATEGORY;
				String categoryB = (annotationB != null && !annotationB.category().isEmpty()) ? annotationB.category() : DEFAULT_CATEGORY;

				return categoryA.compareTo(categoryB);
			}).toArray(Field[]::new);

		return fields;
	}

	/**
	 * Assumes the passed field was already successfully validated.
	 */
	public static Object getDefaultValue(Field field)
	{
		Class<?> settingsClass = field.getDeclaringClass();
		String defaultValueName = field.getName() + "Default";

		try
		{
			Field defaultValueField = settingsClass.getField(defaultValueName);
			Object defaultValue = defaultValueField.get(null);

			return defaultValue;
		}
		catch (NoSuchFieldException | IllegalAccessException ex)
		{
			return null;
		}
	}

	/**
	 * Assumes the passed field was already successfully validated.
	 */
	public static Method getValidator(Field field)
	{
		Class<?> settingsClass = field.getDeclaringClass();
		String fieldName = field.getName();
		String validatorName = fieldName + "Validator";

		try
		{
			Method validator = settingsClass.getMethod(validatorName, field.getType());

			return validator;
		}
		catch (NoSuchMethodException e)
		{
			return null;
		}
	}

	public static Optional<StorageMethods> getLoadSave(Field field)
	{
		Class<?> fieldType = field.getType();
		Class<?> settingType = field.getDeclaringClass();
		String loadMethodName = field.getName() + "Load";
		String saveMethodName = field.getName() + "Save";

		try
		{
			Method loadMethod = settingType.getMethod(loadMethodName, String.class);
			if (!Modifier.isStatic(loadMethod.getModifiers()) || (loadMethod.getReturnType() != fieldType)) return Optional.empty();

			Method saveMethod = settingType.getMethod(saveMethodName, fieldType);
			if (!Modifier.isStatic(saveMethod.getModifiers()) || (saveMethod.getReturnType() != String.class)) return Optional.empty();

			return Optional.of(new StorageMethods(loadMethod, saveMethod));
		}
		catch (NoSuchMethodException e)
		{
			return Optional.empty();
		}
	}

	private static File getConfigPath(String configName)
	{
		if (configName == null || configName.isEmpty()) return null;

		File gameDir = new File(System.getProperty("user.dir"));
		File configDir;

		try
		{
			configDir = new File(gameDir.getCanonicalPath(), "config");
			if (!configDir.exists())
			{
				if (!configDir.mkdir()) return null;
			}
		}
		catch (IOException ex)
		{
			return null;
		}

		File configFile = new File(configDir, configName + ".cfg");

		return configFile;
	}

	private static void createConfig(Path file, Class<?> clazz, boolean initToDefaults)
	{
		StringBuilder builder = new StringBuilder();
		ConfigSetting annotation;

		Field[] fields = getSettingFieldsSortedByCategory(clazz);

		for (Field field : fields)
		{
			if (!isValidSetting(field)) continue;

			annotation = field.getAnnotation(ConfigSetting.class);
			if (initToDefaults) setToDefault(field);

			if (isComplexType(field))
			{
				appendComplexTypeValue(field, annotation, builder);
			}
			else
			{
				appendPrimitiveTypeValue(field, annotation, builder);
			}
		}

		try
		{
			Files.write(file, builder.toString().getBytes());
		}
		catch (IOException e) {}
	}

	/**
	 * Assumes the passed field was already successfully validated.
	 */
	private static void appendPrimitiveTypeValue(Field field, ConfigSetting annotation, StringBuilder builder)
	{
		Object value = null;

		try
		{
			value = field.get(null);

			// :FIELD_SAVE_VALIDATION
			// If the field has a validator and the fields value is not valid, set the field to the default value.
			// The default value can technically also be invalid but at that point there's is no way to resolve this.
			Method validator = getValidator(field);
			if ((validator != null) && (!(boolean)validator.invoke(null, value)))
			{
				setToDefault(field);
				value = field.get(null);
			}
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			return;
		}

		appendComment(builder, annotation);

		builder.append(field.getName());
		builder.append("=");
		builder.append(value);
		builder.append("\n");
	}

	/**
	 * Assumes the passed field was already successfully validated.
	 */
	private static void appendComplexTypeValue(Field field, ConfigSetting annotation, StringBuilder builder)
	{
		try
		{
			Object value = field.get(null);
			// :FIELD_SAVE_VALIDATION
			Method validator = getValidator(field);
			if ((validator != null) && (!(boolean)validator.invoke(null, value)))
			{
				setToDefault(field);
				value = field.get(null);
			}

			StorageMethods storage = getLoadSave(field).get();
			String valueString = (String)storage.save.invoke(null, value);

			appendComment(builder, annotation);

			builder.append(field.getName());
			builder.append("=");
			builder.append(valueString);
			builder.append("\n");
		}
		catch (IllegalAccessException | InvocationTargetException ex) {}
	}

	private static void appendComment(StringBuilder builder, ConfigSetting annotation)
	{
		if (annotation != null && !annotation.descriptionKey().isEmpty())
		{
			builder.append("#");
			builder.append(annotation.description());
			builder.append("\n");
		}
	}

	/**
	 * Assumes the passed field was already successfully validated.
	 */
	private static void setToDefault(Field field)
	{
		Object defaultValue = getDefaultValue(field);

		try
		{
			field.set(null, defaultValue);
			// The validator is called here not to perform any validation, but to allow the validator to
			// perform any other tasks it might have besides validation.
			Method validator = getValidator(field);
			if (validator != null) validator.invoke(null, defaultValue);
		}
		catch (IllegalAccessException | InvocationTargetException e) {}
	}

	private static void loadConfig(Path file, Class<?> clazz)
	{
		List<String> lines;

		try
		{
			lines = Files.readAllLines(file);
		}
		catch (IOException ex)
		{
			return;
		}

		String[] components;
		String settingName;
		String settingValue;
		Field settingField;
		ArrayList<Field> loadedSettings = Lists.newArrayList();

		for (String line : lines)
		{
			line = line.trim();
			if (line.startsWith("#")) continue;	// Skip comments.

			components = line.split("=");
			if (components.length != 2) continue;

			settingName = components[0];
			settingValue = components[1];

			try
			{
				settingField = clazz.getField(settingName);
				if (!isValidSetting(settingField)) continue;

				if (isComplexType(settingField))
				{
					if(!loadComplexValue(settingField, settingValue)) continue;
				}
				else
				{
					if (!loadPrimitiveValue(settingField, settingValue)) continue;
				}

				loadedSettings.add(settingField);
			}
			catch (NoSuchFieldException e) {}
		}

		// Set uninitialized setting fields to their default values, in case not all of them were loaded from file.

		Field[] fields = clazz.getFields();
		boolean settingsMissingInFile = false;

		for (Field field : fields)
		{
			if (loadedSettings.contains(field) || !isValidSetting(field)) continue;
			setToDefault(field);
			settingsMissingInFile = true;
		}

		// Rewrite the config file if there were settings missing during loading.
		// One possible reason for this happening is an old config file.
		if (settingsMissingInFile) createConfig(file, clazz, false);
	}

	private static boolean loadPrimitiveValue(Field field, String valueString)
	{
		Class valueType = field.getType();
		Object value;

		if (valueType == int.class)
		{
			try
			{
				value = Integer.parseInt(valueString);
			}
			catch (NumberFormatException ex)
			{
				return false;
			}
		}
		else if (valueType == float.class)
		{
			try
			{
				value = Float.parseFloat(valueString);
			}
			catch (NumberFormatException ex)
			{
				return false;
			}
		}
		else if (valueType == boolean.class)
		{
			value = Boolean.parseBoolean(valueString);
		}
		else if (valueType == String.class)
		{
			value = valueString;
		}
		else if (valueType.isEnum())
		{
			try
			{
				value = Enum.valueOf(valueType, valueString);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
		}
		else
		{
			return false;
		}

		try
		{
			Method validator = getValidator(field);
			if ((validator != null) && (!(boolean)validator.invoke(null, value))) return false;

			field.set(null, value);

			return true;
		}
		catch (IllegalAccessException | InvocationTargetException ex)
		{
			return false;
		}
	}

	private static boolean loadComplexValue(Field field, String valueString)
	{
		try
		{
			StorageMethods storage = getLoadSave(field).get();
			Object loadedValue = storage.load.invoke(null, valueString);
			Method validator = getValidator(field);
			if ((validator != null) && (!(boolean)validator.invoke(null, loadedValue))) return false;

			field.set(null, loadedValue);

			return true;
		}
		catch (IllegalAccessException | InvocationTargetException ex)
		{
			return false;
		}
	}
}
