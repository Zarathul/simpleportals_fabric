package net.zarathul.simplemodslib.api.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;
import java.util.function.Function;

public record BlockRegistrar(String modId)
{
	public <T extends Block> T register(String name, Function<Block.Properties, Block> factory)
	{
		return register(name, factory, Block.Properties.of());
	}

	public <T extends Block> T register(String name, Function<Block.Properties, Block> factory, Block.Properties properties)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

		return (T) Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(properties.setId(key)));
	}

	public <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id);

		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, new BlockEntityType<>(factory, Set.of(blocks)));
	}
}
