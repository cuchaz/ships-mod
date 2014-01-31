package cuchaz.ships;

import java.util.Iterator;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockUtils;

public class WaterDisplacer
{
	private EntityShip m_ship;
	private TreeSet<ChunkCoordinates> m_displacedBlocks;
	
	// NEXTTIME: debug this bad boy!
	
	public WaterDisplacer( EntityShip ship )
	{
		m_ship = ship;
		m_displacedBlocks = new TreeSet<ChunkCoordinates>();
	}
	
	public void update( double waterHeightInBlockSpace )
	{
		// which blocks should be displaced?
		TreeSet<ChunkCoordinates> shouldBeDisplaced = getBlocksThatShouldBeDisplaced( waterHeightInBlockSpace );
		
		for( ChunkCoordinates coords : shouldBeDisplaced )
		{
			// is the block displaced?
			int blockId = m_ship.worldObj.getBlockId( coords.posX, coords.posY, coords.posZ );
			if( blockId == Block.waterStill.blockID )
			{
				// it's not. We should displace it
				m_ship.worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Ships.m_blockAirWall.blockID );
				m_displacedBlocks.add( coords );
			}
			else if( blockId == Ships.m_blockAirWall.blockID )
			{
				// yes, it's already displaced
				// nothing else to do
			}
		}
		
		// are there any blocks that are displaced, but shouldn't be?
		Iterator<ChunkCoordinates> iter = m_displacedBlocks.iterator();
		while( iter.hasNext() )
		{
			ChunkCoordinates coords = iter.next();
			if( !shouldBeDisplaced.contains( coords ) )
			{
				// restore it
				m_ship.worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
				iter.remove();
			}
		}
	}
	
	public void restore( )
	{
		for( ChunkCoordinates coords : m_displacedBlocks )
		{
			m_ship.worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
		}
		m_displacedBlocks.clear();
	}
	
	private TreeSet<ChunkCoordinates> getBlocksThatShouldBeDisplaced( double waterHeightInBlockSpace )
	{
		TreeSet<ChunkCoordinates> out = new TreeSet<ChunkCoordinates>();
		
		// get all the trapped air blocks
		int surfaceLevelInBlockSpace = MathHelper.floor_double( waterHeightInBlockSpace );
		TreeSet<ChunkCoordinates> trappedAirBlocks = m_ship.getShipWorld().getGeometry().getTrappedAir( surfaceLevelInBlockSpace );
		if( trappedAirBlocks.isEmpty() )
		{
			// the ship is out of the water
			return out;
		}
		
		// find the world blocks that intersect the trapped air blocks
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( ChunkCoordinates coords : trappedAirBlocks )
		{
			// compute the bounding box for the air block
			p.xCoord = coords.posX + 0.5;
			p.yCoord = coords.posY + 0.5;
			p.zCoord = coords.posZ + 0.5;
			m_ship.blocksToShip( p );
			m_ship.shipToWorld( p );
			
			m_ship.getCollider().getBlockWorldBoundingBox( box, coords );
			
			// grow the bounding box just a bit so we get more robust collisions
			final double Delta = 0.1;
			box = box.expand( Delta, Delta, Delta );
			
			// query for all the world water blocks that intersect it
			BlockUtils.worldRangeQuery( out, m_ship.worldObj, box );
		}
		
		return out;
	}
}
