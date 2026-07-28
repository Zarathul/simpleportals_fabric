package net.zarathul.simpleportals.registration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.common.Utils;

import java.util.*;

/**
 * The central registration for all portals.
 */
public final class PortalRegistry extends SavedData
{
	private final ImmutableMap<Direction,Direction[]> cornerSearchDirs;
	private final ListMultimap<BlockPos, Portal> portals;
	private final ListMultimap<Address, Portal> addresses;
	private final ListMultimap<Portal, BlockPos> gauges;
	private final HashMap<Portal, Integer> power;

	/**
	 * Gets all registered portal positions alongside the respective portals.
	 * Primarily used for serialization.
	 *
	 * @return
	 * A list of {@link PortalEntry}s consisting of the portals position and the portal itself.
	 */
	public List<PortalEntry> portals()
	{
		var portalEntries = new ArrayList<PortalEntry>(portals.size());
		portals.forEach((blockPos, portal) -> portalEntries.add(new PortalEntry(blockPos, portal)));

		return portalEntries;
	}

	/**
	 * Gets all registered addresses alongside the respective portals.
	 * Primarily used for serialization.
	 *
	 * @return
	 * A list of {@link AddressEntry}s consisting of the portals address and the portal itself.
	 */
	public List<AddressEntry> addresses()
	{
		var addressEntries = new ArrayList<AddressEntry>(addresses.size());
		addresses.forEach((address, portal) -> addressEntries.add(new AddressEntry(address, portal)));

		return addressEntries;
	}

	/**
	 * Gets all registered portals with power gauges, alongside the power gauges in those portals.
	 * Primarily used for serialization.
	 *
	 * @return
	 * A list of {@link GaugeEntry}s consisting of the portals and the position of the power gauge.
	 */
	public List<GaugeEntry> gauges()
	{
		var gaugeEntries = new ArrayList<GaugeEntry>(gauges.size());
		gauges.forEach((portal, blockPos) -> gaugeEntries.add(new GaugeEntry(portal, blockPos)));

		return gaugeEntries;
	}

	/**
	 * Gets all registered portals alongside their respective power values.
	 * Primarily used for serialization.
	 *
	 * @return
	 * A list of {@link PowerEntry}s consisting of the portals and their power values.
	 */
	public List<PowerEntry> power()
	{
		var powerEntries = new ArrayList<PowerEntry>(power.size());
		power.forEach((portal, integer) -> powerEntries.add(new PowerEntry(portal, integer)));

		return powerEntries;
	}

