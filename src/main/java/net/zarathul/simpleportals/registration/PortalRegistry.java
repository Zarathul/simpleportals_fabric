package net.zarathul.simpleportals.registration;

import com.google.common.collect.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.common.Utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The central registration for all portals.
 */
public final class PortalRegistry
{
	private static final ImmutableMap<Direction,Direction[]> cornerSearchDirs;
	private static final ListMultimap<BlockPos, Portal> portals;
	private static final ListMultimap<Address, Portal> addresses;
	private static final ListMultimap<Portal, BlockPos> gauges;
	private static final HashMap<Portal, Integer> power;
	
	static
	{
		EnumMap<Direction,Direction[]> temp = Maps.newEnumMap(Direction.class);
		temp.put(Direction.DOWN, new Direction[] { Direction.SOUTH, Direction.EAST });
		temp.put(Direction.UP, new Direction[] { Direction.SOUTH, Direction.EAST });
		temp.put(Direction.NORTH, new Direction[] { Direction.DOWN, Direction.EAST });
		temp.put(Direction.SOUTH, new Direction[] { Direction.DOWN, Direction.EAST });
		temp.put(Direction.WEST, new Direction[] { Direction.DOWN, Direction.SOUTH });
		temp.put(Direction.EAST, new Direction[] { Direction.DOWN, Direction.SOUTH });
		cornerSearchDirs = Maps.immutableEnumMap(temp);
		
		portals = ArrayListMultimap.create();
		addresses = ArrayListMultimap.create();
		gauges = ArrayListMultimap.create();
		power = Maps.newHashMap();
	}

	/**
	 * :WARNING: Completely clears the registry. :WARNING:
	 * This does not deactivate any portals, meaning no portals blocks will get removed.
	 */
	public static void clear()
	{
		portals.clear();
		addresses.clear();
		gauges.clear();
		power.clear();

		SimplePortals.portalSaveData.setDirty();
	}
	
	/**
	 * Activate a portal at the specified position.
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param pos
	 * The {@link BlockPos} of the portal frame.
	 * @param side
	 * The {@link Direction} representing the side of the portal frame that was hit
	 * by the portal activator.
	 * @return
	 * <code>true</code> if a portal could be activated, otherwise <code>false</code>.
	 */
	public static boolean activatePortal(ServerLevel world, BlockPos pos, Direction side)
	{
		if (world == null || pos == null || side == null) return false;
		
		Corner corner1 = null;
		Corner corner2;
		Corner corner3;
		Corner corner4;
		Direction firstSearchDir = null;
		
		// Find corners
		
		for (Direction searchDir : cornerSearchDirs.get(side))
		{
			corner1 = findCorner(world, pos, searchDir, side);
			
			if (corner1 != null)
			{
				firstSearchDir = searchDir;

				break;
			}
		}
		
		if (corner1 == null) return false;
		
		corner2 = findCorner(world, pos, firstSearchDir.getOpposite(), side);
		
		if (corner2 == null) return false;
		
		corner3 = findCorner(world, corner1.getPos().offset(side.getNormal()), side, firstSearchDir.getOpposite());
		
		if (corner3 == null) return false;
		
		corner4 = findCorner(world, corner3.getPos().offset(firstSearchDir.getOpposite().getNormal()), firstSearchDir.getOpposite(), side.getOpposite());
		
		if (corner4 == null || !corner4.equals(findCorner(world, corner2.getPos().offset(side.getNormal()), side, firstSearchDir))) return false;
		
		// Check size
		
		if (getDistance(corner1.getPos(), corner2.getPos()) > Settings.maxSize
			|| getDistance(corner1.getPos(), corner3.getPos()) > Settings.maxSize) return false;
		
		// Check address blocks validity
		
		BlockState addBlock1 = world.getBlockState(corner1.getPos());
		
		if (!isValidAddressBlock(addBlock1)) return false;
		
		BlockState addBlock2 = world.getBlockState(corner2.getPos());
		
		if (!isValidAddressBlock(addBlock2)) return false;
		
		BlockState addBlock3 = world.getBlockState(corner3.getPos());
		
		if (!isValidAddressBlock(addBlock3)) return false;
		
		BlockState addBlock4 = world.getBlockState(corner4.getPos());
		
		if (!isValidAddressBlock(addBlock4)) return false;
		
		// Determine portal axis
		
		int corner1Y = corner1.getPos().getY();
		
		boolean isHorizontal = (corner1Y == corner2.getPos().getY()
			&& corner1Y == corner3.getPos().getY()
			&& corner1Y == corner4.getPos().getY());
		
		// Only relevant for vertical portals.
		Axis horizontalCornerFacing = (corner1.getFacingA().getAxis() != Axis.Y)
			? corner1.getFacingA().getAxis()
			: corner1.getFacingB().getAxis();
		
		Axis portalAxis = isHorizontal
			? Axis.Y
			: Utils.getOrthogonalTo(horizontalCornerFacing);
		
		// Create portal data structure
		
		Address address = new Address(
			getAddressBlockId(addBlock1.getBlock()),
			getAddressBlockId(addBlock2.getBlock()),
			getAddressBlockId(addBlock3.getBlock()),
			getAddressBlockId(addBlock4.getBlock()));

		Portal portal = new Portal(world.dimension(), address, portalAxis, corner1, corner2, corner3, corner4);
		
		Iterable<BlockPos> portalPositions = portal.getPortalPositions();
		
		// Ensure that the inside of the frame only contains air blocks
		
		for (BlockPos checkPos : portalPositions)
		{
			if (!world.isEmptyBlock(checkPos)) return false;
		}
		
		// Place portal blocks
		
		for (BlockPos portalPos : portalPositions)
		{
			world.setBlock(portalPos, SimplePortals.blockPortal.defaultBlockState().setValue(BlockPortal.AXIS, portalAxis), Utils.SetBlockFlags.FULL_UPDATE);
		}
		
		// Find power gauges in the frame
		
		List<BlockPos> powerGauges = new ArrayList<>();
		
		for (BlockPos framePos : portal.getFramePositions(false))
		{
			if (world.getBlockState(framePos).getBlock() == SimplePortals.blockPowerGauge)
			{
				powerGauges.add(framePos);
			}
		}
		
		// Register portal
		
		register(world, portal, powerGauges);
		
		return true;
	}
	
