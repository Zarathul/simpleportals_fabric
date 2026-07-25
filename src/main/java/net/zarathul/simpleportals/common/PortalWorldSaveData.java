package net.zarathul.simpleportals.common;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.registration.PortalRegistry;

/**
 * Responsible for saving/loading {@link PortalRegistry} data.
 */
//public class PortalWorldSaveData extends SavedData
//{
//	private static final Identifier ID = Identifier.fromNamespaceAndPath(SimplePortals.MOD_ID, "portal_registry");
//	private static final Codec<PortalWorldSaveData> CODEC =
//
//	public static final SavedDataType<PortalWorldSaveData> TYPE = new SavedDataType<>(
//			ID,
//			PortalWorldSaveData::new,
//			CODEC,
//			null
//	);
//
//	public PortalWorldSaveData()
//	{
//	}
//
//	public static PortalWorldSaveData load(CompoundTag tag)
//	{
//		PortalRegistry.readFromNBT(tag);
//
//		return new PortalWorldSaveData();
//	}
//
//	@Override
//	public CompoundTag save(CompoundTag tag)
//	{
//		PortalRegistry.writeToNBT(tag);
//		return tag;
//	}
//
//	public static PortalWorldSaveData get(ServerLevel world)
//	{
//		if (world == null) return null;
//		var storage = world.getDataStorage();
//
//		return storage.computeIfAbsent(PortalWorldSaveData::load, PortalWorldSaveData::new, DATA_NAME);
//	}
//}
