package net.zarathul.simplemodslib.api.configuration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import net.zarathul.simplemodslib.SimpleModsLib;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Loads and stores arbitrary settings. Optionally, provides UI for editing with server synchronization.<br><br>
 * Things to note:<br>
 * - {@link Config#initialize(String, String, boolean, AddSettingsCallback)} must be called per mod and before anything else. Refer to method documentation for details.<br>
 * - Using the config UI requires setting up networking ({@link Config#registerServerSideNetworking()}, {@link Config#registerClientSideNetworking()} and a
 * way to send the custom packet to the client. E.g. a command, see: {@link Config#registerCommand(CommandDispatcher, String)} and
 * {@link Config#executeCommand(CommandContext, String)}.<br>
 * - Localization keys for settings and their tooltips are constructed as follows:<br>
 * -- {@code config.} and {@link ConfigSetting#descriptionKey}, for tooltips appended by {@code .tooltip}. (e.g. {@code config.mysettings_desctiptionkey} and {@code config.mysettings_desctiptionkey.tooltip})<br>
 * -- If a key was not provided, the path of the settings id is used. The other rules still apply. (e.g. from the id {@code "mymodid:mysetting"} you'll get {@code config.mysetting} and {@code config.mysetting.tooltip})<br>
 * -- Category keys are constructed from the provided lowercase category name with an additional category prefix. Categories have no tooltips. (e.g. {@code config.category.mycategory})
 */
public final class Config
{
	private static final Logger LOG = LogManager.getLogger(SimpleModsLib.MOD_ID);
	private static final Map<Identifier, ConfigSetting> registry = new HashMap<>();
	private static final Map<Identifier, ConfigSetting> serverRegistry = new HashMap<>();
	private static final Map<String, ModInfo> modInfos = new HashMap<>();
	private static boolean isServerSideNetworkingRegistered;
	private static boolean isClientSideNetworkingRegistered;
	private static final String DEFAULT_CATEGORY = "";

	public static final String I18N_CONFIG_PREFIX = "config.";
	public static final String I18N_CATEGORY_PREFIX = I18N_CONFIG_PREFIX + "category.";

	public static Map<Identifier, ConfigSetting> getRegistry() { return Collections.unmodifiableMap(registry); }

	public static ConfigSetting getSetting(Identifier id)
	{
		return registry.getOrDefault(id, null);
	}

	public static boolean setSettingValue(Identifier id, Object value)
	{
		if (!registry.containsKey(id)) return false;

		registry.get(id).value = value;
		return true;
	}

	public static ModInfo getModInfo(String modId) { return modInfos.getOrDefault(modId, null); }

	private Config() {}

	/**
	 * Get a merged list of ConfigSettings (client and server), for display on the {@link ConfigScreen}.<br>
	 * Note: Server side settings will only include settings the player has the permissions for.
	 * @param receivedServerValues
	 * The {@link ConfigValue}s received from the server.
	 * @return
	 * The merged list of {@link ConfigSetting}s.
	 */
	public static List<ConfigSetting> getMergedSettings(String modId, List<ConfigValue> receivedServerValues)
	{
		List<ConfigSetting> settings = new ArrayList<>(registry.size());

		registry.values().stream()
			.filter(setting -> (setting.id.getNamespace().equals(modId) && setting.clientOnly))
			.forEach(settings::add);
		// Since the server will only send ConfigValues for settings the player has the required permissions for,
		// those can be used to filter out the other settings and not show them in the gui.
		receivedServerValues.forEach(configValue -> {
			if (serverRegistry.containsKey(configValue.id)) settings.add(serverRegistry.get(configValue.id));
		});

		return settings;
	}

	public static List<ConfigSetting> getSettings(String modId)
	{
		List<ConfigSetting> settings = new ArrayList<>(registry.size());
		registry.values().stream()
			.filter(setting -> setting.id.getNamespace().equals(modId))
			.forEach(settings::add);

		return settings;
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
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, validator, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addInt(id, defaultValue, null, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addInt(
		Identifier id,
		int defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addInt(id, defaultValue, null, description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
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
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, validator, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addFloat(id, defaultValue, null, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addFloat(
		Identifier id,
		float defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addFloat(id, defaultValue, null, description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
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
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, validator, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addBool(id, defaultValue, null, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addBool(
		Identifier id,
		boolean defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addBool(id, defaultValue, null, description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
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
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, validator, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addString(id, defaultValue, null, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
	}

	public static boolean addString(
		Identifier id,
		String defaultValue,
		String description,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addString(id, defaultValue, null, description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
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
		String category,
		boolean needsWorldRestart,
		int permissionLvl,
		boolean clientOnly)
	{
		return addComplex(id, defaultValue, validator, stringifier, destringifier, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addComplex(id, defaultValue, null, stringifier, destringifier, description, id.getPath(), category, needsWorldRestart, permissionLvl, clientOnly);
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
		return addComplex(id, defaultValue, null, stringifier, destringifier, description, id.getPath(), DEFAULT_CATEGORY, needsWorldRestart, permissionLvl, clientOnly);
	}

	private static void loadOrCreateConfigFile(String modId, boolean skipClientOnly)
	{
		File configFile = getConfigPath(modId);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());

		if (configFile.exists())
		{
			loadConfigFromFile(modId, configFilePath, skipClientOnly);
		}
		else
		{
			createConfigFile(modId, configFilePath, true, skipClientOnly);
		}
	}

	public static void save(String modId)
	{
		save(modId, false);
	}

	public static void save(String modId, boolean skipClientOnly)
	{
		File configFile = getConfigPath(modId);
		if (configFile == null) return;

		Path configFilePath = Paths.get(configFile.toURI());
		createConfigFile(modId, configFilePath, false, skipClientOnly);
	}

	private static File getConfigPath(String modId)
	{
		if (modId == null || modId.isEmpty()) return null;

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

		return new File(configDir, modId + ".cfg");
	}

	private static void createConfigFile(String modId, Path file, boolean initToDefaults, boolean skipClientOnly)
	{
		StringBuilder builder = new StringBuilder();
		// Sort the entries by the key, this way the ordering in the config file does not change all the time.
		var sortedEntries = registry.entrySet().stream()
			.filter(entry -> entry.getKey().getNamespace().equals(modId))
			.sorted(Map.Entry.comparingByKey())
			.toList();

		for (var entry : sortedEntries)
		{
			var configValue = entry.getValue();

			if (skipClientOnly && configValue.clientOnly) continue;	// No need to write clientOnly settings into config file on the dedicated server.
			if (initToDefaults) configValue.setDefaultValue();

			appendConfigValue(entry.getKey().getPath(), configValue, builder);
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

	private static void appendConfigValue(String name, ConfigSetting setting, StringBuilder builder)
	{
		if (setting.isInvalid()) setting.setDefaultValue();

		if (!setting.description.trim().isEmpty())
		{
			builder.append("#");
			builder.append(setting.description);
			builder.append("\n");
		}

		builder.append(name);
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
	private static void loadConfigFromFile(String modId, Path file, boolean skipClientOnlyOnWritingConfigFile)
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

		int parsedSettingsCount = 0;
		boolean parseError = false;

		for (String line : lines)
		{
			line = line.trim();
			if (line.startsWith("#")) continue;	// Skip comments.

			String[] components = line.split("=");
			if (components.length != 2)
			{
				parseError = true;
				continue;
			}

			String settingIdPath = components[0].trim();
			String settingValue = components[1].trim();

			if (settingIdPath.isEmpty() || !Identifier.isValidPath(settingIdPath) || settingValue.isEmpty())
			{
				parseError = true;
				continue;
			}

			Identifier settingId = Identifier.fromNamespaceAndPath(modId, settingIdPath);

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
		if (parseError || parsedSettingsCount != registry.size()) createConfigFile(modId, file, false, skipClientOnlyOnWritingConfigFile);
	}

	/**
	 *
	 * Writes the values of all non client-only settings, the player has the appropriate permission lvl for, into the passed in list.
	 * This is used for changing settings on a server remotely.
	 */
	public static void writeServerSettings(String modId, boolean toRemoteServer, List<ConfigValue> configValues, Player player)
	{
		var activeRegistry = (toRemoteServer) ? serverRegistry : registry;

		for (var registryEntry : activeRegistry.entrySet())
		{
			var id = registryEntry.getKey();
			if (!id.getNamespace().equals(modId)) continue;

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
	 * It is assumed that the buffer was filled by calling {@link Config#writeServerSettings(String, boolean, List, Player)} and that the players
	 * permissions did not change between both calls. Those settings are stored in a separate server-only registry.
	 * This is used for changing settings on a server remotely.
	 */
	public static void readServerSettings(boolean fromRemoteServer, List<ConfigValue> configValues, Player player)
	{
		// Keep received server settings in a different registry in order to separate them from local settings, in case the player
		// decides to play singleplayer with the same client install. Otherwise, the server settings will get written into the local
		// config file, possibly changing those for local play in undesired ways. This is only relevant while being connected
		// either to a dedicated server, or an integrated one that has been opened for LAN, and is not the players machine.
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

	private static void reset(String modId)
	{
		modInfos.remove(modId);

		registry.entrySet().stream()
			.filter(entry -> entry.getKey().getNamespace().equals(modId))
			.toList()
			.forEach(entry -> {
				registry.remove(entry.getKey());
				serverRegistry.remove(entry.getKey());
			});
	}

	public static final StreamCodec<FriendlyByteBuf, List<ConfigValue>> LIST_STREAM_CODEC = new StreamCodec<>()
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
		public static final StreamCodec<FriendlyByteBuf, ConfigValue> STREAM_CODEC = new StreamCodec<>()
		{
			@Override
			public ConfigValue decode(FriendlyByteBuf input)
			{
				var id = input.readIdentifier();
				var setting = registry.get(id);

				Object value = null;

				if (setting != null)
				{
					value = switch (setting.valueType)
					{
						case Int -> input.readInt();
						case Float -> input.readFloat();
						case Boolean -> input.readBoolean();
						case String -> input.readUtf();
						case Complex -> setting.destringify(input.readUtf());
					};
				}
				else
				{
					LOG.error("Failed decoding ConfigValue (id: {}). Setting for the provided id could not be found.", id);
				}

				return new ConfigValue(id, value);
			}

			@Override
			public void encode(FriendlyByteBuf output, ConfigValue configValue)
			{
				output.writeIdentifier(configValue.id);
				var setting = registry.get(configValue.id);

				if (setting != null)
				{
					switch (setting.valueType)
					{
						case Int -> output.writeInt((int) configValue.value);
						case Float -> output.writeFloat((float) configValue.value);
						case Boolean -> output.writeBoolean((boolean) configValue.value);
						case String -> output.writeUtf((String) configValue.value);
						case Complex -> output.writeUtf(setting.stringifier.apply(configValue.value));
					}
				}
				else
				{
					LOG.error("Failed encoding ConfigValue (id: {}). Setting for the provided id could not be found.", configValue.id);
				}
			}
		};
	}

	public record ConfigCommandPayload(String modId, List<ConfigValue> values, boolean fromDedicatedServer) implements CustomPacketPayload
	{
		public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID,"config_command");
		public static final Type<ConfigCommandPayload> TYPE = new Type<>(ID);
		public static final StreamCodec<FriendlyByteBuf, ConfigCommandPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ConfigCommandPayload::modId,
			LIST_STREAM_CODEC, ConfigCommandPayload::values,
			ByteBufCodecs.BOOL, ConfigCommandPayload::fromDedicatedServer,
			ConfigCommandPayload::new
		);

		public @NonNull Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}
	}

	public record ModInfo(String configGuiTitle, boolean onDedicatedServer) {}

	@FunctionalInterface
	public interface AddSettingsCallback
	{
		void addSettings();
	}

	/**
	 * <b>(Mandatory)</b><br>
	 * Initializes the config by resetting, calling the callback and finally loading the settings from the config file if possible.
	 * Otherwise, creates a new config file.
	 *
	 * @param modId
	 * The mod ID. Determines the configs filename and may not be {@code null} or {@code empty}, otherwise saving/loading the config will fail.
	 * @param configGuiTitle
	 * Title for the config gui.
	 * @param onDedicatedServer
	 * Must be set to {@code true} if initialized on a dedicated server, otherwise {@code false}.
	 * @param initCallback
	 * During this callback add settings to the config using the various add methods (e.g. {@link Config#addInt(Identifier, int, String, boolean, int, boolean)}).
	 * <br><br>
	 * Example:
	 * <pre>
	 *     {@code
	 *     Config.initialize("mymodid", "My Mod", false, () -> {
	 *         	Config.addInt(
	 *         		Identifier.fromNamespaceAndPath("mymodid", "myIntSetting"),
	 *         		7,
	 *         		ConfigSetting.INT_BETWEEN(0, 10),
	 *         		"A value between 0 and 10 (default is 7).",
	 *         		"mycategory",
	 *         		false,
	 *         		4,
	 *         		false);
	 *     });
	 *     }
	 * </pre>
	 */
	public static void initialize(String modId, String configGuiTitle, boolean onDedicatedServer, AddSettingsCallback initCallback)
	{
		reset(modId);

		modInfos.put(modId, new ModInfo(configGuiTitle, onDedicatedServer));
		initCallback.addSettings();
		loadOrCreateConfigFile(modId, onDedicatedServer);
	}

	/**
	 * <b>(Optional)</b><br>
	 * Registers the required custom packet and server side packet handler for the config command.<br>
	 * Call this in {@link ModInitializer#onInitialize()}.
	 */
	public static void registerServerSideNetworking()
	{
		if (isServerSideNetworkingRegistered) return;
		isServerSideNetworkingRegistered = true;

		PayloadTypeRegistry.serverboundPlay().register(Config.ConfigCommandPayload.TYPE, Config.ConfigCommandPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Config.ConfigCommandPayload.TYPE, Config.ConfigCommandPayload.STREAM_CODEC);

		// Server side receiver for the config command. Stores the received settings in the config of the server,
		// assuming the player has the required permissions.
		ServerPlayNetworking.registerGlobalReceiver(Config.ConfigCommandPayload.TYPE, (payload, ctx) -> {
			var player = ctx.player();
			Config.readServerSettings(false, payload.values(), player);
			Config.save(payload.modId(), true);
		});
	}

	/**
	 * <b>(Optional)</b><br>
	 * Registers client side packer handler for the config command.<br>
	 * Call this in {@link ClientModInitializer#onInitializeClient()}.
	 */
	@Environment(EnvType.CLIENT)
	public static void registerClientSideNetworking()
	{
		if (isClientSideNetworkingRegistered) return;
		isClientSideNetworkingRegistered = true;

		ConfigClientInit.registerClientSideNetworking();
	}

	/**
	 * <b>(Optional)</b><br>
	 * Register a command to show the config gui (/modId config).<br>
	 * {@link Config#executeCommand(CommandContext, String)} may be used instead in a custom command structure.<br>
	 * Use {@code commands.modId.info} localization key to customize the info text the /modId command shows the client.
	 *
	 */
	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String modId)
	{
		if (modId == null)
		{
			LOG.error("Command registration failed. modId was null.");
			return;
		}

		if (modId.trim().isEmpty())
		{
			LOG.error("Command registration failed. modId was empty.");
			return;
		}

		final String commandDescriptionKey = "commands." + modId + ".info";

		dispatcher.register(
			Commands.literal(modId)
				.executes(context -> {
					context.getSource().sendSuccess(() -> Component.translatable(commandDescriptionKey), false);
					return 1;
				})
				.then(
					Commands.literal("config")
						.executes(context -> executeCommand(context, modId))
				)
		);
	}

	/**
	 * <b>(Optional)</b><br>
	 * Executes the command to show the config gui.<br>
	 * Can be used instead of {@link Config#registerCommand(CommandDispatcher, String)} to integrate the command in
	 * an existing command structure.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 * 	dispatcher.register(
	 * 		Commands.literal("mycommand")
	 * 			.executes(Config::executeCommand)
	 * 	);
	 * }
	 * </pre>
	 *
	 * @param context
	 * The context in which the command is executed.
	 * @return
	 * {@code 1} on success, {@code 0} on failure.
	 */
	public static int executeCommand(CommandContext<CommandSourceStack> context, String modId)
	{
		var player =  context.getSource().getPlayer();
		if (player == null) return 0;

		ModInfo modInfo = modInfos.get(modId);

		List<Config.ConfigValue> configValues = new ArrayList<>();
		writeServerSettings(modId,false, configValues, player);
		ConfigCommandPayload outgoingPayload = new ConfigCommandPayload(modId, configValues, modInfo.onDedicatedServer());

		ServerPlayNetworking.send(player, outgoingPayload);

		return 1;
	}
}