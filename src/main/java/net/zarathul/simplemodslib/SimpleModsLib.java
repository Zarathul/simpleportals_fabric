package net.zarathul.simplemodslib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SimpleModsLib implements ModInitializer
{
	public static final String MOD_ID = "simplemodslib";
	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	// Creative Mode Tab
	public static final Identifier CREATIVE_MODE_TAB_ID = Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID, "creative_tab");
	public static final String CREATIVE_MODE_TAB_TITLE = "Simple Mods";
	public static final List<Item> creativeModeTabItems = new ArrayList<>();
	public static final CreativeModeTab creativeModeTab = makeCreativeModeTab();

	public static Identifier modId(String name) { return Identifier.fromNamespaceAndPath(MOD_ID, name); }

	@Override
	public void onInitialize()
	{
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_ID, creativeModeTab);

		ModBlocks.init();
		ModItems.init();
		ModComponents.init();

		UseBlockCallback.EVENT.register(ModBlocks::useBlockCallback);
		UseItemCallback.EVENT.register(ModItems::useItemCallback);
	}

	private static CreativeModeTab makeCreativeModeTab()
	{
		return FabricCreativeModeTab.builder()
			.title(Component.literal(CREATIVE_MODE_TAB_TITLE))
			.icon(() -> new ItemStack(ModBlocks.LOGO))
			.displayItems(SimpleModsLib::creativeModeTabDisplayItemsGenerator)
			.build();
	}

	private static void creativeModeTabDisplayItemsGenerator(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output)
	{
		for (Item item : creativeModeTabItems)
		{
			output.accept(item);
		}
	}
}
