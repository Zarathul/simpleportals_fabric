package net.zarathul.simplemodslib.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.zarathul.simplemodslib.SimpleModsLib;

import java.util.Collections;
import java.util.List;

public abstract class FluidContainerItemBase extends Item implements IFluidContainerItem
{
	protected FluidContainerItemBase(Properties properties, int defaultCapacity)
	{
		super(properties
			.component(SimpleModsLib.FLUID_CONTAINER_COMPONENT, new FluidContainerComponent(0, defaultCapacity, FluidStack.empty().getRegistryKey(), false))
			.component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1.0f), Collections.emptyList(), Collections.emptyList(), List.of(0)))
		);
	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		FluidContainerComponent componentData = stack.get(SimpleModsLib.FLUID_CONTAINER_COMPONENT);
		if (componentData != null)
		{
			int fillLevel = Mth.clamp(Math.round((componentData.amount() / (float)componentData.capacity()) * 13.0f), 0, 13);
			return fillLevel;
		}
		else return super.getBarWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		FluidContainerComponent componentData = stack.get(SimpleModsLib.FLUID_CONTAINER_COMPONENT);
		if (componentData != null)
		{
			int capacity = componentData.capacity();
			float freeCapacity = Math.max(0.0F, ((float)capacity - componentData.amount()) / capacity);
			return Mth.hsvToRgb(freeCapacity / 3.0F, 1.0F, 1.0F);
		}
		else return super.getBarColor(stack);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand)
	{
		if (!level.isClientSide() && !player.isCrouching())
		{
			ItemStack itemStack = player.getItemInHand(hand);
			FluidContainerComponent component = itemStack.get(SimpleModsLib.FLUID_CONTAINER_COMPONENT);
			if (component != null)
			{
				// Cycle the fill mode between max, always drain/fill the maximum amount, and bucket, drain/fill one bucket at a time.
				boolean newMode = !component.singleBucketMode();
				itemStack.set(SimpleModsLib.FLUID_CONTAINER_COMPONENT, new FluidContainerComponent(component.amount(), component.capacity(), component.fluidId(), newMode));

				return InteractionResult.SUCCESS_SERVER;
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();

		if (!level.isClientSide())
		{
			BlockPos clickedPos = context.getClickedPos();
			// Fluids are click-through, so look at the block in direction of the clicked face.
			BlockPos targetPos = clickedPos.offset(context.getClickedFace().getUnitVec3i());
			FluidState fluidStateAtClickedPos = level.getFluidState(targetPos);
			ItemStack heldItemStack = context.getItemInHand();

			// Try to pick up source block.
			if (!fluidStateAtClickedPos.isEmpty() && fluidStateAtClickedPos.isSource())
			{
				FluidStack sourceFluid = new FluidStack(fluidStateAtClickedPos.getType(), FluidStack.BUCKET_VOLUME);
				int filledAmount = fill(heldItemStack, sourceFluid);

				if (filledAmount == FluidStack.BUCKET_VOLUME)
				{
					if (level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
					{
						var soundEvent = sourceFluid.getFluid().getPickupSound();
						if (soundEvent.isPresent())
						{
							ServerPlayer player = (ServerPlayer)context.getPlayer();

							player.connection.send(new ClientboundSoundPacket(
								Holder.direct(soundEvent.get()),
								SoundSource.BLOCKS,
								player.getX(), player.getY(), player.getZ(),
								1.0f, 1.0f, level.getRandom().nextLong()));
						}

						return InteractionResult.SUCCESS_SERVER;
					}
				}
			}
			else // Try to place a source block, either if the target position is empty or contains a fluid that is not a source block.
			{
				FluidStack fluidStackInItem = FluidStack.getFluid(heldItemStack);
				BlockState blockAtTargetPos = level.getBlockState(targetPos);

				if ((blockAtTargetPos.isAir() || !level.getBlockState(targetPos).getFluidState().isSource()) && fluidStackInItem.getAmount() >= FluidStack.BUCKET_VOLUME)
				{
					FluidStack drainFluidStack = fluidStackInItem.copy();
					drainFluidStack.setAmount(FluidStack.BUCKET_VOLUME);
					drain(heldItemStack, drainFluidStack);

					BlockState fluidBlock = drainFluidStack.getFluid().defaultFluidState().createLegacyBlock();

					if (level.setBlock(targetPos, fluidBlock, Block.UPDATE_ALL))
					{
						var soundEvent = drainFluidStack.getFluid().getPickupSound();
						if (soundEvent.isPresent())
						{
							ServerPlayer player = (ServerPlayer)context.getPlayer();

							player.connection.send(new ClientboundSoundPacket(
								Holder.direct(soundEvent.get()),
								SoundSource.BLOCKS,
								player.getX(), player.getY(), player.getZ(),
								1.0f, 1.0f, level.getRandom().nextLong()));
						}
					}

					return InteractionResult.SUCCESS_SERVER;
				}
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void onFluidChanged(ItemStack itemStack, int amount, int capacity, Identifier fluidId)
	{
		// Update custom model data.
		float fillLevel = Math.clamp(amount / (float)capacity, 0f, 1f);
		itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(fillLevel), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
	}
}