	/**
	 * Deactivate the portal at the specified position.<br>
	 * If multiple portals share the same portal frame block,
	 * all those portals get deactivated.
	 * 
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param pos
	 * The {@link BlockPos} of one of the portals blocks (portal or frame).
	 */
	public static void deactivatePortal(ServerLevel world, BlockPos pos)
	{
		if (world == null || pos == null) return;
		
		// Copying of the list is required because unregister() changes it.
		List<Portal> affectedPortals = new ArrayList<>(portals.get(pos));
		
		// Unregister all affected portals first to avoid unnecessary deactivatePortal()
		// calls that would otherwise be caused by the destruction of the portal blocks.
		for (Portal portal : affectedPortals) unregister(world, portal);
		for (Portal portal : affectedPortals) destroyPortalBlocks(world, portal);
	}
	
	/**
	 * Determines if there is a portal at the specified position in the 
	 * specified dimension.
	 * 
	 * @param pos
	 * The {@link BlockPos} of a portal or frame block.
	 * @param dimension
	 * The dimension the portal is supposed to be in.
	 * @return
	 * <code>true</code> if the block at the specified position is part of 
	 * a registered portal, otherwise <code>false</code>.
	 */
	public static boolean isPortalAt(BlockPos pos, ResourceKey<Level> dimension)
	{
		List<Portal> portals = getPortalsAt(pos, dimension);
		
		return (portals != null) && (portals.size() > 0);
	}

	/**
	 * Gets all registered portals and their positions.
	 *
 	 * @return
	 * An immutable map containing all portals and their positions.
	 */
	public static ImmutableListMultimap<BlockPos, Portal> getPortals()
	{
		return ImmutableListMultimap.copyOf(portals);
	}

	/**
	 * Gets all registered portals and their addresses.
	 *
	 * @return
	 * An immutable map containing all portals and their addresses.
	 */
	public static ImmutableListMultimap<Address, Portal> getAddresses()
	{
		return ImmutableListMultimap.copyOf(addresses);
	}

	/**
	 * Gets the portals registered at the specified position in the
	 * specified dimension.
	 * 
	 * @param pos
	 * The {@link BlockPos} of a portal or frame block.
	 * @param dimension
	 * The dimension the portals should be in.
	 * @return
	 * A read-only list of found portals (may be empty) or <code>null</code> if
	 * <code>pos</code> was <code>null</code>.
	 */
	public static List<Portal> getPortalsAt(BlockPos pos, ResourceKey<Level> dimension)
	{
		if (pos == null) return null;
		
		List<Portal> foundPortals = portals.get(pos).stream().filter(portal -> portal.getDimension() == dimension).collect(Collectors.toList());
		
		return Collections.unmodifiableList(foundPortals);
	}

