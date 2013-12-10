/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.propulsion;

import java.util.Set;

import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipWorld;

public class Sail extends PropulsionMethod
{
	public static final double ThrustPerBlock = 0.2; // N
	
	private int m_numExposedBlocks;
	
	protected Sail( ShipWorld world, Set<ChunkCoordinates> coords, BlockSide frontDirection )
	{
		super( "Sail", "Sails", coords );
		
		m_numExposedBlocks = getNumExposedBlocks( world, coords, frontDirection );
	}
	
	@Override
	public double getThrust( double speed )
	{
		// sail thrust depends on ship speed
		// sails have full strength when the ship isn't moving
		// when the ship reaches the wind speed, the sails don't add anymore thrust
		// although, wind speed is implicit here...
		return ThrustPerBlock*m_numExposedBlocks/( speed + 1 );
	}
	
	public boolean isValid( )
	{
		// if the number of exposed blocks is at least 50% of the sail, it's a good sail
		return m_numExposedBlocks >= getCoords().size()/2;
	}
	
	private int getNumExposedBlocks( ShipWorld world, Set<ChunkCoordinates> blockCoords, BlockSide frontDirection )
	{
		int numExposedBlocks = 0;
		ChunkCoordinates neighborCoords = new ChunkCoordinates();
		BlockSide backDirection = frontDirection.getOppositeSide();
		for( ChunkCoordinates coords : blockCoords )
		{
			neighborCoords.set(
				coords.posX + frontDirection.getDx(),
				coords.posY + frontDirection.getDy(),
				coords.posZ + frontDirection.getDz()
			);
			int frontId = world.getBlockId( neighborCoords );
			neighborCoords.set(
				coords.posX + backDirection.getDx(),
				coords.posY + backDirection.getDy(),
				coords.posZ + backDirection.getDz()
			);
			int backId = world.getBlockId( neighborCoords );
			
			if( frontId == 0 && backId == 0 )
			{
				numExposedBlocks++;
			}
		}
		return numExposedBlocks;
	}
}
