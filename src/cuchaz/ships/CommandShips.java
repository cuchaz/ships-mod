/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockCallback;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.BlockUtils.SearchAction;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockEntry;
import cuchaz.ships.config.BlockProperties;

public class CommandShips extends CommandBase {
	
	// UNDONE: internationalize this
	
	private static enum SubCommand {
		Help("Show all Ships Mod commands", "<command name>") {
			
			@Override
			public void process(ICommandSender sender, String[] args) {
				if (args.length <= 0) {
					listCommands(sender);
				} else if (args.length >= 1) {
					SubCommand subCommand = SubCommand.get(args[0]);
					if (subCommand == null) {
						listCommands(sender);
					} else {
						showCommandDetail(sender, subCommand);
					}
				}
			}
			
			private void listCommands(ICommandSender sender) {
				StringBuilder buf = new StringBuilder();
				buf.append("Ships Mod commands available:\n");
				for (int i = 0; i < SubCommand.values().length; i++) {
					if (i > 0) {
						buf.append(", ");
					}
					buf.append(SubCommand.values()[i].name().toLowerCase());
				}
				buf.append("\n");
				buf.append("To learn more about a command: " + getUsage());
				reply(sender, buf.toString());
			}
		},
		List("Show all ships", "") {
			
			@Override
			public void process(ICommandSender sender, String[] args) {
				List<EntityShip> ships = ShipLocator.getShips(sender.getEntityWorld());
				StringBuilder buf = new StringBuilder();
				buf.append("Found ");
				buf.append(ships.size());
				buf.append(" ships.\n");
				for (EntityShip ship : ships) {
					buf.append(String.format(" id: %8d,   blocks: %5d,   pos: ( %.1f, %.1f, %.1f )\n", ship.getEntityId(), ship.getShipWorld().coords().size(), ship.posX, ship.posY, ship.posZ));
				}
				reply(sender, buf.toString());
			}
		},
		Kill("Removes a ship from the world", "<id>") {
			
			@Override
			public void process(ICommandSender sender, String[] args) {
				if (args.length <= 0) {
					showCommandDetail(sender, this);
					return;
				}
				
				// get the ship id
				int id;
				try {
					id = Integer.parseInt(args[0]);
				} catch (NumberFormatException ex) {
					reply(sender, "Unrecognized id!");
					return;
				}
				
				// get the ship
				EntityShip ship = ShipLocator.getShip(sender.getEntityWorld(), id);
				if (ship == null) {
					reply(sender, String.format("Ship %d was not found!", id));
					return;
				}
				
				// kill the ship
				ship.setDead();
				replyAllAdmins(sender, String.format("Ship %d was killed!", id));
			}
		},
		Dock("Docks a ship", "<id>") {
			
			@Override
			public void process(ICommandSender sender, String[] args) {
				if (args.length <= 0) {
					showCommandDetail(sender, this);
					return;
				}
				
				// get the ship id
				int id;
				try {
					id = Integer.parseInt(args[0]);
				} catch (NumberFormatException ex) {
					reply(sender, "Unrecognized id!");
					return;
				}
				
				// get the ship
				EntityShip ship = ShipLocator.getShip(sender.getEntityWorld(), id);
				if (ship == null) {
					reply(sender, String.format("Ship %d was not found!", id));
					return;
				}
				
				// dock the ship
				ShipUnlauncher unlauncher = new ShipUnlauncher(ship);
				unlauncher.snapToLaunchDirection();
				unlauncher.unlaunch();
				replyAllAdmins(sender, String.format("Ship %d was docked!", id));
			}
		},
		RemoveAirWalls("Remove air walls", "") {
			
			@Override
			public void process(final ICommandSender sender, String[] args) {
				// make a list of positions we will use to seed our search
				List<Coords> seedBlocks = new ArrayList<Coords>();
				for (EntityShip ship : ShipLocator.getShips(sender.getEntityWorld())) {
					seedBlocks.add(new Coords(MathHelper.floor_double(ship.posX), MathHelper.floor_double(ship.posY), MathHelper.floor_double(ship.posZ)));
				}
				seedBlocks.add(new Coords(sender.getPlayerCoordinates()));
				
				// for each seed block, search around it looking for air walls
				final int NumBlocks = 4000;
				final List<Coords> airWallBlocks = new ArrayList<Coords>();
				for (Coords coords : seedBlocks) {
					BlockUtils.exploreBlocks(coords, NumBlocks, new BlockCallback() {
						
						@Override
						public SearchAction foundBlock(Coords coords) {
							Block block = sender.getEntityWorld().getBlock(coords.x, coords.y, coords.z);
							if (block == Ships.m_blockAirWall || block == Ships.m_blockAirRoof) {
								airWallBlocks.add(coords);
							}
							return SearchAction.ContinueSearching;
						}
					}, new BlockExplorer() {
						
						@Override
						public boolean shouldExploreBlock(Coords coords) {
							return true;
						}
					}, Neighbors.Faces);
				}
				
				// replace all the air wall blocks with water, and the air roof blocks with air
				for (Coords coords : airWallBlocks) {
					Block block = sender.getEntityWorld().getBlock(coords.x, coords.y, coords.z);
					if (block == Ships.m_blockAirWall) {
						// TODO: find out what kind of water to use from nearby blocks?
						sender.getEntityWorld().setBlock(coords.x, coords.y, coords.z, Blocks.water);
					} else if (block == Ships.m_blockAirRoof) {
						sender.getEntityWorld().setBlock(coords.x, coords.y, coords.z, Blocks.air);
					}
				}
				replyAllAdmins(sender, String.format("Removed %d air wall blocks.", airWallBlocks.size()));
			}
		},
		BlockProps("List physical block properties", "<block id name or empty to list overridden blocks>") {
			
			@Override
			public void process(ICommandSender sender, String[] args) {
				StringBuilder buf = new StringBuilder();
				if (args.length <= 0) {
					for (Map.Entry<Block,BlockEntry> mapEntry : BlockProperties.overrides()) {
						if (buf.length() > 0) {
							buf.append(", ");
						}
						buf.append(Block.blockRegistry.getNameForObject(mapEntry.getKey()));
					}
				} else {
					String blockId = args[0];
					Block block = (Block)Block.blockRegistry.getObject(blockId);
					if (block == Blocks.air && !blockId.equals(Block.blockRegistry.getNameForObject(Blocks.air))) {
						buf.append("Unrecognized block: " + blockId);
					} else {
						BlockEntry entry = BlockProperties.getEntry(block);
						buf.append(String.format("%-20s Ovr:%s Mass:%.1f Vol:%.1f WT:%s Ignrd:%s Water:%s",
							Block.blockRegistry.getNameForObject(block),
							BlockProperties.isOverridden(block) ? "Y" : "N",
							entry.mass,
							entry.displacement,
							entry.isWatertight ? "Y" : "N",
							entry.isSeparator ? "Y" : "N",
							entry.isWater ? "Y" : "N"
						));
					}
				}
				reply(sender, buf.toString());
			}
		};
		
