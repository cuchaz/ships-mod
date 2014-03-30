/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.propulsion;

import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;

public class Sail extends PropulsionMethod
{
	public static final double ThrustPerBlock = Util.perSecond2ToPerTick2( 10 ); // N
	
	private int m_numExposedBlocks;
	
	protected Sail( BlocksStorage shipBlocks, BlockSet sailBlocks, BlockSide frontDirection )
	{
		super( "Sail", "Sails", sailBlocks );
		
		m_numExposedBlocks = getNumExposedBlocks( shipBlocks, sailBlocks, frontDirection );
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
	
	private int getNumExposedBlocks( BlocksStorage shipBlocks, BlockSet sailBlocks, BlockSide frontDirection )
	{
		int numExposedBlocks = 0;
		Coords neighborCoords = new Coords();
		BlockSide backDirection = frontDirection.getOppositeSide();
		for( Coords coords : sailBlocks )
		{
			neighborCoords.set(
				coords.x + frontDirection.getDx(),
				coords.y + frontDirection.getDy(),
				coords.z + frontDirection.getDz()
			);
			int frontId = shipBlocks.getBlock( neighborCoords ).id;
			neighborCoords.set(
				coords.x + backDirection.getDx(),
				coords.y + backDirection.getDy(),
				coords.z + backDirection.getDz()
			);
			int backId = shipBlocks.getBlock( neighborCoords ).id;
			
			if( frontId == 0 && backId == 0 )
			{
				numExposedBlocks++;
			}
		}
		return numExposedBlocks;
	}
}
