package net.zarathul.simpleportals.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.zarathul.simpleportals.Settings;
import net.zarathul.simpleportals.commands.arguments.BlockArgument;
import net.zarathul.simpleportals.mixin.EntityAccessor;
import net.zarathul.simpleportals.registration.Address;
import net.zarathul.simpleportals.registration.Portal;
import net.zarathul.simpleportals.registration.PortalRegistry;

import java.util.List;
import java.util.stream.Collectors;

public class CommandPortals
{
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
		Items
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		// As of 1.18.2 because the config and list subcommands are client-side only, the whole command tree has to be mirrored in ClientInit.

		dispatcher.register(
			Commands.literal("sportals").requires((commandSource) -> commandSource.hasPermission(4))
			.executes(context -> {
				SendTranslatedMessage(context.getSource(), "commands.sportals.info");
				return 1;
			})
			.then(
				Commands.literal("config")	// Dummy so that the client side command shows up in auto-complete.
				.executes(context -> 1)
			)
			.then(
				Commands.literal("list")		// Dummy so that the client side command shows up in auto-complete.
				.executes(context -> 1)
			)
			.then(
				Commands.literal("deactivate")
				.executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.deactivate.info");
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
				.executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.power.info");
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
					Commands.literal("items")
					.executes(context -> {
						var powerTag = Registry.ITEM.getTag(Settings.powerSourceTag);

						if (powerTag.isEmpty())
						{
							SendTranslatedMessage(context.getSource(), "commands.errors.no_power_items", Settings.powerSource);
							return 1;
						}

						SendTranslatedMessage(context.getSource(), "commands.sportals.power.items.success", powerTag.get().stream().count());

						powerTag.get().forEach(item -> {
							SendTranslatedMessage(context.getSource(), item.value().getDescriptionId());
						});

						return 1;
					})
				)
			)
			.then(
				Commands.literal("cooldown")
				.executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.cooldown.info");
					return 1;
				})
				.then(
					Commands.argument("player", EntityArgument.player())		// sportals cooldown <player>
					.executes(context -> cooldown(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
			)
			.then(
				Commands.literal("clear")
				.executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.clear.info");
					return 1;
				})
				.then(
					Commands.literal("confirmed")		// sportals clear confirmed
					.executes(context -> clear(context.getSource()))
				)
			)
		);
	}

	private static int deactivate(CommandSourceStack source, DeactivateMode mode, Address address, BlockPos pos, ServerLevel dimensionLevel)
	{
		List<Portal> portals = null;

		switch (mode)
		{
			case Address:
				// sportals deactivate <addressBlockId> <addressBlockId> <addressBlockId> <addressBlockId> [dimension]
				portals = PortalRegistry.getPortalsWithAddress(address);

				if (portals == null || portals.size() == 0)
				{
					if (dimensionLevel != null)
					{
						SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address_in_dimension", address, dimensionLevel.dimension().location());
					}
					else
					{
						SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address", address);
					}

					return 0;
				}

				if (dimensionLevel != null)
				{
					// filter out all portals that are not in the specified dimension
					final ResourceKey<Level> dimensionCopy = dimensionLevel.dimension();	// This is necessary because Java wants closures in lambda expressions to be effectively final.
					portals = portals.stream().filter((portal -> portal.getDimension() == dimensionCopy)).collect(Collectors.toList());
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
						dimension = player.level.dimension();
					}
					catch (CommandSyntaxException ex)
					{
						throw new CommandRuntimeException(new TranslatableComponent("commands.errors.unknown_sender_dimension"));
					}
				}
				else
				{
					dimension = dimensionLevel.dimension();
				}

				portals = PortalRegistry.getPortalsAt(pos, dimension);
				if (portals == null || portals.size() == 0)	throw new CommandRuntimeException(new TranslatableComponent("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.location()));

				break;
		}

		BlockPos portalPos;
		ServerLevel dimensionServer;

		for (Portal portal : portals)
		{
			portalPos = portal.getCorner1().getPos();
			dimensionServer = source.getServer().getLevel(portal.getDimension());
			if (dimensionServer == null) throw new CommandRuntimeException(new TranslatableComponent("commands.errors.missing_dimension", portal.getDimension().location()));

			PortalRegistry.deactivatePortal(dimensionServer, portalPos);
			SendTranslatedMessage(source, "commands.sportals.deactivate.success", portalPos.getX(), portalPos.getY(), portalPos.getZ(), portal.getDimension().location());
		}

		return 1;
	}

	private static int power(CommandSourceStack source, PowerMode mode, int amount, BlockPos pos, ServerLevel dimensionLevel)
	{
		ResourceKey<Level> dimension;

		if (dimensionLevel == null)
		{
			// Get the dimension the command sender is currently in.
			try
			{
				ServerPlayer player = source.getPlayerOrException();
				dimension = player.level.dimension();
			}
			catch (CommandSyntaxException ex)
			{
				throw new CommandRuntimeException(new TranslatableComponent("commands.errors.unknown_sender_dimension"));
			}
		}
		else
		{
			dimension = dimensionLevel.dimension();
		}

		List<Portal> portals = PortalRegistry.getPortalsAt(pos, dimension);

		if (portals == null || portals.size() == 0)
		{
			throw new CommandRuntimeException(new TranslatableComponent("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.location()));
		}
		else if (portals.size() > 1)
		{
			throw new CommandRuntimeException(new TranslatableComponent("commands.errors.multiple_portals_found_at_pos_in_dimension", pos.getX(), pos.getY(), pos.getZ(), dimension.location()));
		}

		Portal portal = portals.get(0);
		ServerLevel portalLevel = source.getServer().getLevel(dimension);

		switch (mode)
		{
			case Add:
				// sportals power add <amount> <x> <y> <z> [dimension]
				amount = amount - PortalRegistry.addPower(portal, amount);
				PortalRegistry.updatePowerGauges(portalLevel, portal);
				SendTranslatedMessage(source, "commands.sportals.power.add.success", amount, pos.getX(), pos.getY(), pos.getZ(), dimension.location());
				break;

			case Remove:
				// sportals power remove <amount> <x> <y> <z> [dimension]
				amount = Math.min(amount, PortalRegistry.getPower(portal));
				amount = (PortalRegistry.removePower(portal, amount)) ? amount : 0;
				PortalRegistry.updatePowerGauges(portalLevel, portal);
				SendTranslatedMessage(source, "commands.sportals.power.remove.success", amount, pos.getX(), pos.getY(), pos.getZ(), dimension.location());
				break;

			case Get:
				// sportals power get <x> <y> <z> [dimension]
				amount = PortalRegistry.getPower(portal);
				SendTranslatedMessage(source, "commands.sportals.power.get.success", pos.getX(), pos.getY(), pos.getZ(), dimension.location(), amount);
				break;
		}

		return 1;
	}

	private static int cooldown(CommandSourceStack source, ServerPlayer target)
	{
		// sportals cooldown <player>
		int cooldown = ((EntityAccessor)target).getPortalCooldown();
		SendTranslatedMessage(source, "commands.sportals.cooldown.success", target.getName(), cooldown, cooldown / 20f);	// This assumes normal tick-rate of 20 (TPS).

		return 1;
	}

	private static int clear(CommandSourceStack source)
	{
		// sportals clear confirmed
		PortalRegistry.clear();
		SendTranslatedMessage(source, "commands.sportals.clear.success");

		return 1;
	}

	private static void SendTranslatedMessage(CommandSourceStack source, String message, Object... args)
	{
		source.sendSuccess(new TranslatableComponent(message, args), false);
	}
}
