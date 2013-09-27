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
