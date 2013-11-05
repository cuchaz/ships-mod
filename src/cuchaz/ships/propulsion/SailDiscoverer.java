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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.Neighbors;
import cuchaz.ships.ShipWorld;

public class SailDiscoverer implements PropulsionDiscoverer
{
	@Override
	public List<PropulsionMethod> getPropulsionMethods( ShipWorld world, BlockSide frontDirection )
	{
		// collect all the cloth blocks into connected components
		Set<ChunkCoordinates> clothCoords = new TreeSet<ChunkCoordinates>();
		for( ChunkCoordinates coords : world.coords() )
		{
			if( world.getBlockId( coords ) == Block.cloth.blockID )
			{
				clothCoords.add( coords );
			}
		}
		List<TreeSet<ChunkCoordinates>> clothComponents = BlockUtils.getConnectedComponents( clothCoords, Neighbors.Edges );
		 
		// build the sails
		List<PropulsionMethod> sails = new ArrayList<PropulsionMethod>();
		for( TreeSet<ChunkCoordinates> component : clothComponents )
		{
			Sail sail = new Sail( world, component, frontDirection );
			if( sail.isValid() )
			{
				sails.add( sail );
			}
		}
		return sails;
	}
}
