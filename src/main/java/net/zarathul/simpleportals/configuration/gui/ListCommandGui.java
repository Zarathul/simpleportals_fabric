package net.zarathul.simpleportals.configuration.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.common.Utils;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ListCommandGui extends Screen
{
	private PortalList portalList;
	private final List<PortalInfo> portals;
	private int titleHeight;
	private int portalListY;
	private final Component filterLabel;
	private EnumOptionButton<Filter.Type> filterTypeButton;
	private EnumOptionButton<Filter.Condition> filterConditionButton;
	private EditBox filterValueBox;
	private final Filter filter;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ADDRESS_ITEM_SIZE = 16;
	private static final int GOTO_BUTTON_SIZE = 20;
	private static final int WHITE = ChatFormatting.WHITE.getColor();
	private static final Component dimensionHeader = Component.translatable("config.dimension_header");
	private static final Component locationHeader  = Component.translatable("config.location_header");
	private static final Component addressHeader   = Component.translatable("config.address_header");
	private static final Component powerHeader     = Component.translatable("config.power_header");

	public ListCommandGui(List<PortalInfo> portals)
	{
		this(portals, Filter.NONE);
	}

	public ListCommandGui(List<PortalInfo> portals, Filter filter)
	{
		super(Component.translatable("config.portal_list_header"));

		filterLabel = Component.translatable("config.filter_by");

		this.portals = portals;
		this.filter = filter;
	}

	@Override
	protected void init()
	{
		titleHeight = minecraft.font.wordWrapHeight(title.getString(), width - 2 * PADDING);
		int paddedTitleHeight = titleHeight + PADDING * 2;
		portalListY = titleHeight + 45 + PADDING;
		int filterLabelWidth = minecraft.font.width(filterLabel);

		portalList = new PortalList(portals, filter, minecraft, width, height, portalListY, height - PADDING, 26);
		addRenderableWidget(portalList);

		addRenderableWidget(new Button(width - 60 - PADDING, 0, 60, BUTTON_HEIGHT, Component.translatable("config.back"), button -> minecraft.setScreen(null)));

		int xOffset = filterLabelWidth + PADDING;
		filterTypeButton = new EnumOptionButton<>(Filter.Type.class, "", xOffset, paddedTitleHeight, 80, BUTTON_HEIGHT);
		filterTypeButton.setValue(filter.type);
		addRenderableWidget(filterTypeButton);
		xOffset += 80 + PADDING;

		filterConditionButton = new EnumOptionButton<>(Filter.Condition.class, "", xOffset, paddedTitleHeight, 100, BUTTON_HEIGHT);
		filterConditionButton.setValue(filter.condition);
		addRenderableWidget(filterConditionButton);
		xOffset += 100 + PADDING;

		int applyButtonWidth = 60;
		int filterBoxWidth = width - xOffset - 2 * PADDING - applyButtonWidth;
		filterValueBox = new EditBox(minecraft.font, xOffset, paddedTitleHeight, filterBoxWidth, BUTTON_HEIGHT, CommonComponents.EMPTY);
		filterValueBox.setMaxLength(256);
		if (filter.value != null)
		{
			if (filter.value instanceof BlockPos)
			{
				filterValueBox.setValue(Utils.getReadablyBlockPos((BlockPos)filter.value));
			}
			else
			{
				filterValueBox.setValue(filter.value.toString());
			}
		}
		addRenderableWidget(filterValueBox);
		xOffset += filterBoxWidth + PADDING;

		addRenderableWidget(new Button(xOffset, paddedTitleHeight + 1, applyButtonWidth, BUTTON_HEIGHT, Component.translatable("config.apply"), button -> {
			String valueText = filterValueBox.getValue();
			Object value = (filterTypeButton.getValue() == Filter.Type.Power) ? Integer.valueOf(valueText) : valueText;

			Filter filter = new Filter(filterTypeButton.getValue(), filterConditionButton.getValue(), value);
			// Trying to modify the screen resulted in all kinds of graphical bugs, so let's just make a new one every time the filter changes.
			minecraft.setScreen(new ListCommandGui(portals, filter));
		}));
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(poseStack);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		super.render(poseStack, mouseX, mouseY, partialTicks);
		minecraft.font.draw(poseStack, title.getVisualOrderText(), PADDING, PADDING, WHITE);
		minecraft.font.draw(poseStack, filterLabel.getVisualOrderText(), PADDING, titleHeight + 2 * PADDING + 6, WHITE);

		int widthWithoutPadding = portalList.getRowWidth() - 7 * PADDING;
		int addressItemsWidth = 4 * ADDRESS_ITEM_SIZE + 3 * PADDING;
		int totalBoxesWidth = widthWithoutPadding - addressItemsWidth - (GOTO_BUTTON_SIZE + PADDING);
		int dimensionBoxWidth = (int)Math.floor((4.0f / 12.0f) * totalBoxesWidth);
		int locationBoxWidth  = (int)Math.floor((3.0f / 12.0f) * totalBoxesWidth);
		int addressBoxWidth   = (int)Math.floor((4.0f / 12.0f) * totalBoxesWidth);

		int xOffset = width - portalList.getRowWidth() - 2 * PADDING;
		int yOffset = portalListY - minecraft.font.lineHeight - PADDING;

		minecraft.font.draw(poseStack, dimensionHeader.getVisualOrderText(), xOffset, yOffset, WHITE);
		xOffset += dimensionBoxWidth + PADDING;

		minecraft.font.draw(poseStack, locationHeader.getVisualOrderText(), xOffset, yOffset, WHITE);
		xOffset += locationBoxWidth + GOTO_BUTTON_SIZE + 2 * PADDING;

		minecraft.font.draw(poseStack, addressHeader.getVisualOrderText(), xOffset, yOffset, WHITE);
		xOffset += addressBoxWidth + addressItemsWidth + 2 * PADDING;

		minecraft.font.draw(poseStack, powerHeader.getVisualOrderText(), xOffset, yOffset, WHITE);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (this.portalList != null) portalList.tick();
	}

	@Environment(EnvType.CLIENT)
	public class PortalList extends AbstractSelectionList<PortalList.Entry>
	{
		private static final int LEFT_RIGHT_BORDER = 20;
		private final List<PortalInfo> portals;

		public PortalList(List<PortalInfo> portals, Filter filter, Minecraft mc, int width, int height, int top, int bottom, int itemHeight)
		{
			super(mc, width, height, top, bottom, itemHeight);

			this.portals = portals;
			updateEntries(filter);
		}

		public void updateEntries(Filter filter)
		{
			clearEntries();

			switch (filter.type)
			{
				case Dimension:
					switch (filter.condition)
					{
						case Equals   -> portals.stream().filter(portal -> portal.dimension.toString().equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.dimension.toString().contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}

					break;

				case Location:
					switch (filter.condition)
					{
						case Equals   -> portals.stream().filter(portal -> Utils.getReadablyBlockPos(portal.location).equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> Utils.getReadablyBlockPos(portal.location).contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Address:
					switch (filter.condition)
					{
						case Equals   -> portals.stream().filter(portal -> portal.address.toString().equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.address.toString().contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Power:
					switch (filter.condition)
					{
						case Equals      -> portals.stream().filter(portal -> portal.power == (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
						case LessThan    -> portals.stream().filter(portal -> portal.power  < (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
						case GreaterThan -> portals.stream().filter(portal -> portal.power  > (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				default:
					for (PortalInfo portal : portals)
					{
						addEntry(new Entry(portal));
					}
					break;
			}
		}

		public void tick()
		{
			for (Entry child : this.children())
			{
				child.tick();
			}
		}

		@Override
		public int getRowWidth()
		{
			return width - LEFT_RIGHT_BORDER * 2;
		}

		@Override
		protected int getScrollbarPosition()
		{
			return width - LEFT_RIGHT_BORDER;
		}

		@Override
		public void updateNarration(NarrationElementOutput narrationElementOutput)
		{
		}

		@Environment(EnvType.CLIENT)
		public class Entry extends AbstractSelectionList.Entry<PortalList.Entry>
		{
			private final EditBox dimensionBox;
			private final EditBox locationBox;
			private final ImageButton gotoLocationButton;
			private final EditBox addressBox;
			private final EditBox powerBox;
			private final List<ItemStack> addressItems;
			private final FormattedCharSequence[] countLabels;
			private final int[] countLabelWidths;

			public Entry(PortalInfo portal)
			{
				dimensionBox = new EditBox(minecraft.font, 0, 0, 100, itemHeight - PADDING, CommonComponents.EMPTY);
				dimensionBox.setTextColor(WHITE);
				dimensionBox.setMaxLength(256);
				dimensionBox.setValue(portal.dimension.location().toString());
				dimensionBox.moveCursorToStart();

				locationBox = new EditBox(minecraft.font, 100 + PADDING, 0, 100, itemHeight - PADDING, CommonComponents.EMPTY);
				locationBox.setTextColor(WHITE);
				locationBox.setMaxLength(48);
				locationBox.setValue(Utils.getReadablyBlockPos(portal.location));
				locationBox.moveCursorToStart();

				gotoLocationButton = new ImageButton(0, 0, GOTO_BUTTON_SIZE, GOTO_BUTTON_SIZE, 0, 106, GOTO_BUTTON_SIZE, Button.WIDGETS_LOCATION, 256, 256, button -> {
					minecraft.setScreen(null);

					FriendlyByteBuf sendBuffer = PacketByteBufs.create();
					sendBuffer.writeResourceLocation(portal.dimension.location());	// dimension
					sendBuffer.writeBlockPos(portal.location);						// location

					ClientPlayNetworking.send(SimplePortals.TPD_COMMAND_PACKET_ID, sendBuffer);
				});

				addressItems = new ArrayList<>(4);
				countLabels = new FormattedCharSequence[4];
				countLabelWidths = new int[4];

				for (int i = 0; i < 4; i++)
				{
					countLabels[i] = Component.translatable(String.format("%dx", i + 1)).getVisualOrderText();
					countLabelWidths[i] = minecraft.font.width(countLabels[i]);
				}

				portal.address.forEachAddressComponent((addressBlockName, blockCount) -> {
					Block block = Registry.BLOCK.get(new ResourceLocation(addressBlockName));
					ItemStack item = new ItemStack(block, blockCount);
					addressItems.add(item);
				});

				addressBox = new EditBox(minecraft.font, 200 + 2 * PADDING, 0, 100, itemHeight - PADDING, CommonComponents.EMPTY);
				addressBox.setTextColor(WHITE);
				addressBox.setMaxLength(256);
				addressBox.setValue(portal.address.toString());
				addressBox.moveCursorToStart();

				powerBox = new EditBox(minecraft.font, 300 + 3 * PADDING, 0, 100, itemHeight - PADDING, CommonComponents.EMPTY);
				powerBox.setTextColor(WHITE);
				powerBox.setMaxLength(10);
				powerBox.setValue(Integer.toString(portal.power));
				powerBox.moveCursorToStart();

				addRenderableWidget(dimensionBox);
				addRenderableWidget(locationBox);
				addRenderableWidget(gotoLocationButton);
				addRenderableWidget(addressBox);
				addRenderableWidget(powerBox);
			}

			public void tick()
			{
				dimensionBox.tick();
				locationBox.tick();
				addressBox.tick();
				powerBox.tick();
			}

			@Override
			public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHot, float partialTicks)
			{
				int widthWithoutPadding = width - 7 * PADDING;
				int addressItemsWidth = 4 * ADDRESS_ITEM_SIZE + 3 * PADDING;
				int totalBoxesWidth = widthWithoutPadding - addressItemsWidth - (GOTO_BUTTON_SIZE + PADDING);
				int dimensionBoxWidth = (int)Math.floor((4.0f / 12.0f) * totalBoxesWidth);
				int locationBoxWidth  = (int)Math.floor((3.0f / 12.0f) * totalBoxesWidth);
				int addressBoxWidth   = (int)Math.floor((4.0f / 12.0f) * totalBoxesWidth);
				int powerBoxWidth     = (int)Math.floor((1.0f / 12.0f) * totalBoxesWidth);

				int xOffset = left + PADDING;

				dimensionBox.x = xOffset;
				dimensionBox.y = top;
				dimensionBox.setWidth(dimensionBoxWidth);

				dimensionBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += dimensionBoxWidth + PADDING;

				locationBox.x = xOffset;
				locationBox.y = top;
				locationBox.setWidth(locationBoxWidth);
				locationBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += locationBoxWidth + PADDING;

				gotoLocationButton.x = xOffset;
				gotoLocationButton.y = top;
				gotoLocationButton.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += GOTO_BUTTON_SIZE + PADDING;

				ItemRenderer itemRenderer = minecraft.getItemRenderer();

				for (ItemStack item : addressItems)
				{
					itemRenderer.renderGuiItem(item, xOffset, top);
					minecraft.font.draw(poseStack, countLabels[item.getCount() - 1], xOffset + ADDRESS_ITEM_SIZE - countLabelWidths[item.getCount() - 1], top + ADDRESS_ITEM_SIZE, WHITE);

					xOffset += ADDRESS_ITEM_SIZE + PADDING;
				}

				// Keep the distance to the next EditBox the same even if there are less than 4 address items.
				xOffset += (4 - addressItems.size()) * (ADDRESS_ITEM_SIZE + PADDING);

				addressBox.x = xOffset;
				addressBox.y = top;
				addressBox.setWidth(addressBoxWidth);
				addressBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += addressBoxWidth + PADDING;

				powerBox.x = xOffset;
				powerBox.y = top;
				powerBox.setWidth(powerBoxWidth);
				powerBox.render(poseStack, mouseX, mouseY, partialTicks);
			}
		}
	}
}
