package net.zarathul.simpleportals.configuration.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class CheckboxButtonEx extends AbstractButton
{
	public boolean value;

	private static final ResourceLocation CHECKBOX_TEXTURE = new ResourceLocation("textures/gui/checkbox.png");

	public CheckboxButtonEx(int x, int y, int width, int height, Component message, boolean initialValue)
	{
		super(x, y, width, height, message);

		this.value = initialValue;
	}

	@Override
	public void onPress()
	{
		this.value = !this.value;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getTextureManager().bind(CHECKBOX_TEXTURE);
		RenderSystem.enableDepthTest();
		Font font = minecraft.font;
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.defaultBlendFunc();
		blit(poseStack, this.x, this.y, this.isHovered() ? 20.0F : 0.0F, this.value ? 20.0F : 0.0F, 20, this.height, 64, 64);
		this.renderBg(poseStack, minecraft, mouseX, mouseY);

		if ((this.getMessage() != null) && (!this.getMessage().getString().isEmpty()))
		{
			drawString(poseStack, font, this.getMessage(), this.x + 24, this.y + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
		}
	}
}
