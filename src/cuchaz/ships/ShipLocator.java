package cuchaz.ships;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class ShipLocator
{
	public static EntityShip getFromPlayerLook( EntityPlayer player )
	{
		// find out what entity the player is looking at
		Vec3 eyePos = player.worldObj.getWorldVec3Pool().getVecFromPool(
			player.posX,
			player.posY + player.getEyeHeight(),
			player.posZ
		);
        
		final double toRadians = Math.PI / 180.0;
		float pitch = (float)( player.rotationPitch * toRadians );
		float yaw = (float)( player.rotationYaw * toRadians );
		float cosYaw = MathHelper.cos( -yaw - (float)Math.PI );
		float sinYaw = MathHelper.sin( -yaw - (float)Math.PI );
		float cosPitch = MathHelper.cos( -pitch );
		float sinPitch = MathHelper.sin( -pitch );
		
		double reachDistance = new ItemInWorldManager( player.worldObj ).getBlockReachDistance();
		Vec3 targetPos = eyePos.addVector(
			sinYaw * -cosPitch * reachDistance,
			sinPitch * reachDistance,
			cosYaw * -cosPitch * reachDistance
		);
		
		// get the ships within reach
		AxisAlignedBB queryBox = player.boundingBox.copy();
		queryBox.minX -= reachDistance;
		queryBox.maxX += reachDistance;
		queryBox.minY -= reachDistance;
		queryBox.maxY += reachDistance;
		queryBox.minZ -= reachDistance;
		queryBox.maxZ += reachDistance;
		@SuppressWarnings( "unchecked" )
		List<EntityShip> nearbyShips = (List<EntityShip>)player.worldObj.getEntitiesWithinAABB( EntityShip.class, queryBox );
		
		// are we looking at any of these ships?
		for( EntityShip ship : nearbyShips )
		{
			if( ship.boundingBox.isVecInside( eyePos ) || ship.boundingBox.isVecInside( targetPos ) || ship.boundingBox.calculateIntercept( eyePos, targetPos ) != null )
			{
				return ship;
			}
		}
		
		return null;
	}
	
	public static EntityShip getFromPlayerLocation( EntityPlayer player )
	{
		// make the query box slightly larger than the player
		AxisAlignedBB queryBox = player.boundingBox.copy();
		queryBox.minY -= 0.5;
		queryBox.maxY += 0.5;
		
		@SuppressWarnings( "unchecked" )
		List<EntityShip> nearbyShips = (List<EntityShip>)player.worldObj.getEntitiesWithinAABB( EntityShip.class, queryBox );
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
