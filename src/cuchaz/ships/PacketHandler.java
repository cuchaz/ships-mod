package cuchaz.ships;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler
{
	@Override
	public void onPacketData( INetworkManager manager, Packet250CustomPayload packet, Player iPlayer )
	{
		EntityPlayer player = (EntityPlayer)iPlayer;
		
		try
		{
			if( packet.channel.equals( "makeShip" ) )
			{
				onBuildShip( player, packet );
			}
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	private void onBuildShip( EntityPlayer player, Packet250CustomPayload packet )
	throws IOException
	{
		// read the packet data
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( packet.data ) );
		int x = in.readInt();
		int y = in.readInt();
		int z = in.readInt();
		in.close();
		
		// spawn the ship
		ShipBuilder builder = new ShipBuilder( player.worldObj, x, y, z );
		builder.build();
		if( builder.isValidShip() )
		{
			builder.makeShip();
		}
		else
		{
			// TEMP
			System.out.println( ( player.worldObj.isRemote ? "CLIENT" : "SERVER" ) + " PacketHandler.onBuildShip() not valid ship!" );
		}
	}
}
