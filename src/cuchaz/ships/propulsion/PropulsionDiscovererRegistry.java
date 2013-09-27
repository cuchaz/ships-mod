package cuchaz.ships.propulsion;

import java.util.ArrayList;
import java.util.List;

public class PropulsionDiscovererRegistry
{
	private static List<PropulsionDiscoverer> m_discoverers;
	
	static
	{
		m_discoverers = new ArrayList<PropulsionDiscoverer>();
	}
	
	public static void addDiscoverer( PropulsionDiscoverer discoverer )
	{
		m_discoverers.add( discoverer );
	}
	
	public static Iterable<PropulsionDiscoverer> discoverers( )
	{
		return m_discoverers;
	}
}
