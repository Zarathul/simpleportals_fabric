package net.zarathul.simpleportals.registration;

import com.google.common.base.Strings;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Represents the address of a portal.<br>
 * The address consists of 4 blockIds as provided by
 * {@link PortalRegistry#getAddressBlockId(net.minecraft.world.level.block.Block)}.
 * Multiple blocks with the same name/meta are allowed.
 */
public class Address
{
	private static final int LENGTH = 4;
	
	private String readableName;
	
	private final Map<String, Integer> blockCounts;

	public Address()
	{
		blockCounts = new TreeMap<>();
	}
	
	public Address(String ... blockIds)
	{
		this();
		initBlockCounts(blockIds);
		generateReadableName();
	}
	
	/**
	 * Gets the number of times the specified blockId
	 * is contained in this address.
	 * 
	 * @param blockId
	 * The blockId.
	 * @return
	 * A value between <code>0</code> and <code>4</code>.
	 */
	public int getBlockCount(String blockId)
	{
		if (!Strings.isNullOrEmpty(blockId))
		{
			if (blockCounts.containsKey(blockId)) return blockCounts.get(blockId);
		}
		
		return 0;
	}

	/**
	 * Executes the passed in action on every block name/count pair.
	 */
	public void forEachAddressComponent(BiConsumer<String, Integer> action)
	{
		blockCounts.forEach(action);
	}

	public CompoundTag serializeNBT()
	{
		CompoundTag mainTag = new CompoundTag();
		CompoundTag countTag;
		
		int i = 0;
		
		for (Entry<String, Integer> blockCount : blockCounts.entrySet())
		{
			countTag = new CompoundTag();
			countTag.putString("id", blockCount.getKey());
			countTag.putInt("count", blockCount.getValue());
			
			mainTag.put(String.valueOf(i++), countTag);
		}
		
		return mainTag;
	}
	
	public void deserializeNBT(CompoundTag nbt)
	{
		if (nbt == null) return;
		
		int i = 0;
		String key;
		CompoundTag countTag;
		
		while (nbt.contains(key = String.valueOf(i++)))
		{
			countTag = nbt.getCompound(key);
			blockCounts.put(countTag.getString("id"), countTag.getInt("count"));
		}
		
		generateReadableName();
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blockCounts == null) ? 0 : blockCounts.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Address other = (Address) obj;
		
		// Because the block counts are stored in a TreeMap the order and therefore the readable name
		// should always be identical.
		return readableName.equals(other.toString());
	}
	
	@Override
	public String toString()
	{
		return readableName;
	}
	
	/**
	 * Counts the specified block ids and fills the block count
	 * map accordingly. Also generates the readable name.
	 * 
	 * @param blockIds
	 * The block ids to generate the address from.
	 */
	private void initBlockCounts(String[] blockIds)
	{
		if (blockIds != null && blockIds.length >= LENGTH)
		{
			int oldCount;
			String currentId; 
			
			for (int i = 0; i < LENGTH; i++)
			{
				currentId = blockIds[i];
				
				if (!Strings.isNullOrEmpty(currentId))
				{
					if (!blockCounts.containsKey(currentId))
					{
						blockCounts.put(currentId, 1);
					}
					else
					{
						oldCount = blockCounts.get(currentId);
						blockCounts.put(currentId, ++oldCount);
					}
				}
			}
		}
	}
	
	/**
	 * Generates a readable representation of the address.
	 * 
	 * The format is <code>blockIdxblockCount</code> for every
	 * block id, delimited by "<code>,</code>".
	 */
	private void generateReadableName()
	{
		if (blockCounts == null) return;
		
		StringBuilder nameBuilder = new StringBuilder();
		
		for (Entry<String, Integer> blockCount : blockCounts.entrySet())
		{
			nameBuilder.append(blockCount.getValue());
			nameBuilder.append('x');
			nameBuilder.append(blockCount.getKey());
			nameBuilder.append(", ");
		}
		
		nameBuilder.delete(nameBuilder.length() - 2, nameBuilder.length());
		
		readableName = nameBuilder.toString();
	}
}
