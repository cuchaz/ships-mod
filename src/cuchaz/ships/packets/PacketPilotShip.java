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
	private PilotAction m_action;
	private BlockSide m_sideShipForward;
	
	public PacketPilotShip( )
	{
		super( Channel );
	}
	
	public PacketPilotShip( int entityId, PilotAction action, BlockSide sideFacingPlayer )
	{
		this();
		
		m_entityId = entityId;
		m_action = action;
		m_sideShipForward = sideFacingPlayer;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		if( m_action == null )
		{
			out.writeInt( -1 );
		}
		else
		{
			out.writeInt( m_action.ordinal() );
		}
		out.writeByte( m_sideShipForward.ordinal() );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		int actionId = in.readInt();
		m_action = actionId >= 0 ? PilotAction.values()[actionId] : null;
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
		ship.setPilotAction( m_action, m_sideShipForward );
	}
}
