package net.zarathul.simpleportals.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.common.Utils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/**
 * Power gauge in item form.
 */
public class ItemPowerGauge extends BlockItem
{
	private static final String toolTipKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_POWER_GAUGE_NAME + ".tooltip";
	private static final String toolTipDetailsKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_POWER_GAUGE_NAME + ".tooltip_details";

	public ItemPowerGauge(ResourceKey<Item> id)
	{
		super(SimplePortals.blockPowerGauge, new Item.Properties().setId(id).stacksTo(64).useBlockDescriptionPrefix());
	}

	@Environment(EnvType.CLIENT)
	public void addTooltip(ItemStack stack, TooltipContext tooltipContext, TooltipFlag tooltipFlag, List<Component> lines)
	{
		long windowHandle = Minecraft.getInstance().getWindow().handle();
		int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

		if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
		{
			lines.addAll(Utils.multiLineTranslatable(toolTipDetailsKey));
		}
		else
		{
			lines.add(Component.translatable(toolTipKey));
		}
	}
}