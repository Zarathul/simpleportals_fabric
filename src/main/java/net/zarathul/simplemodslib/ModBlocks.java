package net.zarathul.simplemodslib;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.zarathul.simplemodslib.api.block.BlockRegistrar;
import net.zarathul.simplemodslib.api.fluid.FluidHelper;
import net.zarathul.simplemodslib.api.fluid.IFluidContainerItem;

public final class ModBlocks
{
	private static final BlockRegistrar REGISTRAR = new BlockRegistrar(SimpleModsLib.MOD_ID);
	public static final Block LOGO = REGISTRAR.register("logo", Block::new, Block.Properties.of().instabreak().noOcclusion());

	public static void init()
	{
		SimpleModsLib.LOG.info("Registering blocks.");
	}

	public static InteractionResult useBlockCallback(Player player, Level level, InteractionHand hand, BlockHitResult hit)
	{
		// Make IFluidContainerItems usable on IFluidHandlers while crouching.
		if (level.isClientSide() || (!player.isCrouching()) || player.isSpectator()) return InteractionResult.PASS;

		ItemStack heldItemStack = player.getItemInHand(hand);

		if (heldItemStack.getItem() instanceof IFluidContainerItem && FluidHelper.isFluidHandler(level, hit.getBlockPos()))
		{
			BlockState blockState = level.getBlockState(hit.getBlockPos());
			InteractionResult result = blockState.useItemOn(heldItemStack, level, player, hand, hit);

			return result;
		}

		return InteractionResult.PASS;
	}
}
