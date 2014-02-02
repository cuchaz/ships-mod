package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipWorld;

public class PacketShipLaunched extends Packet
{
	public static final String Channel = "shipLaunched";
	
	private int m_entityId;
	private byte[] m_blocksData;
	private int m_waterHeight;
	private int m_launchX;
	private int m_launchY;
	private int m_launchZ;
	
	private static TreeMap<Integer,PacketShipLaunched> m_packets;
	
	static
	{
		m_packets = new TreeMap<Integer,PacketShipLaunched>();
	}
	
	public PacketShipLaunched( )
	{
		super( Channel );
	}
	
	public PacketShipLaunched( EntityShip ship, int waterHeight, int launchX, int launchY, int launchZ )
	{
		this();
		
		m_entityId = ship.entityId;
		m_blocksData = ship.getShipWorld().getData();
		m_waterHeight = waterHeight;
		m_launchX = launchX;
		m_launchY = launchY;
		m_launchZ = launchZ;
	}

	@Override
	public void writeData( DataOutputStream out ) throws IOException
	{
		out.writeInt( m_entityId );
		out.writeInt( m_blocksData.length );
		out.write( m_blocksData );
		out.writeInt( m_waterHeight );
		out.writeInt( m_launchX );
		out.writeInt( m_launchY );
		out.writeInt( m_launchZ );
	}

	@Override
	public void readData( DataInputStream in ) throws IOException
	{
		m_entityId = in.readInt();
		m_blocksData = new byte[in.readInt()]; // this is potentially risky?
		in.read( m_blocksData );
		m_waterHeight = in.readInt();
		m_launchX = in.readInt();
		m_launchY = in.readInt();
		m_launchZ = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// save the packet for later
		m_packets.put( m_entityId, this );
	}
	
	public static PacketShipLaunched getPacket( EntityShip ship )
	{
		PacketShipLaunched packet = m_packets.get( ship.entityId );
		if( packet != null )
		{
			m_packets.remove( ship.entityId );
		}
		return packet;
	}
	
	public void process( EntityShip ship )
	{
		ShipWorld shipWorld = new ShipWorld( ship.worldObj, m_blocksData );
		ShipLauncher.initShip( ship, shipWorld, m_waterHeight, m_launchX, m_launchY, m_launchZ );
	}
}
