package cuchaz.ships.propulsion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public class Propulsion
{
	public static class MethodCount
	{
		private int m_numInstances;
		private String m_name;
		private String m_namePlural;
		
		public MethodCount( PropulsionMethod method )
		{
			m_numInstances = 0;
			m_name = method.getName();
			m_namePlural = method.getNamePlural();
		}
		
		public String getName( )
		{
			if( m_numInstances == 1 )
			{
				return m_name;
			}
			else
			{
				return m_namePlural;
			}
		}
		
		@Override
		public String toString( )
		{
			return String.format( "%d %s", m_numInstances, getName() );
		}
	}
	
	private ShipWorld m_world;
	private ChunkCoordinates m_helmCoords;
	private BlockSide m_frontSide;
	private List<PropulsionMethod> m_methods;
	private Map<Class<? extends PropulsionMethod>,MethodCount> m_typeCounts;
	private double m_totalThrust;
	
	public Propulsion( ShipWorld world )
	{
		m_world = world;
		
		// compute parameters
		m_helmCoords = findHelm( m_world );
		m_frontSide = BlockSide.getByXZOffset( world.getBlockMetadata( m_helmCoords ) );
		
		// discover the propulsion types
		m_methods = new ArrayList<PropulsionMethod>();
		for( PropulsionDiscoverer discoverer : PropulsionDiscovererRegistry.discoverers() )
		{
			m_methods.addAll( discoverer.getPropulsionMethods( m_world, m_frontSide ) );
		}
		
		// count the types
		m_typeCounts = new HashMap<Class<? extends PropulsionMethod>,MethodCount>();
		for( PropulsionMethod method : m_methods )
		{
			MethodCount count = m_typeCounts.get( method.getClass() );
			if( count == null )
			{
				count = new MethodCount( method );
				m_typeCounts.put( method.getClass(), count );
			}
			count.m_numInstances++;
		}
		
		// calculate the total thrust
		m_totalThrust = 0;
		for( PropulsionMethod method : m_methods )
		{
			m_totalThrust += method.getThrust();
		}
	}
	
	public BlockSide getFrontSide( )
	{
		return m_frontSide;
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
	
	public Iterable<PropulsionMethod> methods( )
	{
		return m_methods;
	}
	
	public Iterable<MethodCount> methodCounts( )
	{
		return m_typeCounts.values();
	}
	
	public double getTotalThrust( )
	{
		return m_totalThrust;
	}
	
	private ChunkCoordinates findHelm( ShipWorld world )
	{
		// UNDONE: optimize this by setting the helm coords instead of searching for the helm
		// could use the propulsion system for that
		
		// always return the direction the helm is facing
		for( ChunkCoordinates coords : world.coords() )
		{
			if( world.getBlockId( coords ) == Ships.m_blockHelm.blockID )
			{
				return coords;
			}
		}
		return null;
	}
}
