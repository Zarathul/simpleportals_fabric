package net.zarathul.simplemodslib;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.zarathul.simplemodslib.api.fluid.FluidContainerComponent;
import net.zarathul.simplemodslib.api.fluid.FluidContainerItemBase;
import net.zarathul.simplemodslib.api.fluid.FluidHelper;
import org.jspecify.annotations.Nullable;

public class SimpleModsLibClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ItemTintSources.ID_MAPPER.put(SimpleModsLib.CONTAINER_ITEM_FLUID_TINT_SOURCE_ID, ContainerItemFluidTintSource.MAP_CODEC);
	}

	/**
	 * Provides a fluid tint color for use in models of items that have a {@link FluidContainerComponent}.
	 * E.g. items derived from {@link FluidContainerItemBase}.<br><br>
	 * Example for item model definition:
	 * <pre>
	 *     {@code
	 * 		"model": {
	 * 			"type": "minecraft:model",
	 * 			"model": "your_mod_id:item/your_model_name",
	 * 			"tints": [
	 * 				{ "type": "simplemodslib:fluid_container_item" }
	 * 			]
	 * 		}
	 * </pre>
	 *
	 */
	@Environment(EnvType.CLIENT)
	public record ContainerItemFluidTintSource() implements ItemTintSource
	{
		public static ContainerItemFluidTintSource INSTANCE = new ContainerItemFluidTintSource();
		public static final MapCodec<ContainerItemFluidTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

		@Override
		public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner)
		{
			FluidContainerComponent dataComponent = itemStack.get(SimpleModsLib.FLUID_CONTAINER_COMPONENT);
			if (dataComponent == null) return -1;

			Identifier itemFluidId = dataComponent.fluidId();

			Fluid fluid = BuiltInRegistries.FLUID.get(itemFluidId).get().value();
			int tintColor = FluidHelper.getFluidColor(fluid, level, (owner != null) ? owner.blockPosition() : null, true);

			return tintColor;
		}

		@Override
		public MapCodec<? extends ItemTintSource> type()
		{
			return MAP_CODEC;
		}
	}
}
