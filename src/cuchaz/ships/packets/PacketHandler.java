/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
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
		m_packetTypes.put( PacketUnlaunchShip.Channel, new PacketUnlaunchShip() );
		m_packetTypes.put( PacketPilotShip.Channel, new PacketPilotShip() );
		m_packetTypes.put( PacketShipBlockEvent.Channel, new PacketShipBlockEvent() );
		m_packetTypes.put( PacketChangedBlocks.Channel, new PacketChangedBlocks() );
		m_packetTypes.put( PacketShipBlocks.Channel, new PacketShipBlocks() );
		m_packetTypes.put( PacketShipBlocksRequest.Channel, new PacketShipBlocksRequest() );
	}
	
	@Override
	public void onPacketData( INetworkManager manager, Packet250CustomPayload customPacket, Player iPlayer )
	{
		EntityPlayer player = (EntityPlayer)iPlayer;
		
		Packet packet = m_packetTypes.get( customPacket.channel );
		if( packet != null )
		{
			packet.readCustomPacket( customPacket );
			packet.onPacketReceived( player );
		}
		else
		{
			Ships.logger.warning( "Received packet on unregistered channel: " + customPacket.channel );
		}
	}
}
