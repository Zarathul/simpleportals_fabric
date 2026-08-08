package net.zarathul.simpleportals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simplemodslib.api.configuration.ConfigSetting;
import net.zarathul.simpleportals.common.Utils;

public final class Settings
{
	private static TagKey<Item> powerSourceTag;

	public static final Identifier MAX_SIZE = Utils.createModIdentifier("max_size");
	public static final Identifier POWER_COST = Utils.createModIdentifier("power_cost");
	public static final Identifier POWER_CAPACITY = Utils.createModIdentifier("power_capacity");
	public static final Identifier PLAYER_TELEPORTATION_DELAY = Utils.createModIdentifier("player_teleportation_delay");
	public static final Identifier POWER_SOURCE = Utils.createModIdentifier("power_source");
	public static final Identifier TELEPORTATION_SOUND_ENABLED = Utils.createModIdentifier("teleportation_sound_enabled");
	public static final Identifier PORTAL_ACTIVATION_SOUND_ENABLED = Utils.createModIdentifier("portal_activation_sound_enabled");
	public static final Identifier NOT_ENOUGH_POWER_SOUND_ENABLED = Utils.createModIdentifier("not_enough_power_sound_enabled");
	public static final Identifier PARTICLES_ENABLED = Utils.createModIdentifier("particles_enabled");
	public static final Identifier AMBIENT_SOUND_ENABLED = Utils.createModIdentifier("ambient_sound_enabled");
	public static final Identifier TRANSITION_EFFECT_ENABLED = Utils.createModIdentifier("transition_effect_enabled");

	public static TagKey<Item> powerSourceTag() { return powerSourceTag; }
	public static int maxSize() { return (int) Config.getSetting(MAX_SIZE).get().value; }
	public static int powerCost() { return (int) Config.getSetting(POWER_COST).get().value; }
	public static int powerCapacity() { return (int) Config.getSetting(POWER_CAPACITY).get().value; }
	public static int playerTeleportationDelay() { return (int) Config.getSetting(PLAYER_TELEPORTATION_DELAY).get().value; }
	public static Identifier powerSource() { return (Identifier) Config.getSetting(POWER_SOURCE).get().value; }
	public static boolean teleportationSoundEnabled() { return (boolean) Config.getSetting(TELEPORTATION_SOUND_ENABLED).get().value; }
	public static boolean portalActivationSoundEnabled() { return (boolean) Config.getSetting(PORTAL_ACTIVATION_SOUND_ENABLED).get().value; }
	public static boolean notEnoughPowerSoundEnabled() { return (boolean) Config.getSetting(NOT_ENOUGH_POWER_SOUND_ENABLED).get().value; }
	public static boolean particlesEnabled() { return (boolean) Config.getSetting(PARTICLES_ENABLED).get().value; }
	public static boolean ambientSoundEnabled() { return (boolean) Config.getSetting(AMBIENT_SOUND_ENABLED).get().value; }
	public static boolean transitionEffectEnabled() { return (boolean) Config.getSetting(TRANSITION_EFFECT_ENABLED).get().value; }

	public static void init()
	{
		Config.addInt(MAX_SIZE, 7, ConfigSetting.INT_BETWEEN(3, 128), "The maximum size of the portal including the frame.", "server", false, 4, false);
		Config.addInt(POWER_COST, 1, ConfigSetting.INT_GREATER_OR_EQUAL_TO_ZERO, "The power cost per use of a portal. Set to 0 for no cost.", "server", false, 4, false);
		Config.addInt(POWER_CAPACITY, 64, ConfigSetting.INT_GREATER_THAN_ZERO, "The amount of power a portal can store.", "server", false, 4, false);
		Config.addInt(PLAYER_TELEPORTATION_DELAY, 30, ConfigSetting.INT_GREATER_OR_EQUAL_TO_ZERO, "The amount of power a portal can store.", "server", false, 4, false);
		Config.addComplex(POWER_SOURCE, Identifier.parse("c:ender_pearls"), Settings::powerSourceIsValid, Object::toString, Identifier::tryParse, "The tag that items must have to be able to power portals (1 power per item).", "server", false, 4, false);
		Config.addBool(TELEPORTATION_SOUND_ENABLED, true, "If enabled, successful teleportation of a player, plays a sound effect.", "server", false, 4, false);
		Config.addBool(PORTAL_ACTIVATION_SOUND_ENABLED, true, "If enabled, successful portal activation plays a sound effect.", "server", false, 4, false);
		Config.addBool(NOT_ENOUGH_POWER_SOUND_ENABLED, true, "If enabled, trying to use a portal without enough power plays a sound effect signifying failure.", "server", false, 4, false);
		Config.addBool(PARTICLES_ENABLED, true, "If enabled, portals emit particles (visual effect).", "client", false, 0, true);
		Config.addBool(AMBIENT_SOUND_ENABLED, true, "If enabled, portals emit an ambient sound.", "client", false, 0, true);
		Config.addBool(TRANSITION_EFFECT_ENABLED, true, "If enabled, players get the same visual effect as if traveling through a Nether portal.", "client", false, 0, true);
	}

	public static boolean powerSourceIsValid(Object id)
	{
		Identifier sourceId = (Identifier)id;
		// Check if the passed ResourceLocation corresponds to a valid tag and cache the result for later use.
		// Note: BuiltInRegistries.ITEM.getTag() doesn't find anything with a new'd TagKey as an argument, that's why
		// the registry is searched manually here.
		var searchResult = BuiltInRegistries.ITEM.getTags().filter(namedItem -> namedItem.key().location().equals(sourceId)).findAny();

		if (searchResult.isPresent())
		{
			powerSourceTag = searchResult.get().key();
			return true;
		}

		return false;
	}
}
