package net.zarathul.simpleportals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.world.item.Item;
import net.zarathul.simpleportals.configuration.ConfigSetting;

public final class Settings
{
	@ConfigSetting(descriptionKey = "max_size", description = "The maximum size of the portal including the frame.", category = "common")
	public static int maxSize;
	public static final int maxSizeDefault = 7;
	public static boolean maxSizeValidator(int value)
	{
		return ((value >= 3) && (value <= 128));
	}

	@ConfigSetting(descriptionKey = "power_cost", description = "The power cost per use of a portal. Set to 0 for no cost.", category = "common")
	public static int powerCost;
	public static final int powerCostDefault = 1;
	public static boolean powerCostValidator(int value)
	{
		return (value >= 0);
	}

	@ConfigSetting(descriptionKey = "power_capacity", description = "The amount of power a portal can store.", category = "common")
	public static int powerCapacity;
	public static final int powerCapacityDefault = 64;
	public static boolean powerCapacityValidator(int value)
	{
		return (value > 0);
	}

	@ConfigSetting(descriptionKey = "player_teleportation_delay", description = "The delay in ticks before a player actually gets teleported. Needs to be lower than the cooldown.", category = "common")
	public static int playerTeleportationDelay;
	public static final int playerTeleportationDelayDefault = 10;
	public static boolean playerTeleportationDelayValidator(int value)
	{
		return ((value >= 0) && (value <= 40));
	}

	@ConfigSetting(descriptionKey = "player_teleportation_cooldown", description = "Cooldown in ticks before a player can be teleported again by a portal.", category = "common")
	public static int playerTeleportationCooldown;
	public static final int playerTeleportationCooldownDefault = 60;
	public static boolean playerTeleportationCooldownValidator(int value)
	{
		return (value >= 60);
	}

	@ConfigSetting(descriptionKey = "power_source", description = "The tag that items must have to be able to power portals (1 power per item).", category = "common")
	public static ResourceLocation powerSource;
	public static final ResourceLocation powerSourceDefault = new ResourceLocation("c:ender_pearls");
	public static ResourceLocation powerSourceLoad(String value)
	{
		return ResourceLocation.tryParse(value);
	}
	public static String powerSourceSave(ResourceLocation value)
	{
		return value.toString();
	}
	public static boolean powerSourceValidator(ResourceLocation value)
	{
		// When the config file is first loaded, data-packs (including tags) are not loaded yet.
		// Without the tag list there is no way to check if a ResourceLocation is valid, so accept them all.
		// Without doing this, a value for powerSource could never be loaded from the config file, because
		// it would always be invalid.
		TagCollection<Item> tags = ItemTags.getAllTags();
		if ((tags == null) || (tags.getAllTags().size() == 0)) return true;

		// Check if the passed ResourceLocation corresponds to a valid tag and cache the result for later use.
		powerSourceTag = tags.getTag(value);
		return (powerSourceTag != null);
	}
	public static Tag<Item> powerSourceTag;

	@ConfigSetting(descriptionKey = "particles_enabled", description = "If enabled, portals emit particles (visual effect).", category = "client")
	public static boolean particlesEnabled;
	public static final boolean particlesEnabledDefault = true;

	@ConfigSetting(descriptionKey = "ambient_sound_enabled", description = "If enabled, portals emit an ambient sound.", category = "client")
	public static boolean ambientSoundEnabled ;
	public static final boolean ambientSoundEnabledDefault = false;

	@ConfigSetting(descriptionKey = "teleportation_sound_enabled", description = "If enabled, a sound effect is played to the player after a successful teleportation.", category = "client")
	public static boolean teleportationSoundEnabled;
	public static final boolean teleportationSoundEnabledDefault = true;
}
