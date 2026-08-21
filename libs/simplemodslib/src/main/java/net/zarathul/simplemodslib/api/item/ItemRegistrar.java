package net.zarathul.simplemodslib.api.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;

public record ItemRegistrar(String modId)
{
	public <T extends Item> T register(String name, Function<Item.Properties, Item> factory)
	{
		return register(name, factory, new Item.Properties());
	}

	public <T extends Item> T register(String name, Function<Item.Properties, Item> factory, Item.Properties properties)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

		return (T)Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties.setId(key)));
	}

	public <T extends BlockItem> T register(String name, Block block, BiFunction<Block, Item.Properties, BlockItem> factory)
	{
		return register(name, block, factory, new Item.Properties());
	}

	public <T extends BlockItem> T register(String name, Block block, BiFunction<Block, Item.Properties, BlockItem> factory, Item.Properties properties)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

		return (T)Registry.register(BuiltInRegistries.ITEM, id, factory.apply(block, properties.setId(key)));
	}
}
