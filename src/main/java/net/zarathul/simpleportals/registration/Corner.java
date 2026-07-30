package net.zarathul.simpleportals.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Represents a corner of a portal.
 */
public record Corner(BlockPos pos, Direction facingA, Direction facingB)
{
	public static final Codec<Corner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Corner::pos),
			Direction.CODEC.fieldOf("facingA").forGetter(Corner::facingA),
			Direction.CODEC.fieldOf("facingB").forGetter(Corner::facingB)
		).apply(instance, Corner::new)
	);

	/**
	 * Gets the position of the block enclosed by the corner.
	 * 
	 * @return
	 * A BlockPos representing the inside of the corner.
	 */
	public BlockPos getInnerCornerPos()
	{
		return (pos != null && facingA != null && facingB != null)
				? pos.offset(facingA.getUnitVec3i()).offset(facingB.getUnitVec3i())
				: null;
	}

	@Override
	public String toString()
	{
		return pos + " : " + facingA + " / " + facingB;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		Corner other = (Corner) obj;

		if (pos == null)
		{
			if (other.pos != null) return false;
		}
		else if (!pos.equals(other.pos)) return false;

		return ((facingA == other.facingA && facingB == other.facingB) || (facingA == other.facingB && facingB == other.facingA));
	}
}