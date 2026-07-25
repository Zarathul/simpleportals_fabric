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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.common.TeleportTask;
import net.zarathul.simpleportals.mixin.EntityAccessor;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the actual portals in the center of the portal multi-block.
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

	public BlockPortal(ResourceKey<Block> id)
	{
		super(Properties.of()
			.setId(id)
			.noLootTable()
			.noCollision()
			.strength(-1.0F) // indestructible by normal means
			.pushReaction(PushReaction.BLOCK)
			.lightLevel((state) -> 11)
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
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise)
	{
		if (!world.isClientSide() && entity.isAlive() && !entity.isPassenger() && !entity.isVehicle() && entity.canUsePortal(false) &&
			Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ())), state.getShape(world, pos), BooleanOp.AND))
		{
			// For players a configurable cooldown is used instead of the value provided by getPortalCooldown(), because
			// that value is very small. A small value is fine for vanilla teleportation mechanics but can cause issues
			// for this mod.
			int cooldown = (entity instanceof ServerPlayer) ? Settings.playerTeleportationCooldown : entity.getDimensionChangingDelay();
			if (entity.isOnPortalCooldown()) return;

			Level entityLevel = null;
			try (var level = entity.level())
			{
				entityLevel = level;
			}
			catch (Exception _) { return; }

			List<Portal> portals = SimplePortals.portalRegistry.getPortalsAt(pos, entityLevel.dimension());
			
			if (portals == null || portals.isEmpty()) return;

			MinecraftServer mcServer = entityLevel.getServer();
			if (mcServer == null) return;

			Portal start = portals.getFirst();

			// Handle power source entering the portal
			
			if (entity instanceof ItemEntity && Settings.powerCost > 0 && Settings.powerCapacity > 0)
			{
				ItemStack itemStack = ((ItemEntity)entity).getItem();

				if (Settings.powerSourceTag == null)
				{
					SimplePortals.log.error("Misconfigured portal power source. The item tag '{}' could not be found.", Settings.powerSource);
					return;
				}

				if ((SimplePortals.portalRegistry.getPortalPower(start) < Settings.powerCapacity) && itemStack.is(Settings.powerSourceTag))
				{
					int surplus = SimplePortals.portalRegistry.addPower(start, itemStack.getCount());

					SimplePortals.portalRegistry.updatePowerGauges((ServerLevel)world, start);
					
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
			if (!bypassPowerCost && SimplePortals.portalRegistry.getPortalPower(start) < Settings.powerCost) return;
			
			portals = SimplePortals.portalRegistry.getPortalsWithAddress(start.address());
			
			if (portals == null || portals.size() < 2) return;
			
			// Get a shuffled list of possible destination portals (portals with the same address)
			List<Portal> destinations = portals.stream()
				.filter(e -> !e.equals(start))
				.collect(Collectors.toList());
			
			if (!destinations.isEmpty())
			{
				Collections.shuffle(destinations);

				int entityHeight = Mth.ceil(entity.getBbHeight());
				ServerLevel destinationWorld = null;
				ResourceKey<Level> dimension = null;
				BlockPos destinationPos = null;
				Portal destinationPortal = null;

				// Pick the first not blocked destination portal
				for (Portal portal : destinations)
				{
					dimension = portal.dimension();
					if (dimension == null) continue;

					destinationWorld = mcServer.getLevel(dimension);
					destinationPos = portal.getPortDestination(destinationWorld, entityHeight);
					
					if (destinationPos != null)
					{
						destinationPortal = portal;
						break;
					}
				}
				
				if ((destinationPos != null && destinationWorld != null) && (bypassPowerCost || Settings.powerCost == 0 || SimplePortals.portalRegistry.removePower(start, Settings.powerCost)))
				{
					// Get a facing pointing away from the destination portal. After porting, the portal 
					// will always be behind the entity. When porting to a horizontal portal the initial
					// facing is not changed.
					Direction entityFacing = (destinationPortal.axis() == Axis.Y)
						? entity.getDirection()
						: (destinationPortal.axis() == Axis.Z)
						? (destinationPos.getZ() > destinationPortal.corner1().pos().getZ())
						? Direction.SOUTH
						: Direction.NORTH
						: (destinationPos.getX() > destinationPortal.corner1().pos().getX())
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
									destinationPortal.dimension(),
									destinationPos,
									entityFacing));
						}
						catch (InterruptedException ex)
						{
							SimplePortals.log.error("Failed to enqueue teleportation task for player '{}' to dimension '{}'.",
													entity.getName(),
													destinationPortal.dimension());
						}
					}
					else
					{
//						entity = Utils.teleportTo(entity, destinationPortal.getDimension(), destinationPos, entityFacing);
						entity.teleportTo(
							mcServer.getLevel(destinationPortal.dimension()),
							destinationPos.getX(),
							destinationPos.getY(),
							destinationPos.getZ(),
							Set.of(),
							entity.getXRot(),
							entity.getYRot(),
							false
						);
					}

					SimplePortals.portalRegistry.updatePowerGauges((ServerLevel)world, start);
				}
			}

			// Put the entity on "cooldown" in order to prevent it from instantly porting again.
			((EntityAccessor)entity).setPortalCooldown(cooldown);
		}
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state)
	{
		if (!level.isClientSide())
		{
			// Deactivate damaged portals.

			ServerLevel serverLevel = (ServerLevel)level;
			List<Portal> affectedPortals = SimplePortals.portalRegistry.getPortalsAt(pos, serverLevel.dimension());
			
			if (affectedPortals.isEmpty()) return;
			
			Portal firstPortal = affectedPortals.getFirst();

			if (firstPortal.isDamaged(serverLevel))
			{
				SimplePortals.portalRegistry.deactivatePortal(serverLevel, pos);
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
}