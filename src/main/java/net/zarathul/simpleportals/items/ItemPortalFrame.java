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
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simplemodslib.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Portal frame in item form.
 */
public class ItemPortalFrame extends BlockItem
{
	private static final String toolTipKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_FRAME_NAME + ".tooltip";
	private static final String toolTipDetailsKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_FRAME_NAME + ".tooltip_details";

	public ItemPortalFrame(ResourceKey<Item> id)
	{
		super(SimplePortals.blockPortalFrame, new Item.Properties().setId(id).stacksTo(64).useBlockDescriptionPrefix());
	}

	@Environment(EnvType.CLIENT)
	public void addTooltip(ItemStack stack, TooltipContext tooltipContext, TooltipFlag tooltipFlag, List<Component> lines)
	{
		long windowHandle = Minecraft.getInstance().getWindow().handle();
		int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

		if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
		{
			lines.addAll(Utils.multiLineTranslate(toolTipDetailsKey));
		}
		else
		{
			lines.add(Component.translatable(toolTipKey));
		}
	}
}