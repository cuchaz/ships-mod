package cuchaz.ships;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet62LevelSound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeDirection;

import org.apache.commons.codec.binary.Base64;

import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.ships.packets.PacketShipBlockEvent;

public class ShipWorld extends DetatchedWorld
{
	private static class BlockStorage
	{
		public int blockId;
		public int blockMeta;
		
		public BlockStorage( )
		{
			blockId = 0;
			blockMeta = 0;
		}
		
		public void writeData( DataOutputStream out ) throws IOException
		{
			out.writeInt( blockId );
			out.writeInt( blockMeta );
		}
		
		public void readData( DataInputStream in ) throws IOException
		{
			blockId = in.readInt();
			blockMeta = in.readInt();
		}
		
		public void copyFromWorld( World world, ChunkCoordinates coords )
		{
			blockId = world.getBlockId( coords.posX, coords.posY, coords.posZ );
			blockMeta = world.getBlockMetadata( coords.posX, coords.posY, coords.posZ );
		}
		
		public void copyToWorld( World world, ChunkCoordinates coords )
		{
			world.setBlock( coords.posX, coords.posY, coords.posZ, blockId, blockMeta, 3 );
		}
	}
	
	// NOTE: this member var is ok since the client/server are single-threaded
	private ChunkCoordinates m_lookupCoords = new ChunkCoordinates( 0, 0, 0 );
	
	private EntityShip m_ship;
	private TreeMap<ChunkCoordinates, BlockStorage> m_blocks;
	private final BlockStorage m_airBlockStorage;
	private ShipGeometry m_geometry;
	private TreeMap<ChunkCoordinates, TileEntity> m_tileEntities;
	
	private ShipWorld( World world )
	{
		super( world, "Ship" );
		
		// init defaults
		m_ship = null;
		m_blocks = null;
		m_airBlockStorage = new BlockStorage();
		m_tileEntities = null;
	}
	
	public ShipWorld( World world, ChunkCoordinates originCoords, List<ChunkCoordinates> blocks )
	{
		this( world );
		
		m_blocks = new TreeMap<ChunkCoordinates, BlockStorage>();
		
		// save the blocks
		for( ChunkCoordinates worldCoords : blocks )
		{
			BlockStorage storage = new BlockStorage();
			storage.copyFromWorld( world, worldCoords );
			
			// make all the blocks relative to the origin block
			ChunkCoordinates relativeCoords = new ChunkCoordinates( worldCoords.posX - originCoords.posX, worldCoords.posY - originCoords.posY, worldCoords.posZ - originCoords.posZ );
			m_blocks.put( relativeCoords, storage );
		}
		
		// copy the tile entities
		m_tileEntities = new TreeMap<ChunkCoordinates, TileEntity>();
		for( ChunkCoordinates worldCoords : blocks )
		{
			// does this block have a tile entity?
			TileEntity tileEntity = world.getBlockTileEntity( worldCoords.posX, worldCoords.posY, worldCoords.posZ );
			if( tileEntity == null )
			{
				continue;
			}
			
			ChunkCoordinates relativeCoords = new ChunkCoordinates( worldCoords.posX - originCoords.posX, worldCoords.posY - originCoords.posY, worldCoords.posZ - originCoords.posZ );
			
			// copy the tile entity
			NBTTagCompound nbt = new NBTTagCompound();
			tileEntity.writeToNBT( nbt );
			TileEntity tileEntityCopy = TileEntity.createAndLoadEntity( nbt );
			
			// initialize the tile entity
			tileEntityCopy.setWorldObj( this );
			tileEntityCopy.xCoord = relativeCoords.posX;
			tileEntityCopy.yCoord = relativeCoords.posY;
			tileEntityCopy.zCoord = relativeCoords.posZ;
			tileEntityCopy.validate();
			
			// save it to the ship world
			m_tileEntities.put( relativeCoords, tileEntityCopy );
		}
		
		computeDependentFields();
	}
	
