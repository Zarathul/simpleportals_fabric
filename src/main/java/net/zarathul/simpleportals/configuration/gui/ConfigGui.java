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
import net.zarathul.simpleportals.configuration.StorageMethods;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ConfigGui extends Screen
{
	private ModOptionList optionList;
	private final Class<?> settingsType;
	private final String configName;
	private final Player player;
	private final Consumer<Player> syncChanges;
	private final HeaderAndFooterLayout layout;

	private static final int PADDING = 5;
	private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
	private static final int ENTRY_HEIGHT = 26;
	private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 2 * PADDING;

	public ConfigGui(Component title, Class<?> settingsType, String configName, Player player, Consumer<Player> syncChanges)
	{
		super(title);

		this.settingsType = settingsType;
		this.configName = configName;
		this.player = player;
		this.syncChanges = syncChanges;


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
		layout.addTitleHeader(title, font);
	}

	private void addContents()
	{
		optionList = new ModOptionList(settingsType, player, minecraft, width, 0, 0, ENTRY_HEIGHT);
		layout.addToContents(optionList);
	}

	private void addFooter()
	{
		LinearLayout horizontalLayout = layout.addToFooter(LinearLayout.horizontal());
		horizontalLayout.spacing(PADDING);

		horizontalLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(200).build());	// Cancel button
		horizontalLayout.addChild(Button.builder(Component.translatable("config.save"), button -> saveConfigAndCloseScreen()).width(200).build());	// Done button
	}

	private void saveConfigAndCloseScreen()
	{
		optionList.commitChanges();
		Config.save(configName, settingsType);
		minecraft.gui.setScreen(null);
		if (syncChanges != null) syncChanges.accept(player);
	}

	@Override
	protected void repositionElements()
	{
		layout.arrangeElements();
		optionList.updateSize(width, layout);
		layout.arrangeElements();
	}
//	@Override
//	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
//	{
//		super.extractRenderState(graphics, mouseX, mouseY, a);
//
//		graphics.text(minecraft.font, title.getVisualOrderText(), PADDING, PADDING, ARGB.white(1));
//	}

	@Environment(EnvType.CLIENT)
	public class ModOptionList extends ContainerObjectSelectionList<ModOptionList.Entry>
	{
		private static final int LEFT_RIGHT_BORDER = 30;
		private static final String I18N_PREFIX = "config.";
		private static final String I18N_TOOLTIP_SUFFIX = ".tooltip";
		private static final String I18N_VALID = "config.input_valid";
		private static final String I18N_INVALID = "config.input_invalid";
		private static final String I18N_NEEDS_WORLD_RESTART = "config.needs_world_restart";

		public ModOptionList(Class<?> settingsType, Player player, Minecraft mc, int width, int height, int top, int itemHeight)
		{
			super(mc, width, height, top, itemHeight);
			generateEntries(settingsType, player);
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
					graphics.setComponentTooltipForNextFrame(minecraft.font, comment, mouseX, mouseY);

					break;
				}
			}
		}
		//		@Override
