package net.zarathul.simplemodslib.api.fluid;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.zarathul.simplemodslib.mixin.OverworldBiomesAccessor;
import net.zarathul.simplemodslib.mixin.SpriteContentsAccessor;

import java.util.HashMap;

public final class FluidHelper
{
	private static final HashMap<Fluid, Item> FLUID_TO_BUCKET = new HashMap<>();
	private static final RandomSource random = RandomSource.create();

	public enum FluidHandlerInteraction
	{
		NONE,
		FILL,
		DRAIN
	}

	public record FluidHandlerInteractionResult(boolean success, FluidHandlerInteraction interaction, FluidStack fluid)
	{
		public static FluidHandlerInteractionResult failure() { return new FluidHandlerInteractionResult(false, FluidHandlerInteraction.NONE, FluidStack.empty()); }
		public static FluidHandlerInteractionResult success(FluidHandlerInteraction interaction, FluidStack fluid) { return new FluidHandlerInteractionResult(true, interaction, fluid); }
	}

	public static FluidHandlerInteractionResult InteractWithFluidHandler(ServerPlayer player, InteractionHand hand, IFluidHandler handler)
	{
		ItemStack items = player.getItemInHand(hand);
		Item heldItem = items.getItem();
		FluidHandlerInteractionResult result;

		if (heldItem == Items.BUCKET)	// empty bucket
		{
			result = fillEmptyBucket(player, hand, handler);
		}
		else if (isFilledBucket(heldItem))
		{
			result = drainBucket(player, hand, handler);
		}
		else if (heldItem == Items.GLASS_BOTTLE)
		{
			result = fillEmptyGlassBottle(player, hand, handler);
		}
		else if (heldItem == Items.POTION)
		{
			result = drainWaterBottle(player, hand, handler);
		}
		else if (isFluidContainerItem(heldItem))
		{
			if (player.isCrouching())
			{
				result = fillFluidContainerItem(items, handler);
			}
			else
			{
				result = drainFluidContainerItem(items, handler);
			}
		}
		else
		{
			result = FluidHandlerInteractionResult.failure();
		}

		if (result.success())
		{
			var soundEvent = result.fluid().getFluid().getPickupSound();
			if (soundEvent.isPresent())
			{
				((ServerPlayer)player).connection.send(new ClientboundSoundPacket(
					Holder.direct(soundEvent.get()),
					SoundSource.BLOCKS,
					player.getX(), player.getY(), player.getZ(),
					1.0f, 1.0f, random.nextLong()));
			}
		}

		return result;
	}

	public static boolean isFluidHandler(Level world, BlockPos pos)
	{
		BlockEntity blockEntity = world.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
		return (blockEntity instanceof IFluidHandler);
	}

	public static boolean isFluidContainerItem(ItemStack item)
	{
		return isFluidContainerItem(item.getItem());
	}

	public static boolean isFluidContainerItem(Item item)
	{
		return IFluidContainerItem.class.isAssignableFrom(item.getClass());
	}

	public static String getFluidName(Identifier fluidId)
	{
		if (fluidId == null) return "";

		var registryResult = BuiltInRegistries.FLUID.get(fluidId);
		if (registryResult.isEmpty()) return "";

		Fluid fluid = registryResult.get().value();
		String fluidName = fluid.defaultFluidState().createLegacyBlock().getBlock().getName().getString();

		return fluidName;
	}

	@Environment(EnvType.CLIENT)
	public static int getFluidColor(Fluid fluid, BlockAndTintGetter level, BlockPos pos)
	{
		return getFluidColor(fluid, level, pos, false);
	}

	@Environment(EnvType.CLIENT)
	public static int getFluidColor(Fluid fluid, BlockAndTintGetter level, BlockPos pos, boolean useDominantTextureColorFallback)
	{
		if (fluid.isSame(Fluids.EMPTY)) return -1;

		FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());

		if (fluidModel.tintSource() == null)
		{
			if (!useDominantTextureColorFallback) return -1;

			TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
			return calculateDominantColor(sprite);
		}

		BlockState defaultBlockState = fluid.defaultFluidState().createLegacyBlock().getBlock().defaultBlockState();

		// Water has a grayscale texture and needs tinting, but without level and pos, the tint source of water returns -1.
		if (level != null && pos != null) return ARGB.opaque(fluidModel.tintSource().colorInWorld(defaultBlockState, level, pos));
		else if (fluid.isSame(Fluids.WATER)) return ARGB.opaque(OverworldBiomesAccessor.getNormalWaterColor());

