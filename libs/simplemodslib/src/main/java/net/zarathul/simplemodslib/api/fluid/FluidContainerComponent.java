package net.zarathul.simplemodslib.api.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record FluidContainerComponent(int amount, int capacity, Identifier fluidId, boolean singleBucketMode)
{
	public static final Codec<FluidContainerComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("amount").forGetter(FluidContainerComponent::amount),
		Codec.INT.fieldOf("capacity").forGetter(FluidContainerComponent::capacity),
		Identifier.CODEC.fieldOf("fluidId").forGetter(FluidContainerComponent::fluidId),
		Codec.BOOL.fieldOf("singleBucketMode").forGetter(FluidContainerComponent::singleBucketMode)
	).apply(instance, FluidContainerComponent::new));
}
