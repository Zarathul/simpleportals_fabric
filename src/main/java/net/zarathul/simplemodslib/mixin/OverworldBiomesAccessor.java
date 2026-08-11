package net.zarathul.simplemodslib.mixin;

import net.minecraft.data.worldgen.biome.OverworldBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverworldBiomes.class)
public interface OverworldBiomesAccessor
{
	@Accessor("NORMAL_WATER_COLOR")
	static int getNormalWaterColor()
	{
		throw new AssertionError("Untransformed @Accessor");
	}
}