		return ARGB.opaque(fluidModel.tintSource().color(defaultBlockState));
	}

	private static final HashMap<Identifier, Integer> dominantColorLookupCache = new HashMap<>();

	/**
	 * Tries to determine the dominant color in a {@link TextureAtlasSprite}.<br>
	 * Note: Results are cached by the sprites atlas location.<br>
	 * To determine the dominant color, each channel is reduced to 4 bits, grouped into buckets and the occurrences counted.
	 * Pixels with very low or high alpha are dismissed in this process. At the end, the bucket with the highest count is
	 * selected and the tint color is constructed from the contained accumulated channel values, weighed by the accumulated
	 * alpha for that bucket.
	 *
	 * @param sprite
	 * The {@link TextureAtlasSprite} to calculate the dominant color for.
	 * @return
	 * An ARGB value representing the dominant color in the sprite.
	 */
	@Environment(EnvType.CLIENT)
	public static int calculateDominantColor(TextureAtlasSprite sprite)
	{
		if (dominantColorLookupCache.containsKey(sprite.atlasLocation())) return dominantColorLookupCache.get(sprite.atlasLocation());

		SpriteContentsAccessor spriteContents = (SpriteContentsAccessor)sprite.contents();
		NativeImage image = spriteContents.simplemodslib$getOriginalImage();

		final int LEVELS = 16;
		final int BUCKET_COUNT = LEVELS * LEVELS * LEVELS;

		int[] counts = new int[BUCKET_COUNT];

		long[] redBuckets   = new long[BUCKET_COUNT];
		long[] greenBuckets = new long[BUCKET_COUNT];
		long[] blueBuckets  = new long[BUCKET_COUNT];
		long[] alphaBuckets = new long[BUCKET_COUNT];

		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getPixel(x, y);

				int alpha = (argb >>> 24);
				int red   = (argb >>> 16) & 0xFF;
				int green = (argb >>> 8)  & 0xFF;
				int blue  = argb & 0xFF;

				if (alpha < 32) {
					continue;
				}

				// Ignore nearly-white highlights.
				if (Math.min(red, Math.min(green, blue)) > 245) continue;

				// 256 -> 16 levels per channel.
				int qr = red   >> 4;
				int qg = green >> 4;
				int qb = blue  >> 4;

				int bucket = (qr * LEVELS + qg) * LEVELS + qb;

				counts[bucket]++;

				redBuckets[bucket]   += (long) red   * alpha;
				greenBuckets[bucket] += (long) green * alpha;
				blueBuckets[bucket]  += (long) blue  * alpha;
				alphaBuckets[bucket] += alpha;
			}
		}

		int dominantBucket = -1;
		int dominantCount = 0;

		for (int i = 0; i < BUCKET_COUNT; i++)
		{
			if (counts[i] > dominantCount)
			{
				dominantCount = counts[i];
				dominantBucket = i;
			}
		}

		if (dominantBucket < 0 || alphaBuckets[dominantBucket] == 0) return 0xFFFFFF;

		long weight = alphaBuckets[dominantBucket];

		int red   = (int) (redBuckets[dominantBucket]   / weight);
		int green = (int) (greenBuckets[dominantBucket] / weight);
		int blue  = (int) (blueBuckets[dominantBucket]  / weight);

		int dominantColor = (0xFF << 24) | (red << 16) | (green << 8) | blue;
		dominantColorLookupCache.put(sprite.atlasLocation(), dominantColor);

		return dominantColor;
	}

	private static FluidHandlerInteractionResult fillEmptyBucket(Player player, InteractionHand hand, IFluidHandler handler)
	{
		FluidStack handlerFluid = handler.getFluid();

		// If the fluid handler has one bucket worth of fluid, drain it and replace
		// the empty bucket in the players hand with a filled one of the correct type.
		if ((handlerFluid.getAmount() >= FluidStack.BUCKET_VOLUME))
		{
			if (!handler.drain(new FluidStack(handlerFluid.getFluid(), FluidStack.BUCKET_VOLUME)).isEmpty())
			{
				if (!player.isCreative())
				{
					Item bucket = getBucketForFluid(handlerFluid.getFluid());
					player.setItemInHand(hand, new ItemStack(bucket));
				}

				return FluidHandlerInteractionResult.success(FluidHandlerInteraction.FILL, new FluidStack(handlerFluid.getFluid(), FluidStack.BUCKET_VOLUME));
			}
		}

		return FluidHandlerInteractionResult.failure();
	}

	private static FluidHandlerInteractionResult drainBucket(Player player, InteractionHand hand, IFluidHandler handler)
	{
		BucketItem heldBucket = (BucketItem)player.getItemInHand(hand).getItem();
		Fluid bucketFluid = heldBucket.getContent();
		FluidStack handlerFluid = handler.getFluid();

		// Try to fill one bucket worth of fluid into the handler, if there is enough room. The type of
		// fluid is determined by the bucket. If successful, replace the bucket in the players hand with an empty one.
		if ((handler.getCapacity() - handlerFluid.getAmount()) >= FluidStack.BUCKET_VOLUME)
		{
			FluidStack fillFluid = new FluidStack(bucketFluid, FluidStack.BUCKET_VOLUME);

			if (handler.fill(fillFluid) > 0)
			{
				if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));

				return FluidHandlerInteractionResult.success(FluidHandlerInteraction.DRAIN, fillFluid);
			}
		}

		return FluidHandlerInteractionResult.failure();
	}

	private static FluidHandlerInteractionResult fillEmptyGlassBottle(ServerPlayer player, InteractionHand hand, IFluidHandler handler)
	{
		FluidStack handlerFluid = handler.getFluid();

		// If the fluid handler has one bucket worth of water, drain it and replace
		// the empty glass bottle in the players hand with a filled one.
		if (handlerFluid.getFluid().isSame(Fluids.WATER) && (handlerFluid.getAmount() >= FluidStack.BUCKET_VOLUME))
		{
			if (!handler.drain(new FluidStack(Fluids.WATER, FluidStack.BUCKET_VOLUME)).isEmpty())
			{
				if (!player.isCreative())
				{
					// Glass bottle filled with water is a potion.
					ItemStack heldItem = player.getItemInHand(hand);
					ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);

					if (!player.getInventory().add(waterBottle))
					{
						player.drop(waterBottle, false);
					}

					// For some reason the water bottle turns back into an empty glass bottle most of the time, if the held item is consumed before adding the new one.
					heldItem.consume(1, player);
				}

				return FluidHandlerInteractionResult.success(FluidHandlerInteraction.FILL, new FluidStack(handlerFluid.getFluid(), FluidStack.BUCKET_VOLUME));
			}
		}

		return FluidHandlerInteractionResult.failure();
	}

	private static FluidHandlerInteractionResult drainWaterBottle(ServerPlayer player, InteractionHand hand, IFluidHandler handler)
	{
		PotionContents potionContents = player.getItemInHand(hand).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		FluidStack handlerFluid = handler.getFluid();

		// Try to fill one bucket worth of fluid into the handler, if there is enough room. The type of
		// fluid is always water, since the only real fluid bottles can contain is water. If successful,
		// replace the water potion in the players hand with an empty one.
		if ((handler.getCapacity() - handlerFluid.getAmount()) >= FluidStack.BUCKET_VOLUME)
		{
			FluidStack fillFluid = new FluidStack(Fluids.WATER, FluidStack.BUCKET_VOLUME);

			if (handler.fill(fillFluid) > 0)
			{
				if (!player.isCreative())
				{
					player.getItemInHand(hand).consume(1, player);
					ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
					if (!player.getInventory().add(emptyBottle))
					{
						player.drop(emptyBottle, false);
					}
				}

				return FluidHandlerInteractionResult.success(FluidHandlerInteraction.DRAIN, fillFluid);
			}
		}

		return FluidHandlerInteractionResult.failure();
	}

	private static FluidHandlerInteractionResult fillFluidContainerItem(ItemStack stack, IFluidHandler handler)
	{
		FluidStack handlerFluid = handler.getFluid();
		IFluidContainerItem heldItem = (IFluidContainerItem)stack.getItem();

		int itemFillAmount = heldItem.fill(stack, handler.getFluid().copy());
		if (itemFillAmount > 0)
		{
			FluidStack fillFluid = new FluidStack(handlerFluid.getFluid(), itemFillAmount);
			handler.drain(fillFluid);

			return FluidHandlerInteractionResult.success(FluidHandlerInteraction.FILL, fillFluid);
		}
		else
		{
			return FluidHandlerInteractionResult.failure();
		}
	}

	private static FluidHandlerInteractionResult drainFluidContainerItem(ItemStack stack, IFluidHandler handler)
	{
		FluidStack handlerFluid = handler.getFluid();
		IFluidContainerItem heldItem = (IFluidContainerItem)stack.getItem();

		int remainingHandlerCapacity = handler.getCapacity() - handlerFluid.getAmount();
		// If the handler is empty, it means it can accept any fluid type. Use the fluid type of the container in that case.
		FluidStack drainableFluid = (handlerFluid.isEmpty()) ? FluidStack.getFluid(stack) : handlerFluid.copy();
		drainableFluid.setAmount(remainingHandlerCapacity);

		FluidStack drainedFluid = heldItem.drain(stack, drainableFluid);
		if (!drainedFluid.isEmpty() && (handler.fill(drainedFluid) > 0))
		{
			 return FluidHandlerInteractionResult.success(FluidHandlerInteraction.DRAIN, drainableFluid);
		}

		return FluidHandlerInteractionResult.failure();
	}

	private static Item getBucketForFluid(Fluid fluid)
	{
		if (fluid.isSame(Fluids.EMPTY)) return Items.BUCKET;

		// TODO: Find a better way to do this
		if (FLUID_TO_BUCKET.isEmpty()) BuiltInRegistries.FLUID.forEach(x -> FLUID_TO_BUCKET.put(x, x.getBucket()));

		return FLUID_TO_BUCKET.get(fluid);
	}

	private static boolean isFilledBucket(Item item)
	{
		if (item == Items.AIR) return false;

		// TODO: Find a better way to do this
		if (FLUID_TO_BUCKET.isEmpty()) BuiltInRegistries.FLUID.forEach(x -> FLUID_TO_BUCKET.put(x, x.getBucket()));

		return FLUID_TO_BUCKET.containsValue(item);
	}
}
