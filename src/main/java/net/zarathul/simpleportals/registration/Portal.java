package net.zarathul.simpleportals.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.blocks.BlockPortal;
import net.zarathul.simpleportals.blocks.BlockPortalFrame;
import net.zarathul.simplemodslib.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a portal.<br>
 * Note: Corner1 and Corner4 must be diagonal to each other,
 * same for Corner2 and Corner3.
 */
public record Portal(ResourceKey<Level> dimension, Address address, Axis axis,
					 Corner corner1, Corner corner2,
					 Corner corner3, Corner corner4)
{
	public static final Codec<Portal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Portal::dimension),
			Address.CODEC.fieldOf("address").forGetter(Portal::address),
			Axis.CODEC.fieldOf("axis").forGetter(Portal::axis),
			Corner.CODEC.fieldOf("corner1").forGetter(Portal::corner1),
			Corner.CODEC.fieldOf("corner2").forGetter(Portal::corner2),
			Corner.CODEC.fieldOf("corner3").forGetter(Portal::corner3),
			Corner.CODEC.fieldOf("corner4").forGetter(Portal::corner4)
		).apply(instance, Portal::new)
	);

	/**
	 * Gets the positions of all blocks making up the portal.
	 * 
	 * @return
	 * An {@link Iterable} of {@link BlockPos}.
	 */
	public Iterable<BlockPos> getAllPositions()
	{
		return BlockPos.betweenClosed(corner1.pos(), corner4.pos());
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
		
		Direction dir1To2 = Utils.getRelativeDirection(corner1.pos(), corner2.pos());
		Direction dir1To3 = Utils.getRelativeDirection(corner1.pos(), corner3.pos());
		Direction dir4To2 = Utils.getRelativeDirection(corner4.pos(), corner2.pos());
		Direction dir4To3 = Utils.getRelativeDirection(corner4.pos(), corner3.pos());
		
		// Offset the corner positions towards their adjacent corners and get all positions
		// in between. This way we get all the frame positions without the corners themselves.
		
		BlockPos from1 = corner1.pos().relative(dir1To2);
		BlockPos to1   = corner2.pos().relative(dir1To2.getOpposite());

		BlockPos from2 = corner1.pos().relative(dir1To3);
		BlockPos to2   = corner3.pos().relative(dir1To3.getOpposite());

		BlockPos from3 = corner4.pos().relative(dir4To2);
		BlockPos to3   = corner2.pos().relative(dir4To2.getOpposite());

		BlockPos from4 = corner4.pos().relative(dir4To3);
		BlockPos to4   = corner3.pos().relative(dir4To3.getOpposite());

		// BlockPos.getAllInBox() delivers wrong results (duplicates and missing positions).
		// So I have to do this nonsense. Minecraft 1.14.4 (10.10.2019)
		for (BlockPos pos : BlockPos.betweenClosed(from1, to1)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from2, to2)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from3, to3)) frame.add(pos.immutable());
		for (BlockPos pos : BlockPos.betweenClosed(from4, to4)) frame.add(pos.immutable());

		if (includeCorners)
		{
			frame.add(corner1.pos());
			frame.add(corner2.pos());
			frame.add(corner3.pos());
			frame.add(corner4.pos());
		}
		
		return frame;
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
			PortalRegistry.getAddressBlockId(world.getBlockState(corner1.pos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner2.pos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner3.pos()).getBlock()),
			PortalRegistry.getAddressBlockId(world.getBlockState(corner4.pos()).getBlock()));
		
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

	/**
	 * Gets a more readable string representation of a portal that does not contain all the information, which would be very lengthy.
	 *
	 * @return
	 * A string containing only the portal position, dimension and address.
	 */
	public String asReadableString()
	{
		return String.format("%s / %s / %s", Utils.getReadableBlockPos(corner1.pos()), dimension.identifier(), address.toString());
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || getClass() != o.getClass()) return false;

		Portal portal = (Portal) o;
		return axis == portal.axis && corner1.equals(portal.corner1) && corner2.equals(portal.corner2) && corner3.equals(portal.corner3) && corner4.equals(portal.corner4) && address.equals(portal.address) && dimension.equals(portal.dimension);
	}

	@Override
	public int hashCode()
	{
		int result = dimension.hashCode();
		result = 31 * result + address.hashCode();
		result = 31 * result + axis.hashCode();
		result = 31 * result + corner1.hashCode();
		result = 31 * result + corner2.hashCode();
		result = 31 * result + corner3.hashCode();
		result = 31 * result + corner4.hashCode();
		return result;
	}
}