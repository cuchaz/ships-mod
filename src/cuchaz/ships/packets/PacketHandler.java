package cuchaz.ships.packets;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

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
	}
}
