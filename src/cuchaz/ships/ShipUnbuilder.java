package cuchaz.ships;

import net.minecraft.util.MathHelper;

public class ShipUnbuilder
{
	private EntityShip m_ship;
	
	public ShipUnbuilder( EntityShip ship )
	{
		m_ship = ship;
	}
	
	public boolean isShipInValidUnbuildPosition( )
	{
		// UNDONE: check that the ship only intersects water and air
		return true;
	}
	
	public void unbuild( )
	{
		m_ship.setDead();
		
		// restore all the blocks
		m_ship.getBlocks().restoreToWorld(
			m_ship.worldObj,
			MathHelper.floor_double( m_ship.posX ),
			MathHelper.floor_double( m_ship.posY ),
			MathHelper.floor_double( m_ship.posZ )
		);
	}
}
