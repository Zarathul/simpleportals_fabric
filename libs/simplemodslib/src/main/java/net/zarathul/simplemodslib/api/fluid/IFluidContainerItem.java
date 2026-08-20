package net.zarathul.simplemodslib.api.fluid;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.zarathul.simplemodslib.ModComponents;

public interface IFluidContainerItem
{
	default
	FluidStack drain(ItemStack itemStack, FluidStack drainFluidStack)
	{
		FluidContainerComponent dataComponent = itemStack.get(ModComponents.FLUID_CONTAINER_COMPONENT);
		if (dataComponent == null) return FluidStack.empty();

		FluidStack itemFluidStack = FluidStack.from(dataComponent);

		int drainAmount = (dataComponent.singleBucketMode()) ? FluidStack.BUCKET_VOLUME : drainFluidStack.getAmount();
		if (itemFluidStack.isEmpty() || !itemFluidStack.isSameFluid(drainFluidStack) || (drainAmount <= 0)) return FluidStack.empty();

		FluidStack drainedFluid = itemFluidStack.copy();
		drainedFluid.setAmount(Math.min(itemFluidStack.getAmount(), drainAmount));
		itemFluidStack.changeAmount(-drainedFluid.getAmount());

		itemStack.set(ModComponents.FLUID_CONTAINER_COMPONENT, new FluidContainerComponent(itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey(), dataComponent.singleBucketMode()));
		onFluidChanged(itemStack, itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey());

		return drainedFluid;
	}

	default
	int fill(ItemStack itemStack, FluidStack fillFluidStack)
	{
		FluidContainerComponent dataComponent = itemStack.get(ModComponents.FLUID_CONTAINER_COMPONENT);

		if (fillFluidStack.isEmpty() || dataComponent == null) return 0;

		FluidStack itemFluidStack = FluidStack.from(dataComponent);
		int itemCapacity = dataComponent.capacity();

		if (itemFluidStack.isEmpty())
		{
			if (dataComponent.singleBucketMode()) itemCapacity = Math.min(itemCapacity, FluidStack.BUCKET_VOLUME);
			itemFluidStack = fillFluidStack.copy();
			// limit the stored fluid to the tanks capacity
			if (!itemFluidStack.isEmpty()) itemFluidStack.setAmount(Math.min(itemFluidStack.getAmount(), itemCapacity));
			itemStack.set(ModComponents.FLUID_CONTAINER_COMPONENT, new FluidContainerComponent(itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey(), dataComponent.singleBucketMode()));
			onFluidChanged(itemStack, itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey());

			return itemFluidStack.getAmount();
		}

		if (!itemFluidStack.isSameFluid(fillFluidStack)) return 0;

		int remainingCapacity = itemCapacity - itemFluidStack.getAmount();
		if (dataComponent.singleBucketMode()) remainingCapacity = Math.min(remainingCapacity, FluidStack.BUCKET_VOLUME);

		int fillAmount = Math.min(remainingCapacity, fillFluidStack.getAmount());
		if (fillAmount > 0)
		{
			itemFluidStack.changeAmount(fillAmount);
			itemStack.set(ModComponents.FLUID_CONTAINER_COMPONENT, new FluidContainerComponent(itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey(), dataComponent.singleBucketMode()));
			onFluidChanged(itemStack, itemFluidStack.getAmount(), dataComponent.capacity(), itemFluidStack.getRegistryKey());
		}

		return fillAmount;
	}

	void onFluidChanged(ItemStack itemStack, int amount, int capacity, Identifier fluidId);
}
