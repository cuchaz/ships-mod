package cuchaz.ships;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;

public class ShipBuilder
{
	private World m_world;
	private int m_x;
	private int m_y;
	private int m_z;
	private List<ChunkCoordinates> m_blocksToBuild;
	private ShipType m_shipType;
	private EntityShip m_ship;
	private boolean m_isValidToBuildRightNumberOfBlocks;
	private boolean m_isValidToBuildNotSubmerged;
	private boolean m_isValidToBuildHasWaterBelow;
	
	private ShipBuilder( World world, int x, int y, int z, List<ChunkCoordinates> blocksToBuild, ShipType shipType, EntityShip ship )
	{
		m_world = world;
		m_x = x;
		m_y = y;
		m_z = z;
		m_blocksToBuild = blocksToBuild;
		m_shipType = shipType;
		m_ship = ship;
		
		// init defaults
		m_isValidToBuildRightNumberOfBlocks = false;
		m_isValidToBuildNotSubmerged = false;
		m_isValidToBuildHasWaterBelow = false;
	}
	
	public static ShipBuilder newFromWorld( World world, int x, int y, int z )
	{
		// UNDONE: get the ship type from the block metadata
		ShipType shipType = ShipType.Small;
		
		// find all the blocks connected to the ship block
		List<ChunkCoordinates> blocks = BlockUtils.graphSearch(
			world, x, y, z,
			shipType.getMaxNumBlocks(),
			new BlockUtils.BlockValidator( )
			{
				@Override
				public boolean isValid( IBlockAccess world, int x, int y, int z )
				{
					Material material = world.getBlockMaterial( x, y, z );
					return material != Material.air && !material.isLiquid() && material != Material.fire;
				}
			}
		);
		
		ShipBuilder builder = new ShipBuilder(
			world, x, y, z,
			blocks,
			shipType,
			null
		);
		
		builder.computeValidChecks();
		
		return builder;
	}
	
	public static ShipBuilder newFromShip( EntityShip ship )
	{
		return new ShipBuilder(
			ship.worldObj,
			MathHelper.floor_double( ship.posX ),
			MathHelper.floor_double( ship.posY ),
			MathHelper.floor_double( ship.posZ ),
			null,
			ship.getShipType(),
			ship
		);
	}
	
	public int getX( )
	{
		return m_x;
	}
	
	public int getY( )
	{
		return m_y;
	}
	
	public int getZ( )
	{
		return m_z;
	}
	
	public EntityShip getShip( )
	{
		return m_ship;
	}
	
	public int getNumBlocksToBuild( )
	{
		return m_blocksToBuild.size();
	}
	
	public int getMaxNumBlocksToBuild( )
	{
		// UNDONE: let the meta encode the quality of the block
		// and hence, the allowed size of the ship
		//int meta = world.getBlockMetadata( x, y, z );
		
		// TEMP: just use 16 for now
		return 16;
	}
	
	public boolean isValidToBuild( )
	{
		return m_isValidToBuildRightNumberOfBlocks
			&& m_isValidToBuildNotSubmerged
			&& m_isValidToBuildHasWaterBelow;
		// UNDONE: remove not submerged requirement
	}
	
	public boolean build( )
	{
		// spawn a ship entity
		m_ship = new EntityShip( m_world );
		m_ship.setShipType( m_shipType );
		m_ship.setPositionAndRotation( m_x, m_y, m_z, 0, 0 );
		m_ship.setBlocks( new ShipWorld( m_world, new ChunkCoordinates( m_x, m_y, m_z ), m_blocksToBuild ) );
		boolean isSpawnSuccessful = m_world.spawnEntityInWorld( m_ship );
		
		if( isSpawnSuccessful )
		{
			// remove all the blocks from the world
			for( ChunkCoordinates block : m_blocksToBuild )
			{
				m_world.setBlockToAir( block.posX, block.posY, block.posZ );
			}
			m_world.setBlockToAir( m_x, m_y, m_z );
		}
		
		return isSpawnSuccessful;
	}
	
	public boolean isShipInValidUnbuildPosition( )
	{
		// UNDONE: actually implement this check
		return true;
	}
	
	public void unbuild( )
	{
		// unspawn the ship
		m_ship.setDead();
		
		// restore all the blocks
		m_ship.getBlocks().restoreToWorld( m_world, m_x, m_y, m_z );
	}
	
	private void computeValidChecks( )
	{
		// right number of blocks
		m_isValidToBuildRightNumberOfBlocks = m_blocksToBuild != null
			&& !m_blocksToBuild.isEmpty()
			&& m_blocksToBuild.size() <= getMaxNumBlocksToBuild();
		
		// not submerged
		if( m_blocksToBuild == null )
		{
			m_isValidToBuildNotSubmerged = true;
		}
		else
		{
			m_isValidToBuildNotSubmerged = true;
			for( ChunkCoordinates coords : m_blocksToBuild )
			{
				for( int i=0; i<4; i++ )
				{
					BlockSide side = BlockSide.getByXZOffset( BlockSide.North, i );
					Material material = m_world.getBlockMaterial(
						coords.posX + side.getDx(),
						coords.posY + side.getDy(),
						coords.posZ + side.getDz()
					);
					if( material.isLiquid() )
					{
						m_isValidToBuildNotSubmerged = false;
						break;
					}
				}
			}
		}
		
		// has water below
		// UNDONE: can optimize this by precomputing an xz projection
		if( m_blocksToBuild == null )
		{
			m_isValidToBuildHasWaterBelow = false;
		}
		else
		{
			m_isValidToBuildHasWaterBelow = true;
			for( ChunkCoordinates coords : m_blocksToBuild )
			{
				if( !hasWaterBelow( coords ) )
				{
					m_isValidToBuildHasWaterBelow = false;
					break;
				}
			}
		}
	}
	
	private boolean hasWaterBelow( ChunkCoordinates coords )
	{
		// drop down until we hit something other than air
		for( int y=coords.posY-1; y>=0; y-- )
		{
			Material material = m_world.getBlockMaterial( coords.posX, y, coords.posZ );
			
			if( material == Material.air )
			{
				continue;
			}
			
			// if it's water, we win!! =D
			return material.isLiquid();
		}
		
		// ran out of blocks to check. =(
		return false;
	}
}
