package net.zarathul.simpleportals.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.blocks.BlockPortalFrame;
import net.zarathul.simplemodslib.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The item used to activate portals.
 */
public class ItemPortalActivator extends Item
{
	private static final String toolTipKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_ACTIVATOR_NAME + ".tooltip";
	private static final String toolTipDetailsKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_ACTIVATOR_NAME + ".tooltip_details";
	
	public ItemPortalActivator(ResourceKey<Item> id)
	{
		super(new Item.Properties().setId(id).stacksTo(1));

		// Custom dispenser behavior that allows dispensers to activate portals with a contained
		// portal activator.
		DispenserBlock.registerBehavior(this, new DefaultDispenseItemBehavior()
		{
			private final DefaultDispenseItemBehavior ejectBehavior = new DefaultDispenseItemBehavior();

			@Override
			protected ItemStack execute(BlockSource source, ItemStack stack)
			{
				ServerLevel world = source.level();
				BlockState dispenser = world.getBlockState(source.pos());

				// Start searching for portal frame blocks in the direction the dispenser is facing.
				Direction dispenserFacing = dispenser.getValue(DispenserBlock.FACING);
				BlockPos searchStartPos = source.pos().relative(dispenserFacing);

				if (world.isEmptyBlock(searchStartPos))
				{
					// Search along the other two axis besides the one the dispenser is facing in.
					// E.g. dispenser faces south: Search one block south of the dispenser, up, down,
					// east and west.
					List<Direction> searchDirections = new ArrayList<>();
					Axis dispenserAxis = dispenserFacing.getAxis();

					for (Axis axis : Axis.values())
					{
						if (axis != dispenserAxis)
						{
							searchDirections.add(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
							searchDirections.add(Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE));
						}
					}

					BlockPos currentPos;

					for (Direction facing : searchDirections)
					{
						currentPos = searchStartPos.relative(facing);

						if (world.getBlockState(currentPos).getBlock() instanceof BlockPortalFrame)
						{
							if (SimplePortals.portalRegistry.activatePortal(world, currentPos, facing.getOpposite()))
							{
								return stack;
							}
						}
					}
				}

				return ejectBehavior.dispense(source, stack);
			}
		});
	}

	@Environment(EnvType.CLIENT)
	public void addTooltip(ItemStack stack, TooltipContext tooltipContext, TooltipFlag tooltipFlag, List<Component> lines)
	{
		long windowHandle = Minecraft.getInstance().getWindow().handle();
		int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

		if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
		{
			lines.addAll(Utils.multiLineTranslate(toolTipDetailsKey, 1));
		}
		else
		{
			lines.add(Component.translatable(toolTipKey));
		}
	}
}