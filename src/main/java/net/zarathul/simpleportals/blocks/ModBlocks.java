package net.zarathul.simpleportals.blocks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.zarathul.simplemodslib.api.block.BlockRegistrar;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.items.ModItems;

public final class ModBlocks
{
	private static final BlockRegistrar REGISTRAR = new BlockRegistrar(SimplePortals.MOD_ID);

	public static final BlockPortal PORTAL = REGISTRAR.register("portal", BlockPortal::new);
	public static final BlockPortalFrame FRAME = REGISTRAR.register("portal_frame", BlockPortalFrame::new);
	public static final BlockPowerGauge POWER_GAUGE = REGISTRAR.register("power_gauge", BlockPowerGauge::new);

	public static void init()
	{
		SimplePortals.LOG.info("Registering blocks.");
	}

	public static InteractionResult useBlockCallback(Player player, Level level, InteractionHand hand, BlockHitResult hit)
	{
		// Necessary for dismantling blocks with the portal activator on sneak right-click.
		// Without this BlockPortalFrame.use() is never called when sneaking.
		if (level.isClientSide() || (!player.isShiftKeyDown()) || player.isSpectator()) return InteractionResult.PASS;

		BlockState blockState = level.getBlockState(hit.getBlockPos());
		Block hitBlock = blockState.getBlock();
		ItemStack usedItem = player.getItemInHand(hand);

		if ((usedItem.getItem() == ModItems.PORTAL_ACTIVATOR) && ((hitBlock == ModBlocks.FRAME) || (hitBlock == ModBlocks.POWER_GAUGE)))
		{
			InteractionResult result = blockState.useItemOn(usedItem, level, player, hand, hit);
			return result;
		}

		return InteractionResult.PASS;
	}
}
