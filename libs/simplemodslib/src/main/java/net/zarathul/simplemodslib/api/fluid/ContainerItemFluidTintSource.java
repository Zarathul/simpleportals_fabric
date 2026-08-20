package net.zarathul.simplemodslib.api.fluid;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.zarathul.simplemodslib.ModComponents;
import org.jspecify.annotations.Nullable;

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
 * 				{ "type": "simplemodslib:container_item_fluid" }
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
	public static final String NAME = "container_item_fluid";

	@Override
	public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner)
	{
		FluidContainerComponent dataComponent = itemStack.get(ModComponents.FLUID_CONTAINER_COMPONENT);
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
