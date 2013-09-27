package cuchaz.ships.propulsion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipWorld;

public class Propulsion
{
	private ShipWorld m_world;
	private List<PropulsionMethod> m_methods;
	
	public Propulsion( ShipWorld world, BlockSide frontDirection )
	{
		m_world = world;
		
		// discover the propulsion types
		m_methods = new ArrayList<PropulsionMethod>();
		for( PropulsionDiscoverer discoverer : PropulsionDiscovererRegistry.discoverers() )
		{
			m_methods.addAll( discoverer.getPropulsionMethods( m_world, frontDirection ) );
		}
	}
	
	public BlockArray getEnevelope( )
	{
		BlockArray envelope = m_world.getGeometry().getEnvelopes().getEnvelope( BlockSide.Top ).newEmptyCopy();
		for( PropulsionMethod method : m_methods )
		{
			for( ChunkCoordinates coords : method.getCoords() )
			{
				// keep the top-most block
				ChunkCoordinates oldCoords = envelope.getBlock( coords.posX, coords.posZ );
				if( oldCoords == null || coords.posY > oldCoords.posY )
				{
					envelope.setBlock( coords.posX, coords.posZ, coords );
				}
			}
		}
		return envelope;
	}
}
