package net.zarathul.simpleportals.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor
{
	@Accessor("portalCooldown")
	int getPortalCooldown();

	@Accessor("portalCooldown")
	void setPortalCooldown(int ticks);

	@Invoker("removeAfterChangingDimensions")
	void invokeRemoveAfterChangingDimensions();
}
