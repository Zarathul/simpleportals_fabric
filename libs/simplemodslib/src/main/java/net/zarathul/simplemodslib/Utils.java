package net.zarathul.simplemodslib;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.function.Supplier;

/**
 * General utility class.
 */
public final class Utils
{
	/**
	 * Get the formatted localized string literal for the specified key and formatting arguments.
	 *
	 * @param key
	 * The localization key.
	 * @param args
	 * Formatting arguments.
	 * @return
	 * The formatted, localized string. The key itself, if no localized string was found. Or the key with {@code " :: Format Error"} appended, if formatting failed.
	 */
	public static String translate(String key, Object... args)
	{
		Language I18N = Language.getInstance();
		if ((key != null) && I18N.has(key))
		{
			String text = I18N.getOrDefault(key);

			try
			{
				return String.format(text, args);
			}
			catch (IllegalFormatException _)
			{
				return key + " :: Format Error";
			}
		}

		return key;
	}

	/**
	 * Gets the localized formatted literal {@link Component} for the specified key.
	 *
	 * @param key
	 * The key for the localized string.
	 * @param args
	 * Formatting arguments.
	 * @return
	 * The localized formatted {@link Component}.
	 */
	public static Component getTranslatedComponent(String key, Object... args)
	{
		return Component.literal(translate(key, args));
	}

	/**
	 * Gets the localized formatted components for the specified key and formatting arguments, split at new lines ('\n')
	 * in the localized text.
	 *
	 * @param key
	 * The localization key.
	 * @param args
	 * Formatting arguments.
	 * @return
	 * A list of localized text components for the specified key, or an empty list if the key was not found.
	 */
	public static ArrayList<Component> multiLineTranslate(String key, Object... args)
	{
		ArrayList<Component> components = new ArrayList<>();

		String text = translate(key, args);
		String[] lines = text.split("\\n");

		for (String line : lines)
		{
			components.add(Component.literal(line));
		}

		return components;
	}

