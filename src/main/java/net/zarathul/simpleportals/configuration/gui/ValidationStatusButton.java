package net.zarathul.simpleportals.configuration.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;

@Environment(EnvType.CLIENT)
class ValidationStatusButton extends Button
{
	private boolean valid;

	public ValidationStatusButton(int x, int y, Button.OnPress clickHandler)
	{
		super(x, y, 15, 15, CommonComponents.EMPTY, clickHandler);

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
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, Button.WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		Icon icon = (this.valid) ? Icon.VALID : Icon.INVALID;

		this.blit(poseStack, this.x, this.y, icon.getX(), icon.getY(), this.width, this.height);
	}

	@Override
	public boolean changeFocus(boolean forward)
	{
		return false;
	}

	enum Icon
	{
		VALID(208, 0),
		INVALID(192, 0);

		private final int x;
		private final int y;

		Icon(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		public int getX()
		{
			return this.x;
		}

		public int getY()
		{
			return this.y;
		}
	}
}
