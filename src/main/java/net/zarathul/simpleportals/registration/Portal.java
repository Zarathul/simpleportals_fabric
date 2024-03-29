package net.zarathul.simpleportals.registration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.blocks.BlockPortalFrame;
import net.zarathul.simpleportals.common.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a portal.<br>
 * Note: Corner1 and Corner4 must be diagonal to each other,
 * same for Corner2 and Corner3.
 */
public class Portal
{
	private ResourceKey<Level> dimension;
	private Address address;
	private Axis axis;
	private Corner corner1;
	private Corner corner2;
	private Corner corner3;
	private Corner corner4;

	public Portal()
	{
	}
	
	public Portal(ResourceKey<Level> dimension, Address address, Axis axis,
				  Corner corner1, Corner corner2,
			      Corner corner3, Corner corner4)
	{
		this.dimension = dimension;
		this.address = address;
		this.axis = axis;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.corner3 = corner3;
		this.corner4 = corner4;
	}
	
	/**
	 * Gets the dimension the portal is located in.
	 * 
	 * @return
	 * A <code>RegistryKey<World></code> representing the dimension.
	 */
	public ResourceKey<Level> getDimension()
	{
		return dimension;
	}
	
	/**
	 * Gets the portals address.
	 * 
	 * @return
	 * The portals {@link Address} or <code>null</code>.
	 */
	public Address getAddress()
	{
		return address;
	}
	
	/**
	 * Gets the axis the portal is aligned to.
	 * 
	 * @return
	 * One of the {@link Axis} values or <code>null</code>.
	 */
	public Axis getAxis()
	{
		return axis;
	}
	
	/**
	 * Gets the portals first corner.
	 * 
	 * @return
	 * A {@link Corner} or <code>null</code>.
	 */
	public Corner getCorner1()
	{
		return corner1;
	}
	
	/**
	 * Gets the portals second corner.
	 * 
	 * @return
	 * A {@link Corner} or <code>null</code>.
	 */
	public Corner getCorner2()
	{
		return corner2;
	}
	
	/**
	 * Gets the portals third corner.
	 * 
	 * @return
	 * A {@link Corner} or <code>null</code>.
	 */
	public Corner getCorner3()
	{
		return corner3;
	}
	
	/**
	 * Gets the portals fourth corner.
	 * 
	 * @return
	 * A {@link Corner} or <code>null</code>.
	 */
	public Corner getCorner4()
	{
		return corner4;
	}
	
	/**
	 * Gets the positions of all blocks making up the portal.
	 * 
	 * @return
	 * An {@link Iterable} of {@link BlockPos}.
	 */
	public Iterable<BlockPos> getAllPositions()
	{
		return BlockPos.betweenClosed(corner1.getPos(), corner4.getPos());
	}
	
	/**
	 * Gets the positions of the actual portal blocks inside the portal frame.
	 * 
	 * @return
	 * An {@link Iterable} of {@link BlockPos}.
	 */
	public Iterable<BlockPos> getPortalPositions()
	{
		return BlockPos.betweenClosed(corner1.getInnerCornerPos(), corner4.getInnerCornerPos());
	}
	
	/**
	 * Gets the positions of all blocks making up the portals frame including the corners.
	 * 
	 * @return
	 * A list of {@link BlockPos}.
	 */
	public List<BlockPos> getFramePositions()
	{
		return getFramePositions(true);
	}
	
