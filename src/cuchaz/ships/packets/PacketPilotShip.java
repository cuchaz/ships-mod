package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.EntityShip;

public class PacketPilotShip extends Packet
{
	public static final String Channel = "pilotShip";
	
	public static enum Action
	{
		Forward
		{
			@Override
			public void pilotShip( EntityShip ship, BlockSide sideFacingPlayer )
			{
				ship.moveByPilot(
					-sideFacingPlayer.getDx(),
					-sideFacingPlayer.getDy(),
					-sideFacingPlayer.getDz()
				);
			}
		},
		Backward
		{
			@Override
			public void pilotShip( EntityShip ship, BlockSide sideFacingPlayer )
			{
				ship.moveByPilot(
					sideFacingPlayer.getDx(),
					sideFacingPlayer.getDy(),
					sideFacingPlayer.getDz()
				);
			}
		},
		Left
		{
			@Override
			public void pilotShip( EntityShip ship, BlockSide sideFacingPlayer )
			{
				// UNDONE
			}
		},
		Right
		{
			@Override
			public void pilotShip( EntityShip ship, BlockSide sideFacingPlayer )
			{
				// UNDONE
			}
		};
		
		public abstract void pilotShip( EntityShip ship, BlockSide sideFacingPlayer );
	}
	
	private int m_entityId;
	private Action m_action;
	private BlockSide m_sideFacingPlayer;
	
	public PacketPilotShip( )
	{
		super( Channel );
	}
	
	public PacketPilotShip( int entityId, Action action, BlockSide sideFacingPlayer )
	{
		this();
		
		m_entityId = entityId;
		m_action = action;
		m_sideFacingPlayer = sideFacingPlayer;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_action.ordinal() );
		out.writeByte( m_sideFacingPlayer.ordinal() );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_entityId = in.readInt();
		m_action = Action.values()[in.readInt()];
		m_sideFacingPlayer = BlockSide.values()[in.readByte()];
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
		m_action.pilotShip( ship, m_sideFacingPlayer );
	}
}