	/**
	 * Gets all the portals registered in the specified dimension.
	 *
	 * @param dimension
	 * The dimension the portals should be in.
	 * @return
	 * A read-only list of found portals (may be empty).
	 */
	public static List<Portal> getPortalsInDimension(ResourceKey<Level> dimension)
	{
		Set<Portal> uniquePortals = new HashSet<>(portals.values());

		return uniquePortals.stream().filter(portal -> portal.getDimension() == dimension).toList();
	}

	/**
	 * Gets all portals with the specified address.
	 * 
	 * @param address
	 * An portal {@link Address}.
	 * @return
	 * A read-only list of found portals (may be empty) or <code>null</code> if
	 * <code>address</code> was <code>null</code>.
	 */
	public static List<Portal> getPortalsWithAddress(Address address)
	{
		if (address == null) return null;
		
		List<Portal> foundPortals = new ArrayList<>(addresses.get(address));
		
		return Collections.unmodifiableList(foundPortals);
	}

	/**
	 * Gets all portals in every dimension.
	 *
	 * @return
	 * A read-only list of all portals (may be empty).
	 */
	public static List<Portal> getAllPortals()
	{
		Set<Portal> uniquePortals = new HashSet<>(portals.values());

		return new ArrayList<>(uniquePortals);
	}

	/**
	 * Gets the positions of all power gauges for the specified portal.
	 * 
	 * @param portal
	 * The {@link Portal} to get the power gauges for.
	 * @return
	 * A read-only list of {@link BlockPos} (may be empty) or <code>null</code> if
	 * <code>portal</code> was <code>null</code>.
	 */
	public static List<BlockPos> getPowerGauges(Portal portal)
	{
		if (portal == null) return null;
		
		List<BlockPos> foundGauges = new ArrayList<>(gauges.get(portal));
		
		return Collections.unmodifiableList(foundGauges);
	}
	
	/**
	 * Adds the specified amount of power to the specified portal.
	 * 
	 * @param portal
	 * The {@link Portal} to which the power should be added.
	 * @param amount
	 * The amount of power to add.
	 * @return
	 * The surplus power that could not be added to the portal.
	 */
	public static int addPower(Portal portal, int amount)
	{
		if (portal == null || amount < 1) return amount;
		
		int oldAmount = getPower(portal);
		int freeCapacity = Math.max(Settings.powerCapacity - oldAmount, 0);
		int amountToAdd = Math.min(freeCapacity, amount);
		int surplus = amount - amountToAdd;
		
		power.put(portal, oldAmount + amountToAdd);
		
		// Trigger save of portal data
		
		SimplePortals.portalSaveData.setDirty();
		
		return surplus;
	}
	
	/**
	 * Removes the specified amount of power from the specified portal.<br>
	 * Only ever removes power if the portal contains enough. 
	 * 
	 * @param portal
	 * The {@link Portal} from which the power should be removed.
	 * @param amount
	 * The amount of power to remove.
	 * @return
	 * <code>true</code> if the amount could be removed, otherwise <code>false</code>.
	 */
	public static boolean removePower(Portal portal, int amount)
	{
		if (portal == null || amount < 1) return false;
		
		int oldAmount = getPower(portal);
		
		if (oldAmount < amount) return false;
		
		power.put(portal, oldAmount - amount);
		
		// Trigger save of portal data
		
		SimplePortals.portalSaveData.setDirty();
		
		return true;
	}
	
	/**
	 * Gets the specified portals power.
	 * 
	 * @param portal
	 * The {@link Portal} to get the power for.
	 * @return
	 * The portals power or <code>0</code>.
	 */
	public static int getPower(Portal portal)
	{
		return (portal != null) ? power.get(portal) : 0;
	}
	
	/**
	 * Triggers comparator update for all power gauge of the specified portal.
	 * 
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param portal
	 * The {@link Portal}.
	 */
	public static void updatePowerGauges(ServerLevel world, Portal portal)
	{
		if (world == null | portal == null) return;
		
		List<BlockPos> gaugePositions = getPowerGauges(portal);
		
		for (BlockPos pos : gaugePositions)
		{
			world.updateNeighbourForOutputSignal(pos, SimplePortals.blockPowerGauge);
		}
	}
	
