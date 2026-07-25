package net.zarathul.simpleportals.configuration.gui;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.common.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Environment(EnvType.CLIENT)
public class ListCommandGui extends Screen
{
	private PortalList portalList;
	private final List<PortalInfo> portals;
	private EnumOptionButton<Filter.Type> filterTypeButton;
	private EnumOptionButton<Filter.Condition> filterConditionButton;
	private EditBox filterValueBox;
	private StringWidget dimensionLabel;
	private StringWidget locationLabel;
	private StringWidget addressLabel;
	private StringWidget powerLabel;
	private final Filter filter;
	private final HeaderAndFooterLayout layout;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
	private static final int ADDRESS_ITEM_SIZE = Button.DEFAULT_HEIGHT;
	private static final int GOTO_BUTTON_SIZE = Button.DEFAULT_HEIGHT;
	private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 2 * PADDING;
	private static final int ENTRY_HEIGHT = 26;

	private static final Component FILTER_LABEL = Component.translatable("config.filter_by");
	private static final Component DIMENSION_HEADER = Component.translatable("config.dimension_header");
	private static final Component LOCATION_HEADER  = Component.translatable("config.location_header");
	private static final Component ADDRESS_HEADER   = Component.translatable("config.address_header");
	private static final Component POWER_HEADER     = Component.translatable("config.power_header");

	private static final int FILTER_TYPE_BUTTON_WIDTH = 80;
	private static final int FILTER_CONDITION_BUTTON_WIDTH = 100;
	private static final int APPLY_BUTTON_WIDTH = 60;
	private final int FILTER_LABEL_WIDTH = font.width(FILTER_LABEL);

	public ListCommandGui(List<PortalInfo> portals)
	{
		this(portals, Filter.NONE);
	}

	public ListCommandGui(List<PortalInfo> portals, Filter filter)
	{
		super(Component.translatable("config.portal_list_header"));

		this.portals = portals;
		this.filter = filter;

		int HEADER_HEIGHT = 2 * font.lineHeight + BUTTON_HEIGHT + 4 * PADDING;
		layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
	}

	@Override
	protected void init()
	{
		addHeader();
		addFooter();
		addContents();

		layout.visitWidgets(this::addRenderableWidget);

		repositionElements();
	}

