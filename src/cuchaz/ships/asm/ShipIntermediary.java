package cuchaz.ships.asm;

import java.lang.reflect.Field;
import java.util.logging.Level;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.RuntimeMapping;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

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
			Field fieldWorld = getField( container,
				RuntimeMapping.getRuntimeName( "worldObj", "field_75161_g" ), // ContainerWorkbench
				RuntimeMapping.getRuntimeName( "worldPointer", "field_75172_h" ), // ContainerEnchantment
				RuntimeMapping.getRuntimeName( "theWorld", "field_82860_h" ) // ContainerRepair
			);
			world = (World)fieldWorld.get( container );
			
			Field fieldX = getField( container,
				RuntimeMapping.getRuntimeName( "posX", "field_75164_h" ), // ContainerWorkbench
				RuntimeMapping.getRuntimeName( "posX", "field_75173_i" ), // ContainerEnchantment
				RuntimeMapping.getRuntimeName( "field_82861_i", "field_82861_i" ) // ContainerRepair
			);
			x = fieldX.getInt( container );
			
			Field fieldY = getField( container,
				RuntimeMapping.getRuntimeName( "posY", "field_75165_i" ), // ContainerWorkbench
				RuntimeMapping.getRuntimeName( "posY", "field_75170_j" ), // ContainerEnchantment
				RuntimeMapping.getRuntimeName( "field_82858_j", "field_82858_j" ) // ContainerRepair
			);
			y = fieldY.getInt( container );
			
			Field fieldZ = getField( container,
				RuntimeMapping.getRuntimeName( "posZ", "field_75163_j" ), // ContainerWorkbench
				RuntimeMapping.getRuntimeName( "posZ", "field_75171_k" ), // ContainerEnchantment
				RuntimeMapping.getRuntimeName( "field_82859_k", "field_82859_k" ) // ContainerRepair
			);
			z = fieldZ.getInt( container );
		}
		catch( Exception ex )
		{
			Ships.logger.log( Level.WARNING, "Unable to reflect on container class: " + container.getClass().getName(), ex );
		}
		
		return translateDistance( world, player, x, y, z );
	}
	
	public static void onEntityMove( Entity entity, double dx, double dy, double dz )
	{
		// save the original entity position
		double oldX = entity.posX;
		double oldY = entity.posY;
		double oldZ = entity.posZ;
		double oldYSize = entity.ySize;
		
		// move the entity like normal
		entity.moveEntity( dx, dy, dz );
		
		if( !entity.noClip )
		{
			// is the entity near any ships?
			for( EntityShip ship : ShipLocator.getFromEntityLocation( entity ) )
			{
				ship.getCollider().onNearbyEntityMoved( oldX, oldY, oldZ,oldYSize, entity );
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
