package net.zarathul.simplemodslib;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.zarathul.simplemodslib.api.fluid.FluidHelper;
import net.zarathul.simplemodslib.api.item.ItemRegistrar;
import net.zarathul.simplemodslib.mixin.ItemAccessor;

public final class ModItems
{
	private static final ItemRegistrar REGISTRAR = new ItemRegistrar(SimpleModsLib.MOD_ID);
	public static final BlockItem LOGO = REGISTRAR.register("logo", ModBlocks.LOGO, BlockItem::new);

	public static void init()
	{
		SimpleModsLib.LOG.info("Registering items.");
	}
	public static void registerTooltips() { REGISTRAR.registerTooltips(); }

	public static InteractionResult useItemCallback(Player player, Level level, InteractionHand hand)
	{
		// Prevent buckets and water bottles from doing their usual thing when right-clicking an IFluidHandler.
		if (player.isSpectator()) return InteractionResult.PASS;

		ItemStack heldItemStack = player.getItemInHand(hand);
		Item heldItem = heldItemStack.getItem();

		if (heldItem instanceof BucketItem || (heldItem instanceof PotionItem && heldItemStack.get(DataComponents.POTION_CONTENTS).is(Potions.WATER)))
		{
			BlockHitResult hit = ItemAccessor.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
			if (hit.getType() == HitResult.Type.BLOCK && FluidHelper.isFluidHandler(level, hit.getBlockPos()))
			{
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}
}
