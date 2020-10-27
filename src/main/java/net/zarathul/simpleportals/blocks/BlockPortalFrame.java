package net.zarathul.simpleportals.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.List;

/**
 * Represents the frame of the portal mutliblock.
 */
public class BlockPortalFrame extends Block
{
	public BlockPortalFrame()
	{
		this(SimplePortals.BLOCK_PORTAL_FRAME_NAME);
	}
	
	public BlockPortalFrame(String registryName)
	{
		super(Block.Properties.of(Material.STONE, MaterialColor.COLOR_BLACK)
				.strength(50.0f, 200.0f)
				.sound(SoundType.STONE)
				.requiresCorrectToolForDrops());
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (!world.isClientSide())
		{
			ItemStack heldStack = player.getItemInHand(hand);
			Item usedItem = heldStack.getItem();

			if (usedItem == SimplePortals.itemPortalActivator)
			{
				if (player.isShiftKeyDown())
				{
					world.destroyBlock(pos, true);
				}
				else if (!PortalRegistry.isPortalAt(pos, player.level.dimension()))
				{
					PortalRegistry.activatePortal((ServerLevel)world, pos, hit.getDirection());
				}
			}
		}

		return super.use(state, world, pos, player, hand, hit);
	}

	@Override
	public void onRemove(BlockState oldState, Level world, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!world.isClientSide())
		{
			// Deactivate damaged portals.

			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (affectedPortals == null || affectedPortals.size() < 1) return;

			Portal firstPortal = affectedPortals.get(0);
			ServerLevel serverWorld = (ServerLevel)world;

			if (firstPortal.isDamaged(serverWorld))
			{
				PortalRegistry.deactivatePortal(serverWorld, pos);
			}
		}

		super.onRemove(oldState, world, pos, newState, isMoving);
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving)
	{
		if (!world.isClientSide() &&
			!neighborBlock.defaultBlockState().isAir() &&
			neighborBlock != SimplePortals.blockPortalFrame	&&
			neighborBlock != SimplePortals.blockPowerGauge &&
			neighborBlock != SimplePortals.blockPortal)
		{
			// Deactivate all portals that share this frame block if an address block was removed or changed.

			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (affectedPortals == null || affectedPortals.size() < 1) return;

			Portal firstPortal = affectedPortals.get(0);
			ServerLevel serverWorld = (ServerLevel)world;

			if (firstPortal.hasAddressChanged(serverWorld))
			{
				PortalRegistry.deactivatePortal(serverWorld, pos);
			}
		}

		super.neighborChanged(state, world, pos, neighborBlock, neighborPos, isMoving);
	}
}