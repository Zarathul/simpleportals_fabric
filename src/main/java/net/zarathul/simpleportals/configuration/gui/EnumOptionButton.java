package net.zarathul.simpleportals.configuration.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Optional;


@Environment(EnvType.CLIENT)
public class EnumOptionButton<E extends Enum<E>> extends Button
{
	private Class<E> clazz;
	private int selectedIndex;
	private String[] names;
	private Component[] i18nNames;
	private boolean[] allowedValues;

	private static final String I18N_ENUM_PREFIX = "config.enums.";
	/**
	 * The button message is automatically localized. Localization keys are of the following format:
	 * "config.enums.enum_name.enum_value" e.g. "config.enums.colors.red". Note that both enum_name
	 * and enum_value need to be lower case.
	 */
	public EnumOptionButton(Class<E> clazz, Optional<E> value, int x, int y, int width, int height, OnPress onPress)
	{
		super(x, y, width, height,
			  Component.translatable(I18N_ENUM_PREFIX + ((clazz != null) ? clazz.getSimpleName().toLowerCase() : "") + "." + (value.isPresent() ? value.get().name().toLowerCase() : "")),
			  onPress,
			  DEFAULT_NARRATION);

		if (clazz != null && value.isPresent())
		{
			this.clazz = clazz;
			selectedIndex = 0;

			int i = 0;

			E[] constants = clazz.getEnumConstants();
			names = new String[constants.length];
			i18nNames = new Component[constants.length];
			allowedValues = new boolean[constants.length];
			E actualValue = value.get();

			for (E e : constants)
			{
				names[i] = e.name();
				i18nNames[i] = Component.translatable(I18N_ENUM_PREFIX + clazz.getSimpleName().toLowerCase() + "." + e.name().toLowerCase());
				allowedValues[i] = true;
				if (e == actualValue) selectedIndex = i;
				i++;
			}
		}
		else
		{
			this.clazz = null;
			names = null;
			i18nNames = null;
			allowedValues = null;
		}
	}

	public EnumOptionButton(int x, int y, int width, int height)
	{
		this(null, Optional.empty(), x, y, width, height);
	}

	public EnumOptionButton(Class<E> clazz, String value, int x, int y, int width, int height)
	{
		E enumValue = E.valueOf(clazz, value);

		this(clazz, Optional.of(enumValue), x, y, width, height, button -> {});
	}

	public EnumOptionButton(Class<E> clazz, Optional<E> value, int x, int y, int width, int height)
	{
		this(clazz, value, x, y, width, height, button -> {});
	}

	public EnumOptionButton(Class<E> clazz, Optional<E> value, int width, int height)
	{
		this(clazz, value, 0, 0, width, height, button -> {});
	}

	public EnumOptionButton(Class<E> clazz, Optional<E> value, int width, int height, OnPress onPress)
	{
		this(clazz, value, 0, 0, width, height, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
	{
		extractDefaultSprite(graphics);
		extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}

	@Override
	public void onPress(InputWithModifiers input)
	{
		// Ensure the onPress handler gets the new value, by calling 'nextValue()' first.
		nextValue();
		super.onPress(input);
	}

	public void init(Class<E> clazz, String valueName)
	{
		this.clazz = clazz;
		E value = E.valueOf(clazz, valueName);
		selectedIndex = 0;

		int i = 0;

		E[] constants = clazz.getEnumConstants();
		names = new String[constants.length];
		i18nNames = new Component[constants.length];
		allowedValues = new boolean[constants.length];

		for (E e : constants)
		{
			names[i] = e.name();
			i18nNames[i] = Component.translatable(I18N_ENUM_PREFIX + clazz.getSimpleName().toLowerCase() + "." + e.name().toLowerCase());
			allowedValues[i] = true;
			if (e == value) selectedIndex = i;
			i++;
		}
	}

	public E getValue()
	{
		// This will crash if there are no allowed values, which it should! Because having no allowed values is a bug.
		return Enum.valueOf(this.clazz, names[selectedIndex]);
	}

	public void setValue(E value)
	{
		for (int i = 0; i < names.length; i++)
		{
			if (names[i].equals(value.name()) && allowedValues[i])
			{
				selectedIndex = i;
				setMessage(i18nNames[i]);
				break;
			}
		}
	}

	public void setAllowedValues(Collection<E> values)
	{
		for (int i = 0; i < names.length; i++)
		{
			E value = E.valueOf(clazz, names[i]);
			allowedValues[i] = values.contains(value);
		}

		boolean isAtLeastOneValueAllowed = false;
		for (var allowedValue : allowedValues)
		{
			if (allowedValue)
			{
				isAtLeastOneValueAllowed = true;
				break;
			}
		}

		if (!isAtLeastOneValueAllowed)
		{
			selectedIndex = -1;
			setMessage(Component.literal("-ERROR-"));
		}
		else if (selectedIndex == -1 || !allowedValues[selectedIndex])
		{
			nextValue();
		}
	}

	public void nextValue()
	{
		// There are no allowed values, so return early. Otherwise, the loop would run forever.
		if (selectedIndex == -1) return;

		do
		{
			selectedIndex = (selectedIndex + 1) % names.length;
		}
		while(!allowedValues[selectedIndex]);

		setMessage(i18nNames[selectedIndex]);
	}
}
