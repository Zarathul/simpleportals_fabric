package net.zarathul.simpleportals.configuration.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.registration.Address;

import java.util.ArrayList;
import java.util.List;

public record PortalInfo(
	ResourceKey<Level> dimension,
	BlockPos location,
	Address address,
	int power
)
{
	public static final StreamCodec<FriendlyByteBuf, PortalInfo> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, PortalInfo>()
	{
		@Override
		public PortalInfo decode(FriendlyByteBuf input)
		{
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, input.readIdentifier());
			BlockPos location = input.readBlockPos();
			Address address = new Address();
			address.deserializeNBT(input.readNbt());
			int power = input.readInt();

			return new PortalInfo(
				dimension,
				location,
				address,
				power
			);
		}

		@Override
		public void encode(FriendlyByteBuf output, PortalInfo info)
		{
			output.writeIdentifier(info.dimension.identifier());	// dimension
			output.writeBlockPos(info.location);					// location
			output.writeNbt(info.address.serializeNBT());			// address
			output.writeInt(info.power);							// power
		}
	};

	public static final StreamCodec<FriendlyByteBuf, List<PortalInfo>> LIST_STREAM_CODEC = new StreamCodec<FriendlyByteBuf, List<PortalInfo>>()
	{
		@Override
		public List<PortalInfo> decode(FriendlyByteBuf input)
		{
			List<PortalInfo> portalInfos = input.readCollection(ArrayList::new, PortalInfo.STREAM_CODEC);
			return portalInfos;
		}

		@Override
		public void encode(FriendlyByteBuf output, List<PortalInfo> values)
		{
			output.writeCollection(values, (buffer, info) -> {
				buffer.writeIdentifier(info.dimension.identifier());	// dimension
				buffer.writeBlockPos(info.location);					// location
				buffer.writeNbt(info.address.serializeNBT());			// address
				buffer.writeInt(info.power);							// power
			});
		}
	};
}
