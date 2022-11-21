package net.zarathul.simpleportals.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlockArgument implements ArgumentType<Block>
{
	private static final DynamicCommandExceptionType INVALID_ADDRESS = new DynamicCommandExceptionType((args) -> Component.translatable("commands.errors.block_argument", args));
	private static final Collection<String> EXAMPLES = Arrays.asList(
		"minecraft:dirt",
		"minecraft:iron_block",
		"minecraft:white_wool");

	private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestionFuture;

	public static BlockArgument block()
	{
		return new BlockArgument();
	}

	public static Block getBlock(CommandContext<CommandSourceStack> context, String name)
	{
		return context.getArgument(name, Block.class);
	}

	@Override
	public Block parse(StringReader reader) throws CommandSyntaxException
	{
		suggestionFuture = (builder) -> SharedSuggestionProvider.suggest(Registry.BLOCK.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList()), builder);

		int i = reader.getCursor();
		ResourceLocation blockResourceLocation = ResourceLocation.read(reader);

		if (Registry.BLOCK.getOptional(blockResourceLocation).isEmpty())
		{
			reader.setCursor(i);
			throw INVALID_ADDRESS.createWithContext(reader, blockResourceLocation);
		}

		suggestionFuture = SuggestionsBuilder::buildFuture;

		return Registry.BLOCK.get(blockResourceLocation);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
	{
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());

		try
		{
			parse(reader);
		}
		catch (CommandSyntaxException ignored) {}

		return this.suggestionFuture.apply(builder.createOffset(reader.getCursor()));
	}

	@Override
	public Collection<String> getExamples()
	{
		return EXAMPLES;
	}
}
