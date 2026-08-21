package net.zarathul.simpleportals.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Portal frame in item form.
 */
public class ItemPortalFrame extends BlockItem
{
	public ItemPortalFrame(Block block, Properties properties)
	{
		super(block, properties
			.stacksTo(64)
			.useBlockDescriptionPrefix()
		);
	}
}