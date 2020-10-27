package net.zarathul.simpleportals.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class TeleportTask
{
	public int creationTickCount;
	public ServerPlayer player;
	public ResourceKey<Level> dimension;
	public BlockPos pos;
	public Direction facing;

	public TeleportTask(int creationTickCount, ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, Direction facing)
	{
		this.creationTickCount = creationTickCount;
		this.player = player;
		this.dimension = dimension;
		this.pos = pos;
		this.facing = facing;
	}
}
