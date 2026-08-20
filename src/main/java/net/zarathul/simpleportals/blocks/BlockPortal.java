package net.zarathul.simpleportals.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the actual portals in the center of the portal multi-block.
 */
public class BlockPortal extends Block implements net.minecraft.world.level.block.Portal
{
	private static final VoxelShape X_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
	private static final VoxelShape Y_AABB = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 10.0D, 16.0D);
	private static final VoxelShape Z_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);

	public static final EnumProperty<Axis> AXIS = EnumProperty.create(
		"axis",
		Axis.class,
		Axis.X, Axis.Y, Axis.Z);

	public BlockPortal(Properties properties)
	{
		super(properties
			.noLootTable()
			.noCollision()
			.strength(-1.0F) // indestructible by normal means
			.pushReaction(PushReaction.BLOCK)
			.lightLevel((_) -> 11)
			.sound(SoundType.GLASS));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(AXIS);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context)
	{
		Axis portalAxis = state.getValue(AXIS);

		return switch (portalAxis)
		{
			case Y -> Y_AABB;
			case Z -> Z_AABB;
			default -> X_AABB;
		};
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise)
	{
		if (handlePowerSourceEnteringPortal(world, pos, entity)) return;
		if (entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state)
	{
		if (!level.isClientSide())
		{
			// Deactivate damaged portals.

			ServerLevel serverLevel = (ServerLevel)level;
			List<Portal> affectedPortals = SimplePortals.PORTAL_REGISTRY.getPortalsAt(pos, serverLevel.dimension());
			
			if (affectedPortals.isEmpty()) return;
			
			Portal firstPortal = affectedPortals.getFirst();

			if (firstPortal.isDamaged(serverLevel))
			{
				SimplePortals.PORTAL_REGISTRY.deactivatePortal(serverLevel, pos);
			}
		}

		super.destroy(level, pos, state);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData)
	{
		return ItemStack.EMPTY;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random)
	{
		if (Settings.ambientSoundEnabled() && random.nextInt(100) == 0)
		{
			world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
		}

		if (Settings.particlesEnabled())
		{
			for (int i = 0; i < 4; ++i)
			{
				double d0 = (double)pos.getX() + random.nextDouble();
				double d1 = (double)pos.getY() + random.nextDouble();
				double d2 = (double)pos.getZ() + random.nextDouble();
				double d3 = ((double)random.nextFloat() - 0.5D) * 0.5D;
				double d4 = ((double)random.nextFloat() - 0.5D) * 0.5D;
				double d5 = ((double)random.nextFloat() - 0.5D) * 0.5D;
				int j = random.nextInt(2) * 2 - 1;

				Axis portalAxis = world.getBlockState(pos).getValue(AXIS);
				if (portalAxis == Axis.X)
				{
					d0 = (double)pos.getX() + 0.5D + 0.25D * (double)j;
					d3 = random.nextFloat() * 2.0F * (float)j;
				}
				else if (portalAxis == Axis.Y)
				{
					d1 = (double)pos.getY() + 0.5D + 0.25D * (double)j;
					d4 = random.nextFloat() * 2.0F * (float)j;
				}
				else
				{
					d2 = (double)pos.getZ() + 0.5D + 0.25D * (double)j;
					d5 = random.nextFloat() * 2.0F * (float)j;
				}

				world.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
			}
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity)
	{
		return (entity instanceof Player) ? Settings.playerTeleportationDelay() : 0;
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos)
	{
		PortalRegistry registry = SimplePortals.PORTAL_REGISTRY;
		Portal startPortal = registry.getPortalsAt(portalEntryPos, currentLevel.dimension()).getFirst();

		List<Portal> potentialDestinationPortals = registry.getPortalsWithAddress(startPortal.address()).stream()
			.filter(e -> !e.equals(startPortal))
			.collect(Collectors.toList());

		// There has to be at least one potential destination portal with the same address
		if (potentialDestinationPortals.isEmpty()) return null;

		// Shuffle the destinations to make the exit portal random.
		Collections.shuffle(potentialDestinationPortals);

		ServerLevel destinationWorld = null;
		ResourceKey<Level> dimension;
		PortalRegistry.TeleportationDestination destination = null;
		MinecraftServer server = currentLevel.getServer();

		// Pick the first not blocked destination portal
		for (Portal portal : potentialDestinationPortals)
		{
			dimension = portal.dimension();
			destinationWorld = server.getLevel(dimension);
			if (destinationWorld == null) continue;

			destination = PortalRegistry.getTeleportDestination(portal, destinationWorld, entity);

			if (destination != null) break;
		}

		// Bypass the power cost for players in creative mode
		boolean bypassPowerCost = (entity instanceof ServerPlayer && ((ServerPlayer)entity).isCreative());

		if (destination != null && (bypassPowerCost || Settings.powerCost() == 0 || SimplePortals.PORTAL_REGISTRY.removePower(startPortal, Settings.powerCost())))
		{
			TeleportTransition.PostTeleportTransition postTransition = TeleportTransition.PLACE_PORTAL_TICKET;
			if (Settings.teleportationSoundEnabled()) postTransition = postTransition.then(TeleportTransition.PLAY_PORTAL_SOUND);
			Vec3 destinationVector = new Vec3(destination.pos().getX() + 0.5d, destination.pos().getY(), destination.pos().getZ() + 0.5d);

			return new TeleportTransition(destinationWorld, destinationVector, Vec3.ZERO, destination.facing().toYRot(), entity.xRotO, postTransition);
		}
		else
		{
			if (Settings.notEnoughPowerSoundEnabled()) currentLevel.playSound(null, portalEntryPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS);

			return null;
		}
	}

	@Override
	public Transition getLocalTransition()
	{
		return (Settings.transitionEffectEnabled()) ? Transition.CONFUSION : Transition.NONE;
	}

	private boolean handlePowerSourceEnteringPortal(Level world, BlockPos pos, Entity entity)
	{
		if (entity instanceof ItemEntity && Settings.powerCost() > 0 && Settings.powerCapacity() > 0)
		{
			if (Settings.powerSourceTag() == null)
			{
				SimplePortals.LOG.error("Misconfigured portal power source. The item tag '{}' could not be found.", Settings.powerSource());
				return false;
			}

			ItemStack itemStack = ((ItemEntity)entity).getItem();
			if (!itemStack.is(Settings.powerSourceTag())) return false;

			List<Portal> portals = SimplePortals.PORTAL_REGISTRY.getPortalsAt(pos, world.dimension());
			if (portals.isEmpty()) return false;

			MinecraftServer server = world.getServer();
			if (server == null) return false;

			Portal portal = portals.getFirst();

			if ((SimplePortals.PORTAL_REGISTRY.getPortalPower(portal) < Settings.powerCapacity()))
			{
				int surplus = SimplePortals.PORTAL_REGISTRY.addPower(portal, itemStack.getCount());

				SimplePortals.PORTAL_REGISTRY.updatePowerGauges((ServerLevel)world, portal);

				if (surplus > 0)
				{
					itemStack.setCount(surplus);
				}
				else
				{
					entity.discard();
				}

				return true;
			}
		}

		return false;
	}
}