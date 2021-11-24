package net.zarathul.simpleportals.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.common.TeleportTask;
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.mixin.EntityAccessor;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Represents the actual portals in the center of the portal multiblock.
 */
public class BlockPortal extends Block
{
	private static final VoxelShape X_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
	private static final VoxelShape Y_AABB = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 10.0D, 16.0D);
	private static final VoxelShape Z_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);

	public static final EnumProperty<Axis> AXIS = EnumProperty.create(
		"axis",
		Axis.class,
		Axis.X, Axis.Y, Axis.Z);

	public BlockPortal()
	{
		super(Properties.of(Material.PORTAL)
			.noDrops()
			 .noCollission()
			.strength(-1.0F) // indestructible by normal means
			.lightLevel((state) -> { return 11; })
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

		switch (portalAxis)
		{
			case Y: return Y_AABB;
			case Z: return Z_AABB;
			case X:
			default:
				return X_AABB;
		}
	}

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)
	{
		if (!world.isClientSide() && entity.isAlive() && !entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions() &&
			Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))), state.getShape(world, pos), BooleanOp.AND))
		{
			// For players a configurable cooldown is used instead of the value provided by getPortalCooldown(), because
			// that value is very small. A small value is fine for vanilla teleportation mechanics but can cause issues
			// for this mod.
			int cooldown = (entity instanceof ServerPlayer) ? Settings.playerTeleportationCooldown : entity.getDimensionChangingDelay();
			if (entity.isOnPortalCooldown()) return;

			List<Portal> portals = PortalRegistry.getPortalsAt(pos, entity.level.dimension());
			
			if (portals == null || portals.size() < 1) return;

			MinecraftServer mcServer = entity.getServer();
			if (mcServer == null) return;

			Portal start = portals.get(0);

			// Handle power source entering the portal
			
			if (entity instanceof ItemEntity && Settings.powerCost > 0 && Settings.powerCapacity > 0)
			{
				ItemStack itemStack = ((ItemEntity)entity).getItem();

				if (Settings.powerSourceTag == null)
				{
					SimplePortals.log.error("Misconfigured portal power source. The item tag '{}' could not be found.", Settings.powerSource);
					return;
				}

				if ((PortalRegistry.getPower(start) < Settings.powerCapacity) && itemStack.is(Settings.powerSourceTag))
				{
					int surplus = PortalRegistry.addPower(start, itemStack.getCount());

					PortalRegistry.updatePowerGauges((ServerLevel)world, start);
					
					if (surplus > 0)
					{
						itemStack.setCount(surplus);
					}
					else
					{
						entity.remove(Entity.RemovalReason.DISCARDED);
					}

					return;
				}
			}
			
			// Bypass the power cost for players in creative mode
			boolean bypassPowerCost = (entity instanceof ServerPlayer && ((ServerPlayer)entity).isCreative());
			
			// Check if portal has enough power for a port
			if (!bypassPowerCost && PortalRegistry.getPower(start) < Settings.powerCost) return;
			
			portals = PortalRegistry.getPortalsWithAddress(start.getAddress());
			
			if (portals == null || portals.size() < 2) return;
			
			// Get a shuffled list of possible destination portals (portals with the same address)
			List<Portal> destinations = portals.stream()
				.filter(e -> !e.equals(start))
				.collect(Collectors.toList());
			
			if (destinations.size() > 0)
			{
				Collections.shuffle(destinations);

				int entityHeight = Mth.ceil(entity.getBbHeight());
				ServerLevel destinationWorld;
				ResourceKey<Level> dimension;
				BlockPos destinationPos = null;
				Portal destinationPortal = null;

				// Pick the first not blocked destination portal
				for (Portal portal : destinations)
				{
					dimension = portal.getDimension();
					if (dimension == null) continue;

					destinationWorld = mcServer.getLevel(dimension);
					destinationPos = portal.getPortDestination(destinationWorld, entityHeight);
					
					if (destinationPos != null)
					{
						destinationPortal = portal;
						break;
					}
				}
				
				if ((destinationPos != null) && (bypassPowerCost || Settings.powerCost == 0 || PortalRegistry.removePower(start, Settings.powerCost)))
				{
					// Get a facing pointing away from the destination portal. After porting, the portal 
					// will always be behind the entity. When porting to a horizontal portal the initial
					// facing is not changed.
					Direction entityFacing = (destinationPortal.getAxis() == Axis.Y)
						? entity.getDirection()
						: (destinationPortal.getAxis() == Axis.Z)
						? (destinationPos.getZ() > destinationPortal.getCorner1().getPos().getZ())
						? Direction.SOUTH
						: Direction.NORTH
						: (destinationPos.getX() > destinationPortal.getCorner1().getPos().getX())
						? Direction.EAST
						: Direction.WEST;
					
					if (entity instanceof ServerPlayer)
					{
						// Player teleportations are queued to avoid at least some of the problems that arise from
						// handling player teleportation inside an entity collision handler. There seem to be all
						// kinds of weird race conditions of movement packets that trigger the dreaded "moved wrongly"
						// and "moved to quickly" checks in 'ServerGamePacketListenerImpl.handleMovePlayer()'. No idea
						// why end portals don't have these problems, considering that I use the same copy and pasted
						// code minus the platform generation stuff.
						try
						{
							SimplePortals.TELEPORT_QUEUE.put(new TeleportTask(
									mcServer.getTickCount(),
									(ServerPlayer)entity,
									destinationPortal.getDimension(),
									destinationPos,
									entityFacing));
						}
						catch (InterruptedException ex)
						{
							SimplePortals.log.error("Failed to enqueue teleportation task for player '{}' to dimension '{}'.",
													((ServerPlayer)entity).getName(),
													destinationPortal.getDimension() != null);
						}
					}
					else
					{
						entity = Utils.teleportTo(entity, destinationPortal.getDimension(), destinationPos, entityFacing);
					}

					PortalRegistry.updatePowerGauges((ServerLevel)world, start);
				}
			}

			// Put the entity on "cooldown" in order to prevent it from instantly porting again.
			((EntityAccessor)entity).setPortalCooldown(cooldown);
		}
	}

	@Override
	public void onRemove(BlockState oldState, Level world, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!world.isClientSide())
		{
			// Deactivate damaged portals.
			
			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());
			
			if (affectedPortals == null || affectedPortals.size() < 1) return;
			
			Portal firstPortal = affectedPortals.get(0);
			ServerLevel serverWorld = (ServerLevel)world;
			
			if (firstPortal.isDamaged(serverWorld))
			{
				PortalRegistry.deactivatePortal(serverWorld, pos);
			}
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ItemStack getCloneItemStack(BlockGetter getter, BlockPos pos, BlockState state)
	{
		return ItemStack.EMPTY;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, Random random)
	{
		if (Settings.ambientSoundEnabled && random.nextInt(100) == 0)
		{
			world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
		}

		if (Settings.particlesEnabled)
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
					d3 = (double)(random.nextFloat() * 2.0F * (float)j);
				}
				else if (portalAxis == Axis.Y)
				{
					d1 = (double)pos.getY() + 0.5D + 0.25D * (double)j;
					d4 = (double)(random.nextFloat() * 2.0F * (float)j);
				}
				else
				{
					d2 = (double)pos.getZ() + 0.5D + 0.25D * (double)j;
					d5 = (double)(random.nextFloat() * 2.0F * (float)j);
				}

				world.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
			}
		}
	}
}