	public ShipWorld( World world, byte[] data )
	{
		this( world );
		
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
		try
		{
			// read the version number
			int version = in.readInt();
			if( version != 1 )
			{
				System.err.println( "ShipBlocks persistence version " + version + " not supported! Blocks loading skipped!" );
			}
			else
			{
				// read the blocks
				m_blocks = new TreeMap<ChunkCoordinates, BlockStorage>();
				int numBlocks = in.readInt();
				for( int i = 0; i < numBlocks; i++ )
				{
					ChunkCoordinates coords = new ChunkCoordinates( in.readInt(), in.readInt(), in.readInt() );
					
					BlockStorage storage = new BlockStorage();
					storage.readData( in );
					
					m_blocks.put( coords, storage );
				}
				
				// read the tile entities
				m_tileEntities = new TreeMap<ChunkCoordinates, TileEntity>();
				int numTileEntities = in.readInt();
				for( int i = 0; i < numTileEntities; i++ )
				{
					// create the tile entity
					NBTTagCompound nbt = (NBTTagCompound)NBTBase.readNamedTag( in );
					TileEntity tileEntity = TileEntity.createAndLoadEntity( nbt );
					ChunkCoordinates coords = new ChunkCoordinates( tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord );
					
					// restore it to the world
					tileEntity.setWorldObj( this );
					tileEntity.validate();
					
					m_tileEntities.put( coords, tileEntity );
				}
				
				computeDependentFields();
			}
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to deserialize blocks!", ex );
		}
	}
	
	private void computeDependentFields( )
	{
		m_geometry = new ShipGeometry( m_blocks.keySet() );
	}
	
	public ShipWorld( World world, String data )
	{
		this( world, Base64.decodeBase64( data ) );
	}
	
	public void restoreToWorld( World world, Map<ChunkCoordinates, ChunkCoordinates> correspondence, int waterSurfaceLevelBlocks )
	{
		for( Map.Entry<ChunkCoordinates,BlockStorage> entry : m_blocks.entrySet() )
		{
			ChunkCoordinates coordsShip = entry.getKey();
			ChunkCoordinates coordsWorld = correspondence.get( coordsShip );
			BlockStorage storage = entry.getValue();
			
			// is there a tile entity?
			TileEntity tileEntity = getBlockTileEntity( coordsShip );
			if( tileEntity != null )
			{
				// copy the tile entity
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT( nbt );
				TileEntity tileEntityCopy = TileEntity.createAndLoadEntity( nbt );
				tileEntityCopy.validate();
				
				// restore the block before the tile entity
				storage.copyToWorld( world, coordsWorld );
				world.setBlockTileEntity( coordsWorld.posX, coordsWorld.posY, coordsWorld.posZ, tileEntityCopy );
				
				// copy the metadata back because something weird with the tile entity changes it
				Chunk chunk = world.getChunkFromChunkCoords( coordsWorld.posX >> 4, coordsWorld.posZ >> 4 );
				chunk.setBlockMetadata( coordsWorld.posX & 15, coordsWorld.posY, coordsWorld.posZ & 15, storage.blockMeta );
			}
			else
			{
				// just restore the block
				storage.copyToWorld( world, coordsWorld );
			}
		}
		
		// bail out the boat if needed (it might have water in the trapped air blocks)
		for( ChunkCoordinates coordsShip : m_geometry.getTrappedAir( waterSurfaceLevelBlocks ) )
		{
			ChunkCoordinates coordsWorld = correspondence.get( coordsShip );
			world.setBlockToAir( coordsWorld.posX, coordsWorld.posY, coordsWorld.posZ );
		}
	}
	
	public EntityShip getShip( )
	{
		return m_ship;
	}
	
	public void setShip( EntityShip val )
	{
		m_ship = val;
	}
	
	public boolean isValid( )
	{
		return m_blocks != null;
	}
	
	public int getNumBlocks( )
	{
		return m_blocks.size();
	}
	
	public Set<ChunkCoordinates> coords( )
	{
		return m_blocks.keySet();
	}
	
	public Set<Map.Entry<ChunkCoordinates, TileEntity>> tileEntities( )
	{
		return m_tileEntities.entrySet();
	}
	
	public ShipGeometry getGeometry( )
	{
		return m_geometry;
	}
	
	public BoundingBoxInt getBoundingBox( )
	{
		return m_geometry.getEnvelopes().getBoundingBox();
	}
	
	public BlockStorage getStorage( ChunkCoordinates coords )
	{
		BlockStorage storage = m_blocks.get( coords );
		if( storage == null )
		{
			storage = m_airBlockStorage;
		}
		return storage;
	}
	
	@Override
	public int getBlockId( int x, int y, int z )
	{
		m_lookupCoords.set( x, y, z );
		return getBlockId( m_lookupCoords );
	}
	
	public int getBlockId( ChunkCoordinates coords )
	{
		return getStorage( coords ).blockId;
	}
	
	@Override
	public TileEntity getBlockTileEntity( int x, int y, int z )
	{
		m_lookupCoords.set( x, y, z );
		return getBlockTileEntity( m_lookupCoords );
	}
	
	public TileEntity getBlockTileEntity( ChunkCoordinates coords )
	{
		return m_tileEntities.get( coords );
	}
	