		private String m_description;
		private String m_usage;
		
		private SubCommand(String description, String usage) {
			m_description = description;
			m_usage = usage;
		}
		
		protected static SubCommand get(String commandName) {
			for (SubCommand subCommand : SubCommand.values()) {
				if (subCommand.name().equalsIgnoreCase(commandName)) {
					return subCommand;
				}
			}
			return null;
		}
		
		public String getDescription() {
			return m_description;
		}
		
		public String getUsage() {
			return "/ships " + name().toLowerCase() + " " + m_usage;
		}
		
		protected void showCommandDetail(ICommandSender sender, SubCommand subCommand) {
			StringBuilder buf = new StringBuilder();
			buf.append(subCommand.name().toLowerCase());
			buf.append(": ");
			buf.append(subCommand.getDescription());
			buf.append("\n");
			buf.append("Usage: ");
			buf.append(subCommand.getUsage());
			reply(sender, buf.toString());
		}
		
		public abstract void process(ICommandSender sender, String[] args);
	}
	
	@Override
	public String getCommandName() {
		return "ships";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2; // same permission level as give/edit blocks/gamemode
	}
	
	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "UNDONE: write ships command usage text.";
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length <= 0) {
			SubCommand.Help.process(sender, args);
			return;
		}
		
		// get the sub command
		SubCommand subCommand = SubCommand.get(args[0]);
		if (subCommand == null) {
			SubCommand.Help.process(sender, args);
			return;
		}
		
		// pass off to the sub command
		// chop off the first argument though
		subCommand.process(sender, Arrays.copyOfRange(args, 1, args.length));
	}
	
	private static void reply(ICommandSender sender, String msg) {
		for (String line : msg.split("\\n")) {
			sender.addChatMessage(new ChatComponentTranslation(line));
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void replyAllAdmins(ICommandSender sender, String msg) {
		for (EntityPlayerMP player : (List<EntityPlayerMP>)MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			boolean isPlayerOpped = MinecraftServer.getServer().getConfigurationManager().func_152603_m().func_152700_a(player.getCommandSenderName()) != null;
			if (isPlayerOpped) {
				player.addChatMessage(new ChatComponentTranslation(msg));
			}
		}
	}
}
