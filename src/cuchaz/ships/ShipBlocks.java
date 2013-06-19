package cuchaz.ships;

import static net.minecraftforge.common.ForgeDirection.DOWN;
import static net.minecraftforge.common.ForgeDirection.UP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockHalfSlab;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockPoweredOre;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3Pool;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ForgeDirection;

import org.apache.commons.codec.binary.Base64;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ShipBlocks implements IBlockAccess
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
	}
	
	// NOTE: this static var is ok since the logic loop is single-threaded
	private static ChunkCoordinates m_lookupCoords = new ChunkCoordinates( 0, 0, 0 );
	
	private ChunkCoordinates m_shipBlock;
	private TreeMap<ChunkCoordinates,BlockStorage> m_blocks;
	private final BlockStorage m_airBlockStorage;
	private final Vec3Pool m_vecPool;
	
	public ShipBlocks( )
	{
		// init defaults
		m_airBlockStorage = new BlockStorage();
		m_vecPool = new Vec3Pool( 10, 100 );
	}
	
	public ShipBlocks( World world, ChunkCoordinates shipBlock, List<ChunkCoordinates> blocks )
	{
		this();
		
		m_shipBlock = shipBlock;
		
		m_blocks = new TreeMap<ChunkCoordinates,BlockStorage>();
		
		// save the ship block
		m_blocks.put( new ChunkCoordinates( 0, 0, 0 ), copyWorldStorage( world, m_shipBlock ) );
		
		// save the rest of the blocks
		for( ChunkCoordinates block : blocks )
		{
			// make all the blocks relative to the ship block
			ChunkCoordinates relativeCoords = new ChunkCoordinates(
				block.posX - m_shipBlock.posX,
				block.posY - m_shipBlock.posY,
				block.posZ - m_shipBlock.posZ
			);
			m_blocks.put( relativeCoords, copyWorldStorage( world, block ) );
		}
	}
	
	public ShipBlocks( byte[] data )
	{
		this();
		
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
				// read the ship block coords
				m_shipBlock = new ChunkCoordinates();
				m_shipBlock.posX = in.readInt();
				m_shipBlock.posY = in.readInt();
				m_shipBlock.posZ = in.readInt();
				
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
			}
		}
		catch( IOException ex )
		{
			throw new Error( "Unable to deserialize blocks!", ex );
		}
	}
	
	public ShipBlocks( String data )
	{
		this( Base64.decodeBase64( data ) );
	}
	
	public int getNumBlocks( )
	{
		return m_blocks.size();
	}
	
	public Iterable<ChunkCoordinates> blocks( )
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
	@SideOnly( Side.CLIENT )
	public int getLightBrightnessForSkyBlocks( int x, int y, int z, int minBlockBrightness )
	{
		int skyBrightness = 15;
        int blockBrightness = 15;
        
        if( blockBrightness < minBlockBrightness )
        {
        	blockBrightness = minBlockBrightness;
        }
        
        return skyBrightness << 20 | blockBrightness << 4;
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
	@SideOnly( Side.CLIENT )
	public float getBrightness( int x, int y, int z, int minLight )
	{
		// apparently only used for tripwires and piston extensions. ie, we don't care
		return 0;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public float getLightBrightness( int x, int y, int z )
	{
		// how bright is this block intrinsically? (eg fluids)
		// returns [0-1] where 1 is the most bright
		
		// fluids aren't part of the boat. ie, we don't care
		return 0;
	}
	
	@Override
	public Material getBlockMaterial( int x, int y, int z )
	{
		int blockId = getBlockId( x, y, z );
        return blockId == 0 ? Material.air : Block.blocksList[blockId].blockMaterial;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public boolean isBlockOpaqueCube( int x, int y, int z )
	{
		Block block = Block.blocksList[getBlockId( x, y, z )];
		return block == null ? false : block.isOpaqueCube();
	}

	@Override
	public boolean isBlockNormalCube( int x, int y, int z )
	{
		Block block = Block.blocksList[getBlockId( x, y, z )];
		
		// copied from Block.isBlockNormalCube() since I can't change the World argument to IBlockAccess
		return block == null ? false : block.blockMaterial.isOpaque() && block.renderAsNormalBlock() && !block.canProvidePower();
	}

	@Override
	@SideOnly( Side.CLIENT )
	public boolean isAirBlock( int x, int y, int z )
	{
		return getBlockId( x, y, z ) == 0;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public BiomeGenBase getBiomeGenForCoords( int x, int z )
	{
		// the biome doesn't matter for our ship
		return BiomeGenBase.plains;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public int getHeight( )
	{
		// copied from World
		return 256;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public boolean extendedLevelsInChunkCache( )
	{
		return false;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public boolean doesBlockHaveSolidTopSurface( int x, int y, int z )
	{
		return isBlockSolidOnSide( x, y, z, ForgeDirection.UP, false );
	}
	
	@Override
	public Vec3Pool getWorldVec3Pool( )
	{
		return m_vecPool;
	}
	
	@Override
	public int isBlockProvidingPowerTo( int x, int y, int z, int direction )
	{
		Block block = Block.blocksList[getBlockId( x, y, z )];
		return block == null ? 0 : block.isProvidingStrongPower( this, x, y, z, direction );
	}

	@Override
	public boolean isBlockSolidOnSide( int x, int y, int z, ForgeDirection side, boolean _default )
	{
		Block block = Block.blocksList[getBlockId( x, y, z )];
		if( block == null )
		{
			return false;
		}
		
		// copied from Block.isBlockSolidOnSide() since I can't change the World argument to IBlockAccess
		int meta = getBlockMetadata( x, y, z );
        if( block instanceof BlockHalfSlab )
        {
            return ( (meta & 8) == 8 && (side == UP) ) || block.isOpaqueCube();
        }
        else if( block instanceof BlockFarmland )
        {
            return side != DOWN && side != UP;
        }
        else if( block instanceof BlockStairs )
        {
            boolean flipped = (meta & 4) != 0;
            return ( (meta & 3) + side.ordinal() == 5 ) || ( side == UP && flipped );
        }
        else if( block instanceof BlockHopper && side == UP )
        {
            return true;
        }
        else if( block instanceof BlockPoweredOre )
        {
            return true;
        }
        
        return isBlockNormalCube( x, y, z );
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
			
			// write the ship block coords
			out.writeInt( m_shipBlock.posX );
			out.writeInt( m_shipBlock.posY );
			out.writeInt( m_shipBlock.posZ );
			
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
	
	private BlockStorage copyWorldStorage( World world, ChunkCoordinates coords )
	{
		BlockStorage storage = new BlockStorage();
		storage.blockId = world.getBlockId( coords.posX, coords.posY, coords.posZ );
		storage.blockMeta = world.getBlockMetadata( coords.posX, coords.posY, coords.posZ );
		return storage;
	}
}
