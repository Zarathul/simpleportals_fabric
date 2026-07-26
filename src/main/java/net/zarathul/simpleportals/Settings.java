package net.zarathul.simpleportals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.zarathul.simpleportals.configuration.ConfigSetting;

public final class Settings
{
	@ConfigSetting(descriptionKey = "max_size", description = "The maximum size of the portal including the frame.", category = "server", permissionLvl = 4)
	public static int maxSize;
	public static final int maxSizeDefault = 7;
	public static boolean maxSizeValidator(int value)
	{
		return ((value >= 3) && (value <= 128));
	}

	@ConfigSetting(descriptionKey = "power_cost", description = "The power cost per use of a portal. Set to 0 for no cost.", category = "server", permissionLvl = 4)
	public static int powerCost;
	public static final int powerCostDefault = 1;
	public static boolean powerCostValidator(int value)
	{
		return (value >= 0 && value <= powerCapacity);
	}

	@ConfigSetting(descriptionKey = "power_capacity", description = "The amount of power a portal can store.", category = "server", permissionLvl = 4)
	public static int powerCapacity;
	public static final int powerCapacityDefault = 64;
	public static boolean powerCapacityValidator(int value)
	{
		return (value > 0 && value >= powerCost);
	}

	@ConfigSetting(descriptionKey = "player_teleportation_delay", description = "The delay in ticks before a player actually gets teleported.", category = "server", permissionLvl = 4)
	public static int playerTeleportationDelay;
	public static final int playerTeleportationDelayDefault = 30;
	public static boolean playerTeleportationDelayValidator(int value) { return value >= 0;	}

	@ConfigSetting(descriptionKey = "power_source", description = "The tag that items must have to be able to power portals (1 power per item).", category = "server", permissionLvl = 4)
	public static Identifier powerSource;
	public static final Identifier powerSourceDefault = Identifier.parse("c:ender_pearls");
	public static Identifier powerSourceLoad(String value)
	{
		return Identifier.tryParse(value);
	}
	public static String powerSourceSave(Identifier value)
	{
		return value.toString();
	}
	public static boolean powerSourceValidator(Identifier sourceId)
	{
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
	public static TagKey<Item> powerSourceTag;

	@ConfigSetting(descriptionKey = "teleportation_sound_enabled", description = "If enabled, successful teleportation of a player, plays a sound effect.", category = "server")
	public static boolean teleportationSoundEnabled;
	public static final boolean teleportationSoundEnabledDefault = true;

	@ConfigSetting(descriptionKey = "portal_activation_sound_enabled", description = "If enabled, successful portal activation plays a sound effect.", category = "server")
	public static boolean portalActivationSoundEnabled;
	public static final boolean portalActivationSoundEnabledDefault = true;

	@ConfigSetting(descriptionKey = "not_enough_power_sound_enabled", description = "If enabled, trying to use a portal without enough power plays a sound effect signifying failure.", category = "server")
	public static boolean notEnoughPowerSoundEnabled;
	public static final boolean notEnoughPowerSoundEnabledDefault = true;

	// Client-only

	@ConfigSetting(descriptionKey = "particles_enabled", description = "If enabled, portals emit particles (visual effect).", category = "client", clientOnly = true)
	public static boolean particlesEnabled;
	public static final boolean particlesEnabledDefault = true;

	@ConfigSetting(descriptionKey = "ambient_sound_enabled", description = "If enabled, portals emit an ambient sound.", category = "client", clientOnly = true)
	public static boolean ambientSoundEnabled;
	public static final boolean ambientSoundEnabledDefault = false;

	@ConfigSetting(descriptionKey = "transition_effect_enabled", description = "If enabled, players get the same visual effect as if traveling through a Nether portal.", category = "client", clientOnly = true)
	public static boolean transitionEffectEnabled;
	public static final boolean transitionEffectEnabledDefault = true;
}
