package net.zarathul.simplemodslib;

import net.minecraft.core.component.DataComponentType;
import net.zarathul.simplemodslib.api.component.ComponentRegistrar;
import net.zarathul.simplemodslib.api.fluid.FluidContainerComponent;

public final class ModComponents
{
	private static final ComponentRegistrar REGISTRAR = new ComponentRegistrar(SimpleModsLib.MOD_ID);
	public static final DataComponentType<FluidContainerComponent> FLUID_CONTAINER_COMPONENT = REGISTRAR.register("fluid_container", FluidContainerComponent.CODEC);

	public static void init()
	{
		SimpleModsLib.LOG.info("Registering components.");
	}
}
