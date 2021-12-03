package net.zarathul.simpleportals.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.mixin.DirectionAccessor;
import net.zarathul.simpleportals.mixin.EntityAccessor;
import net.zarathul.simpleportals.mixin.ServerPlayerAccessor;

import java.util.*;

/**
 * General utility class.
 */
public final class Utils
{
	private static final Language I18N = Language.getInstance();

	/**
	 * Gets the localized formatted string for the specified key.
	 *
	 * @param key
	 * The key for the localized string.
	 * @param parameters
	 * Formatting arguments.
	 * @return
	 * The localized formatted string.
	 */
	public static String translate(String key, Object... parameters)
	{
		return String.format(I18N.getOrDefault(key), parameters);
	}

	/**
	 * Gets the localized formatted strings for the specified key and formatting arguments.
	 *
	 * @param key
	 * The base key without an index (e.g. "myKey" gets "myKey0", "myKey1" ... etc.).
	 * @param args
	 * Formatting arguments.
	 * @return
	 * A list of localized strings for the specified key, or an empty list if the key was not found.
	 */
	public static ArrayList<TextComponent> multiLineTranslate(String key, Object... args)
	{
		ArrayList<TextComponent> lines = new ArrayList<>();

		if (key != null)
		{
			int x = 0;
			String currentKey = key + x;

			while (I18N.has(currentKey))
			{
				lines.add(new TextComponent(String.format(I18N.getOrDefault(currentKey), args)));
				currentKey = key + ++x;
			}
		}

		return lines;
	}

	/**
	 * Gets the coordinate component of a BlockPos for the specified axis.
	 *
	 * @param pos
	 * The coordinate to choose the component from.
	 * @param axis
	 * The axis representing the coordinate component to choose.
	 * @return
	 * <code>0</code> if either pos or axis are <code>null</code>, otherwise the chosen coordinate component.
	 */
	public static int getAxisValue(BlockPos pos, Axis axis)
	{
		if (pos == null || axis == null) return 0;

		if (axis == Axis.X) return pos.getX();
		if (axis == Axis.Y)	return pos.getY();
		if (axis == Axis.Z)	return pos.getZ();

		return 0;
	}

	/**
	 * Gets the relative direction from one {@link BlockPos} to another.
	 *
	 * @param from
	 * The starting point.
	 * @param to
	 * The end point.
	 * @return
	 * One of the {@link Direction} values or <code>null</code> if one of the arguments was <code>null</code>.
	 */
	public static Direction getRelativeDirection(BlockPos from, BlockPos to)
	{
		if (from == null || to == null) return null;

		BlockPos directionVec = to.subtract(from);
		// Normalize, because Direction.fromNormal uses a map lookup and expects a vector with only 0, 1 and -1 as values.
		directionVec = new BlockPos(
				(directionVec.getX() == 0) ? 0 : directionVec.getX() / Mth.abs(directionVec.getX()),
				(directionVec.getY() == 0) ? 0 : directionVec.getY() / Mth.abs(directionVec.getY()),
				(directionVec.getZ() == 0) ? 0 : directionVec.getZ() / Mth.abs(directionVec.getZ()));

		return Direction.fromNormal(directionVec.getX(), directionVec.getY(), directionVec.getZ());
	}

	/**
	 * Gets the axis that is orthogonal to, and on the same plane as the specified one.
	 *
	 * @param axis
	 * The starting axis.
	 * @return
	 * One of the {@link Axis} values or <code>null</code> if the specified axis was <code>null</code> or
	 * there is no other axis on the same plane.
	 */
	public static Axis getOrthogonalTo(Axis axis)
	{
		if (axis == null || axis == Axis.Y) return null;

		return (axis == Axis.X) ? Axis.Z : Axis.X;
	}

