package net.zarathul.simplemodslib.api.fluid;


public interface IFluidHandler
{
	/**
	 * Get the fluid inside the container.
	 *
	 * @return
	 * A {@link FluidStack} or {@link FluidStack#empty()} for empty containers, never <c>null</c>.
	 */
	FluidStack getFluid();

	/**
	 * Sets the type and amount of fluid in the container. Must consider capacity!
	 *
	 * @param newFluid
	 * A {@link FluidStack} or {@link FluidStack#empty()} for empty containers, never <c>null</c>.
	 */
	void setFluid(FluidStack newFluid);

	/**
	 * Get the total capacity the container can hold.
	 *
	 * @return
	 * A value of <c>0</c> or greater.
	 */
	int getCapacity();

	/**
	 * Tries to drain the specified amount of the specified fluid type from the container.
	 * It is assumed that if the container has the amount of fluid requested, it can be drained in one go.
	 *
	 * @param drainFluid
	 * The type and amount of fluid to drain from the container. Must not be <c>null</c>.
	 * If this amount is bigger than the amount of fluid actually container in the container, the draining will
	 * still succeed.
	 * @return
	 * The actually drained fluid or {@link FluidStack#empty()} in case of failure, never <c>null</c>.
	 */
	default
	FluidStack drain(FluidStack drainFluid)
	{
		FluidStack fluid = getFluid();
		if (fluid.isEmpty() || !fluid.isSameFluid(drainFluid) || (drainFluid.getAmount() <= 0)) return FluidStack.empty();

		FluidStack drainedFluid = fluid.copy();
		drainedFluid.setAmount(Math.min(fluid.getAmount(), drainFluid.getAmount()));

		fluid.changeAmount(-drainedFluid.getAmount());
		fluidChanged(FluidChange.AMOUNT);

		return drainedFluid;
	}

	/**
	 * Tries to fill the specified amount of the specified fluid type into the container.
	 * It is assumed that if the container has enough remaining capacity, it can be filled up to that amount in one go.
	 *
	 * @param fillFluid
	 * The type and amount of fluid to fill into the container. Must not be <c>null</c>.
	 * If this amount is bigger than the remaining capacity of the container, the filling will still succeed.
	 * @return
	 * <c>0</c> on failure.
	 */
	default
	int fill(FluidStack fillFluid)
	{
		if (fillFluid.isEmpty()) return 0;

		FluidStack fluid = getFluid();
		if (fluid.isEmpty())
		{
			setFluid(fillFluid);
			fluidChanged(FluidChange.TYPE);

			return getFluid().getAmount();
		}

		if (!fluid.isSameFluid(fillFluid)) return 0;

		int fillAmount = Math.min(getCapacity() - fluid.getAmount(), fillFluid.getAmount());

		if (fillAmount > 0)
		{
			fluid.changeAmount(fillAmount);
			fluidChanged(FluidChange.AMOUNT);
		}

		return fillAmount;
	}

	/**
	 *  Gets called when the fluid in the tank changes.
	 *
	 * @param change
	 * One of the {@link FluidChange} values.{@link FluidChange#TYPE} means always from empty to a fluid, not the other way around.
	 */
	void fluidChanged(FluidChange change);

	enum FluidChange
	{
		TYPE,		// Only going from empty to some fluid is considered a type change, but not going from a fluid to empty.
		AMOUNT;
	}
}
