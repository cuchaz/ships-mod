package cuchaz.ships;

public class ShipUnbuilder
{
	private EntityShip m_ship;
	
	public ShipUnbuilder( EntityShip ship )
	{
		m_ship = ship;
	}
	
	public EntityShip getShip( )
	{
		return m_ship;
	}
	
	public boolean isShipInValidUnmakePosition( )
	{
		// TEMP
		return true;
	}
}
