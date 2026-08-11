package net.zarathul.simplemodslib;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class LogoItem extends BlockItem
{
	public LogoItem(Block block, ResourceKey<Item> id)
	{
		super(block, new Item.Properties().setId(id));
	}
}
