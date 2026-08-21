package net.zarathul.simpleportals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zarathul.simplemodslib.Utils;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simpleportals.items.ModItems;
import net.zarathul.simpleportals.network.ClientPacketHandlers;
import org.lwjgl.glfw.GLFW;

public class ClientInit implements ClientModInitializer
{
	private static final String PORTAL_ACTIVATOR_TOOLTIP_KEY         = "item." + SimplePortals.MOD_ID + "." + ModItems.PORTAL_ACTIVATOR_NAME + ".tooltip";
	private static final String PORTAL_ACTIVATOR_TOOLTIP_DETAILS_KEY = "item." + SimplePortals.MOD_ID + "." + ModItems.PORTAL_ACTIVATOR_NAME + ".tooltip_details";
	private static final String FRAME_TOOLTIP_KEY         = "item." + SimplePortals.MOD_ID + "." + ModItems.FRAME_NAME + ".tooltip";
	private static final String FRAME_TOOLTIP_DETAILS_KEY = "item." + SimplePortals.MOD_ID + "." + ModItems.FRAME_NAME + ".tooltip_details";
	private static final String POWER_GAUGE_TOOLTIP_KEY         = "item." + SimplePortals.MOD_ID + "." + ModItems.POWER_GAUGE_NAME + ".tooltip";
	private static final String POWER_GAUGE_TOOLTIP_DETAILS_KEY = "item." + SimplePortals.MOD_ID + "." + ModItems.POWER_GAUGE_NAME + ".tooltip_details";

	@Override
	public void onInitializeClient()
	{
		// Config
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
			// Loading the config earlier can cause the validator of the power_source setting to fail, if a fabric tag is set,
			// because those are loaded later.
			Config.initialize(SimplePortals.MOD_ID, SimplePortals.CONFIG_GUI_TITLE, false, Settings::init);
		});

		Config.registerClientSideNetworking();

		// Custom Packets
		ClientPacketHandlers.register();

		// Tooltips
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) -> {
			String TOOLTIP_KEY;
			String TOOLTIP_DETAILS_KEY;

			if (stack.getItem() == ModItems.FRAME)
			{
				TOOLTIP_KEY = FRAME_TOOLTIP_KEY;
				TOOLTIP_DETAILS_KEY = FRAME_TOOLTIP_DETAILS_KEY;
			}
			else if (stack.getItem() == ModItems.POWER_GAUGE)
			{
				TOOLTIP_KEY = POWER_GAUGE_TOOLTIP_KEY;
				TOOLTIP_DETAILS_KEY = POWER_GAUGE_TOOLTIP_DETAILS_KEY;
			}
			else if (stack.getItem() == ModItems.PORTAL_ACTIVATOR)
			{
				TOOLTIP_KEY = PORTAL_ACTIVATOR_TOOLTIP_KEY;
				TOOLTIP_DETAILS_KEY = PORTAL_ACTIVATOR_TOOLTIP_DETAILS_KEY;
			}
			else
			{
				return;
			}

			var mc = Minecraft.getInstance();
			long windowHandle = mc.getWindow().handle();
			int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
			int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

			if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
			{
				int maxWidth = mc.getWindow().getGuiScaledWidth() / 3;
				lines.addAll(Utils.multiLineTranslateWithMaxWidth(TOOLTIP_DETAILS_KEY, maxWidth));
			}
			else
			{
				lines.add(Component.translatable(TOOLTIP_KEY));
			}
		});
	}
}
