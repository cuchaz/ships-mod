/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;

public class WaterDisplacer
{
	private EntityShip m_ship;
	private BlockSet m_displacedBlocks;
	private BlockSet m_shouldBeDisplaced;
	
	public WaterDisplacer( EntityShip ship )
	{
		m_ship = ship;
		m_displacedBlocks = new BlockSet();
		m_shouldBeDisplaced = new BlockSet();
	}
	
	public void update( double waterHeightInBlockSpace )
	{
		// which blocks should be displaced?
		m_shouldBeDisplaced.clear();
		getBlocksThatShouldBeDisplaced( m_shouldBeDisplaced, waterHeightInBlockSpace );
		
		// are there any blocks that are displaced, but shouldn't be?
		Iterator<Coords> iter = m_displacedBlocks.iterator();
		while( iter.hasNext() )
		{
			Coords coords = iter.next();
			if( !m_shouldBeDisplaced.contains( coords ) )
			{
				// restore it
				if( m_ship.worldObj.getBlockId( coords.x, coords.y, coords.z ) == Ships.m_blockAirWall.blockID )
				{
					BlockUtils.changeBlockWithoutNotifyingIt( m_ship.worldObj, coords.x, coords.y, coords.z, Block.waterStill.blockID, 0, UpdateRules.UpdateClients );
				}
				iter.remove();
			}
		}
		
		for( Coords coords : m_shouldBeDisplaced )
		{
			// is the block displaced?
			int blockId = m_ship.worldObj.getBlockId( coords.x, coords.y, coords.z );
			if( blockId == Block.waterStill.blockID )
			{
				// it's not. We should displace it
				BlockUtils.changeBlockWithoutNotifyingIt( m_ship.worldObj, coords.x, coords.y, coords.z, Ships.m_blockAirWall.blockID, 0, UpdateRules.UpdateClients );
				m_displacedBlocks.add( coords );
			}
			else if( blockId == Ships.m_blockAirWall.blockID )
			{
				// yes, it's already displaced
				// make sure we remember it's displaced
				m_displacedBlocks.add( coords );
			}
		}
	}
	
	public void restore( )
	{
		// only actually remove blocks on the server
		if( Environment.isServer() )
		{
			for( Coords coords : m_displacedBlocks )
			{
				if( m_ship.worldObj.getBlockId( coords.x, coords.y, coords.z ) == Ships.m_blockAirWall.blockID )
				{
					BlockUtils.changeBlockWithoutNotifyingIt( m_ship.worldObj, coords.x, coords.y, coords.z, Block.waterStill.blockID, 0, UpdateRules.UpdateClients );
				}
			}
		}
		m_displacedBlocks.clear();
	}
	
	private void getBlocksThatShouldBeDisplaced( BlockSet out, double waterHeightInBlockSpace )
	{
		// get all the trapped air blocks
		BlockSet trappedAirBlocks = m_ship.getShipWorld().getDisplacement().getTrappedAirFromWaterHeight( waterHeightInBlockSpace );
		if( trappedAirBlocks.isEmpty() )
		{
			// the ship is out of the water or flooded
			return;
		}
		
		// find the world blocks that intersect the trapped air blocks
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		for( Coords coords : trappedAirBlocks )
		{
			m_ship.getCollider().getBlockWorldBoundingBox( box, coords );
			
			// grow the bounding box just a bit so we get more robust collisions
			final double Delta = 0.1;
			box = box.expand( Delta, Delta, Delta );
			
			// query for all the world blocks that intersect it
			BlockUtils.worldRangeQuery( out, m_ship.worldObj, box );
		}
	}
}
