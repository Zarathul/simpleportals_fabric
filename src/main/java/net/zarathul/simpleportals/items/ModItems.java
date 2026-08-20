package net.zarathul.simpleportals.items;

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

	public static final ItemPortalFrame FRAME = REGISTRAR.register(FRAME_NAME, ModBlocks.FRAME, ItemPortalFrame::new);
	public static final ItemPowerGauge POWER_GAUGE = REGISTRAR.register(POWER_GAUGE_NAME, ModBlocks.POWER_GAUGE, ItemPowerGauge::new);
	public static final ItemPortalActivator PORTAL_ACTIVATOR = REGISTRAR.register(PORTAL_ACTIVATOR_NAME, ItemPortalActivator::new);

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
