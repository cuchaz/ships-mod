package cuchaz.ships.propulsion;

import java.util.Set;

import net.minecraft.util.ChunkCoordinates;

public abstract class PropulsionMethod
{
	private String m_name;
	private Set<ChunkCoordinates> m_coords;
	
	protected PropulsionMethod( String name, Set<ChunkCoordinates> coords )
	{
		m_name = name;
		m_coords = coords;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public Set<ChunkCoordinates> getCoords( )
	{
		return m_coords;
	}
	
	public abstract double getThrust( );
}
