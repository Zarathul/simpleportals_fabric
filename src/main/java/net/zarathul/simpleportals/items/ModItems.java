package net.zarathul.simpleportals.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.zarathul.simplemodslib.SimpleModsLib;
import net.zarathul.simplemodslib.api.item.ItemRegistrar;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.blocks.ModBlocks;

import java.util.Collections;

public final class ModItems
{
	private static final ItemRegistrar REGISTRAR = new ItemRegistrar(SimplePortals.MOD_ID);

	public static final String FRAME_NAME = "portal_frame";
	public static final String POWER_GAUGE_NAME = "power_gauge";
	public static final String PORTAL_ACTIVATOR_NAME = "portal_activator";

	public static final BlockItem FRAME = REGISTRAR.register(FRAME_NAME, ModBlocks.FRAME, BlockItem::new, new Item.Properties().stacksTo(64));
	public static final BlockItem POWER_GAUGE = REGISTRAR.register(POWER_GAUGE_NAME, ModBlocks.POWER_GAUGE, BlockItem::new, new Item.Properties().stacksTo(64));
	public static final ItemPortalActivator PORTAL_ACTIVATOR = REGISTRAR.register(PORTAL_ACTIVATOR_NAME, ItemPortalActivator::new, new Item.Properties().stacksTo(1));

	public static void init()
	{
		SimplePortals.LOG.info("Registering items.");

		Collections.addAll(SimpleModsLib.creativeModeTabItems,
			FRAME,
			POWER_GAUGE,
			PORTAL_ACTIVATOR
		);
	}
}
