package cuchaz.ships;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockUtils;

public class ShipBuilder
{
	private World m_world;
	private int m_x;
	private int m_y;
	private int m_z;
	private List<ChunkCoordinates> m_blocksToBuild;
	private EntityShip m_ship;
	
	private ShipBuilder( )
	{
		m_world = null;
		m_x = 0;
		m_y = 0;
		m_z = 0;
		m_ship = null;
	}
	
	public static ShipBuilder newFromWorld( World world, int x, int y, int z )
	{
		final ShipBuilder builder = new ShipBuilder();
		builder.m_world = world;
		builder.m_x = x;
		builder.m_y = y;
		builder.m_z = z;
		
		// find all the blocks connected to the ship block
		builder.m_blocksToBuild = BlockUtils.graphSearch(
			builder.m_world,
			builder.m_x, builder.m_y, builder.m_z,
			builder.getMaxNumBlocksToBuild(),
			new BlockUtils.BlockValidator( )
			{
				@Override
				public boolean isValid( IBlockAccess world, int x, int y, int z )
				{
					int blockId = builder.m_world.getBlockId( x, y, z );
					return blockId != 0
						&& blockId != Block.waterStill.blockID
						&& blockId != Block.waterMoving.blockID;
				}
			}
		);
		
		return builder;
	}
	
	public static ShipBuilder newFromShip( EntityShip ship )
	{
		ShipBuilder builder = new ShipBuilder();
		builder.m_world = ship.worldObj;
		builder.m_x = MathHelper.floor_double( ship.posX );
		builder.m_y = MathHelper.floor_double( ship.posY );
		builder.m_z = MathHelper.floor_double( ship.posZ );
		builder.m_blocksToBuild = null;
		builder.m_ship = ship;
		return builder;
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
		return m_blocksToBuild != null && !m_blocksToBuild.isEmpty() && m_blocksToBuild.size() <= getMaxNumBlocksToBuild();
	}
	
	public boolean build( )
	{
		// spawn a ship entity
		m_ship = new EntityShip( m_world );
		m_ship.setBlocks( new ShipWorld( m_world, new ChunkCoordinates( m_x, m_y, m_z ), m_blocksToBuild ) );
		m_ship.setPositionAndRotation( m_x, m_y, m_z, 0, 0 );
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
}
