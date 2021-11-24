package net.zarathul.simpleportals.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.registration.PortalRegistry;

/**
 * Responsible for saving/loading {@link PortalRegistry} data.
 */
public class PortalWorldSaveData extends SavedData
{
	private static final String DATA_NAME = SimplePortals.MOD_ID;
	
	public PortalWorldSaveData()
	{
	}

	public static PortalWorldSaveData load(CompoundTag tag)
	{
		PortalRegistry.readFromNBT(tag);

		return new PortalWorldSaveData();
	}

	@Override
	public CompoundTag save(CompoundTag tag)
	{
		PortalRegistry.writeToNBT(tag);
		return tag;
	}

	public static PortalWorldSaveData get(ServerLevel world)
	{
		if (world == null) return null;
		DimensionDataStorage storage = world.getDataStorage();

		return storage.computeIfAbsent(PortalWorldSaveData::load, PortalWorldSaveData::new, DATA_NAME);
	}
}