	/**
	 * Generates an address block id for the specified block.
	 * 
	 * @param block
	 * The {@link Block} to generate the address id for.
	 * @return
	 * A string of the format "registryName#meta" or <code>null</code>
	 * if <code>blockState</code> is <code>null</code>.
	 */
	public static String getAddressBlockId(Block block)
	{
		if (block == null) return null;

		return Registry.BLOCK.getKey(block).toString();
	}
	
	/**
	 * Destroys the specified portals' portal blocks (the center).
	 * 
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param portal
	 * The {@link Portal}.
	 */
	private static void destroyPortalBlocks(ServerLevel world, Portal portal)
	{
		for (BlockPos portalPos : portal.getPortalPositions())
		{
			world.destroyBlock(portalPos, false);
		}
	}
	
	/**
	 * Gets the distance between 2 positions.
	 * 
	 * @param pos1
	 * The first {@link BlockPos}.
	 * @param pos2
	 * The second {@link BlockPos}.
	 * @return
	 * The distance or <code>-1</code> if either position was <code>null</code>.
	 */
	private static int getDistance(BlockPos pos1, BlockPos pos2)
	{
		if (pos1 == null || pos2 == null) return -1;
		
		return Math.abs(pos1.getX() - pos2.getX() + pos1.getY() - pos2.getY() + pos1.getZ() - pos2.getZ()) + 1;
	}
	
	/**
	 * Find a corner starting at the specified position.
	 * 
	 * @param world
	 * The {@link ServerLevel}.
	 * @param startPos
	 * The starting {@link BlockPos}.
	 * @param searchDir
	 * The direction to search in.
	 * @param cornerFacing
	 * One of the directions the corner is enclosed by.
	 * @return
	 * A {@link Corner} or <code>null</code> if one of the parameters was <code>null</code> or
	 * no corner could be found.
	 */
	private static Corner findCorner(ServerLevel world, BlockPos startPos, Direction searchDir, Direction cornerFacing)
	{
		if (startPos == null || searchDir == null || cornerFacing == null) return null;
		
		BlockPos currentPos = startPos;
		int size = 0;
		
		do
		{
			if (!isPortalFrame(world, currentPos))
			{
				if (isPortalFrame(world, currentPos.relative(cornerFacing)))
				{
					return new Corner(currentPos, searchDir.getOpposite(), cornerFacing);
				}
				
				break;
			}
			
			currentPos = currentPos.relative(searchDir);
			size++;
		}
		while (size <= Settings.maxSize - 1);
		
		return null;
	}
	
	/**
	 * Registers the specified portal. 
	 * 
	 * @param world
	 * The {@link ServerLevel}.
	 * @param portal
	 * The {@link Portal} to register.
	 * @param powerGauges
	 * The power gauges that are part or the portal.
	 */
	private static void register(ServerLevel world, Portal portal, List<BlockPos> powerGauges)
	{
		if (world == null || portal == null) return;
		
		for (BlockPos portalPos : portal.getAllPositions())
		{
			portals.put(portalPos.immutable(), portal);
		}
		
		addresses.put(portal.getAddress(), portal);
		power.put(portal, 0);
		powerGauges.forEach(pos -> gauges.put(portal, pos));
		
		updatePowerGauges(world, portal);
		
		// Trigger save of portal data
		
		SimplePortals.portalSaveData.setDirty();
	}
	
	/**
	 * Unregisters the specified portal.
	 * 
	 * @param world
	 * The {@link ServerLevel}.
	 * @param portal
	 * The {@link Portal} to unregister.
	 */
	private static void unregister(ServerLevel world, Portal portal)
	{
		if (world == null || portal == null) return;
		
		for (BlockPos portalPos : portal.getAllPositions())
		{
			portals.remove(portalPos, portal);
		}
		
		addresses.remove(portal.getAddress(), portal);
		power.remove(portal);
		
		updatePowerGauges(world, portal);
		
		List<BlockPos> gaugesToRemove = getPowerGauges(portal);
		gaugesToRemove.forEach(pos -> gauges.remove(portal, pos));
		
		// Trigger save of portal data
		
		SimplePortals.portalSaveData.setDirty();
	}
	
	/**
	 * Checks if the block at the specified position is 
	 * a portal frame.
	 * 
	 * @param world
	 * The {@link ServerLevel}.
	 * @param pos
	 * The {@link BlockPos} to check.
	 * @return
	 * <code>true</code> if the block is a portal frame (power gauges also count as part of the frame), otherwise <code>false</code>.
	 */
	private static boolean isPortalFrame(ServerLevel world, BlockPos pos)
	{
		if (world == null || pos == null) return false;
		
		Block block = world.getBlockState(pos).getBlock();

		return (block == SimplePortals.blockPortalFrame || block == SimplePortals.blockPowerGauge);
	}
	
