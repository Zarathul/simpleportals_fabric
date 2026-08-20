package net.zarathul.simplemodslib;

import net.minecraft.world.level.block.Block;

public class LogoBlock extends Block
{
	public LogoBlock(Properties properties)
	{
		super(properties
			.instabreak()
			.noOcclusion()
		);
	}
}
