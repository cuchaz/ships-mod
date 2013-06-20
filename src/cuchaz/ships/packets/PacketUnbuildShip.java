package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class PacketUnbuildShip extends Packet
{
	public static final String Channel = "unbuildShip";
	
	private int m_entityId;
	
	public PacketUnbuildShip( )
	{
		super( Channel );
	}
	
	public PacketUnbuildShip( int entityId )
	{
		this();
		
		m_entityId = entityId;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// unspawn the entity
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity == null )
		{
			return;
		}
		
		entity.setDead();
	}
}
