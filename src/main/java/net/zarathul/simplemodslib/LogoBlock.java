package net.zarathul.simplemodslib;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public class LogoBlock extends Block
{
	public LogoBlock(ResourceKey<Block> id)
	{
		super(Block.Properties.of().setId(id).instabreak().noOcclusion());
	}
}