	/**
	 * Checks if the specified block can be used in a portal address.<br>
	 * Valid blocks may not have TileEntities and must be full blocks.
	 * 
	 * @param state
	 * The {@link BlockState} of the block to check.
	 * @return
	 * <code>true</code> if the block is valid, otherwise <code>false</code>.
	 */
	private static boolean isValidAddressBlock(BlockState state)
	{
		return (state != null && !state.hasBlockEntity() && !state.isAir());
	}
	
	/**
	 * Writes the registry data to a NBT compound tag.
	 * 
	 * @param nbt
	 * The {@link CompoundTag} to save the registry data in.
	 */
	public static void writeToNBT(CompoundTag nbt)
	{
		if (nbt == null) return;
		
		CompoundTag portalsTag = new CompoundTag();
		CompoundTag portalBlocksTag = new CompoundTag();
		CompoundTag powerTag = new CompoundTag();
		CompoundTag subTag;
		
		// Serialization of all Portals into a list.
		
		int i = 0;
		HashMap<Portal, Integer> portalIDs = Maps.newHashMap();
		Set<Portal> uniquePortals = new HashSet<>(portals.values());
		
		for (Portal portal : uniquePortals)
		{
			portalIDs.put(portal, i);
			powerTag.putInt(String.valueOf(i), power.get(portal));
			portalsTag.put(String.valueOf(i++), portal.serializeNBT());
		}
		
		i = 0;
		int x = 0;
		
		// Serialization of BlockPos to Portal map.
		
		for (BlockPos pos : portals.keySet())
		{
			subTag = new CompoundTag();
			subTag.putLong("pos", pos.asLong());
			subTag.putBoolean("isGauge", gauges.containsValue(pos));
			
			for (Portal portal : portals.get(pos))
			{
				subTag.putInt("portal" + x++, portalIDs.get(portal));
			}
			
			x = 0;
			
			portalBlocksTag.put(String.valueOf(i++), subTag);
		}
		
		nbt.put("portals", portalsTag);
		nbt.put("portalBlocks", portalBlocksTag);
		nbt.put("power", powerTag);
	}
	
	/**
	 * Reads the registry data from a NBT compound tag.
	 * 
	 * @param nbt
	 * The {@link CompoundTag} to read the registry data from.
	 */
	public static void readFromNBT(CompoundTag nbt)
	{
		if (nbt == null) return;
		
		portals.clear();
		addresses.clear();
		
		CompoundTag portalsTag = nbt.getCompound("portals");
		CompoundTag portalBlocksTag = nbt.getCompound("portalBlocks");
		CompoundTag powerTag = nbt.getCompound("power");
		
		int i = 0;
		String key;
		CompoundTag tag;
		Portal portal;
		
		// Get the portals and their IDs.
		
		HashMap<Integer, Portal> portalIDs = Maps.newHashMap();
		
		while(portalsTag.contains(key = String.valueOf(i)))
		{
			tag = portalsTag.getCompound(key);
			
			portal = new Portal();
			portal.deserializeNBT(tag);

			portalIDs.put(i++, portal);
		}
		
		// Deserialization of BlockPos to Portal map.
		
		i = 0;
		int x = 0;
		String subKey;
		BlockPos portalPos;
		boolean isGauge;
		
		while (portalBlocksTag.contains(key = String.valueOf(i++)))
		{
			tag = portalBlocksTag.getCompound(key);
			
			portalPos = BlockPos.of(tag.getLong("pos"));
			isGauge = tag.getBoolean("isGauge");
			
			while (tag.contains(subKey = "portal" + x++))
			{
				portal = portalIDs.get(tag.getInt(subKey));
				portals.put(portalPos, portal);
				
				if (isGauge) gauges.put(portal, portalPos);
			}
			
			x = 0;
		}
		
		// Regeneration of Address to Portal map.
		
		for (Portal p : portalIDs.values())
		{
			addresses.put(p.getAddress(), p);
		}
		
		// Generate power map.
		
		i = 0;
		
		while (powerTag.contains(key = String.valueOf(i)))
		{
			power.put(portalIDs.get(i++), powerTag.getInt(key));
		}
	}
}