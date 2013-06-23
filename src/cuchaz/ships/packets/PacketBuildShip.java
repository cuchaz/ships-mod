package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.ShipBuilder;

public class PacketBuildShip extends Packet
{
	public static final String Channel = "buildShip";
	
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketBuildShip( )
	{
		super( Channel );
	}
	
	public PacketBuildShip( int x, int y, int z )
	{
		this();
		
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_x );
		out.writeInt( m_y );
		out.writeInt( m_z );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_x = in.readInt();
		m_y = in.readInt();
		m_z = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// spawn the ship
		ShipBuilder builder = new ShipBuilder( player.worldObj, m_x, m_y, m_z );
		if( builder.isValidToBuild() )
		{
			builder.build();
		}
	}
}
