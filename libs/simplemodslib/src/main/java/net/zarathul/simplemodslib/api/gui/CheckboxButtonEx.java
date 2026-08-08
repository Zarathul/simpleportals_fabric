package net.zarathul.simplemodslib.api.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
public class CheckboxButtonEx extends AbstractButton
{
	public boolean value;
	private WidgetSprites sprites = new WidgetSprites(
			Identifier.withDefaultNamespace("widget/checkbox_selected"),
			Identifier.withDefaultNamespace("widget/checkbox"),
			Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted"),
			Identifier.withDefaultNamespace("widget/checkbox_highlighted")
	);

	public CheckboxButtonEx(int x, int y, int width, int height, boolean initialValue)
	{
		super(x, y, width, height, Component.empty());

		this.value = initialValue;
	}

	@Override
	public void onPress(InputWithModifiers input)
	{
		this.value = !this.value;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
	{
		Identifier sprite = sprites.get(value, isHoveredOrFocused());
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height, ARGB.white(alpha));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output)
	{
	}
}
