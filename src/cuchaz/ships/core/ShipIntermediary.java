/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.core;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.Environment;
import cuchaz.ships.Collider;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PlayerRespawner;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public class ShipIntermediary {
	
	public static final String Path = "cuchaz/ships/core/ShipIntermediary";
	
	public static World translateWorld(World world, InventoryPlayer inventory) {
		// are we looking at a ship?
		EntityShip ship = ShipLocator.getFromPlayerLook(inventory.player);
		if (ship != null) {
			return ship.getShipWorld();
		}
		
		// otherwise, just pass through the original world
		return world;
	}
	
	public static double getEntityDistanceSq(EntityPlayer player, double tileEntityX, double tileEntityY, double tileEntityZ, TileEntity tileEntity) {
		return translateDistance(tileEntity.getWorldObj(), player, tileEntityX, tileEntityY, tileEntityZ);
	}
	
	public static double getEntityDistanceSq(EntityPlayer player, double containerX, double containerY, double containerZ, Container container) {
		// get private data from the container
		World world = null;
		int x = 0;
		int y = 0;
		int z = 0;
		try {
			Field fieldWorld = getField(container, Environment.getRuntimeName("worldObj", "field_75161_g"), // ContainerWorkbench
					Environment.getRuntimeName("worldPointer", "field_75172_h"), // ContainerEnchantment
					Environment.getRuntimeName("theWorld", "field_82860_h") // ContainerRepair
			);
			world = (World)fieldWorld.get(container);
			
			Field fieldX = getField(container, Environment.getRuntimeName("posX", "field_75164_h"), // ContainerWorkbench
					Environment.getRuntimeName("posX", "field_75173_i"), // ContainerEnchantment
					Environment.getRuntimeName("field_82861_i", "field_82861_i") // ContainerRepair
			);
			x = fieldX.getInt(container);
			
			Field fieldY = getField(container, Environment.getRuntimeName("posY", "field_75165_i"), // ContainerWorkbench
					Environment.getRuntimeName("posY", "field_75170_j"), // ContainerEnchantment
					Environment.getRuntimeName("field_82858_j", "field_82858_j") // ContainerRepair
			);
			y = fieldY.getInt(container);
			
			Field fieldZ = getField(container, Environment.getRuntimeName("posZ", "field_75163_j"), // ContainerWorkbench
					Environment.getRuntimeName("posZ", "field_75171_k"), // ContainerEnchantment
					Environment.getRuntimeName("field_82859_k", "field_82859_k") // ContainerRepair
			);
			z = fieldZ.getInt(container);
		} catch (Exception ex) {
			Ships.logger.warning(ex, "Unable to reflect on container class: %s", container.getClass().getName());
		}
		
		return translateDistance(world, player, x, y, z);
	}
	
	public static void onEntityMove(Entity entity, double dx, double dy, double dz) {
		// just forward to the collider
		Collider.onEntityMove(entity, dx, dy, dz);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getShipsWithinAABB(List out, World world, AxisAlignedBB box, IEntitySelector selector) {
		for (EntityShip ship : ShipLocator.findShipsInBox(world, box)) {
			if (selector == null || selector.isEntityApplicable(ship)) {
				out.add(ship);
			}
		}
		return out;
	}
	
	public static boolean checkBlockCollision(World world, AxisAlignedBB box) {
		for (EntityShip ship : ShipLocator.findShipsInBox(world, box)) {
			if (ship.getCollider().isColliding(box)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isEntityOnShipLadder(EntityLivingBase entity) {
		// just forward to the collider
		return Collider.isEntityOnShipLadder(entity);
	}
	
	public static double getDistanceSqToEntity(Entity src, Entity dest) {
		// is either entity a ship?
		if (src instanceof EntityShip) {
			return ((EntityShip)src).getCollider().getDistanceSqToEntity(dest);
		} else if (dest instanceof EntityShip) {
			return ((EntityShip)dest).getCollider().getDistanceSqToEntity(src);
		}
		
		// returning a negative number signals that the original distance function should be executed
		return -1;
	}
	
	public static void onPlayerWakeUp(EntityPlayer player, boolean wasSleepSuccessful) {
		PlayerRespawner.onPlayerWakeUp(player, wasSleepSuccessful);
	}
	
	public static void onPlayerRespawn(EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, int dimension) {
		PlayerRespawner.onPlayerRespawn(oldPlayer, newPlayer, dimension);
	}
	
	public static boolean isPlayerInBed(EntityPlayer player) {
		return PlayerRespawner.isPlayerInBerth(player);
	}
	
	private static double translateDistance(World world, EntityPlayer player, double x, double y, double z) {
		// is the block on a ship?
		if (world != null && world instanceof ShipWorld) {
			EntityShip ship = ((ShipWorld)world).getShip();
			
			// transform the coordinates to world space!
			Vec3 v = Vec3.createVectorHelper(x, y, z);
			ship.blocksToShip(v);
			ship.shipToWorld(v);
			
			return player.getDistanceSq(v.xCoord, v.yCoord, v.zCoord);
		} else {
			// no ship? just return the original result
			return player.getDistanceSq(x, y, z);
		}
	}
	
	private static Field getField(Object obj, String... names) {
		for (Field field : obj.getClass().getDeclaredFields()) {
			for (String name : names) {
				if (field.getName().equals(name)) {
					field.setAccessible(true);
					return field;
				}
			}
		}
		return null;
	}
}
