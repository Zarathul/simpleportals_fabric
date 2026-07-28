package net.zarathul.simpleportals.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.zarathul.simpleportals.common.Utils;

import java.util.Set;

public class CommandTeleport
{
	private enum TeleportMode
	{
		ToPlayer,
		ToPosition
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(
			Commands.literal("tpd").requires((commandSource) -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
			.executes(context -> {
				Utils.SendTranslatedMessage(context.getSource(), "commands.tpd.info");
				return 1;
			})
			.then(
				Commands.argument("dimension", DimensionArgument.dimension())
				.executes(context -> tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension"), null, null, null))
				.then(
					Commands.argument("position", BlockPosArgument.blockPos())
					.executes(context -> tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension"), BlockPosArgument.getLoadedBlockPos(context, "position"), null, null))
					.then(
						Commands.argument("player", EntityArgument.player())		// tpd <dimension> [<x> <y> <z>] [player]
						.executes(context -> tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension"), BlockPosArgument.getLoadedBlockPos(context, "position"), null, EntityArgument.getPlayer(context, "player")))
					)
				)
			)
			.then(
				Commands.argument("targetPlayer", EntityArgument.player())
				.executes(context -> tp(context.getSource(), TeleportMode.ToPlayer, null, null, EntityArgument.getPlayer(context, "targetPlayer"), null))
				.then(
					Commands.argument("player", EntityArgument.player())		// tpd <targetPlayer> [player]
					.executes(context -> tp(context.getSource(), TeleportMode.ToPlayer, null, null, EntityArgument.getPlayer(context, "targetPlayer"), EntityArgument.getPlayer(context, "player")))
				)
			)
		);
	}

	private static int tp(CommandSourceStack source, TeleportMode mode, ServerLevel dimension, BlockPos destination, ServerPlayer targetPlayer, ServerPlayer player) throws CommandSyntaxException
	{
		if (player == null)
		{
			try
			{
				player = source.getPlayerOrException();
			}
			catch (CommandSyntaxException ex)
			{
				throw new SimpleCommandExceptionType(Component.translatable("commands.errors.unknown_sender")).create();
			}
		}

		switch (mode)
		{
			case ToPosition:
				if (destination == null) destination = player.blockPosition();
				break;

			case ToPlayer:
				destination = targetPlayer.blockPosition();
				dimension = targetPlayer.level();

				break;
		}

		player.teleportTo(
			source.getServer().getLevel(dimension.dimension()),
			destination.getX(),
			destination.getY(),
			destination.getZ(),
			Set.of(),
			player.getYRot(),
			player.getXRot(),
			false
		);
		Utils.SendTranslatedMessage(source, "commands.tpd.success", player.getName().getString(), destination.getX(), destination.getY(), destination.getZ(), dimension.dimension().identifier());

		return 1;
	}
}
