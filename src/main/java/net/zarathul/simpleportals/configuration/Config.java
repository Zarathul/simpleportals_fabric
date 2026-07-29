package net.zarathul.simpleportals.configuration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class Config
{
	private static final Logger LOG = LogManager.getLogger("simpleconfig");
	private static final Map<Identifier, ConfigSetting> registry = new HashMap<>();
	private static final Map<Identifier, ConfigSetting> serverRegistry = new HashMap<>();
	private static final String DEFAULT_CATEGORY = "";

	public static final String CATEGORY_I18N_PREFIX = "config.";
	public static Map<Identifier, ConfigSetting> getRegistry() { return Collections.unmodifiableMap(registry); }
	public static Optional<ConfigSetting> getSetting(Identifier id) { return (registry.containsKey(id)) ? Optional.of(registry.get(id)) : Optional.empty(); }
	public static boolean setSettingValue(Identifier id, Object value)
	{
		if (!registry.containsKey(id)) return false;

		registry.get(id).value = value;
		return true;
	}

	public static List<ConfigSetting> getMergedSettings(List<ConfigValue> receivedServerValues)
	{
		List<ConfigSetting> settings = new ArrayList<>(registry.size());

		registry.values().stream().filter(setting -> setting.clientOnly).forEach(settings::add);
		// Since the server will only send ConfigValues for settings the player has the required permissions for,
		// those can be used to filter out the other settings and not show them in the gui.
		receivedServerValues.forEach(configValue -> {
			if (serverRegistry.containsKey(configValue.id)) settings.add(serverRegistry.get(configValue.id));
		});

		return settings;
	}

	public static List<ConfigSetting> getSettings()
	{
		List<ConfigSetting> settings = new ArrayList<>(registry.size());
		settings.addAll(registry.values());

		return settings;
	}

	private static boolean addInt(
		Identifier id,
		int defaultValue,
		Optional<Function<Object, Boolean>> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		if (registry.containsKey(id)) return false;

		var setting = new ConfigSetting(id, ConfigSetting.Type.Int, defaultValue, defaultValue, validator, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);

		try
		{
			registry.put(id, setting);
			if (!setting.clientOnly) serverRegistry.put(id, setting.copy());
		}
		catch (Exception _)
		{
			LOG.error("Failed to register int setting with the id: {}", id);
			return false;
		}

		return true;
	}

	public static boolean addInt(
		Identifier id,
		int defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, Optional.of(validator), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addInt(
		Identifier id,
		int defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, Optional.of(validator), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addInt(
		Identifier id,
		int defaultValue,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, Optional.empty(), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addInt(
		Identifier id,
		int defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, Optional.empty(), description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	private static boolean addFloat(
		Identifier id,
		float defaultValue,
		Optional<Function<Object, Boolean>> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		if (registry.containsKey(id)) return false;

		var setting = new ConfigSetting(id, ConfigSetting.Type.Float, defaultValue, defaultValue, validator, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);

		try
		{
			registry.put(id, setting);
			if (!setting.clientOnly) serverRegistry.put(id, setting.copy());
		}
		catch (Exception _)
		{
			LOG.error("Failed to register float setting with the id: {}", id);
			return false;
		}

		return true;
	}

	public static boolean addFloat(
		Identifier id,
		float defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, Optional.of(validator), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addFloat(
		Identifier id,
		float defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, Optional.of(validator), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addFloat(
		Identifier id,
		float defaultValue,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, Optional.empty(), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addFloat(
		Identifier id,
		float defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, Optional.empty(), description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	private static boolean addBool(
		Identifier id,
		boolean defaultValue,
		Optional<Function<Object, Boolean>> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		if (registry.containsKey(id)) return false;

		var setting = new ConfigSetting(id, ConfigSetting.Type.Boolean, defaultValue, defaultValue, validator, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);

		try
		{
			registry.put(id, setting);
			if (!setting.clientOnly) serverRegistry.put(id, setting.copy());
		}
		catch (Exception _)
		{
			LOG.error("Failed to register boolean setting with the id: {}", id);
			return false;
		}

		return true;
	}

	public static boolean addBool(
		Identifier id,
		boolean defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, Optional.of(validator), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addBool(
		Identifier id,
		boolean defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, Optional.of(validator), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addBool(
		Identifier id,
		boolean defaultValue,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, Optional.empty(), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addBool(
		Identifier id,
		boolean defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, Optional.empty(), description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	private static boolean addString(
		Identifier id,
		String defaultValue,
		Optional<Function<Object, Boolean>> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		if (registry.containsKey(id)) return false;

		var setting = new ConfigSetting(id, ConfigSetting.Type.String, defaultValue, defaultValue, validator, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);

		try
		{
			registry.put(id, setting);
			if (!setting.clientOnly) serverRegistry.put(id, setting.copy());
		}
		catch (Exception _)
		{
			LOG.error("Failed to register string setting with the id: {}", id);
			return false;
		}

		return true;
	}

	public static boolean addString(
		Identifier id,
		String defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, Optional.of(validator), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addString(
		Identifier id,
		String defaultValue,
		Function<Object, Boolean> validator,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, Optional.of(validator), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addString(
		Identifier id,
		String defaultValue,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, Optional.empty(), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addString(
		Identifier id,
		String defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, Optional.empty(), description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	private static boolean addComplex(
		Identifier id,
		Object defaultValue,
		Optional<Function<Object, Boolean>> validator,
		Optional<Function<Object, String>> stringifier,
		Optional<Function<String, Object>> destringifier,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		if (registry.containsKey(id)) return false;

		try
		{
			// Complex types without stringifier or destringifier cause an exception.
			var setting = new ConfigSetting(id, ConfigSetting.Type.Complex, defaultValue, defaultValue, validator, stringifier, destringifier, description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);

			registry.put(id, setting);
			if (!setting.clientOnly) serverRegistry.put(id, setting.copy());
		}
		catch (Exception _)
		{
			LOG.error("Failed to register complex setting with the id: {}", id);
			return false;
		}

		return true;
	}

	public static boolean addComplex(
		Identifier id,
		Object defaultValue,
		Function<Object, Boolean> validator,
		Function<Object, String> stringifier,
		Function<String, Object> destringifier,
		String description,
		String descriptionKey,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addComplex(id, defaultValue, Optional.of(validator), Optional.of(stringifier), Optional.of(destringifier), description, descriptionKey, category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addComplex(
		Identifier id,
		Object defaultValue,
		Function<Object, Boolean> validator,
		Function<Object, String> stringifier,
		Function<String, Object> destringifier,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addComplex(id, defaultValue, Optional.of(validator), Optional.of(stringifier), Optional.of(destringifier), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addComplex(
		Identifier id,
		Object defaultValue,
		Function<Object, String> stringifier,
		Function<String, Object> destringifier,
		String description,
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addComplex(id, defaultValue, Optional.empty(), Optional.of(stringifier), Optional.of(destringifier), description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addComplex(
		Identifier id,
		Object defaultValue,
		Function<Object, String> stringifier,
		Function<String, Object> destringifier,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addComplex(id, defaultValue, Optional.empty(), Optional.of(stringifier), Optional.of(destringifier), description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static void loadOrCreateConfigFile(String configName, boolean skipClientOnly)
	{
		File configFile = getConfigPath(configName);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());

		if (configFile.exists())
		{
			loadConfigFromFile(configFilePath, skipClientOnly);
		}
		else
		{
			createConfigFile(configFilePath, true, skipClientOnly);
		}
	}

	public static void save(String configName)
	{
		save(configName, false);
	}

	public static void save(String configName, boolean skipClientOnly)
	{
		File configFile = getConfigPath(configName);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());
		createConfigFile(configFilePath, false, skipClientOnly);
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
				if (!configDir.mkdir())
				{
					LOG.error("Creating config directory failed. ({})", configDir);
					return null;
				}
			}
		}
		catch (IOException ex)
		{
			LOG.error("Config file path could not be found.");
			return null;
		}

		return new File(configDir, configName + ".cfg");
	}

	private static void createConfigFile(Path file, boolean initToDefaults, boolean skipClientOnly)
	{
		StringBuilder builder = new StringBuilder();
		// Sort the entries by the key, this way the ordering in the config file does not change all the time.
		var sortedEntries = registry.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).toList();

		for (var entry : sortedEntries)
		{
			var configValue = entry.getValue();

			if (skipClientOnly && configValue.clientOnly) continue;	// No need to write clientOnly settings into config file on the dedicated server.
			if (initToDefaults) configValue.setDefaultValue();

			appendConfigValue(entry.getKey(), configValue, builder);
		}

		try
		{
			Files.write(file, builder.toString().getBytes());
		}
		catch (IOException ignored)
		{
			LOG.error("Writing config to disk failed. ({})", file);
		}
	}

	private static void appendConfigValue(Identifier id, ConfigSetting setting, StringBuilder builder)
	{
		if (setting.isInvalid()) setting.setDefaultValue();

		if (!setting.description.trim().isEmpty())
		{
			builder.append("#");
			builder.append(setting.description);
			builder.append("\n");
		}

		builder.append(id);
		builder.append("=");

		if (setting.isComplex())
		{
			builder.append(setting.stringify());
		}
		else
		{
			builder.append(setting.value);
		}

		builder.append("\n");
	}


	// The skipClientOnlyOnWritingConfigFile flag might be confusing at first glance. If not all settings
	// are found when parsing a config file, the config file gets rewritten with the parsed settings and
	// default values for those that were missing. Because clientOnly settings are not stored in the config
	// file on dedicated servers, the flag needs to be carried through here!
	private static void loadConfigFromFile(Path file, boolean skipClientOnlyOnWritingConfigFile)
	{
		List<String> lines;

		try
		{
			lines = Files.readAllLines(file);
		}
		catch (IOException ex)
		{
			LOG.error("Reading config from disk failed. ({})", file);
			return;
		}

		String[] components;
		Identifier settingId;
		String settingValue;
		int parsedSettingsCount = 0;
		boolean parseError = false;

		for (String line : lines)
		{
			line = line.trim();
			if (line.startsWith("#")) continue;	// Skip comments.

			components = line.split("=");
			if (components.length != 2) continue;

			settingId = Identifier.parse(components[0]);
			settingValue = components[1];

			if (registry.containsKey(settingId))
			{
				var setting = registry.get(settingId);

				try
				{
					setting.value = switch (setting.valueType)
					{
						case Int -> Integer.parseInt(settingValue);
						case Float -> Float.parseFloat(settingValue);
						case Boolean -> Boolean.parseBoolean(settingValue);
						case String -> settingValue;
						case Complex -> setting.destringify(settingValue);
					};
				}
				catch (Exception _)
				{
					parseError = true;
					LOG.error("Error while parsing setting {}. Using default. Line was: {}.", settingId, line);
					setting.setDefaultValue();
				}

				if (setting.isInvalid())
				{
					parseError = true;
					LOG.error("Error while parsing setting {}. Value {} is invalid. Using default. Line was: {}.", settingId, settingValue, line);
					setting.setDefaultValue();
				}

				parsedSettingsCount++;
			}
		}

		// Rewrite the config file if there were settings missing during loading.
		// One possible reason for this happening is an old config file.
		if (parseError || parsedSettingsCount != registry.size()) createConfigFile(file, false, skipClientOnlyOnWritingConfigFile);
	}

	/**
	 *
	 * Writes the values of all non client-only settings, the player has the appropriate permission lvl for, into the passed in list.
	 * This is used for changing settings on a server remotely.
	 */
	public static void writeServerSettings(boolean toRemoteServer, List<ConfigValue> configValues, Player player)
	{
		var activeRegistry = (toRemoteServer) ? serverRegistry : registry;

		for (var registryEntry : activeRegistry.entrySet())
		{
			var id = registryEntry.getKey();
			var configSetting = registryEntry.getValue();

			var requiredPermissionLevel = PermissionLevel.byId(configSetting.permissionLvl);
			var requiredPermission = new Permission.HasCommandLevel(requiredPermissionLevel);

			// :BROKEN_PERMISSIONS
			if (configSetting.clientOnly || (configSetting.permissionLvl > 0 && !player.permissions().hasPermission(requiredPermission))) continue;

			configValues.add(new ConfigValue(id, configSetting.value));
		}
	}

	/**
	 * Reads the values of all non client-only settings, the player has the appropriate permission lvl for, from the passed in list.
	 * It is assumed that the buffer was filled by calling {@link Config#writeServerSettings(boolean, List, Player)} and that the players
	 * permissions did not change between both calls. Those settings are stored in a separate server-only registry.
	 * This is used for changing settings on a server remotely.
	 */
	public static void readServerSettings(boolean fromRemoteServer, List<ConfigValue> configValues, Player player)
	{
		// Keep received server settings in a different registry in order to separate them from local settings, in case the player
		// decides to play singleplayer with the same client install. Otherwise, the server settings will get written into the local
		// config file, possibly changing those for local play in an undesired ways. This is only relevant while being connected
		// either to a dedicated server or an integrated one, that is not the players machine, that has been opened for LAN.
		var activeRegistry = (fromRemoteServer) ? serverRegistry : registry;

		for (var configValue : configValues)
		{
			if (!activeRegistry.containsKey(configValue.id)) continue;

			var setting = activeRegistry.get(configValue.id);
			var requiredPermissionLevel = PermissionLevel.byId(setting.permissionLvl);
			var requiredPermission = new Permission.HasCommandLevel(requiredPermissionLevel);

			//:BROKEN_PERMISSIONS
			if (setting.permissionLvl > 0 && !player.permissions().hasPermission(requiredPermission)) continue;

			setting.value = configValue.value;

			if (setting.isInvalid()) setting.setDefaultValue();
		}
	}

	public static void reset()
	{
		registry.clear();
	}

	public static final StreamCodec<FriendlyByteBuf, List<ConfigValue>> LIST_STREAM_CODEC = new StreamCodec<FriendlyByteBuf, List<ConfigValue>>()
	{
		@Override
		public List<ConfigValue> decode(FriendlyByteBuf input)
		{
			return input.readCollection(ArrayList::new, ConfigValue.STREAM_CODEC);
		}

		@Override
		public void encode(FriendlyByteBuf output, List<ConfigValue> values)
		{
			output.writeCollection(values, ConfigValue.STREAM_CODEC);
		}
	};

	public record ConfigValue(Identifier id, Object value)
	{
		public static final StreamCodec<FriendlyByteBuf, ConfigValue> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, ConfigValue>()
		{
			@Override
			public ConfigValue decode(FriendlyByteBuf input)
			{
				var id = input.readIdentifier();
				var setting = registry.get(id);

				Object value = switch (setting.valueType)
				{
					case Int ->  input.readInt();
					case Float -> input.readFloat();
					case Boolean -> input.readBoolean();
					case String -> input.readUtf();
					case Complex -> setting.destringify(input.readUtf());
				};

				return new ConfigValue(id, value);
			}

			@Override
			public void encode(FriendlyByteBuf output, ConfigValue configValue)
			{
				output.writeIdentifier(configValue.id);
				var setting = registry.get(configValue.id);

				switch (setting.valueType)
				{
					case Int -> output.writeInt((int)configValue.value);
					case Float -> output.writeFloat((float)configValue.value);
					case Boolean -> output.writeBoolean((boolean)configValue.value);
					case String -> output.writeUtf((String)configValue.value);
					case Complex -> output.writeUtf(setting.stringifier.get().apply(configValue.value));
				}
			}
		};
	}
}