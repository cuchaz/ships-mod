package cuchaz.ships;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockUtils;

public class ShipBuilder
{
	public World world;
	public int x;
	public int y;
	public int z;
	private List<ChunkCoordinates> m_blocks;
	
	public ShipBuilder( World world, int x, int y, int z )
	{
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void build( )
	{
		// find all the blocks connected to the ship block
		m_blocks = BlockUtils.graphSearch( world, x, y, z, getMaxNumBlocks(), new BlockUtils.BlockValidator( )
		{
			@Override
			public boolean isValid( World world, int x, int y, int z )
			{
				int blockId = world.getBlockId( x, y, z );
				return blockId != 0 && blockId != Block.waterStill.blockID && blockId != Block.waterMoving.blockID;
			}
		} );
	}
	
	public boolean isValidShip( )
	{
		return m_blocks != null && !m_blocks.isEmpty() && m_blocks.size() <= getMaxNumBlocks();
	}
	
	public int getNumBlocks( )
	{
		return m_blocks.size();
	}
	
	public int getMaxNumBlocks( )
	{
		// UNDONE: let the meta encode the quality of the block
		// and hence, the sllowed size of the ship
		//int meta = world.getBlockMetadata( x, y, z );
		
		// TEMP: just use 16 for now
		return 16;
	}
	
	public void makeShip( )
	{
		// UNDONE: change all the ship blocks to air
		// UNDONE: replace blocks with a ship entity
		
		// TEMP: for now, just spawn a ship entity
		EntityShip ship = new EntityShip( world, new ShipBlocks( world, new ChunkCoordinates( x, y, z ), m_blocks ) );
		ship.setPositionAndRotation( x, y + 2 /* TEMP */, z, 0, 0 );
		boolean spawnSuccess = world.spawnEntityInWorld( ship );
		
		// TEMP
		System.out.println( ( world.isRemote ? "CLIENT" : "SERVER" ) + " " + ( spawnSuccess ? "Made ship!" : "Ship spawn failed!" ) );
	}
}
