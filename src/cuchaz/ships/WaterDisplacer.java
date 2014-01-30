package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockUtils;

public class WaterDisplacer
{
	private EntityShip m_ship;
	private TreeSet<ChunkCoordinates> m_previouslyDisplacedWaterBlocks;
	
	public WaterDisplacer( EntityShip ship )
	{
		m_ship = ship;
		m_previouslyDisplacedWaterBlocks = new TreeSet<ChunkCoordinates>();
	}
	
	public void updateWater( )
	{
		// UNDONE: this whole method needs to be redone
		
		// get all the trapped air blocks
		int surfaceLevelBlocks = MathHelper.floor_double( waterHeightBlocks );
		TreeSet<ChunkCoordinates> trappedAirBlocks = m_shipWorld.getGeometry().getTrappedAir( surfaceLevelBlocks );
		if( trappedAirBlocks.isEmpty() )
		{
			// the ship is out of the water
			return;
		}
		
		// find the world water blocks that intersect the trapped air blocks
		TreeSet<ChunkCoordinates> displacedBlocks = new TreeSet<ChunkCoordinates>();
		List<ChunkCoordinates> queryBlocks = new ArrayList<ChunkCoordinates>();
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( ChunkCoordinates coords : trappedAirBlocks )
		{
			// compute the bounding box for the air block
			p.xCoord = coords.posX + 0.5;
			p.yCoord = coords.posY + 0.5;
			p.zCoord = coords.posZ + 0.5;
			blocksToShip( p );
			shipToWorld( p );
			
			m_collider.getBlockWorldBoundingBox( box, coords );
			
			// grow the bounding box just a bit so we get more robust collisions
			final double Delta = 0.1;
			box = box.expand( Delta, Delta, Delta );
			
			// query for all the world water blocks that intersect it
			queryBlocks.clear();
			BlockUtils.worldRangeQuery( queryBlocks, worldObj, box );
			
			for( ChunkCoordinates queryCoords : queryBlocks )
			{
				Material material = worldObj.getBlockMaterial( queryCoords.posX, queryCoords.posY, queryCoords.posZ );
				if( material == Material.water || material == Material.air || material == Ships.m_materialAirWall )
				{
					displacedBlocks.add( coords );
				}
			}
		}
		
		// which are new blocks to displace?
		for( ChunkCoordinates coords : displacedBlocks )
		{
			if( m_previouslyDisplacedWaterBlocks == null || !m_previouslyDisplacedWaterBlocks.contains( coords ) )
			{
				worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Ships.m_blockAirWall.blockID );
			}
		}
		
		// which blocks are no longer displaced?
		if( m_previouslyDisplacedWaterBlocks != null )
		{
			for( ChunkCoordinates coords : m_previouslyDisplacedWaterBlocks )
			{
				if( !displacedBlocks.contains( coords ) )
				{
					worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
					
					// UNDONE: can get the fill effect back by only turning the surface level into air?
					// or make a special wake block that will self-convert back to water
				}
			}
		}
		
		m_previouslyDisplacedWaterBlocks = displacedBlocks;
	}
	
	public void restoreWater( )
	{
		// UNDONE: take all the water that was displaced and put it back!
	}
}
