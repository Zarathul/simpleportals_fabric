package net.zarathul.simpleportals.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Power gauge in item form.
 */
public class ItemPowerGauge extends BlockItem
{
	public ItemPowerGauge(Block block, Properties properties)
	{
		super(block,properties
			.stacksTo(64)
			.useBlockDescriptionPrefix()
		);
	}
}