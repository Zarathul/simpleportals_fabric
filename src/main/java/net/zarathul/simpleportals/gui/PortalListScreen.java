package net.zarathul.simpleportals.gui;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.zarathul.simplemodslib.Utils;
import net.zarathul.simplemodslib.api.gui.CycleButtonEx;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.network.payloads.DeactivatePortalPayload;
import net.zarathul.simpleportals.network.payloads.SetPortalPowerPayload;
import net.zarathul.simpleportals.network.payloads.TpdCommandPayload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class PortalListScreen extends Screen
{
	private PortalList portalList;
	private final List<PortalInfo> portals;
	private CycleButtonEx<PortalListSettings.Filter.Type> filterTypeButton;
	private CycleButtonEx<PortalListSettings.Filter.Condition> filterConditionButton;
	private EditBox filterValueBox;
	private PlainTextButton dimensionLabel;
	private PlainTextButton locationLabel;
	private PlainTextButton addressLabel;
	private PlainTextButton powerLabel;
	private final PortalListSettings.Sorting sorting;
	private final PortalListSettings.Filter filter;
	private final HeaderAndFooterLayout layout;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
	private static final int ADDRESS_ITEM_SIZE = Button.DEFAULT_HEIGHT;
	private static final int IMAGE_BUTTON_SIZE = Button.DEFAULT_HEIGHT;
	private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 2 * PADDING;
	private static final int ENTRY_HEIGHT = 26;

	private static final String    I18N_PREFIX         = "gui.portal_list.";
	private static final String    I18N_ENUM_PREFIX    = I18N_PREFIX + "enums.";
	private static final String    I18N_HEADER_SUFFIX  = ".header";
	private static final String    I18N_TOOLTIP_SUFFIX = ".tooltip";
	private static final Component APPLY_BUTTON_LABEL  = Component.translatable(I18N_PREFIX + "apply");
	private static final Component FILTER_LABEL        = Component.translatable(I18N_PREFIX + "filter_by");
	private static final Component LIST_HEADER         = Component.translatable(I18N_PREFIX + "header");
	private static final Component DIMENSION_HEADER    = Component.translatable(I18N_PREFIX + "dimension" + I18N_HEADER_SUFFIX);
	private static final Component LOCATION_HEADER     = Component.translatable(I18N_PREFIX + "location" + I18N_HEADER_SUFFIX);
	private static final Component ADDRESS_HEADER      = Component.translatable(I18N_PREFIX + "address" + I18N_HEADER_SUFFIX);
	private static final Component POWER_HEADER        = Component.translatable(I18N_PREFIX + "power" + I18N_HEADER_SUFFIX);

	private static final int FILTER_TYPE_BUTTON_WIDTH = 80;
	private static final int FILTER_CONDITION_BUTTON_WIDTH = 100;
	private static final int APPLY_BUTTON_WIDTH = 60;
	private final int FILTER_LABEL_WIDTH = font.width(FILTER_LABEL);

	public PortalListScreen(List<PortalInfo> portals)
	{
		this(portals, PortalListSettings.DEFAULT);
	}

	public PortalListScreen(List<PortalInfo> portals, PortalListSettings portalListSettings)
	{
		super(LIST_HEADER);

		this.portals = portals;
		this.sorting = portalListSettings.sorting();
		this.filter = portalListSettings.filter();

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

		filterTypeButton = new CycleButtonEx<>(BUTTON_HEIGHT, FILTER_TYPE_BUTTON_WIDTH, this::stringifyEnumAsTranslatableKey, (button -> {
			filterConditionButton.setValues(PortalListSettings.Filter.CONDITIONS_BY_TYPE.get(filterTypeButton.getSelectedValue()));
		}));
		filterTypeButton.setSortingComparator((o1, o2) -> Integer.compare(o1.ordinal(), o2.ordinal()));	// The enum is already in the same order as the columns of the list, so just use the ordinal value for simplicity.
		filterTypeButton.setValues(PortalListSettings.Filter.CONDITIONS_BY_TYPE.keySet());
		filterTypeButton.setSelectedValue(filter.type());
		filterHorizontalLayout.addChild(filterTypeButton, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterConditionButton = new CycleButtonEx<>(BUTTON_HEIGHT, FILTER_CONDITION_BUTTON_WIDTH, this::stringifyEnumAsTranslatableKey);
		filterConditionButton.setSortingComparator((o1, o2) -> Integer.compare(o1.ordinal(), o2.ordinal()));	// The enum is already in the same order as the columns of the list, so just use the ordinal value for simplicity.
		filterConditionButton.setValues(PortalListSettings.Filter.CONDITIONS_BY_TYPE.get(filter.type()));
		filterConditionButton.setSelectedValue(filter.condition());
		filterHorizontalLayout.addChild(filterConditionButton, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterValueBox = new EditBox(font, 0, 0, 10, BUTTON_HEIGHT, CommonComponents.EMPTY);
		filterValueBox.setMaxLength(256);
		resizeFilterValueBox();
		filterValueBox.setValue(filter.value());
		filterHorizontalLayout.addChild(filterValueBox, layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		filterHorizontalLayout.addChild(Button.builder(APPLY_BUTTON_LABEL, this::applyFilter).width(APPLY_BUTTON_WIDTH).build(), layoutSettings -> layoutSettings.alignVerticallyMiddle().paddingRight(PADDING));

		// Third row, containing text headers for the elements in the portal list. This LinearLayout is only used to get another row. In row, the positioning is done manually (see 'repositionLabels()').
		LinearLayout thirdRow = LinearLayout.horizontal();
		verticalLayout.addChild(thirdRow);

		dimensionLabel = new PlainTextButton(0, 0, font.width(DIMENSION_HEADER.getVisualOrderText()), 9, DIMENSION_HEADER, _ -> onColumnLabelPressed(PortalListSettings.Sorting.Type.Dimension), font);
		thirdRow.addChild(dimensionLabel);
		locationLabel = new PlainTextButton(0, 0, font.width(LOCATION_HEADER.getVisualOrderText()), 9, LOCATION_HEADER, _ -> onColumnLabelPressed(PortalListSettings.Sorting.Type.Location), font);
		thirdRow.addChild(locationLabel);
		addressLabel = new PlainTextButton(0, 0, font.width(ADDRESS_HEADER.getVisualOrderText()), 9, ADDRESS_HEADER, _ -> onColumnLabelPressed(PortalListSettings.Sorting.Type.Address), font);
		thirdRow.addChild(addressLabel);
		powerLabel = new PlainTextButton(0, 0, font.width(POWER_HEADER.getVisualOrderText()), 9, POWER_HEADER, _ -> onColumnLabelPressed(PortalListSettings.Sorting.Type.Power), font);
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

	private void onColumnLabelPressed(PortalListSettings.Sorting.Type type)
	{
		minecraft.gui.setScreen(new PortalListScreen(portals, new PortalListSettings((sorting.type() == type) ? sorting.invert() : new PortalListSettings.Sorting(type), filter)));
	}

	private void applyFilter(Button button)
	{
		PortalListSettings.Filter filter = new PortalListSettings.Filter(filterTypeButton.getSelectedValue(), filterConditionButton.getSelectedValue(), filterValueBox.getValue());
		minecraft.gui.setScreen(new PortalListScreen(portals, new PortalListSettings(sorting, filter)));
	}

	private <T extends Enum<?>> String stringifyEnumAsTranslatableKey(T component)
	{
		return I18N_ENUM_PREFIX + component.getClass().getSimpleName().toLowerCase() + "." + component.name().toLowerCase();
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
		xOffset += editBoxWidths[1] + addressItemsWidth + PADDING;
		addressLabel.setX(Utils.centerIn(xOffset, editBoxWidths[2], addressLabel.getWidth()));
		xOffset += editBoxWidths[2] + PADDING;
		powerLabel.setX(Utils.centerIn(xOffset, editBoxWidths[3], powerLabel.getWidth()));
	}

	@Environment(EnvType.CLIENT)
	public class PortalList extends ContainerObjectSelectionList<PortalList.Entry>
	{
		private static final int LEFT_RIGHT_BORDER = 20;
		private final List<PortalInfo> portals;

		public PortalList(List<PortalInfo> portals, PortalListSettings.Filter filter, Minecraft mc, int width, int height, int top, int itemHeight)
		{
			super(mc, width, height, top, itemHeight);

			this.portals = portals;
			updateEntries(filter);
		}

		@Override
		protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
		{
			super.extractTooltipForNextRenderPass(graphics, mouseX, mouseY);

			String tooltip;

			for (PortalList.Entry entry : this.children())
			{
				tooltip = entry.getTooltip();

				if (tooltip != null && !tooltip.isEmpty())
				{
					List<Component> comment = Arrays.stream(tooltip.split("\n")).map(Component::translatable).collect(Collectors.toList());
					graphics.setComponentTooltipForNextFrame(font, comment, mouseX, mouseY);

					break;
				}
			}
		}

		public void updateEntries(PortalListSettings.Filter filter)
		{
			clearEntries();

			switch (sorting.type())
			{
				// Descending order is achieved by swapping the input parameters to the comparator.
				case Dimension -> this.portals.sort(Utils.invertComparator(sorting::isDescending, (o1, o2) -> o1.dimension().identifier().compareTo(o2.dimension().identifier())));
				case Location -> this.portals.sort(Comparator.<PortalInfo, Identifier>comparing(portalInfo -> portalInfo.dimension().identifier()).thenComparing(Utils.invertComparator(sorting::isDescending, (o1, o2) -> o1.location().compareTo(o2.location()))));
				case Address -> this.portals.sort(Utils.invertComparator(sorting::isDescending, (o1, o2) -> o1.address().compareTo(o2.address())));
				case Power -> this.portals.sort(Utils.invertComparator(sorting::isDescending, (o1, o2) -> Integer.compare(o1.power(), o2.power())));
			}

			switch (filter.type())
			{
				case Dimension:
					switch (filter.condition())
					{
						case Equals   -> portals.stream().filter(portal -> portal.dimension().toString().equals(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.dimension().toString().contains(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
					}

					break;

				case Location:
					switch (filter.condition())
					{
						case Equals   -> portals.stream().filter(portal -> Utils.getReadableBlockPos(portal.location()).equals(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> Utils.getReadableBlockPos(portal.location()).contains(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Address:
					switch (filter.condition())
					{
						case Equals   -> portals.stream().filter(portal -> portal.address().toString().equals(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
						case Contains -> portals.stream().filter(portal -> portal.address().toString().contains(filter.value())).forEach(portal -> addEntry(new Entry(portal)));
					}
					break;

				case Power:
					try
					{
						int power = Integer.parseInt(filter.value());
						switch (filter.condition())
						{
							case Equals      -> portals.stream().filter(portal -> portal.power() == power).forEach(portal -> addEntry(new Entry(portal)));
							case LessThan    -> portals.stream().filter(portal -> portal.power()  < power).forEach(portal -> addEntry(new Entry(portal)));
							case GreaterThan -> portals.stream().filter(portal -> portal.power()  > power).forEach(portal -> addEntry(new Entry(portal)));
						}
					}
					catch (NumberFormatException _) {}

					break;

				default:
					portals.forEach(portalInfo -> addEntry(new Entry(portalInfo)));
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

		private static int[] calculateEditBoxWidths(int rowWidth)
		{
			int[] editBoxWidths = new int[4];

			int addressItemsWidth = 4 * ADDRESS_ITEM_SIZE;
			int totalBoxesWidth = rowWidth - addressItemsWidth - ((3 * IMAGE_BUTTON_SIZE) + (3 * PADDING));
			editBoxWidths[0] = (int)Math.floor(totalBoxesWidth * 0.25f);	// dimension
			editBoxWidths[1] = (int)Math.floor(totalBoxesWidth * 0.25f);	// location
			editBoxWidths[2] = (int)Math.floor(totalBoxesWidth * 0.45f);	// address
			editBoxWidths[3] = (int)Math.floor(totalBoxesWidth * 0.05f);	// power

			return editBoxWidths;
		}

		@Environment(EnvType.CLIENT)
		public class Entry extends ContainerObjectSelectionList.Entry<PortalList.Entry>
		{
			private static final String I18N_GOTO_LOCATION_TOOLTIP = I18N_PREFIX + "goto_portal" + I18N_TOOLTIP_SUFFIX;
			private static final String I18N_SET_POWER_TOOLTIP     = I18N_PREFIX + "set_power" + I18N_TOOLTIP_SUFFIX;
			private static final String I18N_DEACTIVATE_TOOLTIP    = I18N_PREFIX + "deactivate" + I18N_TOOLTIP_SUFFIX;

			private final EditBox dimensionBox;
			private final EditBox locationBox;
			private final EditBox addressBox;
			private final EditBox powerBox;
			private final List<ItemDisplayWidget> addressBlockDisplays;
			private final ImageButton setPowerButton;
			private final ImageButton gotoLocationButton;
			private final ImageButton deactivateButton;
			private String tooltipText;

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

				addressBlockDisplays = new ArrayList<>(4);

				for (var addressComponent : portal.address().getBlockCounts().entrySet())
				{
					Identifier blockId = Identifier.parse(addressComponent.getKey());
					Block addressBlock = BuiltInRegistries.BLOCK.getValue(blockId);
					ItemStack addressBlockItem = new ItemStack(addressBlock, addressComponent.getValue());

					addressBlockDisplays.add(new ItemDisplayWidget(minecraft, 0, 0, ADDRESS_ITEM_SIZE, ADDRESS_ITEM_SIZE, Component.empty(), addressBlockItem, true, true));
				}

				for (int i = 0; i < 4 - portal.address().getBlockCounts().size(); i++)
				{
					addressBlockDisplays.add(new ItemDisplayWidget(minecraft, 0, 0, ADDRESS_ITEM_SIZE, ADDRESS_ITEM_SIZE, Component.empty(), ItemStack.EMPTY, false, false));
				}

				addressBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				addressBox.setMaxLength(256);
				addressBox.setValue(portal.address().toString());
				addressBox.setEditable(false);
				addressBox.moveCursorToStart(false);

				powerBox = new EditBox(minecraft.font, 0, 0, 100, defaultEntryHeight - PADDING, CommonComponents.EMPTY);
				powerBox.setMaxLength(10);
				powerBox.setValue(Integer.toString(portal.power()));
				powerBox.moveCursorToStart(false);

				setPowerButton = new ImageButton(0, 0, IMAGE_BUTTON_SIZE, IMAGE_BUTTON_SIZE, new WidgetSprites(SimplePortals.modId("set_power_button"), SimplePortals.modId("set_power_button_highlighted")), button -> {
					try
					{
						int power = Integer.parseInt(powerBox.getValue());
						ClientPlayNetworking.send(new SetPortalPowerPayload(portal.dimension().identifier(), portal.location(), power, new PortalListSettings(sorting, filter)));
					}
					catch (NumberFormatException _) {}
				});

				gotoLocationButton = new ImageButton(0, 0, IMAGE_BUTTON_SIZE, IMAGE_BUTTON_SIZE, new WidgetSprites(SimplePortals.modId("teleport"), SimplePortals.modId("teleport_highlighted")), button -> {
					minecraft.gui.setScreen(null);
					ClientPlayNetworking.send(new TpdCommandPayload(portal.dimension().identifier(), portal.location()));
				});

				deactivateButton = new ImageButton(0, 0, IMAGE_BUTTON_SIZE, IMAGE_BUTTON_SIZE, new WidgetSprites(SimplePortals.modId("deactivate_button"), SimplePortals.modId("deactivate_button_highlighted")), button -> {
					ClientPlayNetworking.send(new DeactivatePortalPayload(portal.dimension().identifier(), portal.location(), new PortalListSettings(sorting, filter)));
				});

				tooltipText = null;
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

				dimensionBox.setPosition(xOffset, getContentY());
				dimensionBox.setWidth(dimensionBoxWidth);
				dimensionBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += dimensionBoxWidth + PADDING;

				locationBox.setPosition(xOffset, getContentY());
				locationBox.setWidth(locationBoxWidth);
				locationBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += locationBoxWidth + PADDING;

				for (var display : addressBlockDisplays)
				{
					display.setPosition(xOffset, getContentY());
					display.extractRenderState(graphics, mouseX, mouseY, a);

					xOffset += display.getWidth();
				}

				xOffset += PADDING;

				addressBox.setPosition(xOffset, getContentY());
				addressBox.setWidth(addressBoxWidth);
				addressBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += addressBoxWidth + PADDING;

				powerBox.setPosition(xOffset, getContentY());
				powerBox.setWidth(powerBoxWidth);
				powerBox.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += powerBoxWidth + PADDING;

				setPowerButton.setPosition(xOffset, getContentY());
				setPowerButton.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += IMAGE_BUTTON_SIZE + PADDING;

				gotoLocationButton.setPosition(xOffset, getContentY());
				gotoLocationButton.extractRenderState(graphics, mouseX, mouseY, a);

				xOffset += IMAGE_BUTTON_SIZE + PADDING;

				deactivateButton.setPosition(xOffset, getContentY());
				deactivateButton.extractRenderState(graphics, mouseX, mouseY, a);

				Language I18N = Language.getInstance();

				if (mouseIsInsideWidgetsBounds(gotoLocationButton, mouseX, mouseY))
				{
					tooltipText = I18N.getOrDefault(I18N_GOTO_LOCATION_TOOLTIP);
				}
				else if (mouseIsInsideWidgetsBounds(setPowerButton, mouseX, mouseY))
				{
					tooltipText = I18N.getOrDefault(I18N_SET_POWER_TOOLTIP);

				}
				else if (mouseIsInsideWidgetsBounds(deactivateButton, mouseX, mouseY))
				{
					tooltipText = I18N.getOrDefault(I18N_DEACTIVATE_TOOLTIP);
				}
				else
				{
					tooltipText = null;
				}
			}

			public String getTooltip()
			{
				return tooltipText;
			}

			private static boolean mouseIsInsideWidgetsBounds(AbstractWidget widget, int mouseX, int mouseY)
			{
				return ((mouseX >= widget.getX()) &&
						(mouseX <  widget.getRight()) &&
						(mouseY >= widget.getY()) &&
						(mouseY <  widget.getBottom()));
			}

			@Override
			public List<? extends NarratableEntry> narratables()
			{
				return ImmutableList.of();
			}

			@Override
			public List<? extends GuiEventListener> children()
			{
				return ImmutableList.of(dimensionBox, locationBox, addressBox, addressBlockDisplays.get(0), addressBlockDisplays.get(1), addressBlockDisplays.get(2), addressBlockDisplays.get(3), powerBox, setPowerButton, gotoLocationButton, deactivateButton);
			}
		}
	}

}
