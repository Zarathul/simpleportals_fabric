package net.zarathul.simplemodslib.api.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CycleButtonEx<E> extends Button
{
	private List<E> values = new ArrayList<>();
	private List<String> i18nValues = new ArrayList<>();
	private int selectedIndex = -1;
	private Function<E, String> stringifier;
	private Comparator<E> comparator;

	private static final Component INVALID_INDEX = Component.literal(":invalid index:");

	protected CycleButtonEx(int x, int y, int width, int height, Component message, Function<E, String> stringifier, OnPress onPress, CreateNarration createNarration)
	{
		super(x, y, width, height, message, onPress, createNarration);
		this.stringifier = stringifier;
	}

	public CycleButtonEx(int height, Function<E, String> stringifier)
	{
		this(0, 0, 100, height, Component.empty(), stringifier, button -> {}, DEFAULT_NARRATION);
	}

	public CycleButtonEx(int height, int width, Function<E, String> stringifier)
	{
		this(0, 0, width, height, Component.empty(), stringifier, button -> {}, DEFAULT_NARRATION);
	}

	public CycleButtonEx(int height, Function<E, String> stringifier, OnPress onPress)
	{
		this(0, 0, 100, height, Component.empty(), stringifier, onPress, DEFAULT_NARRATION);
	}

	public CycleButtonEx(int height, int width, Function<E, String> stringifier, OnPress onPress)
	{
		this(0, 0, width, height, Component.empty(), stringifier, onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
	{
		extractDefaultSprite(graphics);
		extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}

	private void updateI18nValues()
	{
		var I18N = Language.getInstance();

		for (var value : values)
		{
			String stringifiedValue = stringifier.apply(value);
			i18nValues.add(I18N.getOrDefault(stringifiedValue));
		}
	}

	private Component selectedValueComponent()
	{
		return Component.literal(i18nValues.get(selectedIndex));
	}

	@Override
	public void onPress(InputWithModifiers input)
	{
		// Ensure the onPress handler gets the new value, by calling 'nextValue()' first.
		if (input.hasShiftDown()) previousValue();
		else nextValue();

		super.onPress(input);
	}

	public void nextValue()
	{
		if (selectedIndex == -1) return;

		selectedIndex = (selectedIndex + 1) % values.size();
		setMessage(selectedValueComponent());
	}

	public void previousValue()
	{
		if (selectedIndex == -1) return;

		selectedIndex = (selectedIndex - 1 + values.size()) % values.size();
		setMessage(selectedValueComponent());
	}

	public E getSelectedValue()
	{
		return (selectedIndex >= 0) ? values.get(selectedIndex) : null;
	}

	public void setSelectedValue(E value)
	{
		selectedIndex = values.indexOf(value);

		var message = (selectedIndex >= 0) ? selectedValueComponent() : INVALID_INDEX;
		setMessage(message);
	}

	public void setSelectedIndex(int index)
	{
		selectedIndex = (index < 0 || index >= values.size()) ? -1 : index;

		var message = (selectedIndex >= 0) ? selectedValueComponent() : INVALID_INDEX;
		setMessage(message);
	}

	public void setSortingComparator(Comparator<E> comparator)
	{
		this.comparator = comparator;
	}

	public void setValues(Collection<E> values)
	{
		clear();

		if (!values.isEmpty())
		{
			this.values.addAll(values);
			if (comparator != null) this.values.sort(comparator);

			updateI18nValues();
			selectedIndex = 0;
			setMessage(selectedValueComponent());
		}
	}

	public void setStringifier(Function<E, String> stringifier)
	{
		this.stringifier = stringifier;
		i18nValues.clear();
		updateI18nValues();
	}

	public void clear()
	{
		values.clear();
		i18nValues.clear();
		selectedIndex = -1;
	}
}
