package cuchaz.ships;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class ShipLocator
{
	public static EntityShip getFromPlayerLocation( World world, EntityPlayer player )
	{
		// make the query box slightly larger than the player
		AxisAlignedBB queryBox = player.boundingBox.copy();
		queryBox.minY -= 0.5;
		queryBox.maxY += 0.5;
		
		@SuppressWarnings( "unchecked" )
		List<EntityShip> nearbyShips = (List<EntityShip>)world.getEntitiesWithinAABB( EntityShip.class, queryBox );
		for( EntityShip ship : nearbyShips )
		{
			if( ship.getRiders().contains( player ) )
			{
				return ship;
			}
		}
		
		return null;
	}
}
