package net.zarathul.simplemodslib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.zarathul.simplemodslib.api.fluid.FluidContainerComponent;
import net.zarathul.simplemodslib.api.fluid.FluidHelper;
import net.zarathul.simplemodslib.api.fluid.IFluidContainerItem;
import net.zarathul.simplemodslib.mixin.ItemAccessor;

import java.util.ArrayList;
import java.util.List;

public class SimpleModsLib implements ModInitializer
{
	public static final String MOD_ID = "simplemodslib";

	// Creative Mode Tab
	public static final Identifier CREATIVE_MODE_TAB_ID = Identifier.fromNamespaceAndPath(SimpleModsLib.MOD_ID, "creative_tab");
	public static final String CREATIVE_MODE_TAB_TITLE = "Simple Mods";
	public static final List<Item> creativeModeTabItems = new ArrayList<>();
	public static final CreativeModeTab creativeModeTab = makeCreativeModeTab();

	// This block and item only exist as providers for the creative tab icon
	public static final Identifier LOGO_BLOCK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "logo");
	public static final LogoBlock logoBlock = new LogoBlock(ResourceKey.create(Registries.BLOCK, LOGO_BLOCK_ID));
	public static final LogoItem logoItem = new LogoItem(logoBlock, ResourceKey.create(Registries.ITEM, LOGO_BLOCK_ID));

	// Fluid API
	public static final Identifier CONTAINER_ITEM_FLUID_TINT_SOURCE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "container_item_fluid");
	public static DataComponentType<FluidContainerComponent> FLUID_CONTAINER_COMPONENT = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(MOD_ID, "fluid_container"),
		DataComponentType.<FluidContainerComponent>builder().persistent(FluidContainerComponent.CODEC).build());


	@Override
	public void onInitialize()
	{
		Registry.register(BuiltInRegistries.ITEM, LOGO_BLOCK_ID, logoItem);
		Registry.register(BuiltInRegistries.BLOCK, LOGO_BLOCK_ID, logoBlock);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_ID, creativeModeTab);

		// Make IFluidContainerItems usable on IFluidHandlers while crouching.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide() || (!player.isCrouching()) || player.isSpectator()) return InteractionResult.PASS;

			ItemStack heldItem = player.getItemInHand(hand);
			if (heldItem.getItem() instanceof IFluidContainerItem && FluidHelper.isFluidHandler(world, hit.getBlockPos()))
			{
				BlockState blockState = world.getBlockState(hit.getBlockPos());
				InteractionResult result = blockState.useItemOn(heldItem, world, player, hand, hit);

				return result;
			}

			return InteractionResult.PASS;
		});

		// Prevent buckets and water bottles from doing their usual thing when right-clicking an IFluidHandler.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player.isSpectator()) return InteractionResult.PASS;

			ItemStack heldItemStack = player.getItemInHand(hand);
			Item heldItem = heldItemStack.getItem();

			if (heldItem instanceof BucketItem || (heldItem instanceof PotionItem && heldItemStack.get(DataComponents.POTION_CONTENTS).is(Potions.WATER)))
			{
				BlockHitResult hit = ItemAccessor.getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);
				if (hit.getType() == HitResult.Type.BLOCK && FluidHelper.isFluidHandler(world, hit.getBlockPos()))
				{
					return InteractionResult.SUCCESS;
				}
			}

			return InteractionResult.PASS;
		});
	}

	private static CreativeModeTab makeCreativeModeTab()
	{
		return FabricCreativeModeTab.builder()
			.title(Component.literal(CREATIVE_MODE_TAB_TITLE))
			.icon(() -> new ItemStack(logoBlock))
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
