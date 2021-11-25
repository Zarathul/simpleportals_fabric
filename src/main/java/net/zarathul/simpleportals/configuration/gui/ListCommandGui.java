package net.zarathul.simpleportals.configuration.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.zarathul.simpleportals.common.Utils;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ListCommandGui extends Screen
{
	private PortalList portalList;
	private List<PortalInfo> portals;
	private int titleHeight;
	private Component filterLabel;
	private EnumOptionButton<Filter.Type> filterTypeButton;
	private EnumOptionButton<Filter.Condition> filterConditionButton;
	private EditBox filterValueBox;
	private Filter filter;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = 20;

	public ListCommandGui(List<PortalInfo> portals)
	{
		this(portals, Filter.NONE);
	}

	public ListCommandGui(List<PortalInfo> portals, Filter filter)
	{
		super(new TextComponent("Portals"));

		filterLabel = new TranslatableComponent("config.filter_by");

		this.portals = portals;
		this.filter = filter;
	}

	@Override
	protected void init()
	{
		titleHeight = minecraft.font.wordWrapHeight(title.getString(), width - 2 * PADDING);
		int paddedTitleHeight = titleHeight + PADDING * 2;
		int portalListY = titleHeight + 30 + PADDING;
		int filterLabelWidth = minecraft.font.width(filterLabel);

		portalList = new PortalList(portals, filter, minecraft, width, height, portalListY, height - PADDING, 26);
		addRenderableWidget(portalList);

		addRenderableWidget(new Button(width - 60 - PADDING, 0, 60, BUTTON_HEIGHT, new TranslatableComponent("config.back"), button -> minecraft.setScreen(null)));

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
		filterValueBox = new EditBox(minecraft.font, xOffset, paddedTitleHeight, filterBoxWidth, BUTTON_HEIGHT, TextComponent.EMPTY);
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

		addRenderableWidget(new Button(xOffset, paddedTitleHeight, applyButtonWidth, BUTTON_HEIGHT, new TranslatableComponent("config.apply"), button -> {
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
		final int white = ChatFormatting.WHITE.getColor();
		this.renderBackground(poseStack);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		super.render(poseStack, mouseX, mouseY, partialTicks);
		minecraft.font.draw(poseStack, title.getVisualOrderText(), PADDING, PADDING, white);
		minecraft.font.draw(poseStack, filterLabel.getVisualOrderText(), PADDING, titleHeight + 2 * PADDING + 6, white);
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
		private List<PortalInfo> portals;

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
			private final EditBox addressBox;
			private final EditBox powerBox;

			public Entry(PortalInfo portal)
			{
				final int white = ChatFormatting.WHITE.getColor();

				dimensionBox = new EditBox(minecraft.font, 0, 0, 100, itemHeight - PADDING, TextComponent.EMPTY);
				dimensionBox.setTextColor(white);
				dimensionBox.setMaxLength(256);
				dimensionBox.setValue(portal.dimension.toString());
				dimensionBox.moveCursorToStart();

				locationBox = new EditBox(minecraft.font, 100 + PADDING, 0, 100, itemHeight - PADDING, TextComponent.EMPTY);
				locationBox.setTextColor(white);
				locationBox.setMaxLength(48);
				locationBox.setValue(Utils.getReadablyBlockPos(portal.location));
				locationBox.moveCursorToStart();

				addressBox = new EditBox(minecraft.font, 200 + 2 * PADDING, 0, 100, itemHeight - PADDING, TextComponent.EMPTY);
				addressBox.setTextColor(white);
				addressBox.setMaxLength(256);
				addressBox.setValue(portal.address.toString());
				addressBox.moveCursorToStart();

				powerBox = new EditBox(minecraft.font, 300 + 3 * PADDING, 0, 100, itemHeight - PADDING, TextComponent.EMPTY);
				powerBox.setTextColor(white);
				powerBox.setMaxLength(10);
				powerBox.setValue(Integer.toString(portal.power));
				powerBox.moveCursorToStart();

				addRenderableWidget(dimensionBox);
				addRenderableWidget(locationBox);
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
				int boxHalfWidth = (width - 5 * PADDING) / 8;
				int boxWidth = (width - 5 * PADDING - boxHalfWidth) / 3;
				int xOffset = left + PADDING;

				dimensionBox.x = xOffset;
				dimensionBox.y = top;
				dimensionBox.setWidth(boxWidth);
				dimensionBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += boxWidth + PADDING;

				locationBox.x = xOffset;
				locationBox.y = top;
				locationBox.setWidth(boxWidth);
				locationBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += boxWidth + PADDING;

				addressBox.x = xOffset;
				addressBox.y = top;
				addressBox.setWidth(boxWidth);
				addressBox.render(poseStack, mouseX, mouseY, partialTicks);

				xOffset += boxWidth + PADDING;

				powerBox.x = xOffset;
				powerBox.y = top;
				powerBox.setWidth(boxHalfWidth);
				powerBox.render(poseStack, mouseX, mouseY, partialTicks);
			}
		}
	}
}
