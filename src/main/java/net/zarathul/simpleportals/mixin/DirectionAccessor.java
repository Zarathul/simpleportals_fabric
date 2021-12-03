package net.zarathul.simpleportals.mixin;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Direction.class)
public interface DirectionAccessor
{
	@Accessor("BY_NAME")
	static Map<String, Direction> getByNameMap()
	{
		throw new AssertionError();
	}
}
