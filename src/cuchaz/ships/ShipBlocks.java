package cuchaz.ships;

import static net.minecraftforge.common.ForgeDirection.DOWN;
import static net.minecraftforge.common.ForgeDirection.UP;

import java.util.List;
import java.util.TreeMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
	}
	
	// NOTE: this static var is ok since the logic loop is single-threaded
	private static ChunkCoordinates m_lookupCoords = new ChunkCoordinates( 0, 0, 0 );
	
	private TreeMap<ChunkCoordinates,BlockStorage> m_blocks;
	private BlockStorage m_airBlockStorage;
	private final Vec3Pool m_vecPool;
	private float[] m_lightBrightnessTable;
	
	public ShipBlocks( World world, List<ChunkCoordinates> blocks )
	{
		// save the block ids and metadata
		m_blocks = new TreeMap<ChunkCoordinates,BlockStorage>();
		for( ChunkCoordinates block : blocks )
		{
			BlockStorage storage = new BlockStorage();
			storage.blockId = world.getBlockId( block.posX, block.posY, block.posZ );
			storage.blockMeta = world.getBlockMetadata( block.posX, block.posY, block.posZ );
			m_blocks.put( block, storage );
		}
		
		// init defaults
		m_airBlockStorage = new BlockStorage();
		m_vecPool = new Vec3Pool( 10, 100 );
		
		// build the light brightness table (copied from WorldProvider)
		m_lightBrightnessTable = new float[16];
        for( int i=0; i<=15; i++ )
        {
            float something = 1.0f - (float)i/15.0f;
            m_lightBrightnessTable[i] = ( 1.0f - something )/( something*3.0f + 1.0f );
        }
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
	public int getLightBrightnessForSkyBlocks( int i, int j, int k, int l )
	{
		// TEMP: always return 0
		return 0;
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
		// TEMP: always return light 15
		return m_lightBrightnessTable[15];
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public float getLightBrightness( int x, int y, int z )
	{
		// TEMP: always return light 15
		return m_lightBrightnessTable[15];
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
}
