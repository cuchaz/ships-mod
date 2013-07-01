package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class PacketPilotShip extends Packet
{
	public static final String Channel = "pilotShip";
	
	private int m_entityId;
	private int m_actions;
	private BlockSide m_sideShipForward;
	
	public PacketPilotShip( )
	{
		super( Channel );
	}
	
	public PacketPilotShip( int entityId, int actions, BlockSide sideFacingPlayer )
	{
		this();
		
		m_entityId = entityId;
		m_actions = actions;
		m_sideShipForward = sideFacingPlayer;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_actions );
		out.writeByte( m_sideShipForward.ordinal() );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_actions = in.readInt();
		m_sideShipForward = BlockSide.values()[in.readByte()];
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// get the ship
		Entity entity = player.worldObj.getEntityByID( m_entityId );
		if( entity == null || !( entity instanceof EntityShip ) )
		{
			return;
		}
		EntityShip ship = (EntityShip)entity;
		
		// handle ship movement
		ship.setPilotActions( m_actions, m_sideShipForward );
	}
}
