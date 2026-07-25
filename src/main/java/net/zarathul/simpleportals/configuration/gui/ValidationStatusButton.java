package net.zarathul.simpleportals.configuration.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
class ValidationStatusButton extends Button
{
	private boolean valid;
	private WidgetSprites sprites = new WidgetSprites(
			Identifier.withDefaultNamespace("dialog/warning_button"),
			Identifier.withDefaultNamespace("dialog/warning_button_disabled"),
			Identifier.withDefaultNamespace("dialog/warning_button_highlighted"),
			Identifier.withDefaultNamespace("dialog/warning_button_disabled")
	);

	public ValidationStatusButton(int x, int y, int width, int height, Button.OnPress clickHandler)
	{
		super(x, y, width, height, CommonComponents.EMPTY, clickHandler, DEFAULT_NARRATION);

		this.valid = true;
	}

	public void setValid(boolean isValid)
	{
		this.valid = isValid;
	}

	public void setValid()
	{
		this.valid = true;
	}

	public void setInvalid()
	{
		this.valid = false;
	}

	public boolean isValid()
	{
		return this.valid;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
	{
		Identifier sprite = sprites.get(!valid, isHoveredOrFocused());
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height, ARGB.white(alpha));
	}

	@Override
	public boolean shouldTakeFocusAfterInteraction()
	{
		return false;
	}
}
