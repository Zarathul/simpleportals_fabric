package net.zarathul.simpleportals.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.registration.Portal;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Represents the frame of the portal multi-block.
 */
public class BlockPortalFrame extends Block
{
	private static final SingleThreadedRandomSource RANDOM = new SingleThreadedRandomSource(Instant.now().toEpochMilli());

	public BlockPortalFrame(ResourceKey<Block> id)
	{
		super(Block.Properties.of()
			.setId(id)
			.mapColor(MapColor.COLOR_BLACK)
			.strength(50.0f, 200.0f)
			.sound(SoundType.STONE)
			.requiresCorrectToolForDrops());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (!world.isClientSide())
		{
			ItemStack heldStack = player.getItemInHand(hand);
			Item usedItem = heldStack.getItem();
			Level playerLevel = player.level();;

			if (usedItem == SimplePortals.itemPortalActivator)
			{
				if (player.isShiftKeyDown())
				{
					world.destroyBlock(pos, true);
					return InteractionResult.SUCCESS_SERVER;
				}
				else if (!SimplePortals.portalRegistry.isPortalAt(pos, playerLevel.dimension()))
				{
					if (SimplePortals.portalRegistry.activatePortal((ServerLevel)world, pos, hit.getDirection()))
					{
						if (Settings.portalActivationSoundEnabled) world.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.5f, 1.0f);

						return InteractionResult.SUCCESS_SERVER;
					}

					return InteractionResult.PASS;
				}

				return InteractionResult.PASS;
			}
		}

		return super.useItemOn(itemStack, state, world, pos, player, hand, hit);
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state)
	{
		if (!level.isClientSide())
		{
			// Deactivate damaged portals.

			ServerLevel serverLevel = (ServerLevel)level;
			List<Portal> affectedPortals = SimplePortals.portalRegistry.getPortalsAt(pos, serverLevel.dimension());

			if (!affectedPortals.isEmpty())
			{
				Portal firstPortal = affectedPortals.getFirst();

				if (firstPortal.isDamaged(serverLevel))
				{
					SimplePortals.portalRegistry.deactivatePortal(serverLevel, pos);
				}
			}
		}

		super.destroy(level, pos, state);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston)
	{
		if (!world.isClientSide() &&
			!neighborBlock.defaultBlockState().isAir() &&
			neighborBlock != SimplePortals.blockPortalFrame	&&
			neighborBlock != SimplePortals.blockPowerGauge &&
			neighborBlock != SimplePortals.blockPortal)
		{
			// Deactivate all portals that share this frame block if an address block was removed or changed.

			List<Portal> affectedPortals = SimplePortals.portalRegistry.getPortalsAt(pos, world.dimension());

			if (!affectedPortals.isEmpty())
			{
				Portal firstPortal = affectedPortals.getFirst();
				ServerLevel serverWorld = (ServerLevel)world;

				if (firstPortal.hasAddressChanged(serverWorld))
				{
					SimplePortals.portalRegistry.deactivatePortal(serverWorld, pos);
				}
			}
		}

		super.neighborChanged(state, world, pos, neighborBlock, orientation, movedByPiston);
	}
}