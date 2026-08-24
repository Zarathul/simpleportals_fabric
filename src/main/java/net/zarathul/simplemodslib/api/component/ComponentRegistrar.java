package net.zarathul.simplemodslib.api.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record ComponentRegistrar(String modId)
{
	public <T> DataComponentType<T> register(String name, Codec<T> codec)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);

		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, DataComponentType.<T>builder().persistent(codec).build());
	}

	public <T> DataComponentType<T> register(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);

		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, DataComponentType.<T>builder().networkSynchronized(codec).build());
	}

	public <T> DataComponentType<T> register(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);

		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, DataComponentType.<T>builder().persistent(codec).networkSynchronized(streamCodec).build());
	}
}