	/**
	 * Gets the localized formatted components for the specified key and formatting arguments, split at new lines ('\n')
	 * in the localized text and the resulting segments at {@code maxWidth}.<br>
	 * Note: {@link net.minecraft.client.gui.Font#splitIgnoringLanguage(FormattedText, int)} is used to split at {@code maxWidth}
	 * which, as the name suggests, will probably not work with all languages. Styles are also not preserved at the site of the split.
	 * A workaround is to style words individually, e.g. instead of {@code §3Foo Bar§r} use {@code §3Foo §3Bar§r}.
	 * Unfortunately, {@link net.minecraft.client.gui.Font#split(FormattedText, int)}
	 * produces a list of FormattedCharSequences, which are meant for rendering and have equally broken styling.
	 *
	 * @param key
	 * The localization key.
	 * @param maxWidth
	 * The maximum width at which to split the segment.
	 * @param args
	 * Formatting arguments.
	 * @return
	 * A list of localized text components for the specified key, or an empty list if the key was not found.
	 */
	@Environment(EnvType.CLIENT)
	public static ArrayList<Component> multiLineTranslateWithMaxWidth(String key, int maxWidth, Object... args)
	{
		var mc = Minecraft.getInstance();

		ArrayList<Component> components = new ArrayList<>();
		String text = translate(key, args);
		String[] fixedSegments = text.split("\\n");

		for (var segment : fixedSegments)
		{
			FormattedText formattedText = FormattedText.of(segment);
			List<FormattedText> lines = mc.font.splitIgnoringLanguage(formattedText, maxWidth);

			for (var line : lines)
			{
				components.add(Component.literal(line.getString()));
			}
		}

		return components;
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

	/**
	 * Calculates the comparator redstone signal strength based on the quotient of the specified values.
	 *
	 * @param numerator
	 * The numerator.
	 * @param denominator
	 * The denominator.
	 * @return
	 * A value between 0 and 15.
	 */
	public static int getComparatorLevel(float numerator, float denominator)
	{
		int level = (denominator != 0) ? ((int) Math.floor((numerator / denominator) * 14.0f)) + ((numerator > 0) ? 1 : 0) : 0;

		return level;
	}

	// Belongs to getMetricFormattedNumber
	private static final int FACTOR = 1000;
	private static final double FACTOR_LOG = Math.log(FACTOR);
	private static final char[] METRIC_SUFFIXES = { 'k', 'M', 'G', 'T', 'P', 'E' };

	/**
	 * Shortens a number using metric suffixes and applies the provided format and locale.
	 *
	 * @param number
	 * The number to shorten. Numbers lower than 1000 remain unchanged.
	 * @param shortFormat
	 * The string format to apply in case {@code number} gets shortened (3 arguments). The first argument is the
	 * shortened number (floating point), the second is the metric suffix and the third is the passed in object
	 * ({@code misc}) which can be anything.
	 * @param longFormat
	 * The string format to apply in case {@code number} is not shortened (2 arguments). The first argument is the
	 * unmodified number (decimal) and the second is the passed in object ({@code misc}) which can be anything.
	 * @param locale
	 * The locale to use for the number format.
	 * @param misc
	 * Can be used to add additional text to the output string.
	 * @return
	 * <c>null</c> if either {@code shortFormat}, {@code longFormat} or {@code locale} is <c>null</c>, otherwise the
	 * potentially shortened and formatted number.
	 */
	public static String getMetricFormattedNumber(long number, String shortFormat, String longFormat, Locale locale, Object misc)
	{
		if (shortFormat == null || longFormat == null || locale == null) return null;
		if (number < FACTOR) return String.format(locale, longFormat, number, misc);

		int exponent = (int)(Math.log(number) / FACTOR_LOG);

		return String.format(locale, shortFormat, number / Math.pow(FACTOR, exponent), METRIC_SUFFIXES[exponent - 1], misc);
	}

	/**
	 * Shortens a number using metric suffixes and applies the provided format (UK locale).
	 *
	 * @param number
	 * The number to shorten. Numbers lower than 1000 remain unchanged.
	 * @param shortFormat
	 * The string format to apply in case {@code number} gets shortened (3 arguments). The first argument is the
	 * shortened number (floating point), the second is the metric suffix and the third is the passed in object
	 * ({@code misc}) which can be anything.
	 * @param longFormat
	 * The string format to apply in case {@code number} is not shortened (2 arguments). The first argument is the
	 * unmodified number (decimal) and the second is the passed in object ({@code misc}) which can be anything.
	 * @param misc
	 * Can be used to add additional text to the output string.
	 * @return
	 * <c>null</c> if either {@code shortFormat}, {@code longFormat} or {@code locale} is <c>null</c>, otherwise the
	 * potentially shortened and formatted number.
	 */
	public static String getMetricFormattedNumber(long number, String shortFormat, String longFormat, Object misc)
	{
		return getMetricFormattedNumber(number, shortFormat, longFormat, Locale.UK, misc);
	}

	/**
	 * Cache for {@code isInterfaceAvailable()} return values.
	 */
	private static final HashMap<String, Boolean> InterfaceLookupCache = new HashMap<>();

	/**
	 * Checks if the given interface is available. This is used to call into APIs of other mods that may not always be
	 * there.
	 *
	 * @param packageName
	 * The name of the package containing the interface.
	 * @param interfaceName
	 * The name of the interface to check.
	 *
	 * @return
	 * <c>true</c> if the interface exists, otherwise <c>false</c>.
	 */
	public static boolean isInterfaceAvailable(String packageName, String interfaceName)
	{
		String FullyQualifiedName = packageName + "." + interfaceName;

		if (InterfaceLookupCache.containsKey(FullyQualifiedName)) return InterfaceLookupCache.get(FullyQualifiedName);

		try
		{
			Class<?> Interface = Class.forName(FullyQualifiedName);
			InterfaceLookupCache.put(FullyQualifiedName, true);

			return true;
		}
		catch (Exception e)
		{
			InterfaceLookupCache.put(FullyQualifiedName, false);

			return false;
		}
	}
}