	public PortalRegistry()
	{
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public PortalRegistry(List<PortalEntry> portals, List<AddressEntry> addresses, List<GaugeEntry> gauges, List<PowerEntry> power)
	{
		// CopyPasta from the default constructor, because calling 'this()' would also initialize all the maps.
		// Factoring out the initialization is also not an option, because the 'cornerSearchDirs' field is final.
		EnumMap<Direction,Direction[]> temp = Maps.newEnumMap(Direction.class);
		temp.put(Direction.DOWN, new Direction[] { Direction.SOUTH, Direction.EAST });
		temp.put(Direction.UP, new Direction[] { Direction.SOUTH, Direction.EAST });
		temp.put(Direction.NORTH, new Direction[] { Direction.DOWN, Direction.EAST });
		temp.put(Direction.SOUTH, new Direction[] { Direction.DOWN, Direction.EAST });
		temp.put(Direction.WEST, new Direction[] { Direction.DOWN, Direction.SOUTH });
		temp.put(Direction.EAST, new Direction[] { Direction.DOWN, Direction.SOUTH });
		cornerSearchDirs = Maps.immutableEnumMap(temp);

		this.portals = ArrayListMultimap.create();
		portals.forEach(portalEntry -> this.portals.put(portalEntry.pos, portalEntry.portal));

		this.addresses = ArrayListMultimap.create();
		addresses.forEach(addressEntry -> this.addresses.put(addressEntry.address, addressEntry.portal));

		this.gauges = ArrayListMultimap.create();
		gauges.forEach(gaugeEntry -> this.gauges.put(gaugeEntry.portal, gaugeEntry.pos));

		this.power = Maps.newHashMap();
		power.forEach(powerEntry -> this.power.put(powerEntry.portal, powerEntry.power));
	}

	/**
	 * :WARNING: Completely clears the registry. :WARNING:
	 * This does not deactivate any portals, meaning no portals blocks will get removed.
	 */
	public void clear()
	{
		portals.clear();
		addresses.clear();
		gauges.clear();
		power.clear();

		// Trigger save of portal data
		setDirty();
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
	public boolean activatePortal(ServerLevel world, BlockPos pos, Direction side)
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
		
		corner3 = findCorner(world, corner1.pos().offset(side.getUnitVec3i()), side, firstSearchDir.getOpposite());
		
		if (corner3 == null) return false;
		
		corner4 = findCorner(world, corner3.pos().offset(firstSearchDir.getOpposite().getUnitVec3i()), firstSearchDir.getOpposite(), side.getOpposite());
		
		if (corner4 == null || !corner4.equals(findCorner(world, corner2.pos().offset(side.getUnitVec3i()), side, firstSearchDir))) return false;
		
		// Check size
		
		if (getDistance(corner1.pos(), corner2.pos()) > Settings.maxSize()
			|| getDistance(corner1.pos(), corner3.pos()) > Settings.maxSize()) return false;
		
		// Check address blocks validity
		
		BlockState addBlock1 = world.getBlockState(corner1.pos());
		
		if (!isValidAddressBlock(addBlock1)) return false;
		
		BlockState addBlock2 = world.getBlockState(corner2.pos());
		
		if (!isValidAddressBlock(addBlock2)) return false;
		
		BlockState addBlock3 = world.getBlockState(corner3.pos());
		
		if (!isValidAddressBlock(addBlock3)) return false;
		
		BlockState addBlock4 = world.getBlockState(corner4.pos());
		
		if (!isValidAddressBlock(addBlock4)) return false;
		
		// Determine portal axis
		
		int corner1Y = corner1.pos().getY();
		
		boolean isHorizontal = (corner1Y == corner2.pos().getY()
			&& corner1Y == corner3.pos().getY()
			&& corner1Y == corner4.pos().getY());
		
		// Only relevant for vertical portals.
		Axis horizontalCornerFacing = (corner1.facingA().getAxis() != Axis.Y)
			? corner1.facingA().getAxis()
			: corner1.facingB().getAxis();
		
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
	public void deactivatePortal(ServerLevel world, BlockPos pos)
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
	public boolean isPortalAt(BlockPos pos, ResourceKey<Level> dimension)
	{
		List<Portal> portals = getPortalsAt(pos, dimension);
		
		return (portals != null) && (!portals.isEmpty());
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
	public List<Portal> getPortalsAt(BlockPos pos, ResourceKey<Level> dimension)
	{
		if (pos == null) return null;

		return portals.get(pos).stream().filter(portal -> portal.dimension() == dimension).toList();
	}

	/**
	 * Gets all the portals registered in the specified dimension.
	 *
	 * @param dimension
	 * The dimension the portals should be in.
	 * @return
	 * A read-only list of found portals (may be empty).
	 */
	public List<Portal> getPortalsInDimension(ResourceKey<Level> dimension)
	{
		Set<Portal> uniquePortals = new HashSet<>(portals.values());

		return uniquePortals.stream().filter(portal -> portal.dimension() == dimension).toList();
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
	public List<Portal> getPortalsWithAddress(Address address)
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
	public List<Portal> getAllPortals()
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
	public List<BlockPos> getPowerGauges(Portal portal)
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
	public int addPower(Portal portal, int amount)
	{
		if (portal == null || amount < 1) return amount;
		
		int oldAmount = getPortalPower(portal);
		int freeCapacity = Math.max(Settings.powerCapacity() - oldAmount, 0);
		int amountToAdd = Math.min(freeCapacity, amount);
		int surplus = amount - amountToAdd;
		
		power.put(portal, oldAmount + amountToAdd);
		
		// Trigger save of portal data
		setDirty();
		
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
	public boolean removePower(Portal portal, int amount)
	{
		if (portal == null || amount < 1) return false;
		
		int oldAmount = getPortalPower(portal);
		
		if (oldAmount < amount) return false;
		
		power.put(portal, oldAmount - amount);
		
		// Trigger save of portal data
		setDirty();
		
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
	public int getPortalPower(Portal portal)
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
	public void updatePowerGauges(ServerLevel world, Portal portal)
	{
		if (world == null | portal == null) return;
		
		List<BlockPos> gaugePositions = getPowerGauges(portal);
		
		for (BlockPos pos : gaugePositions)
		{
			world.updateNeighbourForOutputSignal(pos, SimplePortals.blockPowerGauge);
		}
	}

	/***
	 * Gets a location in direct vicinity of the portal where an entity can be transported to and
	 * a facing direction for the entity looking away from the portal.
	 *
	 * @param portal
	 * The {@link Portal} to teleport to.
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param entity
	 * The {@link Entity} which is supposed to be teleported.
	 * @return
	 * A {@link TeleportationDestination}, or <code>null</code> if no location could be found.
	 */
	public static TeleportationDestination getTeleportDestination(Portal portal, ServerLevel world, Entity entity)
	{
		BlockPos destinationPos = getPortDestination(portal, world, (int)Math.ceil(entity.getBbHeight()));
		if (destinationPos == null) return null;

		// Get a facing pointing away from the destination portal. After porting, the portal
		// will always be behind the entity. When porting to a horizontal portal the initial
		// facing is not changed.
		Direction entityFacing = switch (portal.axis())
		{
			case X -> (destinationPos.getX() > portal.corner1().pos().getX()) ? Direction.EAST : Direction.WEST;
			case Y -> entity.getDirection();
			case Z -> (destinationPos.getZ() > portal.corner1().pos().getZ()) ? Direction.SOUTH : Direction.NORTH;
		};

		return new TeleportationDestination(destinationPos, entityFacing);
	}

	/**
	 * Gets a possible spawn location for an entity of the specified height for a given portal.
	 *
	 * @param portal
	 * The {@link Portal} which to find the spawn location for.
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param entityHeight
	 * The height of the entity the spawn point should be searched for.
	 * @return
	 * A {@link BlockPos} representing a possible spawn location or <code>null</code>.
	 */
	public static BlockPos getPortDestination(Portal portal, ServerLevel world, int entityHeight)
	{
		if (world == null || entityHeight < 1) return null;

		// Horizontal portal.

		if (portal.axis() == Axis.Y)
		{
			List<BlockPos> framePositions = portal.getFramePositions();

			BlockPos spawnLocation;

			// Check for valid spawn positions on top of the frame blocks.

			for (BlockPos framePos : framePositions)
			{
				spawnLocation = framePos.above();
				if (canEntitySpawnAt(world, spawnLocation, entityHeight)) return spawnLocation;
			}

			// Check for valid spawn positions below the portal blocks starting at the center.

			BlockPos portal1 = portal.corner1().getInnerCornerPos();
			BlockPos portal2 = portal.corner4().getInnerCornerPos();
			Axis[] portalAxis = new Axis[] { Axis.X, Axis.Z };
			int[] mins = new int[2];
			int[] maxes = new int[2];

			for (int i = 0; i < portalAxis.length; i++)
			{
				if (Utils.getAxisValue(portal1, portalAxis[i]) < Utils.getAxisValue(portal2, portalAxis[i]))
				{
					mins[i] = Utils.getAxisValue(portal1, portalAxis[i]);
					maxes[i] = Utils.getAxisValue(portal2, portalAxis[i]);
				}
				else
				{
					mins[i] = Utils.getAxisValue(portal2, portalAxis[i]);
					maxes[i] = Utils.getAxisValue(portal1, portalAxis[i]);
				}
			}

			int minX = mins[0];
			int maxX = maxes[0];
			int minZ = mins[1];
			int maxZ = maxes[1];
			int y = portal1.getY() - entityHeight;
			int width = Math.abs(maxX - minX) + 1;
			int height = Math.abs(maxZ - minZ) + 1;
			int halfWidth = Math.floorDiv(width, 2);
			int halfHeight = Math.floorDiv(height, 2);

			Direction[] zAxisFacings = new Direction[] { Direction.SOUTH, Direction.NORTH };
			BlockPos center = new BlockPos(minX + halfWidth, y, minZ + halfHeight);
			BlockPos currentPos;

			for (int z = 0; z <= halfHeight; z++)
			{
				for (Direction zFacing : zAxisFacings)
				{
					for (int x = 0; x <= halfWidth; x++)
					{
						currentPos = center.east(x).relative(zFacing, z);

						if (currentPos.getX() <= maxX && currentPos.getZ() <= maxZ)
						{
							if (canEntitySpawnAt(world, currentPos, entityHeight)) return currentPos;
						}

						currentPos = center.west(x).relative(zFacing, z);

						if (currentPos.getX() >= minX && currentPos.getZ() >= minZ)
						{
							if (canEntitySpawnAt(world, currentPos, entityHeight)) return currentPos;
						}
					}
				}
			}

			return null;
		}

		// Vertical portal.

		BlockPos portal1 = portal.corner1().getInnerCornerPos();
		BlockPos portal2 = portal.corner4().getInnerCornerPos();
		int width, height, lowBound, highBound;

		// Get the axis for possible spawn locations and the corresponding
		// axis values for the 2 corner portal blocks.
		Axis cornerAxis = Utils.getOrthogonalTo(portal.axis());
		int portal1AxisValue = Utils.getAxisValue(portal1, cornerAxis);
		int portal2AxisValue = Utils.getAxisValue(portal2, cornerAxis);

		width = Math.abs(portal1AxisValue - portal2AxisValue) + 1;
		height = Math.abs(portal1.getY() - portal2.getY()) + 1;

		if (portal1AxisValue < portal2AxisValue)
		{
			lowBound = portal1AxisValue;
			highBound = portal2AxisValue;
		}
		else
		{
			lowBound = portal2AxisValue;
			highBound = portal1AxisValue;
		}

		int halfWidth = Math.floorDiv(width, 2);
		int middle = lowBound + halfWidth;
		int startHeight = Math.min(portal1.getY(), portal2.getY());

		// e.g. Axis.Z and AxisDirection.POSITIVE returns Direction.SOUTH.
		Direction searchDirPositive = Direction.fromAxisAndDirection(cornerAxis, Direction.AxisDirection.POSITIVE);
		Direction searchDirNegative = Direction.fromAxisAndDirection(cornerAxis, Direction.AxisDirection.NEGATIVE);

		BlockPos searchStartPos1 = (portal.axis() == Axis.Z)
								   ? new BlockPos(middle, startHeight, portal1.south(1).getZ())
								   : new BlockPos(portal1.east(1).getX(), startHeight, middle);

		BlockPos searchStartPos2 = (portal.axis() == Axis.Z)
								   ? new BlockPos(middle, startHeight, portal1.north(1).getZ())
								   : new BlockPos(portal1.west(1).getX(), startHeight, middle);

		BlockPos[] searchStartPositions = new BlockPos[] { searchStartPos1, searchStartPos2 };

		BlockPos feetPos;
		BlockPos currentFeetPos;

		// Find the lowest position where the entity can spawn at either side.
		// Search order is south before north and east before west.

		for (int y = 0; y <= height - entityHeight; y++)
		{
			for (BlockPos startPos : searchStartPositions)
			{
				currentFeetPos = startPos.above(y);

				for (int x = 0; x <= halfWidth; x++)
				{
					feetPos = currentFeetPos.relative(searchDirPositive, x);

					if (Utils.getAxisValue(feetPos, cornerAxis) <= highBound)
					{
						if (canEntitySpawnAt(world, feetPos, entityHeight)) return feetPos;
					}

					feetPos = currentFeetPos.relative(searchDirNegative, x);

					if (Utils.getAxisValue(feetPos, cornerAxis) >= lowBound)
					{
						if (canEntitySpawnAt(world, feetPos, entityHeight)) return feetPos;
					}
				}
			}
		}

		return null;
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

		return BuiltInRegistries.BLOCK.getKey(block).toString();
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
		int size = 1;
		
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
		while (size <= Settings.maxSize());
		
		return null;
	}

	/**
	 * Check if an entity of the specified height can spawn at the specified
	 * position.
	 *
	 * @param world
	 * The {@link ServerLevel} to check in.
	 * @param pos
	 * The position of the lowest point (feet) of the entity.
	 * @param entityHeight
	 * The entities' height.
	 * @return
	 * <code>true</code> if the entity can spawn at the location, otherwise <code>false</code>.
	 */
	private static boolean canEntitySpawnAt(ServerLevel world, BlockPos pos, int entityHeight)
	{
		if (world == null || pos == null || entityHeight < 1) return false;

		for (int i = 0; i < entityHeight; i ++)
		{
			if (!world.isEmptyBlock(pos.above(i))) return false;
		}

		return true;
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
	private void register(ServerLevel world, Portal portal, List<BlockPos> powerGauges)
	{
		if (world == null || portal == null) return;
		
		for (BlockPos portalPos : portal.getAllPositions())
		{
			portals.put(portalPos.immutable(), portal);
		}
		
		addresses.put(portal.address(), portal);
		power.put(portal, 0);
		powerGauges.forEach(pos -> gauges.put(portal, pos));
		
		updatePowerGauges(world, portal);
		
		// Trigger save of portal data
		setDirty();
	}
	
	/**
	 * Unregisters the specified portal.
	 * 
	 * @param world
	 * The {@link ServerLevel}.
	 * @param portal
	 * The {@link Portal} to unregister.
	 */
	private void unregister(ServerLevel world, Portal portal)
	{
		if (world == null || portal == null) return;
		
		for (BlockPos portalPos : portal.getAllPositions())
		{
			portals.remove(portalPos, portal);
		}
		
		addresses.remove(portal.address(), portal);
		power.remove(portal);
		
		updatePowerGauges(world, portal);
		
		List<BlockPos> gaugesToRemove = getPowerGauges(portal);
		gaugesToRemove.forEach(pos -> gauges.remove(portal, pos));
		
		// Trigger save of portal data
		setDirty();
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

	public record TeleportationDestination(BlockPos pos, Direction facing) {}

	// Used for serialization. Using these as an intermediary step is a workaround, because I could not figure out how to serialize ArrayListMultimap directly.
	public record PortalEntry(BlockPos pos, Portal portal)
	{
		public static final Codec<PortalEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(PortalEntry::pos),
				Portal.CODEC.fieldOf("portal").forGetter(PortalEntry::portal)
		).apply(instance, PortalEntry::new));
	}

	public record AddressEntry(Address address, Portal portal)
	{
		public static final Codec<AddressEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Address.CODEC.fieldOf("address").forGetter(AddressEntry::address),
				Portal.CODEC.fieldOf("portal").forGetter(AddressEntry::portal)
		).apply(instance, AddressEntry::new));
	}

	public record GaugeEntry(Portal portal, BlockPos pos)
	{
		public static final Codec<GaugeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Portal.CODEC.fieldOf("portal").forGetter(GaugeEntry::portal),
				BlockPos.CODEC.fieldOf("pos").forGetter(GaugeEntry::pos)
		).apply(instance, GaugeEntry::new));
	}

	public record PowerEntry(Portal portal, int power)
	{
		public static final Codec<PowerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Portal.CODEC.fieldOf("portal").forGetter(PowerEntry::portal),
				Codec.INT.fieldOf("power").forGetter(PowerEntry::power)
		).apply(instance, PowerEntry::new));
	}

	public static final Codec<PortalRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.list(PortalEntry.CODEC).fieldOf("portals").forGetter(PortalRegistry::portals),
					Codec.list(AddressEntry.CODEC).fieldOf("addresses").forGetter(PortalRegistry::addresses),
					Codec.list(GaugeEntry.CODEC).fieldOf("gauges").forGetter(PortalRegistry::gauges),
					Codec.list(PowerEntry.CODEC).fieldOf("power").forGetter(PortalRegistry::power)
			).apply(instance, PortalRegistry::new)
	);

	public static final SavedDataType<PortalRegistry> TYPE = new SavedDataType<>(Utils.createModIdentifier("portal_registry"), PortalRegistry::new, CODEC, null	);
}