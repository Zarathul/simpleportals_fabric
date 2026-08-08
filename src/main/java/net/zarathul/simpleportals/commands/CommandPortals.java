package net.zarathul.simpleportals.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.zarathul.simplemodslib.api.configuration.Config;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.SimplePortals;
import net.zarathul.simpleportals.commands.arguments.BlockArgument;
import net.zarathul.simpleportals.common.Utils;
import net.zarathul.simpleportals.mixin.EntityAccessor;
import net.zarathul.simpleportals.registration.Address;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.List;
import java.util.stream.Collectors;

import static net.zarathul.simpleportals.SimplePortals.portalRegistry;

public class CommandPortals
{
	private static final SimpleCommandExceptionType ERROR_UNKNOWN_DIMENSION = new SimpleCommandExceptionType(Component.translatable("commands.errors.unknown_sender_dimension"));

	private enum DeactivateMode
	{
		Address,
		Position
	}

	private enum PowerMode
	{
		Add,
		Remove,
		Get,
		Set,
		Items
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(
			Commands.literal("sportals")
			.executes(context -> {
				Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.info");
				return 1;
			})
			.then(
				Commands.literal("config")
				.executes(Config::executeCommand)
			)
			.then(
				Commands.literal("list")
				.requires(commandSource -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
				.executes(context -> {
					var player = context.getSource().getPlayer();

					if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER))
					{
						Utils.SendTranslatedMessage(context.getSource(), "error.missing_permission");
						return 0;
					}

					SimplePortals.ListCommandPayload.send(player);

					return 1;
				})
			)
			.then(
				Commands.literal("deactivate")
				.requires((commandSource) -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
				.executes(context -> {
					Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.deactivate.info");
					return 1;
				})
				.then(
					Commands.argument("address1", BlockArgument.block())
					.then(
						Commands.argument("address2", BlockArgument.block())
						.then(
							Commands.argument("address3", BlockArgument.block())
							.then(
								Commands.argument("address4", BlockArgument.block())
								.executes(context -> {
									Address address = new Address(PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address1")),
										PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address2")),
										PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address3")),
										PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address4")));

									return deactivate(context.getSource(), DeactivateMode.Address, address, null, null);
								})
								.then(
									Commands.argument("dimension", DimensionArgument.dimension())		// sportals deactivate <addressBlockId> <addressBlockId> <addressBlockId> <addressBlockId> [dimension]
									.executes(context -> {
										Address address = new Address(PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address1")),
											PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address2")),
											PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address3")),
											PortalRegistry.getAddressBlockId(BlockArgument.getBlock(context, "address4")));

										return deactivate(context.getSource(), DeactivateMode.Address, address, null, DimensionArgument.getDimension(context, "dimension"));
									})
								)
							)
						)
					)
				)
				.then(
					Commands.argument("position", BlockPosArgument.blockPos())
					.executes(context -> deactivate(context.getSource(), DeactivateMode.Position, null, BlockPosArgument.getLoadedBlockPos(context, "position"), null))
					.then(
						Commands.argument("dimension", DimensionArgument.dimension())		// sportals deactivate <x> <y> <z> [dimension]
						.executes(context -> deactivate(context.getSource(), DeactivateMode.Position, null, BlockPosArgument.getLoadedBlockPos(context, "position"), DimensionArgument.getDimension(context, "dimension")))
					)
				)
			)
			.then(
				Commands.literal("power")
				.requires((commandSource) -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
				.executes(context -> {
					Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.power.info");
					return 1;
				})
				.then(
					Commands.literal("add")
					.then(
						Commands.argument("amount", IntegerArgumentType.integer(1))
						.then(
							Commands.argument("position", BlockPosArgument.blockPos())
							.executes(context -> power(context.getSource(), PowerMode.Add, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), null))
							.then(
								Commands.argument("dimension", DimensionArgument.dimension())		// sportals power add <amount> <x> <y> <z> [dimension]
								.executes(context -> power(context.getSource(), PowerMode.Add, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), DimensionArgument.getDimension(context, "dimension")))
							)
						)
					)
				)
				.then(
					Commands.literal("remove")
					.then(
						Commands.argument("amount", IntegerArgumentType.integer(1))
						.then(
							Commands.argument("position", BlockPosArgument.blockPos())
							.executes(context -> power(context.getSource(), PowerMode.Remove, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), null))
							.then(
								Commands.argument("dimension", DimensionArgument.dimension())		// sportals power remove <amount> <x> <y> <z> [dimension]
								.executes(context -> power(context.getSource(), PowerMode.Remove, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), DimensionArgument.getDimension(context, "dimension")))
							)
						)
					)
				)
				.then(
					Commands.literal("get")
					.then(
						Commands.argument("position", BlockPosArgument.blockPos())
						.executes(context -> power(context.getSource(), PowerMode.Get, 0, BlockPosArgument.getLoadedBlockPos(context, "position"), null))
						.then(
							Commands.argument("dimension", DimensionArgument.dimension())		// sportals power get <x> <y> <z> [dimension]
							.executes(context -> power(context.getSource(), PowerMode.Get, 0, BlockPosArgument.getLoadedBlockPos(context, "position"), DimensionArgument.getDimension(context, "dimension")))
						)
					)
				)
				.then(
					Commands.literal("set")
					.then(
						Commands.argument("amount", IntegerArgumentType.integer(0))
							.then(
								Commands.argument("position", BlockPosArgument.blockPos())
								.executes(context -> power(context.getSource(), PowerMode.Set, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), null))
								.then(
									Commands.argument("dimension", DimensionArgument.dimension())		// sportals power set <amount> <x> <y> <z> [dimension]
									.executes(context -> power(context.getSource(), PowerMode.Set, IntegerArgumentType.getInteger(context, "amount"), BlockPosArgument.getLoadedBlockPos(context, "position"), DimensionArgument.getDimension(context, "dimension")))
							)
						)
					)
				)
				.then(
					Commands.literal("items")
					.executes(context -> {
						var powerTag = BuiltInRegistries.ITEM.get(Settings.powerSourceTag());

						if (powerTag.isEmpty())
						{
							Utils.SendTranslatedMessage(context.getSource(), "commands.errors.no_power_items", Settings.powerSource().toString());
							return 1;
						}

						Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.power.items.success", powerTag.get().stream().count());

						powerTag.get().forEach(item -> {
							Utils.SendTranslatedMessage(context.getSource(), item.value().getDescriptionId());
						});

						return 1;
					})
				)
			)
			.then(
				Commands.literal("cooldown")
				.requires((commandSource) -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
				.executes(context -> {
					Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.cooldown.info");
					return 1;
				})
				.then(
					Commands.argument("player", EntityArgument.player())		// sportals cooldown <player>
					.executes(context -> cooldown(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
			)
			.then(
				Commands.literal("clear")
				.requires((commandSource) -> commandSource.permissions().hasPermission(Permissions.COMMANDS_OWNER))
				.executes(context -> {
					Utils.SendTranslatedMessage(context.getSource(), "commands.sportals.clear.info");
					return 1;
				})
				.then(
					Commands.literal("confirmed")		// sportals clear confirmed
					.executes(context -> clear(context.getSource()))
				)
			)
		);
	}

	private static int deactivate(CommandSourceStack source, DeactivateMode mode, Address address, BlockPos pos, ServerLevel dimensionLevel) throws CommandSyntaxException
	{
		List<Portal> portals = null;

		switch (mode)
		{
			case Address:
				// sportals deactivate <addressBlockId> <addressBlockId> <addressBlockId> <addressBlockId> [dimension]
				portals = portalRegistry.getPortalsWithAddress(address);

				if (portals == null || portals.isEmpty())
				{
					if (dimensionLevel != null)
					{
						Utils.SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address_in_dimension", address, dimensionLevel.dimension().identifier());
					}
					else
					{
						Utils.SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address", address);
					}

					return 0;
				}

				if (dimensionLevel != null)
				{
					// filter out all portals that are not in the specified dimension
					final ResourceKey<Level> dimensionCopy = dimensionLevel.dimension();	// This is necessary because Java wants closures in lambda expressions to be effectively final.
					portals = portals.stream().filter((portal -> portal.dimension() == dimensionCopy)).collect(Collectors.toList());
				}

				break;

			case Position:
				// sportals deactivate <x> <y> <z> [dimension]
				ResourceKey<Level> dimension;

				if (dimensionLevel == null)
				{
					try
					{
						// Get the dimension the command sender is currently in.
						ServerPlayer player = source.getPlayerOrException();
						dimension = player.level().dimension();
					}
					catch (CommandSyntaxException ex)
					{
						throw new SimpleCommandExceptionType(Component.translatable("commands.errors.unknown_sender_dimension")).create();
					}
				}
				else
				{
					dimension = dimensionLevel.dimension();
				}

				portals = portalRegistry.getPortalsAt(pos, dimension);
				if (portals == null || portals.isEmpty()) throw new SimpleCommandExceptionType(Utils.translate("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.identifier())).create();

				break;
		}

		BlockPos portalPos;
		ServerLevel dimensionServer;

		for (Portal portal : portals)
		{
			portalPos = portal.corner1().pos();
			dimensionServer = source.getServer().getLevel(portal.dimension());
			if (dimensionServer == null) throw new SimpleCommandExceptionType(Utils.translate("commands.errors.missing_dimension", portal.dimension().identifier())).create();

			portalRegistry.deactivatePortal(dimensionServer, portalPos);
			Utils.SendTranslatedMessage(source, "commands.sportals.deactivate.success", portalPos.getX(), portalPos.getY(), portalPos.getZ(), portal.dimension().identifier().toString());
		}

		return 1;
	}

	private static int power(CommandSourceStack source, PowerMode mode, int amount, BlockPos pos, ServerLevel dimensionLevel) throws CommandSyntaxException
	{
		ResourceKey<Level> dimension;

		if (dimensionLevel == null)
		{
			// Get the dimension the command sender is currently in.
			try
			{
				ServerPlayer player = source.getPlayerOrException();
				dimension = player.level().dimension();
			}
			catch (CommandSyntaxException ex)
			{
				throw new SimpleCommandExceptionType(Component.translatable("commands.errors.unknown_sender_dimension")).create();
			}
		}
		else
		{
			dimension = dimensionLevel.dimension();
		}

		List<Portal> portals = portalRegistry.getPortalsAt(pos, dimension);

		if (portals == null || portals.isEmpty())
		{
			throw new SimpleCommandExceptionType(Utils.translate("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.identifier())).create();
		}
		else if (portals.size() > 1)
		{
			throw new SimpleCommandExceptionType(Utils.translate("commands.errors.multiple_portals_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.identifier())).create();
		}

		Portal portal = portals.getFirst();
		ServerLevel portalLevel = source.getServer().getLevel(dimension);

		switch (mode)
		{
			case Add:
				// sportals power add <amount> <x> <y> <z> [dimension]
				amount = amount - portalRegistry.addPower(portal, amount);
				portalRegistry.updatePowerGauges(portalLevel, portal);
				Utils.SendTranslatedMessage(source, "commands.sportals.power.add.success", amount, pos.getX(), pos.getY(), pos.getZ(), dimension.identifier().toString());
				break;

			case Remove:
				// sportals power remove <amount> <x> <y> <z> [dimension]
				amount = Math.min(amount, portalRegistry.getPortalPower(portal));
				amount = (portalRegistry.removePower(portal, amount)) ? amount : 0;
				portalRegistry.updatePowerGauges(portalLevel, portal);
				Utils.SendTranslatedMessage(source, "commands.sportals.power.remove.success", amount, pos.getX(), pos.getY(), pos.getZ(), dimension.identifier().toString());
				break;

			case Get:
				// sportals power get <x> <y> <z> [dimension]
				amount = portalRegistry.getPortalPower(portal);
				Utils.SendTranslatedMessage(source, "commands.sportals.power.get.success", pos.getX(), pos.getY(), pos.getZ(), dimension.identifier().toString(), amount);
				break;

			case Set:
				// sportals power set <x> <y> <z> [dimension]
				amount = portalRegistry.setPower(portal, amount);
				Utils.SendTranslatedMessage(source, "commands.sportals.power.set.success", amount, pos.getX(), pos.getY(), pos.getZ(), dimension.identifier().toString());
				break;
		}

		return 1;
	}

	private static int cooldown(CommandSourceStack source, ServerPlayer target)
	{
		// sportals cooldown <player>
		int cooldown = ((EntityAccessor)target).getPortalCooldown();
		Utils.SendTranslatedMessage(source, "commands.sportals.cooldown.success", target.getName().getString(), cooldown, cooldown / 20f);	// This assumes normal tick-rate of 20 (TPS).

		return 1;
	}

	private static int clear(CommandSourceStack source)
	{
		// sportals clear confirmed
		portalRegistry.clear();
		Utils.SendTranslatedMessage(source, "commands.sportals.clear.success");

		return 1;
	}
}