	@Override
	public int getBlockMetadata( int x, int y, int z )
	{
		m_lookupCoords.set( x, y, z );
		return getBlockMetadata( m_lookupCoords );
	}
	
	public int getBlockMetadata( ChunkCoordinates coords )
	{
		return getStorage( coords ).blockMeta;
	}
	
	@Override
	public boolean setBlock( int par1, int par2, int par3, int par4, int par5, int par6 )
	{
		// do nothing. Ships are immutable
		return false;
	}
	
	@Override
	public void setBlockTileEntity( int x, int y, int z, TileEntity tileEntity )
	{
		// do nothing. Ships are immutable
	}
	
	@Override
	public boolean isBlockSolidOnSide( int x, int y, int z, ForgeDirection side, boolean defaultValue )
	{
		m_lookupCoords.set( x, y, z );
		Block block = Block.blocksList[getBlockId( m_lookupCoords )];
		if( block == null )
		{
			return defaultValue;
		}
		return block.isBlockSolidOnSide( this, x, y, z, side );
	}
	
	@Override
	@SuppressWarnings( "rawtypes" )
	public List getEntitiesWithinAABB( Class theClass, AxisAlignedBB box )
	{
		// there are no entities in ship world
		return new ArrayList();
		
		// UNDONE: actually do this query?
		// get the AABB for the query box in world coords
		// get the entities from the real world
		// transform them into ship world
	}
	
	@Override
	public void updateTileEntityChunkAndDoNothing( int x, int y, int z, TileEntity tileEntity )
	{
		// don't need to do anything
	}
	
	@Override
	public void updateEntities( )
	{
		// update the tile entities
		for( TileEntity entity : m_tileEntities.values() )
		{
			entity.updateEntity();
		}
	}
	
	@Override
	public void addBlockEvent( int x, int y, int z, int blockId, int eventId, int eventParam )
	{
		if( m_ship == null || blockId == 0 || getBlockId( x, y, z ) != blockId )
		{
			return;
		}
		
		// on the client, just deliver to the block
		boolean eventWasAccepted = Block.blocksList[blockId].onBlockEventReceived( this, x, y, z, eventId, eventParam );
		
		// on the server, also send a packet to the client
		if( !isRemote && eventWasAccepted )
		{
			// get the pos in world space
			Vec3 v = Vec3.createVectorHelper( x, y, z );
			m_ship.blocksToShip( v );
			m_ship.shipToWorld( v );
			
			MinecraftServer.getServer().getConfigurationManager().sendToAllNear(
				v.xCoord, v.yCoord, v.zCoord, 64,
				m_ship.worldObj.provider.dimensionId,
				new PacketShipBlockEvent( m_ship.entityId, x, y, z, blockId, eventId, eventParam ).getCustomPacket()
			);
		}
	}
	
	@Override
	public void playSoundEffect( double x, double y, double z, String sound, float volume, float pitch )
    {
		if( sound == null )
		{
			return;
		}
		
		// on the server, send a packet to the clients
		if( !isRemote )
		{
			// get the pos in world space
			Vec3 v = Vec3.createVectorHelper( x, y, z );
			m_ship.blocksToShip( v );
			m_ship.shipToWorld( v );
			
			MinecraftServer.getServer().getConfigurationManager().sendToAllNear(
				v.xCoord, v.yCoord, v.zCoord, volume > 1.0F ? (double)(16.0F * volume) : 16.0D,
				m_ship.worldObj.provider.dimensionId,
				new Packet62LevelSound( sound, v.xCoord, v.yCoord, v.zCoord, volume, pitch )
			);
		}
		
		// on the client, just ignore. Sounds actually get played by the packet handler
    }
	
	public byte[] getData( )
	{
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( data );
		
		// UNDONE: we could use compression here if we need it
		
		try
		{
			// write out persistence version number
			out.writeInt( 1 );
			
			// write out the blocks
			out.writeInt( m_blocks.size() );
			for( Map.Entry<ChunkCoordinates, BlockStorage> entry : m_blocks.entrySet() )
			{
				ChunkCoordinates coords = entry.getKey();
				BlockStorage storage = entry.getValue();
				
				out.writeInt( coords.posX );
				out.writeInt( coords.posY );
				out.writeInt( coords.posZ );
				storage.writeData( out );
			}
			
			// write out the tile entities
			out.writeInt( m_tileEntities.size() );
			for( TileEntity tileEntity : m_tileEntities.values() )
			{
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT( nbt );
				NBTBase.writeNamedTag( nbt, out );
			}
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to serialize blocks!", ex );
		}
		
		return data.toByteArray();
	}
	
	public String getDataString( )
	{
		return Base64.encodeBase64String( getData() );
	}
}
