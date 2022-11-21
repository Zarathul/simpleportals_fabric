package net.zarathul.simpleportals.configuration.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.registration.Address;

public class PortalInfo
{
	public ResourceKey<Level> dimension;
	public BlockPos location;
	public Address address;
	public int power;
}