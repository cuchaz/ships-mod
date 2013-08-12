package cuchaz.ships.asm;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipWorld;

public class ShipTileEntityIntermediary
{
	public static double getEntityDistanceSq( EntityPlayer player, double tileEntityX, double tileEntityY, double tileEntityZ, TileEntity tileEntity )
	{
		// is the tile entity on a ship?
		EntityShip ship = null;
		if( tileEntity.worldObj instanceof ShipWorld )
		{
			ship = ((ShipWorld)tileEntity.worldObj).getShip();
		}
		
		if( ship == null )
		{
			// no ship? just return the original result
			return player.getDistanceSq( tileEntityX, tileEntityY, tileEntityZ );
		}
		else
		{
			// transform the coordinates to world space!
			Vec3 v = Vec3.createVectorHelper( tileEntityX, tileEntityY, tileEntityZ );
			ship.blocksToShip( v );
			ship.shipToWorld( v );
			
			return player.getDistanceSq( v.xCoord, v.yCoord, v.zCoord );
		}
	}
}
