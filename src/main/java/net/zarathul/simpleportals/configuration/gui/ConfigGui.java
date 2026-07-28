package net.zarathul.simpleportals.configuration.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.ConfigSetting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ConfigGui extends Screen
{
	private SettingsList settingsList;
	private List<ConfigSetting> settings;
	private final String configName;
	private final Player player;
	private final Consumer<Player> syncChanges;
	private final HeaderAndFooterLayout layout;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
	private static final int ENTRY_HEIGHT = 26;
	private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 2 * PADDING;

	public ConfigGui(List<ConfigSetting> settings, Component title, String configName, Player player, Consumer<Player> syncChanges)
	{
		super(title);

		this.settings = settings;
		this.configName = configName;
		this.player = player;
		this.syncChanges = syncChanges;


		int HEADER_HEIGHT = 2 * font.lineHeight + 2 * PADDING;
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
		layout.addTitleHeader(title, font);
	}

	private void addContents()
	{
		settingsList = new SettingsList(settings, player, minecraft, width, 0, 0, ENTRY_HEIGHT);
		layout.addToContents(settingsList);
	}

	private void addFooter()
	{
		LinearLayout horizontalLayout = layout.addToFooter(LinearLayout.horizontal());
		horizontalLayout.spacing(PADDING);

		horizontalLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).width(200).build());	// Cancel button
		horizontalLayout.addChild(Button.builder(Component.translatable("config.save"), button -> saveConfigAndCloseScreen()).width(200).build());	// Done button
	}

	private void saveConfigAndCloseScreen()
	{
		settingsList.commitChanges();
		Config.save(configName);
		if (syncChanges != null) syncChanges.accept(player);

		minecraft.gui.setScreen(new ConfigGui(settings, title, configName, player, syncChanges));
	}

	@Override
	protected void repositionElements()
	{
		layout.arrangeElements();
		settingsList.updateSize(width, layout);
		layout.arrangeElements();
	}

	@Environment(EnvType.CLIENT)
	public class SettingsList extends ContainerObjectSelectionList<SettingsList.Entry>
	{
		private static final int LEFT_RIGHT_BORDER = 30;
		private static final String I18N_PREFIX = "config.";
		private static final String I18N_TOOLTIP_SUFFIX = ".tooltip";
		private static final String I18N_INVALID = "config.input_invalid";
		private static final String I18N_NEEDS_WORLD_RESTART = "config.needs_world_restart";
		private static final String I18N_RESET = "config.reset_to_default";

		public SettingsList(List<ConfigSetting> settings, Player player, Minecraft mc, int width, int height, int top, int itemHeight)
		{
			super(mc, width, height, top, itemHeight);
			generateEntries(settings, player);
		}

		@Override
		protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
		{
			super.extractTooltipForNextRenderPass(graphics, mouseX, mouseY);

			String tooltip;

			for (Entry entry : this.children())
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

		@Override
		public int getRowWidth()
		{
			return width - LEFT_RIGHT_BORDER * 2;
		}

		@Override
		protected int scrollBarX()
		{
			return width - LEFT_RIGHT_BORDER;
		}

		public void commitChanges()
		{
			for (Entry entry : this.children())
			{
				entry.commitChanges();
			}
		}

		private void generateEntries(List<ConfigSetting> settings, Player player)
		{
			String lastCategory = null;
			settings.sort(getSettingComparator());

			for (var setting : settings)
			{
				String category = setting.category;

				if (!category.isEmpty() && !category.equals(lastCategory))
				{
					String localizedCategory = getLocalizedCategory(setting);
					addEntry(new CategoryEntry(localizedCategory));

					lastCategory = category;
				}

				addEntry(new SettingEntry(setting, player));
			}
		}

		// Sorts by the localized category, then booleans first, enums second everything else after, and finally by localized description.
		// This way every setting is under its category and in every category the checkboxes are at the top, followed by cyclebuttons and
		// editboxes at the end.
		private static Comparator<ConfigSetting> getSettingComparator()
		{
			return Comparator.<ConfigSetting, String>comparing(setting -> getLocalizedCategory((ConfigSetting)setting))
				.thenComparing((o1, o2) -> {
					if (o1.valueType == o2.valueType) return 0;
					if (o1.isBoolean()) return -1;
					if (o2.isBoolean()) return 1;
					if (o1.isComplex() && o1.value instanceof Enum<?>) return -1;
					return 1;
				})
				.thenComparing(SettingsList::getLocalizedDescription);
		}

		private static String getLocalizedCategory(ConfigSetting setting)
		{
			var I18N = Language.getInstance();
			String i18nKey = Config.CATEGORY_I18N_PREFIX + setting.category;

			return I18N.getOrDefault(i18nKey); // If the key is not found, the key itself is returned instead of the translated text.
		}

		private static String getLocalizedDescription(ConfigSetting setting)
		{
			var I18N = Language.getInstance();
			String i18nKey = I18N.getOrDefault(I18N_PREFIX + setting.descriptionKey, setting.id.getPath());

			return I18N.getOrDefault(i18nKey); // If the key is not found, the key itself is returned instead of the translated text.
		}

		@Environment(EnvType.CLIENT)
		public abstract class Entry extends ContainerObjectSelectionList.Entry<Entry>
		{
			public abstract void commitChanges();
			public abstract String getTooltip();
		}

		@Environment(EnvType.CLIENT)
		public class CategoryEntry extends Entry
		{
			private final StringWidget categoryHeader;

			public CategoryEntry(String text)
			{
				categoryHeader = new StringWidget(Component.literal(text), font);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a)
			{
				int xPos = Utils.centerIn(0, width, categoryHeader.getWidth());
				int yPos = Utils.centerIn(getContentY(), ENTRY_HEIGHT, categoryHeader.getHeight());
				categoryHeader.setPosition(xPos, yPos);
				categoryHeader.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public boolean shouldTakeFocusAfterInteraction()
			{
				return false;
			}

			@Override
			public void commitChanges()
			{
			}

			@Override
			public String getTooltip()
			{
				return null;
			}

			@Override
			public List<? extends NarratableEntry> narratables()
			{
				return List.of();
			}

			@Override
			public List<? extends GuiEventListener> children()
			{
				return List.of(categoryHeader);
			}
		}

		@Environment(EnvType.CLIENT)
		public class SettingEntry extends Entry
		{
			private final StringWidget settingLabel;
			private final EditBox editBox;
			private final CheckboxButtonEx checkBox;
			private final CycleButtonEx<Object> enumButton;
			private String tooltipText;
			private boolean isValid;
			private final ImageButton needsWorldRestartButton;
			private final ImageButton resetButton;
			private final ImageButton validatedButton;
			private final ConfigSetting setting;

			public SettingEntry(ConfigSetting setting, Player player)
			{
				this.setting = setting;

				// :BROKEN_PERMISSIONS: For some reason a player without op in multiplayer has no permission level at all instead of 0.
				var neededPermission = new Permission.HasCommandLevel(PermissionLevel.byId(setting.permissionLvl));
				boolean widgetIsActive = (setting.permissionLvl == 0) || player.permissions().hasPermission(neededPermission);

				String label = getLocalizedDescription(setting);
				settingLabel = new StringWidget(Component.literal(label), font);

				checkBox = new CheckboxButtonEx(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, false);
				enumButton = new CycleButtonEx<>(BUTTON_HEIGHT, value -> ((Enum)value).name());
				editBox = new EditBox(minecraft.font, 0, 0, 100, BUTTON_HEIGHT, CommonComponents.EMPTY);
				editBox.setMaxLength(256);
				editBox.moveCursorToStart(false);
				editBox.setResponder(this::validateTextFieldInput);

				// CheckboxButton and CycleButtonEx don't need validation because they can never produce values in an invalid state.
				boolean widgetNeedsValidation;

				if (setting.isBoolean())
				{
					checkBox.active = widgetIsActive;
					checkBox.value = (boolean)setting.value;
					widgetNeedsValidation = false;
					isValid = true;
				}
				else if (setting.isComplex() && setting.value instanceof Enum)
				{
					enumButton.active = widgetIsActive;
					enumButton.setValues(Arrays.stream(setting.value.getClass().getEnumConstants()).collect(Collectors.toList()));
					enumButton.setSelectedValue(setting.value);
					widgetNeedsValidation = false;
					isValid = true;
				}
				else
				{
					editBox.setEditable(widgetIsActive);
					editBox.active = widgetIsActive;
					editBox.setValue(setting.value.toString());
					widgetNeedsValidation = true;
				}

				resetButton = new ImageButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, new WidgetSprites(Utils.createModIdentifier("reset_button"), Utils.createModIdentifier("reset_button_highlighted")), button -> resetValue());
				resetButton.active = widgetIsActive;

				needsWorldRestartButton = new ImageButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, new WidgetSprites(Identifier.withDefaultNamespace("icon/link"), Identifier.withDefaultNamespace("icon/link_highlighted")), (b) -> {});
				needsWorldRestartButton.active = false;
				needsWorldRestartButton.visible = setting.needsWorldRestart;

				validatedButton = new ImageButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, new WidgetSprites(Identifier.withDefaultNamespace("world_list/error_highlighted"), Identifier.withDefaultNamespace("world_list/error")), (b) -> {});
				validatedButton.active = false;
				validatedButton.visible = widgetNeedsValidation;

				tooltipText = null;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a)
			{
				int centerX = width / 2;
				settingLabel.setPosition(centerX - settingLabel.getWidth() - PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, settingLabel.getHeight()));
				settingLabel.extractRenderState(graphics, mouseX, mouseY, a);

				int xOffsetFromRight = getContentRight() - needsWorldRestartButton.getWidth() - PADDING;
				needsWorldRestartButton.setPosition(xOffsetFromRight, Utils.centerIn(getContentY(), ENTRY_HEIGHT, needsWorldRestartButton.getHeight()));
				needsWorldRestartButton.extractRenderState(graphics, mouseX, mouseY, a);

				xOffsetFromRight -= (resetButton.getWidth() + PADDING);
				resetButton.setPosition(xOffsetFromRight, Utils.centerIn(getContentY(), ENTRY_HEIGHT, needsWorldRestartButton.getHeight()));
				resetButton.extractRenderState(graphics, mouseX, mouseY, a);

				xOffsetFromRight -= (validatedButton.getWidth() + PADDING);
				validatedButton.setPosition(xOffsetFromRight, Utils.centerIn(getContentY(), ENTRY_HEIGHT, validatedButton.getHeight()));
				validatedButton.visible = !isValid;
				validatedButton.extractRenderState(graphics, mouseX, mouseY, a);

				if (setting.isBoolean())
				{
					checkBox.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, checkBox.getHeight()));
					checkBox.extractRenderState(graphics, mouseX, mouseY, a);
				}
				else if (setting.isComplex() && setting.value instanceof Enum)
				{
					enumButton.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, enumButton.getHeight()));
					enumButton.setWidth(validatedButton.getX() - enumButton.getX() - PADDING);
					enumButton.extractRenderState(graphics, mouseX, mouseY, a);
				}
				else
				{
					editBox.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, editBox.getHeight()));
					editBox.setWidth(validatedButton.getX() - editBox.getX() - PADDING);
					editBox.extractRenderState(graphics, mouseX, mouseY, a);
				}

				Language I18N = Language.getInstance();

				// Set tooltip to be rendered. This could be moved to mouseMoved(), but either the tooltip for the description text
				// would have to stay here or its bounds would have to be stored. To not complicate things, keep everything here for now.
				if ((mouseX >= settingLabel.getX()) &&
					(mouseX < (settingLabel.getX() + settingLabel.getWidth())) &&
					(mouseY >= settingLabel.getY()) &&
					(mouseY < (settingLabel.getY() + settingLabel.getHeight())))
				{
					// Tooltip for the description.
					// If the key is not found, the key itself is returned instead of the translated text.
					String i18nTooltipKey = I18N_PREFIX + setting.descriptionKey + I18N_TOOLTIP_SUFFIX;
					String i18nTooltipText = I18N.getOrDefault(i18nTooltipKey);
					tooltipText = (!i18nTooltipText.equals(i18nTooltipKey)) ? i18nTooltipText : setting.description;
				}
				else if ((mouseX >= resetButton.getX()) &&
						 (mouseX < (resetButton.getX() + resetButton.getWidth())) &&
						 (mouseY >= resetButton.getY()) &&
						 (mouseY < (resetButton.getY() + resetButton.getHeight())))
				{
					// Tooltip for the validation button.
					tooltipText = I18N.getOrDefault(I18N_RESET);
				}
				else if (validatedButton.visible &&
						(mouseX >= validatedButton.getX()) &&
						(mouseX < (validatedButton.getX() + validatedButton.getWidth())) &&
						(mouseY >= validatedButton.getY()) &&
						(mouseY < (validatedButton.getY() + validatedButton.getHeight())))
				{
					// Tooltip for the validation button.
					tooltipText = I18N.getOrDefault(I18N_INVALID);
				}
				else if (setting.needsWorldRestart &&
						(mouseX >= needsWorldRestartButton.getX()) &&
						(mouseX < (needsWorldRestartButton.getX() + needsWorldRestartButton.getWidth())) &&
						(mouseY >= needsWorldRestartButton.getY()) &&
						(mouseY < (needsWorldRestartButton.getY() + needsWorldRestartButton.getHeight())))
				{
					// Tooltip for the needs world restart button.
					tooltipText = I18N.getOrDefault(I18N_NEEDS_WORLD_RESTART);
				}
				else
				{
					tooltipText = null;
				}
			}

			@Override
			public void commitChanges()
			{
				if (setting.isBoolean())
				{
					setting.value = checkBox.value;
				}
				else if (setting.isComplex() && setting.value instanceof Enum)
				{
					setting.value = enumButton.getSelectedValue();
				}
				else
				{
					String inputText = editBox.getValue();

					try
					{
						switch (setting.valueType)
						{
							case Int ->
							{
								int parsedValue = Integer.parseInt(inputText);
								if (setting.isValid(parsedValue)) setting.value = parsedValue;
							}
							case Float ->
							{
								float parsedValue = Float.parseFloat(inputText);
								if (setting.isValid(parsedValue)) setting.value = parsedValue;
							}
							case String ->
							{
								if (setting.isValid(inputText)) setting.value = inputText;
							}

							case Complex ->
							{
								Object parsedValue = setting.destringify(inputText);
								if (setting.isValid(parsedValue)) setting.value = parsedValue;
							}
						}
					}
					catch (NumberFormatException ignored) {}
				}
			}

			@Override
			public String getTooltip()
			{
				return this.tooltipText;
			}

			private void resetValue()
			{
				if (setting.isBoolean())
				{
					checkBox.value = (boolean)setting.defaultValue;
				}
				else if (setting.isComplex() && setting.value instanceof Enum)
				{
					enumButton.setSelectedValue(setting.defaultValue);
				}
				else
				{
					editBox.setValue(setting.canStringify() ? setting.stringifyDefaultValue() : setting.defaultValue.toString());
				}
			}

			private void validateTextFieldInput(String text)
			{
				isValid = setting.validator.isEmpty();

				if (!isValid)
				{
					var validator = setting.validator.get();

					try
					{
						isValid = switch (setting.valueType)
						{
							case Int -> validator.apply(Integer.parseInt(text));
							case Float -> validator.apply(Float.parseFloat(text));
							case String -> validator.apply(text);
							case Complex -> validator.apply(setting.destringify(text));
							default -> true;
						};
					}
					catch (NumberFormatException _)
					{
						isValid = false;
					}
				}
			}

			@Override
			public List<? extends NarratableEntry> narratables()
			{
				return List.of();
			}

			@Override
			public List<? extends GuiEventListener> children()
			{
				return List.of(settingLabel, editBox, checkBox, enumButton, resetButton, needsWorldRestartButton, validatedButton);
			}
		}
	}
}