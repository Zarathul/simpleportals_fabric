package net.zarathul.simplemodslib.api.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.zarathul.simplemodslib.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ItemRegistrar
{
	private static final String TOOLTIP_KEY_FORMAT = "item.%s.%s.tooltip";
	private static final String TOOLTIP_DETAILS_KEY_FORMAT = TOOLTIP_KEY_FORMAT + "_details";

	private final String modId;
	private final HashMap<Item, TooltipKeys> TOOLTIP_KEYS = new HashMap<>();

	public ItemRegistrar(String modId)
	{
		this.modId = modId;
	}

	public <T extends Item> T register(String name, Function<Item.Properties, T> factory)
	{
		return register(name, factory, new Item.Properties(), null, null);
	}

	public <T extends Item> T register(String name, Function<Item.Properties, T> factory, Function<ItemStack, Object[]> tooltipFormatArgsGetter, Function<ItemStack, Object[]> tooltipDetailsFormatArgsGetter)
	{
		return register(name, factory, new Item.Properties(), tooltipFormatArgsGetter, tooltipDetailsFormatArgsGetter);
	}

	public <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties)
	{
		return register(name, factory, properties, null, null);
	}

	public <T extends Item> T register(
		String name,
		Function<Item.Properties, T> factory,
		Item.Properties properties,
		Function<ItemStack, Object[]> tooltipFormatArgsGetter,
		Function<ItemStack, Object[]> tooltipDetailsFormatArgsGetter
	)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		TooltipKeys tooltipKeys = new TooltipKeys(String.format(TOOLTIP_KEY_FORMAT, modId, name), String.format(TOOLTIP_DETAILS_KEY_FORMAT, modId, name), tooltipFormatArgsGetter, tooltipDetailsFormatArgsGetter);

		T item = (T)Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties.setId(key)));
		TOOLTIP_KEYS.put(item, tooltipKeys);

		return item;
	}

	public <T extends BlockItem> T register(String name, Block block, BiFunction<Block, Item.Properties, T> factory)
	{
		return register(name, block, factory, new Item.Properties(), null, null);
	}

	public <T extends BlockItem> T register(String name, Block block, BiFunction<Block, Item.Properties, T> factory, Function<ItemStack, Object[]> tooltipFormatArgsGetter, Function<ItemStack, Object[]> tooltipDetailsFormatArgsGetter)
	{
		return register(name, block, factory, new Item.Properties(), tooltipFormatArgsGetter, tooltipDetailsFormatArgsGetter);
	}

	public <T extends BlockItem> T register(String name, Block block, BiFunction<Block, Item.Properties, T> factory, Item.Properties properties)
	{
		return register(name, block, factory, properties, null, null);
	}

	public <T extends BlockItem> T register(
		String name,
		Block block,
		BiFunction<Block, Item.Properties, T> factory,
		Item.Properties properties,
		Function<ItemStack, Object[]> tooltipFormatArgsGetter,
		Function<ItemStack, Object[]> tooltipDetailsFormatArgsGetter
	)
	{
		Identifier id = Identifier.fromNamespaceAndPath(modId, name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		TooltipKeys tooltipKeys = new TooltipKeys(String.format(TOOLTIP_KEY_FORMAT, modId, name), String.format(TOOLTIP_DETAILS_KEY_FORMAT, modId, name), tooltipFormatArgsGetter, tooltipDetailsFormatArgsGetter);

		T item = (T)Registry.register(BuiltInRegistries.ITEM, id, factory.apply(block, properties.setId(key)));
		TOOLTIP_KEYS.put(item, tooltipKeys);

		return item;
	}

	@Environment(EnvType.CLIENT)
	public void registerTooltips()
	{
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) -> {
			TooltipKeys keys = TOOLTIP_KEYS.get(stack.getItem());
			if (keys == null) return;

			var mc = Minecraft.getInstance();
			long windowHandle = mc.getWindow().handle();
			boolean isShiftPressed = (GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
			int maxWidth = mc.getWindow().getGuiScaledWidth() / 3;

			if (isShiftPressed)
			{
				lines.addAll(Utils.multiLineTranslateWithMaxWidth(keys.detailsKey(), maxWidth, keys.getDetailsFormatArgs(stack)));
			}
			else
			{
				lines.addAll(Utils.multiLineTranslate(keys.key(), keys.getFormatArgs(stack)));
			}
		});
	}

	private record TooltipKeys(String key, String detailsKey, Function<ItemStack, Object[]> formatArgsGetter, Function<ItemStack, Object[]> detailsFormatArgsGetter)
	{
		public Object[] getFormatArgs(ItemStack stack)
		{
			return (formatArgsGetter != null) ? formatArgsGetter.apply(stack) : new Object[0];
		}

		public Object[] getDetailsFormatArgs(ItemStack stack)
		{
			return (detailsFormatArgsGetter != null) ? detailsFormatArgsGetter.apply(stack) : new Object[0];
		}
	}
}