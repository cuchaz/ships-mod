package cuchaz.ships.asm;

import java.lang.reflect.Field;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;

public class ShipIntermediary
{
	public static final String Path = "cuchaz/ships/asm/ShipIntermediary";
	
	public static World translateWorld( World world, InventoryPlayer inventory )
	{
		// are we looking at a ship?
		EntityShip ship = ShipLocator.getFromPlayerLook( inventory.player );
		if( ship != null )
		{
			return ship.getBlocks();
		}
		
		// otherwise, just pass through the original world
		return world;
	}
	
	public static double getEntityDistanceSq( EntityPlayer player, double tileEntityX, double tileEntityY, double tileEntityZ, TileEntity tileEntity )
	{
		return translateDistance( tileEntity.worldObj, player, tileEntityX, tileEntityY, tileEntityZ );
	}
	
	public static double getEntityDistanceSq( EntityPlayer player, double containerX, double containerY, double containerZ, Container container )
	{
		// get private data from the container
		World world = null;
		int x = 0;
		int y = 0;
		int z = 0;
		try
		{
			// UNDONE: how does obfuscation play into this?
			Field fieldWorld = getField( container, "worldObj", "theWorld", "worldPointer" );
			world = (World)fieldWorld.get( container );
			Field fieldX = getField( container, "posX", "field_82861_i" );
			x = fieldX.getInt( container );
			Field fieldY = getField( container, "posY", "field_82858_j" );
			y = fieldY.getInt( container );
			Field fieldZ = getField( container, "posZ", "field_82859_k" );
			z = fieldZ.getInt( container );
		}
		catch( IllegalArgumentException ex )
		{
			ex.printStackTrace( System.err );
		}
		catch( IllegalAccessException ex )
		{
			ex.printStackTrace( System.err );
		}
		
		return translateDistance( world, player, x, y, z );
	}
	
	public static void onEntityMove( Entity entity, double dx, double dy, double dz )
	{
		// save the original entity position
		double oldX = entity.posX;
		double oldY = entity.posY;
		double oldZ = entity.posZ;
		
		// move the entity like normal
		entity.moveEntity( dx, dy, dz );
		
		if( !entity.noClip )
		{
			// is the entity near a ship?
			for( EntityShip ship : ShipLocator.getFromEntityLocation( entity ) )
			{
				ship.getCollider().onNearbyEntityMoved( oldX, oldY, oldZ, entity );
			}
		}
	}
	
	private static double translateDistance( World world, EntityPlayer player, double x, double y, double z )
	{
		// is the block on a ship?
		if( world != null && world instanceof ShipWorld )
		{
			EntityShip ship = ((ShipWorld)world).getShip();
			
			// transform the coordinates to world space!
			Vec3 v = Vec3.createVectorHelper( x, y, z );
			ship.blocksToShip( v );
			ship.shipToWorld( v );
			
			return player.getDistanceSq( v.xCoord, v.yCoord, v.zCoord );
		}
		else
		{
			// no ship? just return the original result
			return player.getDistanceSq( x, y, z );
		}
	}
	
	private static Field getField( Object obj, String ... names )
	{
		for( Field field : obj.getClass().getDeclaredFields() )
		{
			for( String name : names )
			{
				if( field.getName().equals( name ) )
				{
					field.setAccessible( true );
					return field;
				}
			}
		}
		return null;
	}
}
