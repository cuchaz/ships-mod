package cuchaz.ships.propulsion;

import java.util.List;

import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipWorld;

public interface PropulsionDiscoverer
{
	public List<PropulsionMethod> getPropulsionMethods( ShipWorld world, BlockSide frontDirection );
}
