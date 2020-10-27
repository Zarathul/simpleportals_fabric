package net.zarathul.simpleportals.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor
{
	@Accessor("lastSentFood")
	void setLastSendFood(int ticks);

	@Accessor("isChangingDimension")
	void setIsChangingDimension(boolean value);
}
