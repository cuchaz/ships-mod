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
import java.util.TreeSet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.apache.commons.codec.binary.Base64;

import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;
import cuchaz.modsShared.BlockUtils.BlockValidator;
import cuchaz.modsShared.BoxCorner;
import cuchaz.modsShared.RotatedBB;

public class ShipWorld extends DetatchedWorld
{
	private static class BlockStorage
	{
		public int blockId;
		public int blockMeta;
		
		// UNDONE: save other block data?
		
		public BlockStorage( )
		{
			blockId = 0;
			blockMeta = 0;
		}
		
		public void writeData( DataOutputStream out )
		throws IOException
		{
			out.writeInt( blockId );
			out.writeInt( blockMeta );
		}

		public void readData( DataInputStream in )
		throws IOException
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
			world.setBlock( coords.posX, coords.posY, coords.posZ, blockId );
			world.setBlockMetadataWithNotify( coords.posX, coords.posY, coords.posZ, blockMeta, 3 );
		}
	}
	
	// NOTE: this static var is ok since the logic loop is single-threaded
	private static ChunkCoordinates m_lookupCoords = new ChunkCoordinates( 0, 0, 0 );
	
	private EntityShip m_ship;
	private TreeMap<ChunkCoordinates,BlockStorage> m_blocks;
	private final BlockStorage m_airBlockStorage;
	private TreeSet<ChunkCoordinates> m_outerBoundary;
	private List<TreeSet<ChunkCoordinates>> m_holes;
	private TreeMap<Integer,TreeSet<ChunkCoordinates>> m_trappedAir;
	
	private ShipWorld( World world )
	{
		super( world, "Ship" );
	    
		// init defaults
		m_ship = null;
		m_blocks = null;
		m_airBlockStorage = new BlockStorage();
		m_outerBoundary = null;
		m_holes = null;
		m_trappedAir = null;
	}
	
	public ShipWorld( World world, ChunkCoordinates originCoords, List<ChunkCoordinates> blocks )
	{
		this( world );
		
		m_blocks = new TreeMap<ChunkCoordinates,BlockStorage>();
		
		// save the rest of the blocks
		for( ChunkCoordinates worldCoords : blocks )
		{
			BlockStorage storage = new BlockStorage();
			storage.copyFromWorld( world, worldCoords );
			
			// make all the blocks relative to the ship block
			ChunkCoordinates relativeCoords = new ChunkCoordinates(
				worldCoords.posX - originCoords.posX,
				worldCoords.posY - originCoords.posY,
				worldCoords.posZ - originCoords.posZ
			);
			m_blocks.put( relativeCoords, storage );
		}
		
		computeBoundaryAndHoles();
		computeTrappedAir();
	}
	
	public ShipWorld( World world, byte[] data )
	{
		this( world );
		
		DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) );
		try
		{
			// read the version number
			int version = in.readInt();
			if( version != 0 )
			{
				System.err.println( "ShipBlocks persistence version " + version + " not supported! Blocks loading skipped!" );
			}
			else
			{
				// read the blocks
				m_blocks = new TreeMap<ChunkCoordinates,BlockStorage>();
				int numBlocks = in.readInt();
				for( int i=0; i<numBlocks; i++ )
				{
					ChunkCoordinates coords = new ChunkCoordinates(
						in.readInt(),
						in.readInt(),
						in.readInt()
					);
					
					BlockStorage storage = new BlockStorage();
					storage.readData( in );
					
					m_blocks.put( coords, storage );
				}
				
				// update secondary structures
				computeBoundaryAndHoles();
				computeTrappedAir();
			}
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to deserialize blocks!", ex );
		}
	}
	
	public ShipWorld( World world, String data )
	{
		this( world, Base64.decodeBase64( data ) );
	}
	
	public void restoreToWorld( World world, int x, int y, int z )
	{
		for( Map.Entry<ChunkCoordinates,BlockStorage> entry : m_blocks.entrySet() )
		{
			ChunkCoordinates coords = entry.getKey();
			BlockStorage storage = entry.getValue();
			storage.copyToWorld( world, new ChunkCoordinates( coords.posX + x, coords.posY + y, coords.posZ + z ) );
		}
	}
	
	public void restoreToWorld( World world, Map<ChunkCoordinates,ChunkCoordinates> correspondence )
	{
		for( Map.Entry<ChunkCoordinates,BlockStorage> entry : m_blocks.entrySet() )
		{
			ChunkCoordinates coords = correspondence.get( entry.getKey() );
			BlockStorage storage = entry.getValue();
			storage.copyToWorld( world, new ChunkCoordinates( coords.posX, coords.posY, coords.posZ ) );
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
	
	public int getNumBlocks( )
	{
		return m_blocks.size();
	}
	
	public Iterable<ChunkCoordinates> coords( )
	{
		return m_blocks.keySet();
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
		// UNDONE: support tile entities?
		return null;
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
		// do nothing. Blocks are immutable
		return false;
	}
	
	public ChunkCoordinates getMin( )
	{
		ChunkCoordinates min = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates coords : m_blocks.keySet() )
		{
			min.posX = Math.min( min.posX, coords.posX );
			min.posY = Math.min( min.posY, coords.posY );
			min.posZ = Math.min( min.posZ, coords.posZ );
		}
		return min;
	}
	
	public ChunkCoordinates getMax( )
	{
		ChunkCoordinates max = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates coords : m_blocks.keySet() )
		{
			max.posX = Math.max( max.posX, coords.posX );
			max.posY = Math.max( max.posY, coords.posY );
			max.posZ = Math.max( max.posZ, coords.posZ );
		}
		return max;
	}
	
	public byte[] getData( )
	{
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( data );
		
		// UNDONE: we could use compression here if we need it
		
		try
		{
			// write out persistence version number
			out.writeInt( 0 );
			
			// write out the blocks
			out.writeInt( m_blocks.size() );
			for( Map.Entry<ChunkCoordinates,BlockStorage> entry : m_blocks.entrySet() )
			{
				ChunkCoordinates coords = entry.getKey();
				BlockStorage storage = entry.getValue();
				
				out.writeInt( coords.posX );
				out.writeInt( coords.posY );
				out.writeInt( coords.posZ );
				storage.writeData( out );
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
	
	public List<ChunkCoordinates> rangeQuery( RotatedBB box )
	{
		// get the bounds in y
		int minY = MathHelper.floor_double( box.getMinY() );
		int maxY = MathHelper.floor_double( box.getMaxY() );
		
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int y=minY; y<=maxY; y++ )
		{
			// add up the blocks from the xz range query
			blocks.addAll( xzRangeQuery( y, box ) );
		}
		return blocks;
	}
	
	public List<ChunkCoordinates> xzRangeQuery( int y, RotatedBB box )
	{
		// UNDONE: we can probably optimize this using a better algorithm
		
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		
		// get the bounds in x and z
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for( BoxCorner corner : BlockSide.Top.getCorners() )
		{
			box.getCorner( p, corner );
			int x = MathHelper.floor_double( p.xCoord );
			int z = MathHelper.floor_double( p.zCoord );
			
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );
			minZ = Math.min( minZ, z );
			maxZ = Math.max( maxZ, z );
		}
		
		// search over the blocks in the range
		List<ChunkCoordinates> blocks = new ArrayList<ChunkCoordinates>();
		for( int x=minX; x<=maxX; x++ )
		{
			for( int z=minZ; z<=maxZ; z++ )
			{
				// is there even a block here?
				if( getBlockId( x, y, z ) == 0 )
				{
					continue;
				}
				
				if( blockIntersectsBoxXZ( x, z, box ) )
				{
					blocks.add( new ChunkCoordinates( x, y, z ) );
				}
			}
		}
		return blocks;
	}
	
	private boolean blockIntersectsBoxXZ( int x, int z, RotatedBB box )
	{
		// return true if any xz corner of the block is in the rotated box
		double y = ( box.getMinY() + box.getMaxY() )/2;
		return box.containsPoint( x + 0, y, z + 0 )
			|| box.containsPoint( x + 0, y, z + 1 )
			|| box.containsPoint( x + 1, y, z + 0 )
			|| box.containsPoint( x + 1, y, z + 1 )
			|| anyCornerIsInBlockXZ( box, x, z );
	}
	
	private boolean anyCornerIsInBlockXZ( RotatedBB box, int x, int z )
	{
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( BoxCorner corner : BlockSide.Top.getCorners() )
		{
			box.getCorner( p, corner );
			if( isPointInBlockXZ( p.xCoord, p.zCoord, x, z ) )
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isPointInBlockXZ( double px, double pz, int blockX, int blockZ )
	{
		return px >= blockX && px <= blockX + 1
			&& pz >= blockZ && pz <= blockZ + 1;
	}
	
	private void computeBoundaryAndHoles( )
	{
		// first, get all boundary blocks
		final TreeSet<ChunkCoordinates> boundaryBlocks = new TreeSet<ChunkCoordinates>();
		ChunkCoordinates neighborCoords = new ChunkCoordinates( 0, 0, 0 );
		for( ChunkCoordinates coords : m_blocks.keySet() )
		{
			// for each neighbor
			for( BlockSide side : BlockSide.values() )
			{
				neighborCoords.posX = coords.posX + side.getDx();
				neighborCoords.posY = coords.posY + side.getDy();
				neighborCoords.posZ = coords.posZ + side.getDz();
				
				// if it's not a ship block, it's a boundary block
				if( !m_blocks.keySet().contains( neighborCoords ) )
				{
					boundaryBlocks.add( new ChunkCoordinates( neighborCoords ) );
				}
			}
		}
		
		// boundaryBlocks will have some number of connected components. Find them all and classify each as inner/outer
		m_holes = new ArrayList<TreeSet<ChunkCoordinates>>();
		while( !boundaryBlocks.isEmpty() )
		{
			// get a block
			ChunkCoordinates coords = boundaryBlocks.first();
			
			// do BFS from this block to find the connected component
			TreeSet<ChunkCoordinates> component = new TreeSet<ChunkCoordinates>( BlockUtils.searchForBlocks(
				coords,
				boundaryBlocks.size(),
				new BlockValidator( )
				{
					@Override
					public boolean isValid( ChunkCoordinates coords )
					{
						return boundaryBlocks.contains( coords );
					}
				}
			) );
			
			// remove the component from the boundary blocks
			boundaryBlocks.removeAll( component );
			
			// is this component the outer boundary?
			if( isConnectedToShell( component.first() ) )
			{
				if( m_outerBoundary != null )
				{
					throw new Error( "Cannot have more than one outer boundary!" );
				}
				
				m_outerBoundary = component;
			}
			else
			{
				// compute the hole from the boundary
				m_holes.add( getHoleFromInnerBoundary( component ) );
			}
		}
	}
	
	private boolean isConnectedToShell( ChunkCoordinates coords )
	{
		// UNDONE: modify for y-limit
		
		// determine the shell dimensions
		final ChunkCoordinates min = getMin();
		final ChunkCoordinates max = getMax();
		
		// don't check more blocks than can fit in the shell
		int volume = ( max.posX - min.posX + 3 ) * ( max.posY - min.posY + 3 ) * ( max.posZ - min.posZ + 3 );
		
		Boolean result = BlockUtils.searchForCondition(
			coords,
			volume,
			new BlockConditionValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !m_blocks.keySet().contains( coords );
				}

				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					return coords.posX < min.posX || coords.posX > max.posX
						|| coords.posY < min.posY || coords.posY > max.posY
						|| coords.posZ < min.posZ || coords.posZ > max.posZ;
				}
			}
		);
		
		// just in case...
		if( result == null )
		{
			throw new Error( "We evaluated too many blocks checking for the shell. This shouldn't have happened." );
		}
		
		return result;
	}
	
	private TreeSet<ChunkCoordinates> getHoleFromInnerBoundary( TreeSet<ChunkCoordinates> component )
	{
		// get the number of blocks inside the shell to use as an upper bound
		ChunkCoordinates min = getMin();
		ChunkCoordinates max = getMax();
		int volume = ( max.posX - min.posX + 1 ) * ( max.posY - min.posY + 1 ) * ( max.posZ - min.posZ + 1 );
		
		// use BFS to find the enclosed volume
		List<ChunkCoordinates> holeBlocks = BlockUtils.searchForBlocks(
			component.first(),
			volume,
			new BlockValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !m_blocks.keySet().contains( coords );
				}
			}
		);
		
		// just in case...
		if( holeBlocks == null )
		{
			throw new Error( "Found too many enclosed blocks!" );
		}
		
		return new TreeSet<ChunkCoordinates>( holeBlocks );
	}
	
	private void computeTrappedAir( )
	{
		// needs blocks and boundaries
		if( m_blocks == null )
		{
			throw new Error( "Need blocks!" );
		}
		if( m_outerBoundary == null || m_holes == null )
		{
			throw new Error( "Need boundaries!" );
		}
		
		TreeSet<ChunkCoordinates> shipBlocks = new TreeSet<ChunkCoordinates>();
		TreeSet<ChunkCoordinates> outerBoundaryBlocks = new TreeSet<ChunkCoordinates>();
		
		// get the y-range
		int minY = getMin().posY;
		int maxY = getMax().posY;
		
		// check the ship layer-by layer starting from the bottom
		m_trappedAir = new TreeMap<Integer,TreeSet<ChunkCoordinates>>();
		for( int waterLevel=minY; waterLevel<=maxY+1; waterLevel++ )
		{
			TreeSet<ChunkCoordinates> trappedAirAtThisWaterLevel = new TreeSet<ChunkCoordinates>();
			
			for( int y=minY; y<=waterLevel; y++ )
			{
				// UNDONE: this could be optimized if we could answer y= queries efficiently
				
				// inner boundary blocks are always trapped air
				for( Set<ChunkCoordinates> innerBoundary : m_holes )
				{
					for( ChunkCoordinates coords : innerBoundary )
					{
						if( coords.posY == y )
						{
							trappedAirAtThisWaterLevel.add( coords );
						}
					}
				}
				
				// add all the ship blocks on this layer
				for( ChunkCoordinates coords : m_blocks.keySet() )
				{
					if( coords.posY == y )
					{
						shipBlocks.add( coords );
					}
				}
				
				// UNDONE: handle blocks on the outer boundary
				// do a y-capped fill to find the "volume" blocks
			}
		}
	}
}