	/**
	 * Gets the positions of all blocks making up the portals frame.
	 * Inclusion of corner address blocks is optional.
	 * 
	 * @param includeCorners
	 * Determines if corner blocks should be included.
	 * @return
	 * A list of {@link BlockPos}.
	 */
	public List<BlockPos> getFramePositions(boolean includeCorners)
	{
		ArrayList<BlockPos> frame = new ArrayList<>();

		// Get relative directions of the first and forth corners to their adjacent corners.
		
		Direction dir1To2 = Utils.getRelativeDirection(corner1.getPos(), corner2.getPos());
		Direction dir1To3 = Utils.getRelativeDirection(corner1.getPos(), corner3.getPos());
		Direction dir4To2 = Utils.getRelativeDirection(corner4.getPos(), corner2.getPos());
		Direction dir4To3 = Utils.getRelativeDirection(corner4.getPos(), corner3.getPos());
		
		// Offset the corner positions towards their adjacent corners and get all positions
		// in between. This way we get all the frame positions without the corners themselves.
		
		BlockPos from1 = corner1.getPos().relative(dir1To2);
		BlockPos to1   = corner2.getPos().relative(dir1To2.getOpposite());

		BlockPos from2 = corner1.getPos().relative(dir1To3);
		BlockPos to2   = corner3.getPos().relative(dir1To3.getOpposite());

		BlockPos from3 = corner4.getPos().relative(dir4To2);
		BlockPos to3   = corner2.getPos().relative(dir4To2.getOpposite());

		BlockPos from4 = corner4.getPos().relative(dir4To3);
		BlockPos to4   = corner3.getPos().relative(dir4To3.getOpposite());

		// BlockPos.getAllInBox() delivers wrong results (duplicates and missing positions).
		// So I have to do this nonsense. Minecraft 1.14.4 (10.10.2019)
		for (BlockPos pos : BlockPos.betweenClosed(from1, to1)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from2, to2)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from3, to3)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from4, to4)) frame.add(pos.immutable());

		if (includeCorners)
		{
			frame.add(corner1.getPos());
			frame.add(corner2.getPos());
			frame.add(corner3.getPos());
			frame.add(corner4.getPos());
		}
		
		return frame;
	}
	
	/**
	 * Gets a possible spawn location for an entity of the specified height.
	 * 
	 * @param world
	 * The {@link ServerLevel} the portal is located in.
	 * @param entityHeight
	 * The height of the entity the spawn point should be searched for.
	 * @return
	 * A {@link BlockPos} representing a possible spawn location or <code>null</code>.
	 */
	public BlockPos getPortDestination(ServerLevel world, int entityHeight)
	{
		if (world == null || entityHeight < 1) return null;
		
		// Horizontal portal.
		
		if (axis == Axis.Y)
		{
			List<BlockPos> framePositions = getFramePositions();
			
			BlockPos spawnLocation;
			
			// Check for valid spawn positions on top of the frame blocks.
			
			for (BlockPos framePos : framePositions)
			{
				spawnLocation = framePos.above();
				if (canEntitySpawnAt(world, spawnLocation, entityHeight)) return spawnLocation;
			}
			
			// Check for valid spawn positions below the portal blocks starting at the center.
			
			BlockPos portal1 = corner1.getInnerCornerPos();
			BlockPos portal2 = corner4.getInnerCornerPos();
			Axis[] portalAxis = new Axis[] { Axis.X, Axis.Z };
			int[] mins = new int[2];
			int[] maxs = new int[2];
			
			for (int i = 0; i < portalAxis.length; i++)
			{
				if (Utils.getAxisValue(portal1, portalAxis[i]) < Utils.getAxisValue(portal2, portalAxis[i]))
				{
					mins[i] = Utils.getAxisValue(portal1, portalAxis[i]);
					maxs[i] = Utils.getAxisValue(portal2, portalAxis[i]);
				}
				else
				{
					mins[i] = Utils.getAxisValue(portal2, portalAxis[i]);
					maxs[i] = Utils.getAxisValue(portal1, portalAxis[i]);
				}
			}
			
			int minX = mins[0];
			int maxX = maxs[0];
			int minZ = mins[1];
			int maxZ = maxs[1];
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
		
		BlockPos portal1 = corner1.getInnerCornerPos();
		BlockPos portal2 = corner4.getInnerCornerPos();
		int width, height, lowBound, highBound;
		
		// Get the axis for possible spawn locations and the corresponding 
		// axis values for the 2 corner portal blocks.
		Axis cornerAxis = Utils.getOrthogonalTo(axis);
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
		Direction searchDirPositive = Direction.fromAxisAndDirection(cornerAxis, AxisDirection.POSITIVE);
		Direction searchDirNegative = Direction.fromAxisAndDirection(cornerAxis, AxisDirection.NEGATIVE);
		
		BlockPos searchStartPos1 = (axis == Axis.Z)
			? new BlockPos(middle, startHeight, portal1.south(1).getZ())
			: new BlockPos(portal1.east(1).getX(), startHeight, middle);
		
		BlockPos searchStartPos2 = (axis == Axis.Z)
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
	 * Determines if the portals address blocks have changed.
	 * 
	 * @param world
	 * The world.
	 * @return
	 * <code>true</code> if the address has changed, otherwise <code>false</code>.
	 */
	public boolean hasAddressChanged(ServerLevel world)
	{
		if (world == null) return false;
		
		Address actualAddress = new Address(
			PortalRegistry.getAddressBlockId(world.getBlockState(corner1.getPos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner2.getPos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner3.getPos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner4.getPos()).getBlock()));
		
		return !actualAddress.equals(address);
	}
	
	/**
	 * Determines if the portal is missing any blocks.
	 * 
	 * @param world
	 * The world.
	 * @return
	 * <code>true</code> if the portal is missing one or more blocks, otherwise <code>false</code>.
	 */
	public boolean isDamaged(ServerLevel world)
	{
		if (world == null) return false;
		
		for (BlockPos pos : getFramePositions(false))
		{
			if (!(world.getBlockState(pos).getBlock() instanceof BlockPortalFrame)) return true;
		}
		
		for (BlockPos pos : getPortalPositions())
		{
			if (!(world.getBlockState(pos).getBlock() instanceof BlockPortal)) return true;
		}
		
		return hasAddressChanged(world);
	}
	
	public CompoundTag serializeNBT()
	{
		CompoundTag tag = new CompoundTag();
		tag.putString("dimension", (dimension.location() != null) ? dimension.location().toString() : "");
		tag.put("address", address.serializeNBT());
		tag.putString("axis", axis.getSerializedName());
		tag.put("corner1", corner1.serializeNBT());
		tag.put("corner2", corner2.serializeNBT());
		tag.put("corner3", corner3.serializeNBT());
		tag.put("corner4", corner4.serializeNBT());
		
		return tag;
	}
	
	public void deserializeNBT(CompoundTag nbt)
	{
		if (nbt == null) return;

		ResourceLocation dimensionLocation = new ResourceLocation(nbt.getString("dimension"));
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionLocation);

		address = new Address();
		address.deserializeNBT(nbt.getCompound("address"));
		
		axis = Axis.byName(nbt.getString("axis"));
		
		corner1 = new Corner();
		corner1.deserializeNBT(nbt.getCompound("corner1"));
		
		corner2 = new Corner();
		corner2.deserializeNBT(nbt.getCompound("corner2"));
		
		corner3 = new Corner();
		corner3.deserializeNBT(nbt.getCompound("corner3"));
		
		corner4 = new Corner();
		corner4.deserializeNBT(nbt.getCompound("corner4"));
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + dimension.hashCode();
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((axis == null) ? 0 : axis.hashCode());
		result = prime * result + ((corner1 == null) ? 0 : corner1.hashCode());
		result = prime * result + ((corner2 == null) ? 0 : corner2.hashCode());
		result = prime * result + ((corner3 == null) ? 0 : corner3.hashCode());
		result = prime * result + ((corner4 == null) ? 0 : corner4.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Portal other = (Portal) obj;
		
		if (dimension != other.dimension) return false;
		
		if (axis != other.axis) return false;
		
		if (address == null)
		{
			if (other.address != null)
				return false;
		}
		else if (!address.equals(other.address)) return false;
		
		if (corner1 == null)
		{
			if (other.corner1 != null) return false;
		}
		else if (!corner1.equals(other.corner1)) return false;
		
		if (corner2 == null)
		{
			if (other.corner2 != null) return false;
		}
		else if (!corner2.equals(other.corner2)) return false;
		
		if (corner3 == null)
		{
			if (other.corner3 != null) return false;
		}
		else if (!corner3.equals(other.corner3)) return false;
		
		if (corner4 == null)
		{
			if (other.corner4 != null) return false;
		}
		else if (!corner4.equals(other.corner4)) return false;

		return true;
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
	private boolean canEntitySpawnAt(ServerLevel world, BlockPos pos, int entityHeight)
	{
		if (world == null || pos == null || entityHeight < 1) return false;
		
		for (int i = 0; i < entityHeight; i ++)
		{
			if (!world.isEmptyBlock(pos.above(i))) return false;
		}

		return true;
	}
}