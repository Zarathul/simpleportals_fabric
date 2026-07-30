package net.zarathul.simpleportals.common;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.zarathul.simpleportals.SimplePortals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Supplier;

/**
 * General utility class.
 */
public final class Utils
{
	/**
	 * Gets the localized formatted literal {@link Component} for the specified key.
	 *
	 * @param key
	 * The key for the localized string.
	 * @param parameters
	 * Formatting arguments.
	 * @return
	 * The localized formatted {@link Component}.
	 */
	public static Component translate(String key, Object... parameters)
	{
		Language I18N = Language.getInstance();
		return Component.literal(String.format(I18N.getOrDefault(key), parameters));
	}

	/**
	 * Gets translatable, formatted components for the specified key and formatting arguments.
	 *
	 * @param key
	 * The base key without an index (e.g. "myKey" gets "myKey0", "myKey1" ... etc.).
	 * @param args
	 * Formatting arguments.
	 * @return
	 * A list translatable components for the specified key, or an empty list if the key was not found.
	 */
	public static ArrayList<MutableComponent> multiLineTranslatable(String key, Object... args)
	{
		Language I18N = Language.getInstance();
		ArrayList<MutableComponent> lines = new ArrayList<>();

		if (key != null && !key.isEmpty())
		{
			int x = 0;
			String currentKey = key + x;

			while (I18N.has(currentKey))
			{
				lines.add(Component.translatable(currentKey, args));
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

		return Direction.getNearest(directionVec.getX(), directionVec.getY(), directionVec.getZ(), Direction.DOWN);
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
	 * Creates an Identifier with the Mod-ID as the namespace.
	 *
	 * @param path
	 * The path to create the Identifier for.
	 * @return
	 * An Identifier with <code>SimplePortals.MOD_ID</code> as the namespace, and <code>path</code> as the path.
	 */
	public static Identifier createModIdentifier(String path)
	{
		return Identifier.fromNamespaceAndPath(SimplePortals.MOD_ID, path);
	}

	/**
	 * Centers an object of a given size in a container at a specified offset.
	 *
	 * @param offset
	 * Offest at which the container resides.
	 * @param containerSize
	 * Size of the container.
	 * @param objectSize
	 * @return
	 * May return a negative value, in case the object size is bigger than the container.
	 */
	public static int centerIn(int offset, int containerSize, int objectSize)
	{
		return offset + (containerSize - objectSize) / 2;
	}

	/**
	 * Sends a localized message to the command source avoiding the issue with {@link Component#translatable(String, Object...)}
	 * throwing exceptions if the <code>args</code> look like an {@link Identifier}. First tries to get the localized text for
	 * the passed in <code>id</code>, then formats it using the passed in <code>args</code>.
	 * Used for command feedback.
	 *
	 * @param source
	 * The command source.
	 * @param id
	 * The localization {@link Identifier} as a string.
	 * @param args
	 * The arguments passed to {@link String#format(String, Object...)}.
	 */
	public static void SendTranslatedMessage(CommandSourceStack source, String id, Object... args)
	{
		// 'Component.translatable()' throws exceptions if something in 'args' looks like an identifier. So do the translation manually.

		var I18N = Language.getInstance();
		String translatedMessage = String.format(I18N.getOrDefault(id), args);
		source.sendSuccess(() -> Component.literal(translatedMessage), false);
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
		 * Gets passed as true to {@link net.minecraft.world.level.block.Block#onPlace(BlockState, Level, BlockPos, BlockState, boolean)}
		 * when set.
		* */
		public static final int IS_MOVING = 64;
		public static final int SKIP_LIGHTING_UPDATE = 128;

		public static final int FULL_UPDATE = DO_COMPARATOR_UPDATE | DO_BLOCK_UPDATE;
	}

	/**
	 * Gets a better readable string for the passed in BlockPos than what toString() offers.
	 * @return
	 * A string of the format "x=1234, z=1234, y=1234".
	 */
	public static String getReadableBlockPos(BlockPos pos)
	{
		return String.format("x=%d, z=%d, y=%d", pos.getX(), pos.getZ(), pos.getY());
	}

	/**
	 * Swaps the input parameters to {@code comparator} if {@code condition} returns {@code true}
	 *
	 * @param condition
	 * Determines if the input parameters to {@code comparator} will be swapped.
	 * @param comparator
	 * A comparator that gets it input parameters swapped based on {@code condition}.
	 * @param <T>
	 * Type of the object the {@code comparator} compares.
	 * @return
	 * A comparator that applies {@code comparator} with its own parameters but swapped, if {@code condition} returned {@code true}.
	 */
	public static <T> Comparator<T> invertComparator(Supplier<Boolean> condition, Comparator<T> comparator)
	{
		return (condition.get()) ? (a, b) -> comparator.compare(b, a) : comparator;
	}
}
