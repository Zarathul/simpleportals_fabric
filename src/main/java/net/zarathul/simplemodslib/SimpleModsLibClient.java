package net.zarathul.simplemodslib;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.color.item.ItemTintSources;
import net.zarathul.simplemodslib.api.fluid.ContainerItemFluidTintSource;

public class SimpleModsLibClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ItemTintSources.ID_MAPPER.put(SimpleModsLib.modId(ContainerItemFluidTintSource.NAME), ContainerItemFluidTintSource.MAP_CODEC);
	}
}