	/**
	 * Teleport an entity to the specified position in the specified dimensionId
	 * facing the specified direction.
	 *
	 * @param entity
	 * The entity to teleport. Can be any entity (item, mob, player).
	 * @param dimension
	 * The dimension to port to.
	 * @param destination
	 * The position to port to.
	 * @param facing
	 * The direction the entity should face after porting.
	 * @return
	 * The entity after the teleportation process. For non-player entities, if the teleportation was successful,
	 * this will be a different entity than the one passed in. This is the case, because the entity needs to be
	 * recreated in the destination dimension.
	 */
	public static Entity teleportTo(Entity entity, ResourceKey<Level> dimension, BlockPos destination, Direction facing)
	{
		if (entity == null || dimension == null || destination == null || !entity.canChangeDimensions() || entity.isVehicle() || entity.isPassenger()) return entity;

		ServerPlayer player = (entity instanceof ServerPlayer) ? (ServerPlayer) entity : null;
		boolean interdimensional = (entity.getCommandSenderWorld().dimension() != dimension);
		entity.setDeltaMovement(Vec3.ZERO);

		if (player != null)
		{
			if (interdimensional)
			{
				teleportPlayerToDimension(player, dimension, destination, getYaw(facing), 0.0f);
			}
			else
			{
				player.connection.teleport(destination.getX() + 0.5d,
										   destination.getY(),
										   destination.getZ() + 0.5d,
										   getYaw(facing),
										   0.0f);
			}

			// Play teleportation sound.
			if (Settings.teleportationSoundEnabled) player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
		}
		else
		{
			if (interdimensional)
			{
				return teleportNonPlayerEntityToDimension(entity, dimension, destination, getYaw(facing));
			}
			else
			{
				entity.moveTo(destination.getX() + 0.5d,
							  destination.getY(),
							  destination.getZ() + 0.5d,
							  getYaw(facing),
							  0.0f);
			}
		}

		return entity;
	}

