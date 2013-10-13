package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class ShipLocator
{
	private static List<EntityShip> m_ships;
	
	static
	{
		m_ships = new ArrayList<EntityShip>();
	}
	
	public static void registerShip( EntityShip ship )
	{
		m_ships.add( ship );
	}
	
	public static void unregisterShip( EntityShip ship )
	{
		m_ships.remove( ship );
	}
	
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
		
		// are we looking at any of these ships?
		for( EntityShip ship : findShipsInBox( queryBox ) )
		{
			if( ship.boundingBox.isVecInside( eyePos ) || ship.boundingBox.isVecInside( targetPos ) || ship.boundingBox.calculateIntercept( eyePos, targetPos ) != null )
			{
				return ship;
			}
		}
		
		return null;
	}
	
	public static List<EntityShip> getFromEntityLocation( Entity entity )
	{
		// make the query box slightly larger than the player
		AxisAlignedBB queryBox = entity.boundingBox.copy();
		final double delta = 0.5;
		queryBox.minX -= delta;
		queryBox.minY -= delta;
		queryBox.minZ -= delta;
		queryBox.minX += delta;
		queryBox.minY += delta;
		queryBox.minZ += delta;
		
		return findShipsInBox( queryBox );
	}
	
	private static List<EntityShip> findShipsInBox( AxisAlignedBB box )
	{
		// sadly, we can't use this because ship entities are too big.
		// World.getEntitiesWithinAABB() will only return entities whose positions are within 2 blocks of the query box.
		// it doesn't do a box-to-box test. =(
		//return (List<EntityShip>)entity.worldObj.getEntitiesWithinAABB( EntityShip.class, queryBox );
		
		List<EntityShip> ships = new ArrayList<EntityShip>();
		for( EntityShip ship : m_ships )
		{
			if( ship.boundingBox.intersectsWith( box ) )
			{
				ships.add( ship );
			}
		}
		return ships;
	}
}
