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

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatMessageComponent;

public class CommandShips extends CommandBase
{
	// UNDONE: internationalize this
	
	private static enum SubCommand
	{
		Help( "Show all Ships Mod commands", "<command name>" )
		{
			@Override
			public void process( ICommandSender sender, String[] args )
			{
				if( args.length <= 0 )
				{
					listCommands( sender );
				}
				else if( args.length >= 1 )
				{
					SubCommand subCommand = SubCommand.get( args[0] );
					if( subCommand == null )
					{
						listCommands( sender );
					}
					else
					{
						showCommandDetail( sender, subCommand );
					}
				}
			}
			
			private void listCommands( ICommandSender sender )
			{
				StringBuilder buf = new StringBuilder();
				buf.append( "Ships Mod commands available:\n" );
				for( int i=0; i<SubCommand.values().length; i++ )
				{
					if( i > 0 )
					{
						buf.append( ", " );
					}
					buf.append( SubCommand.values()[i].name().toLowerCase() );
				}
				buf.append( "\n" );
				buf.append( "To learn more about a command, type: " + getUsage() );
				reply( sender, buf.toString() );
			}
		},
		List( "Show all ships", "" )
		{
			@Override
			public void process( ICommandSender sender, String[] args )
			{
				List<EntityShip> ships = ShipLocator.getShipsServer();
				StringBuilder buf = new StringBuilder();
				buf.append( "Found " );
				buf.append( ships.size() );
				buf.append( " ships.\n" );
				for( EntityShip ship : ships )
				{
					buf.append( String.format(
						" id: %8d,   blocks: %5d,   pos: ( %.1f, %.1f, %.1f )\n",
						ship.entityId,
						ship.getShipWorld().coords().size(),
						ship.posX, ship.posY, ship.posZ
					) );
				}
				reply( sender, buf.toString() );
			}
		},
		Kill( "Removes a ship from the world", "<id>" )
		{
			@Override
			public void process( ICommandSender sender, String[] args )
			{
				if( args.length <= 0 )
				{
					showCommandDetail( sender, this );
					return;
				}
				
				// get the ship id
				int id;
				try
				{
					id = Integer.parseInt( args[0] );
				}
				catch( NumberFormatException ex )
				{
					reply( sender, "Unrecognized id!" );
					return;
				}
				
				// get the ship
				EntityShip ship = ShipLocator.getShipServer( id );
				if( ship == null )
				{
					reply( sender, String.format( "Ship %d was not found!", id ) );
					return;
				}
				
				// kill the ship
				ship.setDead();
				replyAllAdmins( sender, String.format( "Ship %d was killed!", id ) );
			}
		},
		Dock( "Docks a ship", "<id>" )
		{
			@Override
			public void process( ICommandSender sender, String[] args )
			{
				if( args.length <= 0 )
				{
					showCommandDetail( sender, this );
					return;
				}
				
				// get the ship id
				int id;
				try
				{
					id = Integer.parseInt( args[0] );
				}
				catch( NumberFormatException ex )
				{
					reply( sender, "Unrecognized id!" );
					return;
				}
				
				// get the ship
				EntityShip ship = ShipLocator.getShipServer( id );
				if( ship == null )
				{
					reply( sender, String.format( "Ship %d was not found!", id ) );
					return;
				}
				
				// dock the ship
				ShipUnlauncher unlauncher = new ShipUnlauncher( ship );
				unlauncher.snapToLaunchDirection();
				unlauncher.unlaunch();
				replyAllAdmins( sender, String.format( "Ship %d was docked!", id ) );
			}
		};
		
		private String m_description;
		private String m_usage;
		
		private SubCommand( String description, String usage )
		{
			m_description = description;
			m_usage = usage;
		}
		
		protected static SubCommand get( String commandName )
		{
			for( SubCommand subCommand : SubCommand.values() )
			{
				if( subCommand.name().equalsIgnoreCase( commandName ) )
				{
					return subCommand;
				}
			}
			return null;
		}

		public String getDescription( )
		{
			return m_description;
		}
		
		public String getUsage( )
		{
			return "/ships " + name().toLowerCase() + " " + m_usage;
		}
		
		protected void showCommandDetail( ICommandSender sender, SubCommand subCommand )
		{
			StringBuilder buf = new StringBuilder();
			buf.append( subCommand.name().toLowerCase() );
			buf.append( ": " );
			buf.append( subCommand.getDescription() );
			buf.append( "\n" );
			buf.append( "Usage: " );
			buf.append( subCommand.getUsage() );
			reply( sender, buf.toString() );
		}
		
		public abstract void process( ICommandSender sender, String[] args );
	}
	
	@Override
	public String getCommandName( )
	{
		return "ships";
	}
	
	@Override
	public int getRequiredPermissionLevel()
    {
        return 2; // same permission level as give/edit blocks/gamemode
    }
	
	@Override
	public String getCommandUsage( ICommandSender sender )
	{
		return "UNDONE: write ships command usage text.";
	}
	
	@Override
	public void processCommand( ICommandSender sender, String[] args )
	{
		if( args.length <= 0 )
		{
			SubCommand.Help.process( sender, args );
			return;
		}
		
		// get the sub command
		SubCommand subCommand = SubCommand.get( args[0] );
		if( subCommand == null )
		{
			SubCommand.Help.process( sender, args );
			return;
		}
		
		// pass off to the sub command
		// chop off the first argument though
		subCommand.process( sender, Arrays.copyOfRange( args, 1, args.length ) );
	}
	
	private static void reply( ICommandSender sender, String msg )
	{
		sender.sendChatToPlayer( ChatMessageComponent.createFromText( msg ) );
	}
	
	@SuppressWarnings( "unchecked" )
	private static void replyAllAdmins( ICommandSender sender, String msg )
	{
		for( EntityPlayerMP player : (List<EntityPlayerMP>)MinecraftServer.getServer().getConfigurationManager().playerEntityList )
		{
			if( MinecraftServer.getServer().getConfigurationManager().isPlayerOpped( player.getCommandSenderName() ) )
			{
				player.sendChatToPlayer( ChatMessageComponent.createFromText( msg ) );
			}
		}
	}
}
