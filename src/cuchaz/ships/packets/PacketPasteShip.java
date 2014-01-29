package cuchaz.ships.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import cuchaz.ships.BlocksStorage;

public class PacketPasteShip extends Packet
{
	public static final String Channel = "pasteShip";
	
	private BlocksStorage m_blocks;
	private int m_dx;
	private int m_dy;
	private int m_dz;
	
	public PacketPasteShip( )
	{
		super( Channel );
	}
	
	public PacketPasteShip( BlocksStorage blocks, int dx, int dy, int dz )
	{
		this();
		
		m_blocks = blocks;
		m_dx = dx;
		m_dy = dy;
		m_dz = dz;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		m_blocks.writeToStream( out );
		out.writeInt( m_dx );
		out.writeInt( m_dy );
		out.writeInt( m_dz );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_blocks = new BlocksStorage();
		m_blocks.readFromStream( in );
		m_dx = in.readInt();
		m_dy = in.readInt();
		m_dz = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		// paste the blocks on the server
		Map<ChunkCoordinates,ChunkCoordinates> correspondence = new TreeMap<ChunkCoordinates,ChunkCoordinates>();
		for( ChunkCoordinates shipCoords : m_blocks.coords() )
		{
			ChunkCoordinates worldCoords = new ChunkCoordinates(
				shipCoords.posX + m_dx,
				shipCoords.posY + m_dy,
				shipCoords.posZ + m_dz
			);
			correspondence.put( shipCoords, worldCoords );
		}
		
		m_blocks.writeToWorld( player.worldObj, correspondence );
	}
}