//		public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
//		{
//			super.render(poseStack, mouseX, mouseY, partialTicks);
//
//			String tooltip;
//
//			for (Entry entry : this.children())
//			{
//				tooltip = entry.getTooltip();
//
//				if (tooltip != null && !tooltip.isEmpty())
//				{
//					List<Component> comment = Arrays.stream(tooltip.split("\n")).map(Component::translatable).collect(Collectors.toList());
//					renderComponentTooltip(poseStack, comment, mouseX, mouseY);
//
//					break;
//				}
//			}
//		}

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

		private void generateEntries(Class<?> clazz, Player player)
		{
			Field[] fields = Config.getSettingFieldsSortedByCategory(clazz);

			ConfigSetting annotation;
			String category;
			String lastCategory = null;
			var I18N = Language.getInstance();

			for (Field valueField : fields)
			{
				if (!Config.isValidSetting(valueField, true)) continue;

				annotation = valueField.getAnnotation(ConfigSetting.class);
				category = ((annotation != null) && !annotation.category().isEmpty()) ? annotation.category() : Config.DEFAULT_CATEGORY;

				if (!category.equals(lastCategory))
				{
					String i18nKey = "config." + category;
					// If the key is not found, the key itself is returned instead of the translated text.
					String i18nText = I18N.getOrDefault(i18nKey);
					String categoryLabel = (!i18nText.equals(i18nKey)) ? i18nText : category;

					addEntry(new CategoryEntry(categoryLabel));

					lastCategory = category;
				}

				addEntry(new OptionEntry(valueField, annotation, player));
			}
		}

		@Environment(EnvType.CLIENT)
		public abstract class Entry extends ContainerObjectSelectionList.Entry<ModOptionList.Entry>
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
		public class OptionEntry extends Entry
		{
			private StringWidget optionLabel;
			private EditBox editBox;
			private CheckboxButtonEx checkBox;
			private EnumOptionButton enumButton;
			private final ImageButton needsWorldRestartButton;
			private final ValidationStatusButton validatedButton;
			private String tooltipText;
			private final Field valueField;
			private final Method validatorMethod;
			private final Method loadMethod;
			private final ConfigSetting annotation;
			private Object value;

			public OptionEntry(Field valueField, ConfigSetting annotation, Player player)
			{
				this.valueField = valueField;
				this.validatorMethod = Config.getValidator(valueField);
				this.annotation = valueField.getAnnotation(ConfigSetting.class);
				Optional<StorageMethods> loadSave = Config.getLoadSave(valueField);
				loadMethod = (loadSave.isPresent()) ? loadSave.get().load : null;

				Object defaultValue = Config.getDefaultValue(valueField);

				var neededPermission = new Permission.HasCommandLevel(PermissionLevel.byId(annotation.permissionLvl()));
				boolean widgetIsActive = player.permissions().hasPermission(neededPermission);

				Language I18N = Language.getInstance();
				String label = I18N.getOrDefault(I18N_PREFIX + annotation.descriptionKey(), valueField.getName());
				optionLabel = new StringWidget(Component.translatable(label), font);

				// Has to be instantiated before the rest, because 'validateTextFieldInput()', which is called by 'editBox' on validation, sets 'validatedButton' state.
				validatedButton = new ValidationStatusButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, button -> {
					if (this.editBox != null)
					{
						this.editBox.setValue(defaultValue.toString());
						this.editBox.setFocused(false);
					}
					else if (this.checkBox != null)
					{
						this.checkBox.value = (boolean)defaultValue;
					}
					else if (this.enumButton != null)
					{
						this.enumButton.setValue((Enum)defaultValue);
					}
				});
				this.validatedButton.active = widgetIsActive;

//				Object value = null;

				try
				{
					value = valueField.get(null);
				}
				catch (IllegalAccessException ignored)
				{
					value = null;
				}

				checkBox = new CheckboxButtonEx(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, false);
				enumButton = new EnumOptionButton(0, 0, 100, BUTTON_HEIGHT);
				editBox = new EditBox(minecraft.font, 0, 0, 100, BUTTON_HEIGHT, CommonComponents.EMPTY);
				editBox.setMaxLength(256);
				editBox.moveCursorToStart(false);
				editBox.setResponder(this::validateTextFieldInput);

				if (value instanceof Boolean)
				{
					checkBox.active = widgetIsActive;
					checkBox.value = (boolean)value;
				}
				else if (value instanceof Enum)
				{
					enumButton.init(value.getClass(), value.toString());
					enumButton.active = widgetIsActive;
				}
				else
				{
					editBox.setEditable(widgetIsActive);
					editBox.active = widgetIsActive;
					editBox.setValue(value.toString());
				}

				this.needsWorldRestartButton = new ImageButton(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, new WidgetSprites(Identifier.withDefaultNamespace("icon/link")), (b) -> {});
				this.needsWorldRestartButton.active = false;
				this.needsWorldRestartButton.visible = annotation.needsWorldRestart();

				this.tooltipText = null;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a)
			{
				int centerX = width / 2;
				optionLabel.setPosition(centerX - optionLabel.getWidth() - PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, optionLabel.getHeight()));
				optionLabel.extractRenderState(graphics, mouseX, mouseY, a);

				needsWorldRestartButton.setPosition(getContentRight() - needsWorldRestartButton.getWidth() - PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, needsWorldRestartButton.getHeight()));
				needsWorldRestartButton.extractRenderState(graphics, mouseX, mouseY, a);

				validatedButton.setPosition(getContentRight() - validatedButton.getWidth() - needsWorldRestartButton.getWidth() - 2 * PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, needsWorldRestartButton.getHeight()));
				validatedButton.extractRenderState(graphics, mouseX, mouseY, a);

				if (checkBox != null && (value instanceof Boolean))
				{
					checkBox.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, checkBox.getHeight()));
					checkBox.extractRenderState(graphics, mouseX, mouseY, a);
				}
				else if (enumButton != null && (value instanceof Enum))
				{
					enumButton.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, enumButton.getHeight()));
					enumButton.setWidth(validatedButton.getX() - enumButton.getX() - PADDING);
					enumButton.extractRenderState(graphics, mouseX, mouseY, a);
				}
				else if (editBox != null)
				{
					editBox.setPosition(centerX + PADDING, Utils.centerIn(getContentY(), ENTRY_HEIGHT, editBox.getHeight()));
					editBox.setWidth(validatedButton.getX() - editBox.getX() - PADDING);
					editBox.extractRenderState(graphics, mouseX, mouseY, a);
				}

				Language I18N = Language.getInstance();

				// Set tooltip to be rendered by the ModOptionList. This could be moved to mouseMoved(), but either
				// the tooltip for the description text would have to stay here or its bounds would have to be stored.
				// To not complicate things, keep everything here for now.
				if ((mouseX >= optionLabel.getX()) &&
					(mouseX < (optionLabel.getX() + optionLabel.getWidth())) &&
					(mouseY >= optionLabel.getY()) &&
					(mouseY < (optionLabel.getY() + optionLabel.getHeight())))
				{
					// Tooltip for the description.
					// If the key is not found, the key itself is returned instead of the translated text.
					String i18nTooltipKey = I18N_PREFIX + annotation.descriptionKey() + I18N_TOOLTIP_SUFFIX;
					String i18nTooltipText = I18N.getOrDefault(i18nTooltipKey);
					tooltipText = (!i18nTooltipText.equals(i18nTooltipKey)) ? i18nTooltipText : annotation.description();
				}
				else if ((mouseX >= validatedButton.getX()) &&
						(mouseX < (validatedButton.getX() + validatedButton.getWidth())) &&
						(mouseY >= validatedButton.getY()) &&
						(mouseY < (validatedButton.getY() + validatedButton.getHeight())))
				{
					// Tooltip for the validation button.
					tooltipText = (validatedButton.isValid()) ? I18N.getOrDefault(I18N_VALID) : I18N.getOrDefault(I18N_INVALID);
				}
				else if (annotation.needsWorldRestart() &&
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
				Class<?> fieldType = valueField.getType();

				if (fieldType == boolean.class)
				{
					try
					{
						valueField.set(null, this.checkBox.value);
					}
					catch (IllegalAccessException ignored) {}
				}
				else if (fieldType.isEnum())
				{
					try
					{
						valueField.set(null, this.enumButton.getValue());
					}
					catch (IllegalAccessException ignored) {}
				}
				else
				{
					String text = this.editBox.getValue();

					try
					{
						if (fieldType == int.class)
						{
							int parsedValue = Integer.parseInt(text);

							if ((this.validatorMethod == null) || (boolean)this.validatorMethod.invoke(null, parsedValue))
							{
								valueField.set(null, parsedValue);
							}
						}
						else if (fieldType == float.class)
						{
							float parsedValue = Float.parseFloat(text);

							if ((this.validatorMethod == null) || (boolean)this.validatorMethod.invoke(null, parsedValue))
							{
								valueField.set(null, parsedValue);
							}
						}
						else if (fieldType == String.class)
						{
							if ((this.validatorMethod == null) || (boolean)this.validatorMethod.invoke(null, text))
							{
								valueField.set(null, text);
							}
						}
						else
						{
							Object parsedValue = this.loadMethod.invoke(null, text);

							if ((parsedValue != null) && ((this.validatorMethod == null) || (boolean)this.validatorMethod.invoke(null, parsedValue)))
							{
								valueField.set(null, parsedValue);
							}
						}
					}
					catch (NumberFormatException | IllegalAccessException | InvocationTargetException ignored) {}
				}
			}

			@Override
			public String getTooltip()
			{
				return this.tooltipText;
			}

			// Sets the state of the ValidationStatusButton button based on the input in the EditBox.
			private void validateTextFieldInput(String text)
			{
				Object value;

				try
				{
					value = this.valueField.get(null);

					if (value instanceof Integer)
					{
						int parsedValue = Integer.parseInt(text);
						boolean isValid = (this.validatorMethod == null) || (boolean) this.validatorMethod.invoke(null, parsedValue);
						this.validatedButton.setValid(isValid);
					}
					else if (value instanceof Float)
					{
						float parsedValue = Float.parseFloat(text);
						boolean isValid = (this.validatorMethod == null) || (boolean) this.validatorMethod.invoke(null, parsedValue);
						this.validatedButton.setValid(isValid);
					}
					else if (value instanceof String)
					{
						boolean isValid = (this.validatorMethod == null) || (boolean) this.validatorMethod.invoke(null, text);
						this.validatedButton.setValid(isValid);
					}
					else
					{
						Object parsedValue = this.loadMethod.invoke(null, text);
						boolean isValid = (this.validatorMethod == null) || (boolean) this.validatorMethod.invoke(null, parsedValue);
						this.validatedButton.setValid(isValid);
					}
				}
				catch (NumberFormatException | IllegalAccessException | InvocationTargetException ex)
				{
					this.validatedButton.setInvalid();
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
				return List.of(optionLabel, editBox, checkBox, enumButton, validatedButton, needsWorldRestartButton);
			}
		}
	}
}