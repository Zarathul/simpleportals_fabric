package net.zarathul.simplemodslib.api.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.zarathul.simplemodslib.SimpleModsLib;

import java.util.Objects;

public class FluidStack
{
	public static final int BUCKET_VOLUME = 1000;	// in mB (milli-Buckets)

	private static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);
	private static final Identifier EMPTY_FLUID_ID = BuiltInRegistries.FLUID.getDefaultKey();
	private static final String FLUID_ID = "fluid_id";
	private static final String FLUID_AMOUNT = "fluid_amount";

	private Fluid fluid;
	private int amount;
	private Identifier fluidId;

	public FluidStack(Fluid fluid, int amount)
	{
		this(fluid, amount, BuiltInRegistries.FLUID.getKey(fluid));
	}

	private FluidStack(Fluid fluid, int amount, Identifier fluidId)
	{
		this.fluid   = fluid;
		this.amount  = amount;
		this.fluidId = fluidId;
	}

	private FluidStack()
	{
	}

	public FluidStack copy()
	{
		return new FluidStack(fluid, amount, fluidId);
	}

	public Fluid getFluid()
	{
		return fluid;
	}

	public int getAmount()
	{
		return amount;
	}

	public void setAmount(int amount)
	{
		this.amount = amount;
		if (this.amount <= 0) makeEmpty();
	}

	public void changeAmount(int delta)
	{
		this.amount += delta;
		if (this.amount <= 0) makeEmpty();
	}

	public Identifier getRegistryKey()
	{
		return fluidId;
	}

	public boolean isEmpty()
	{
		return ((this == EMPTY) || (amount <= 0));
	}

	public boolean isSameFluid(FluidStack other)
	{
		return (other.fluid.isSame(this.fluid));
	}

	public static FluidStack empty()
	{
		return EMPTY.copy();
	}

	public void save(ValueOutput output)
	{
		output.putInt(FLUID_AMOUNT, amount);
		output.putString(FLUID_ID, fluidId.toString());
	}

	public void load(ValueInput input)
	{
		amount = input.getInt(FLUID_AMOUNT).get();
		if (amount <= 0)
		{
			makeEmpty();
			return;
		}

		fluidId = Identifier.parse(input.getString(FLUID_ID).get());

		var registryGetResult = BuiltInRegistries.FLUID.get(fluidId);
		if (registryGetResult.isPresent())
		{
			fluid = registryGetResult.get().value();
		}
		else
		{
			// In case the fluid is no longer available, e.g. a mod got removed.
			makeEmpty();
		}
	}

	private void makeEmpty()
	{
		amount  = 0;
		fluidId = EMPTY_FLUID_ID;
		fluid   = EMPTY.fluid;
	}

	public static FluidStack from(FluidContainerComponent input)
	{
		int amount = input.amount();
		if (amount <= 0) return EMPTY;

		Identifier id = input.fluidId();

		var registryGetResult = BuiltInRegistries.FLUID.get(id);
		if (registryGetResult.isPresent())
		{
			Fluid fluid = registryGetResult.get().value();
			FluidStack stack = new FluidStack(fluid, amount, id);

			return stack;
		}

		return EMPTY;
	}

	public static FluidStack getFluid(ItemStack stack)
	{
		var component = stack.get(SimpleModsLib.FLUID_CONTAINER_COMPONENT);
		if (component == null) return FluidStack.empty();

		FluidStack fluidStack = FluidStack.from(component);

		return fluidStack;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof FluidStack)) return false;

		FluidStack otherStack = (FluidStack)other;
		return (otherStack.getFluid().isSame(fluid) && (otherStack.getAmount() == amount));
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(fluid, amount, fluidId);
	}
}