	private static void teleportPlayerToDimension(ServerPlayer player, ResourceKey<Level> dimensionKey, BlockPos destination, float yaw, float pitch)
	{
		// Setting this flag circumvents at least a part of the shitty speed hack checks in
		// 'ServerGamePacketListenerImpl.handleMovePlayer()' that cause nothing but trouble.
		((ServerPlayerAccessor)player).setIsChangingDimension(true);

		MinecraftServer server = player.getServer();
		if (server == null) return;

		ServerLevel destinationDimension = server.getLevel(dimensionKey);
		if (destinationDimension == null) return;

		ServerLevel originDimension = player.getLevel();
		LevelData levelData = destinationDimension.getLevelData();

		player.connection.send(new ClientboundRespawnPacket(destinationDimension.dimensionType(), destinationDimension.dimension(), BiomeManager.obfuscateSeed(destinationDimension.getSeed()), player.gameMode.getGameModeForPlayer(), player.gameMode.getPreviousGameModeForPlayer(), destinationDimension.isDebug(), destinationDimension.isFlat(), true));
		player.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));

		PlayerList playerList = player.server.getPlayerList();
		playerList.sendPlayerPermissionLevel(player);
		originDimension.removePlayerImmediately(player, Entity.RemovalReason.CHANGED_DIMENSION);
		// player.remove(Entity.RemovalReason.CHANGED_DIMENSION); FIXME: No longer needed ?

		player.setLevel(destinationDimension);
		destinationDimension.addDuringPortalTeleport(player);

		player.setYRot(yaw);
		player.setXRot(pitch);
		player.moveTo(destination.getX() + 0.5d, destination.getY(), destination.getZ() + 0.5d);

		player.setDeltaMovement(Vec3.ZERO);

		player.gameMode.setLevel(destinationDimension);
		player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
		playerList.sendLevelInfo(player, destinationDimension);
		playerList.sendAllPlayerInfo(player);

		for (MobEffectInstance effect : player.getActiveEffects())
		{
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
		}

		player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
		player.giveExperienceLevels(0);	// This is just to set lastSentExp to -1
		player.resetSentInfo();	// Set lastSentHealth to -1.0F
		((ServerPlayerAccessor)player).setLastSendFood(-1);
	}

	/**
	 * Teleport a non-player entity to the specified position in the specified dimension
	 * facing the specified direction.
	 * ({@link Entity#changeDimension(ServerLevel)} without the hardcoded dimension specific vanilla code)
	 *
	 * @param entity
	 * The entity to teleport. Can be any entity except players (e.g. item, mob).
	 * @param dimensionKey
	 * The dimension to port to.
	 * @param destination
	 * The position to port to.
	 * @param yaw
	 * The rotation yaw the entity should have after porting.
	 * @return
	 * The entity after the teleportation process. If the teleportation was successful, this will be a different entity
	 * than the one passed in. This is the case, because the entity needs to be recreated in the destination dimension.
	 */
	private static Entity teleportNonPlayerEntityToDimension(Entity entity, ResourceKey<Level> dimensionKey, BlockPos destination, float yaw)
	{
		if (entity.level instanceof ServerLevel && !entity.isRemoved())
		{
			MinecraftServer server = entity.getServer();
			if (server == null)	return entity;

			ServerLevel destinationWorld = server.getLevel(dimensionKey);
			if (destinationWorld == null) return entity;

			ServerLevel originWorld = (ServerLevel)entity.level;

			entity.unRide();
			Entity newEntity = entity.getType().create(destinationWorld);

			if (newEntity != null)
			{
				newEntity.restoreFrom(entity);
				newEntity.moveTo(destination.getX(), destination.getY(), destination.getZ(), yaw, newEntity.getXRot());
				newEntity.setDeltaMovement(Vec3.ZERO);
				//destinationWorld.addFromAnotherDimension(newEntity); FIXME: remove
				destinationWorld.addDuringTeleport(newEntity);
			}

			// This mixin might be overkill, since all this method does 99.9% of the time is setting Entity.removed
			// to true, but who knows what other peoples mods do. Better safe than sorry I guess.
			((EntityAccessor)entity).invokeRemoveAfterChangingDimensions();

			originWorld.resetEmptyTime();
			destinationWorld.resetEmptyTime();

			return (newEntity != null) ? newEntity : entity;
		}

		return entity;
	}

	/**
	 * Converts the specified facing to a degree value.
	 *
	 * @param facing
	 * The facing to convert.
	 * @return
	 * <code>0</code> if facing is <code>null</code>, otherwise a value between <code>0</code> and <code>270</code> that
	 * is a multiple of <code>90</code>.
	 */
	public static float getYaw(Direction facing)
	{
		if (facing == null) return 0;

		float yaw = switch (facing)
		{
			case EAST  -> 270.0f;
			case WEST  -> 90.0f;
			case NORTH -> 180.0f;
			default    -> 0.0f;
		};

		return yaw;
	}

	/**
	 * Checks if a string is a valid representation of a ResourceLocation.
	 * Allowed characters in the namespace are: [a-z0-9_.-]
	 * Allowed characters in the path are: [a-z0-9._-/]
	 * Namespace and path are separated by [:].
	 *
	 * @param locationString
	 * The string to check.
	 * @return
	 * <code>true</code> if only valid characters where found, otherwise <code>false</code>.
	 */
	public static boolean isValidResourceLocation(String locationString)
	{
		if (locationString == null || locationString.isEmpty()) return false;

		String[] components = locationString.split(":", 2);
		if (components.length != 2 || components[0].length() == 0 || components[1].length() == 0) return false;

		String namespace = components[0];
		String      path = components[1];

		return (namespace.chars().allMatch(c -> (c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.')) &&
					 path.chars().allMatch(c -> (c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.' || c == '/')));
	}

	/**
	 * Flags for {@link net.minecraft.world.level.Level#setBlock(BlockPos, BlockState, int)}.
	 */
	public static final class SetBlockFlags
	{
		public static final int NONE = 0;
		public static final int DO_COMPARATOR_UPDATE = 1;
		public static final int DO_BLOCK_UPDATE = 2;
		public static final int SKIP_CLIENT_UPDATE = 4;
		public static final int SKIP_NEIGHBOUR_SHAPE_UPDATE = 16;
		/**
		 * Unsure about that name (IS_MOVING). Forge called it that way.
		 * Gets passed as true to {@link net.minecraft.world.level.block.Block#onRemove(BlockState, Level, BlockPos, BlockState, boolean)}
		 * when set.
		* */
		public static final int IS_MOVING = 64;
		public static final int SKIP_LIGHTING_UPDATE = 128;

		public static final int FULL_UPDATE = DO_COMPARATOR_UPDATE | DO_BLOCK_UPDATE;
	}

	private static final Map<String, Direction> DIRECTION_NAME_MAP = DirectionAccessor.getByNameMap();

	/**
	 * Get a direction by name.
	 * For some reason this is not available in the server environment, that's why this method exists.
	 * @param name
	 * The name representing the direction.
	 * @return
	 * A <code>Direction</code> if a valid name was passed, otherwise <code>null</code>.
	 */
	public static Direction getDirectionByName(String name)
	{
		return (name == null) ? null : DIRECTION_NAME_MAP.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Gets a better readable string for the passed in BlockPos than what toString() offers.
	 * @return
	 * A string of the format "x=1234, z=1234, y=1234".
	 */
	public static String getReadablyBlockPos(BlockPos pos)
	{
		return String.format("x=%d, z=%d, y=%d", pos.getX(), pos.getZ(), pos.getY());
	}
}
