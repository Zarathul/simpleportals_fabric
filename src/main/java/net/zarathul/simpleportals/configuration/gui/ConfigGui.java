package net.zarathul.simpleportals.configuration.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.zarathul.simpleportals.configuration.Config;
import net.zarathul.simpleportals.configuration.ConfigSetting;
import net.zarathul.simpleportals.configuration.StorageMethods;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ConfigGui extends Screen
{
	private Screen parent;
	private ModOptionList optionList;
	private Class<?> settingsType;
	private String configName;

	private static final int PADDING = 5;

	public ConfigGui(TextComponent title, Screen parent, Class<?> settingsType, String configName)
	{
		super(title);

		this.parent = parent;
		this.settingsType = settingsType;
		this.configName = configName;
	}

	@Override
	public void init(Minecraft mc, int width, int height)
	{
		super.init(mc, width, height);

		int titleHeight = mc.font.wordWrapHeight(title.getString(), width - 2 * PADDING);
		int paddedTitleHeight = titleHeight + PADDING * 2;

		addButton(width - 120 - 2 * PADDING, 0, 60, paddedTitleHeight, "Back", button -> mc.setScreen(parent));
		addButton(width - 60 - PADDING, 0, 60, paddedTitleHeight, "Save", button -> {
			this.optionList.commitChanges();
			Config.save(this.configName, this.settingsType);
			mc.setScreen(parent);
		});

		int optionListHeaderHeight = titleHeight + 2 * PADDING;
		this.optionList = new ModOptionList(this.settingsType, minecraft, width, height, optionListHeaderHeight, height - optionListHeaderHeight, 26);
		this.children.add(optionList);
	}

	private void addButton(int x, int y, int width, int height, String label, Button.OnPress pressHandler)
	{
		Button button = new Button(x, y, width, height, new TextComponent(label), pressHandler);

		children.add(button);
		buttons.add(button);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(poseStack);
		if (this.optionList != null) this.optionList.render(poseStack, mouseX, mouseY, partialTicks);
		RenderSystem.disableLighting();	// Rendering the tooltip enables lighting but buttons etc. assume lighting to be disabled.
		super.render(poseStack, mouseX, mouseY, partialTicks);
		minecraft.font.draw(poseStack, title.getVisualOrderText(), PADDING, PADDING, 16777215);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (this.optionList != null) optionList.tick();
	}

	@Environment(EnvType.CLIENT)
	public class ModOptionList extends AbstractSelectionList<ModOptionList.Entry>
	{
		private static final int LEFT_RIGHT_BORDER = 30;
		private static final String I18N_PREFIX = "config.";
		private static final String I18N_TOOLTIP_SUFFIX = ".tooltip";
		private static final String I18N_VALID = "config.input_valid";
		private static final String I18N_INVALID = "config.input_invalid";
		private static final String I18N_NEEDS_WORLD_RESTART = "config.needs_world_restart";

		public ModOptionList(Class<?> settingsType, Minecraft mc, int width, int height, int top, int bottom, int itemHeight)
		{
			super(mc, width, height, top, bottom, itemHeight);
			generateEntries(settingsType);
		}

		@Override
		public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
		{
			super.render(poseStack, mouseX, mouseY, partialTicks);

			String tooltip;

			for (Entry entry : this.children())
			{
				tooltip = entry.getTooltip();

				if (tooltip != null && !tooltip.isEmpty())
				{
					List<Component> comment = Arrays.stream(tooltip.split("\n")).map(TextComponent::new).collect(Collectors.toList());
					renderComponentTooltip(poseStack, comment, mouseX, mouseY);

					break;
				}
			}
		}

		public void tick()
		{
			for (GuiEventListener child : this.children())
			{
				((Entry)child).tick();
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
		public boolean mouseClicked(double x, double y, int button)
		{
			if (super.mouseClicked(x, y, button))
			{
				GuiEventListener focusedChild = getFocused();

				for (GuiEventListener child : this.children())
				{
					if (child != focusedChild) ((Entry)child).clearFocus();
				}

				return true;
			}

			return false;
		}

		public void commitChanges()
		{
			for (Entry entry : this.children())
			{
				entry.commitChanges();
			}
		}

		private void generateEntries(Class<?> clazz)
		{
			Field[] fields = Config.getSettingFieldsSortedByCategory(clazz);

			ConfigSetting annotation;
			String category;
			String lastCategory = null;

			for (Field valueField : fields)
			{
				if (!Config.isValidSetting(valueField, true)) continue;

				annotation = valueField.getAnnotation(ConfigSetting.class);
				category = ((annotation != null) && !annotation.category().isEmpty()) ? annotation.category() : Config.DEFAULT_CATEGORY;

				if (!category.equals(lastCategory))
				{
					String i18nKey = "config." + category;
					String categoryLabel = (I18n.exists(i18nKey)) ? I18n.get(i18nKey) : category;

					addEntry(new CategoryEntry(categoryLabel));

					lastCategory = category;
				}

				addEntry(new OptionEntry(valueField, annotation));
			}
		}

		@Environment(EnvType.CLIENT)
		public abstract class Entry extends AbstractSelectionList.Entry<ConfigGui.ModOptionList.Entry>
		{
			public abstract void clearFocus();
			public abstract void commitChanges();
			public abstract void tick();
			public abstract String getTooltip();
		}

		@Environment(EnvType.CLIENT)
		public class CategoryEntry extends Entry
		{
			private final String text;
			private final int width;

			public CategoryEntry(String text)
			{
				this.text = text;
				this.width = minecraft.font.width(text);
			}

			@Override
			public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHot, float partialTicks)
			{
				minecraft.font.drawShadow(poseStack, this.text, minecraft.screen.width / 2.0f - this.width / 2.0f, top + height - 9 - 1, 16777215);
			}

			@Override
			public boolean changeFocus(boolean forward)
			{
				return false;
			}

			@Override
			public void clearFocus()
			{
			}

			@Override
			public void commitChanges()
			{
			}

			@Override
			public void tick()
			{
			}

			@Override
			public String getTooltip()
			{
				return null;
			}
		}

		@Environment(EnvType.CLIENT)
		public class OptionEntry extends Entry
		{
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

			public OptionEntry(Field valueField, ConfigSetting annotation)
			{
				this.valueField = valueField;
				this.validatorMethod = Config.getValidator(valueField);
				this.annotation = valueField.getAnnotation(ConfigSetting.class);
				Optional<StorageMethods> loadSave = Config.getLoadSave(valueField);
				this.loadMethod = (loadSave.isPresent()) ? loadSave.get().load : null;

				Object defaultValue = Config.getDefaultValue(valueField);

				this.validatedButton = new ValidationStatusButton(0, 0, button -> {
					if (this.editBox != null)
					{
						this.editBox.setValue(defaultValue.toString());
						this.editBox.setFocus(false);
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

				this.needsWorldRestartButton = new ImageButton(0, 0, 15, 12, 182, 24, 0, Button.WIDGETS_LOCATION, 256, 256, (b) -> {;});
				this.needsWorldRestartButton.active = false;
				this.needsWorldRestartButton.visible = annotation.needsWorldRestart();

				Object value = null;

				try
				{
					value = valueField.get(null);
				}
				catch (IllegalAccessException e) {}

				children.add(this.validatedButton);
				children.add(this.needsWorldRestartButton);

				if (value instanceof Boolean)
				{
					this.checkBox = new CheckboxButtonEx(0, 0, 20, 20, TextComponent.EMPTY, (boolean)value);

					children.add(this.checkBox);
				}
				else if (value instanceof Enum)
				{
					this.enumButton = new EnumOptionButton(value.getClass(), value.toString(), 0, 0, 100, itemHeight - PADDING);

					children.add(this.enumButton);
				}
				else
				{
					this.editBox = new EditBox(minecraft.font, 0, 0, 100, itemHeight - PADDING, TextComponent.EMPTY);
					this.editBox.setTextColor(16777215);
					if (value != null) this.editBox.setValue(value.toString());
					this.editBox.setMaxLength(256);
					this.editBox.setCanLoseFocus(true);
					this.editBox.setFilter(this::validateTextFieldInput);

					children.add(this.editBox);
				}

				this.tooltipText = null;
			}

			@Override
			public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHot, float partialTicks)
			{
				this.validatedButton.x = getScrollbarPosition() - this.validatedButton.getWidth() - this.needsWorldRestartButton.getWidth() - 2 * PADDING;
				this.validatedButton.y = top + ((itemHeight - this.validatedButton.getHeight()) / 2) - 1;
				this.validatedButton.render(poseStack, mouseX, mouseY, partialTicks);

				// This needs to be here because the TextFieldWidget changes the GL state and never sets it back,
				// nor does the ImageButton set the correct values to render properly. Without this call, the
				// ImageButtons are just black after the first TextFieldWidget is rendered.
				// Update: No longer needed because the ValidationStatusButton sets up the state correctly and is rendered
				// BEFORE this ImageButton. DON'T delete this comment to avoid confusion in the future.
				// RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0f);

				this.needsWorldRestartButton.x = getScrollbarPosition() - this.needsWorldRestartButton.getWidth() - PADDING;
				this.needsWorldRestartButton.y = top + ((itemHeight - this.needsWorldRestartButton.getHeight()) / 2) - 1;
				this.needsWorldRestartButton.render(poseStack, mouseX, mouseY, partialTicks);

				if (this.editBox != null)
				{
					this.editBox.x = left + (width / 2) + PADDING;
					this.editBox.y = top;
					this.editBox.setWidth((width / 2) - this.validatedButton.getWidth() - this.needsWorldRestartButton.getWidth() - 4 * PADDING - 6);
					this.editBox.render(poseStack, mouseX, mouseY, partialTicks);
				}
				else if (this.checkBox != null)
				{
					this.checkBox.x = left + (width / 2) + PADDING;
					this.checkBox.y = top;
					this.checkBox.render(poseStack, mouseX, mouseY, partialTicks);
				}
				else if (this.enumButton != null)
				{
					this.enumButton.x = left + (width / 2) + PADDING;
					this.enumButton.y = top;
					this.enumButton.setWidth((width / 2) - this.validatedButton.getWidth() - this.needsWorldRestartButton.getWidth() - 4 * PADDING - 6);
					this.enumButton.render(poseStack, mouseX, mouseY, partialTicks);
				}

				// Getting translations during rendering is not exactly a smart thing to do, but it's just the config UI so .. meh.
				String description = I18n.get(I18N_PREFIX + this.annotation.descriptionKey());
				int descriptionWidth = minecraft.font.width(description);
				int descriptionLeft = left + (width / 2) - descriptionWidth - PADDING;
				int descriptionTop = top + (itemHeight / 2) - PADDING - minecraft.font.lineHeight / 2 + 2;
				minecraft.font.drawShadow(poseStack, description, descriptionLeft, descriptionTop, 16777215);

				// Set tooltip to be rendered by the ModOptionList. This could be moved to mouseMoved(), but either
				// the tooltip for the description text would have to stay here or its bounds would have to be stored.
				// To not complicate things, keep everything here for now.
				if ((mouseX >= descriptionLeft) &&
					(mouseX < (descriptionLeft + descriptionWidth)) &&
					(mouseY >= descriptionTop) &&
					(mouseY < (descriptionTop + minecraft.font.lineHeight)))
				{
					// Tooltip for the description
					String i18nTooltipKey = I18N_PREFIX + this.annotation.descriptionKey() + I18N_TOOLTIP_SUFFIX;
					this.tooltipText = (I18n.exists(i18nTooltipKey)) ?
									   I18n.get(i18nTooltipKey) :
									   this.annotation.description();
				}
				else if ((mouseX >= this.validatedButton.x) &&
						 (mouseX < (this.validatedButton.x + this.validatedButton.getWidth())) &&
						 (mouseY >= this.validatedButton.y) &&
						 (mouseY < (this.validatedButton.y + this.validatedButton.getHeight())))
				{
					// Tooltip for the validation button.
					this.tooltipText = (this.validatedButton.isValid()) ? I18n.get(I18N_VALID) : I18n.get(I18N_INVALID);
				}
				else if (this.annotation.needsWorldRestart() &&
						 (mouseX >= this.needsWorldRestartButton.x) &&
						 (mouseX < (this.needsWorldRestartButton.x + this.needsWorldRestartButton.getWidth())) &&
						 (mouseY >= this.needsWorldRestartButton.y) &&
						 (mouseY < (this.needsWorldRestartButton.y + this.needsWorldRestartButton.getHeight())))
				{
					// Tooltip for the needs world restart button.
					this.tooltipText = I18n.get(I18N_NEEDS_WORLD_RESTART);
				}
				else
				{
					this.tooltipText = null;
				}
			}

			@Override
			public void clearFocus()
			{
				if (this.editBox != null)
				{
					this.editBox.setFocus(false);
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
					catch (IllegalAccessException ex) {}
				}
				else if (fieldType.isEnum())
				{
					try
					{
						valueField.set(null, this.enumButton.getValue());
					}
					catch (IllegalAccessException e) {}
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
					catch (NumberFormatException | IllegalAccessException | InvocationTargetException ex) {}
				}
			}

			@Override
			public void tick()
			{
				if (this.editBox != null)
				{
					this.editBox.tick();
				}
			}

			@Override
			public String getTooltip()
			{
				return this.tooltipText;
			}

			// Sets the state of the ValidationStatusButton button based on the input in the EditBox.
			private boolean validateTextFieldInput(String text)
			{
				Object value = null;

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

				return true;
			}
		}
	}
}