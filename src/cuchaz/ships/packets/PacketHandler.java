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
package cuchaz.ships.packets;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cuchaz.ships.Ships;

public class PacketHandler implements IPacketHandler
{
	private HashMap<String,Packet> m_packetTypes;
	
	public PacketHandler( )
	{
		// register packet types
		m_packetTypes = new HashMap<String,Packet>();
		m_packetTypes.put( PacketLaunchShip.Channel, new PacketLaunchShip() );
		m_packetTypes.put( PacketShipLaunched.Channel, new PacketShipLaunched() );
		m_packetTypes.put( PacketUnlaunchShip.Channel, new PacketUnlaunchShip() );
		m_packetTypes.put( PacketRequestShipBlocks.Channel, new PacketRequestShipBlocks() );
		m_packetTypes.put( PacketShipBlocks.Channel, new PacketShipBlocks() );
		m_packetTypes.put( PacketPilotShip.Channel, new PacketPilotShip() );
		m_packetTypes.put( PacketShipBlockEvent.Channel, new PacketShipBlockEvent() );
		m_packetTypes.put( PacketChangedBlocks.Channel, new PacketChangedBlocks() );
		m_packetTypes.put( PacketPasteShip.Channel, new PacketPasteShip() );
		m_packetTypes.put( PacketEraseShip.Channel, new PacketEraseShip() );
		m_packetTypes.put( PacketShipPlaque.Channel, new PacketShipPlaque() );
		m_packetTypes.put( PacketPlayerSleepInBerth.Channel, new PacketPlayerSleepInBerth() );
	}
	
	@Override
	public void onPacketData( INetworkManager manager, Packet250CustomPayload customPacket, Player iPlayer )
	{
		EntityPlayer player = (EntityPlayer)iPlayer;
		
		// does this packet even have data? (dost thou even lift?!)
		if( customPacket.data == null )
		{
			Ships.logger.warning( "Received packet with no data on channel: %s! Dropping it. Check your Forge log (on the client AND server). There's probably an upstream exception", customPacket.channel );
			return;
		}
		
		Packet packet = m_packetTypes.get( customPacket.channel );
		if( packet != null )
		{
			packet.readCustomPacket( customPacket );
			packet.onPacketReceived( player );
		}
		else
		{
			Ships.logger.warning( "Received packet on unregistered channel: %s", customPacket.channel );
		}
	}
}