	private void addHeader()
	{
		LinearLayout verticalLayout = layout.addToHeader(LinearLayout.vertical());
		verticalLayout.spacing(PADDING);

		// First row, containing only the title.
		verticalLayout.addChild(new StringWidget(title, font), layoutSettings -> layoutSettings.alignHorizontallyCenter().paddingTop(PADDING));

		// Second row, containing filtering widgets (Buttons, EditBox etc.).
		LinearLayout filterHorizontalLayout = LinearLayout.horizontal();
		verticalLayout.addChild(filterHorizontalLayout);

		filterHorizontalLayout.addChild(new StringWidget(FILTER_LABEL_WIDTH, 9, FILTER_LABEL, font), layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingHorizontal(PADDING));

		filterTypeButton = new EnumOptionButton<>(Filter.Type.class, Optional.of(filter.type), FILTER_TYPE_BUTTON_WIDTH, BUTTON_HEIGHT, (button -> {
			filterConditionButton.setAllowedValues(Filter.CONDITIONS_BY_TYPE.get(filterTypeButton.getValue()));
		}));
		filterHorizontalLayout.addChild(filterTypeButton, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterConditionButton = new EnumOptionButton<>(Filter.Condition.class, Optional.of(filter.condition), FILTER_CONDITION_BUTTON_WIDTH, BUTTON_HEIGHT);
		filterConditionButton.setAllowedValues(Filter.CONDITIONS_BY_TYPE.get(filter.type));
		filterHorizontalLayout.addChild(filterConditionButton, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterValueBox = new EditBox(font, 0, 0, 10, BUTTON_HEIGHT, CommonComponents.EMPTY);
		filterValueBox.setMaxLength(256);
		resizeFilterValueBox();
		if (filter.value != null)
		{
			if (filter.value instanceof BlockPos)
			{
				filterValueBox.setValue(Utils.getReadableBlockPos((BlockPos)filter.value));
			}
			else
			{
				filterValueBox.setValue(filter.value.toString());
			}
		}
		filterHorizontalLayout.addChild(filterValueBox, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterHorizontalLayout.addChild(Button.builder(Component.translatable("config.apply"), this::applyFilter).width(APPLY_BUTTON_WIDTH).build(), layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		// Third row, containing text headers for the elements in the portal list. This LinearLayout is only used to get another row. In row, the positioning is done manually (see 'repositionLabels()').
		LinearLayout thirdRow = LinearLayout.horizontal();
		verticalLayout.addChild(thirdRow);

		dimensionLabel = new StringWidget(0, 0, font.width(DIMENSION_HEADER.getVisualOrderText()), 9, DIMENSION_HEADER, font);
		thirdRow.addChild(dimensionLabel);
		locationLabel = new StringWidget(0, 0, font.width(LOCATION_HEADER.getVisualOrderText()), 9, LOCATION_HEADER, font);
		thirdRow.addChild(locationLabel);
		addressLabel = new StringWidget(0, 0, font.width(ADDRESS_HEADER.getVisualOrderText()), 9, ADDRESS_HEADER, font);
		thirdRow.addChild(addressLabel);
		powerLabel = new StringWidget(0, 0, font.width(POWER_HEADER.getVisualOrderText()), 9, POWER_HEADER, font);
		thirdRow.addChild(powerLabel);
	}

	private void addContents()
	{
		portalList = new PortalList(portals, filter, minecraft, width, 0, 0, ENTRY_HEIGHT);
		layout.addToContents(portalList);
	}

	private void addFooter()
	{
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());	// Done button
	}

	private void applyFilter(Button button)
	{
		String valueText = filterValueBox.getValue();
		Object value = (filterTypeButton.getValue() == Filter.Type.Power) ? Integer.valueOf(valueText) : valueText;

		Filter filter = new Filter(filterTypeButton.getValue(), filterConditionButton.getValue(), value);
		// Trying to modify the screen resulted in all kinds of graphical bugs, so let's just make a new one every time the filter changes.
		minecraft.gui.setScreen(new ListCommandGui(portals, filter));
	}

	@Override
	protected void repositionElements()
	{
		resizeFilterValueBox();

		layout.arrangeElements();
		portalList.updateSize(width, layout);
		layout.arrangeElements();

		// This needs to happen after 'layout.arrangeElements()', otherwise the positions are overridden.
		// The layout positioning is very rigid, so I see no other options than doing this manually.
		repositionLabels();
	}

	private void resizeFilterValueBox()
	{
		filterValueBox.setWidth(width - FILTER_LABEL_WIDTH - FILTER_TYPE_BUTTON_WIDTH - FILTER_CONDITION_BUTTON_WIDTH - APPLY_BUTTON_WIDTH - 6 * PADDING);
	}

	private void repositionLabels()
	{
		int rowWidth = width - PortalList.LEFT_RIGHT_BORDER * 2 - 4 * PADDING;
		int[] editBoxWidths = PortalList.calculateEditBoxWidths(rowWidth);

		int addressItemsWidth = 4 * ADDRESS_ITEM_SIZE;
		int xOffset = PortalList.LEFT_RIGHT_BORDER + 3;
		dimensionLabel.setX(Utils.centerIn(xOffset, editBoxWidths[0], dimensionLabel.getWidth()));
		xOffset += editBoxWidths[0] + PADDING;
		locationLabel.setX(Utils.centerIn(xOffset, editBoxWidths[1], locationLabel.getWidth()));
		xOffset += editBoxWidths[1] + GOTO_BUTTON_SIZE + addressItemsWidth + 2 * PADDING;
		addressLabel.setX(Utils.centerIn(xOffset, editBoxWidths[2], addressLabel.getWidth()));
		xOffset += editBoxWidths[2] + PADDING;
		powerLabel.setX(Utils.centerIn(xOffset, editBoxWidths[3], powerLabel.getWidth()));
	}

	@Environment(EnvType.CLIENT)
	public class PortalList extends ContainerObjectSelectionList<PortalList.Entry>
	{
		public static final int LEFT_RIGHT_BORDER = 20;
		private final List<PortalInfo> portals;

		public PortalList(List<PortalInfo> portals, Filter filter, Minecraft mc, int width, int height, int top, int itemHeight)
		{
			super(mc, width, height, top, itemHeight);

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
						case Equals   -> portals.stream().filter(portal -> portal.dimension().toString().equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.dimension().toString().contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}

					break;

				case Location:
					switch (filter.condition)
					{
						case Equals   -> portals.stream().filter(portal -> Utils.getReadableBlockPos(portal.location()).equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> Utils.getReadableBlockPos(portal.location()).contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Address:
					switch (filter.condition)
					{
						case Equals   -> portals.stream().filter(portal -> portal.address().toString().equals(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.address().toString().contains(filter.value.toString())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Power:
					switch (filter.condition)
					{
						case Equals      -> portals.stream().filter(portal -> portal.power() == (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
						case LessThan    -> portals.stream().filter(portal -> portal.power()  < (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
						case GreaterThan -> portals.stream().filter(portal -> portal.power()  > (int)filter.value).forEach(portal -> addEntry(new Entry(portal)));
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

		@Override
		public int getRowWidth()
		{
			return width - LEFT_RIGHT_BORDER * 2;
		}

		@Override
		protected int scrollBarX() { return width - LEFT_RIGHT_BORDER; }

		public static int[] calculateEditBoxWidths(int rowWidth)
		{
			int[] editBoxWidths = new int[4];

			int addressItemsWidth = 4 * ADDRESS_ITEM_SIZE;
			int totalBoxesWidth = rowWidth - addressItemsWidth - (GOTO_BUTTON_SIZE + PADDING);
			float onePercentOfTotalBoxesWidth = totalBoxesWidth / 100f;
			editBoxWidths[0] = (int)Math.floor(onePercentOfTotalBoxesWidth * 25);	// dimension
			editBoxWidths[1] = (int)Math.floor(onePercentOfTotalBoxesWidth * 25);	// location
			editBoxWidths[2] = (int)Math.floor(onePercentOfTotalBoxesWidth * 45);	// address
			editBoxWidths[3] = (int)Math.floor(onePercentOfTotalBoxesWidth *  5);	// power

			return editBoxWidths;
		}

		@Environment(EnvType.CLIENT)
		public class Entry extends ContainerObjectSelectionList.Entry<PortalList.Entry>
		{
			private final EditBox dimensionBox;
			private final EditBox locationBox;
			private final ImageButton gotoLocationButton;
			private final EditBox addressBox;
			private final EditBox powerBox;
			private final List<ItemStack> addressItems = new ArrayList<>(4);
			private final FormattedCharSequence[] addressBlockCountLabels = new FormattedCharSequence[4];
			private final int[] addressBlockCountLabelWidths = new int[4];

			public Entry(PortalInfo portal)
			{
				dimensionBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				dimensionBox.setMaxLength(256);
				dimensionBox.setValue(portal.dimension().identifier().toString());
				dimensionBox.setEditable(false);
				dimensionBox.moveCursorToStart(false);

				locationBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				locationBox.setMaxLength(48);
				locationBox.setValue(Utils.getReadableBlockPos(portal.location()));
				locationBox.setEditable(false);
				locationBox.moveCursorToStart(false);

				gotoLocationButton = new ImageButton(0, 0, GOTO_BUTTON_SIZE, GOTO_BUTTON_SIZE, new WidgetSprites(Identifier.withDefaultNamespace("icon/link")), button -> {
					minecraft.gui.setScreen(null);
					ClientPlayNetworking.send(new SimplePortals.TpdCommandPayload(portal.dimension().identifier(), portal.location()));
				});

				int i = 0;

				for (var addressComponent : portal.address().getBlockCounts().entrySet())
				{
					addressBlockCountLabels[i] = Component.literal(String.format("%dx", addressComponent.getValue())).getVisualOrderText();
					addressBlockCountLabelWidths[i] = font.width(addressBlockCountLabels[i]);
					i++;

					Block addressBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(addressComponent.getKey()));
					ItemStack addressBlockItem = new ItemStack(addressBlock, 1);
					addressItems.add(addressBlockItem);
				}

				addressBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				addressBox.setMaxLength(256);
				addressBox.setValue(portal.address().toString());
				addressBox.setEditable(false);
				addressBox.moveCursorToStart(false);

				powerBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				powerBox.setMaxLength(10);
				powerBox.setValue(Integer.toString(portal.power()));
				powerBox.setEditable(false);
				powerBox.moveCursorToStart(false);
			}

			@Override
			public List<? extends NarratableEntry> narratables()
			{
				return ImmutableList.of();
			}

			@Override
			public List<? extends GuiEventListener> children()
			{
				return ImmutableList.of(dimensionBox, locationBox, gotoLocationButton, addressBox, powerBox);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a)
			{
				int widthWithoutPadding = getContentWidth() - 4 * PADDING;
				int[] boxWidths = calculateEditBoxWidths(widthWithoutPadding);
				int dimensionBoxWidth = boxWidths[0];
				int locationBoxWidth  = boxWidths[1];
				int addressBoxWidth   = boxWidths[2];
				int powerBoxWidth     = boxWidths[3];

				int xOffset = getContentX();

				dimensionBox.setX(xOffset);
				dimensionBox.setY(getContentY());
				dimensionBox.setWidth(dimensionBoxWidth);
				dimensionBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += dimensionBoxWidth + PADDING;

				locationBox.setX(xOffset);
				locationBox.setY(getContentY());
				locationBox.setWidth(locationBoxWidth);
				locationBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += locationBoxWidth + PADDING;

				gotoLocationButton.setX(xOffset);
				gotoLocationButton.setY(getContentY());
				gotoLocationButton.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += GOTO_BUTTON_SIZE + PADDING;

				for (int i = 0; i < addressItems.size(); i++)
				{
					ItemStack item = addressItems.get(i);
					graphics.item(item, xOffset, getContentY());
					graphics.text(minecraft.font, addressBlockCountLabels[i], Utils.centerIn(xOffset, ADDRESS_ITEM_SIZE, addressBlockCountLabelWidths[i]), getContentY() + ADDRESS_ITEM_SIZE - 3, -1);

					xOffset += ADDRESS_ITEM_SIZE;
				}

				// Keep the distance to the next EditBox the same even if there are less than 4 address items.
				xOffset += (4 - addressItems.size()) * (ADDRESS_ITEM_SIZE);
				xOffset += PADDING;

				addressBox.setX(xOffset);
				addressBox.setY(getContentY());
				addressBox.setWidth(addressBoxWidth);
				addressBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += addressBoxWidth + PADDING;

				powerBox.setX(xOffset);
				powerBox.setY(getContentY());
				powerBox.setWidth(powerBoxWidth);
				powerBox.extractRenderState(graphics, mouseX, mouseY, a);
			}
		}
	}
}
