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
package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.EntityUtils;

public class ShipLocator {
	
	public static List<EntityShip> getShips(World world) {
		List<EntityShip> ships = new ArrayList<EntityShip>();
		for (Object entity : world.loadedEntityList) {
			if (entity instanceof EntityShip) {
				ships.add((EntityShip)entity);
			}
		}
		return ships;
	}
	
	public static EntityShip getShip(World world, int entityId) {
		return getShip(getShips(world), entityId);
	}
	
	public static EntityShip getShip(World world, UUID uuid) {
		return getShip(getShips(world), uuid);
	}
	
	private static EntityShip getShip(List<EntityShip> ships, int entityId) {
		for (EntityShip ship : ships) {
			if (ship.getEntityId() == entityId) {
				return ship;
			}
		}
		return null;
	}
	
	private static EntityShip getShip(List<EntityShip> ships, UUID uuid) {
		for (EntityShip ship : ships) {
			if (ship.getPersistentID().equals(uuid)) {
				return ship;
			}
		}
		return null;
	}
	
	public static EntityShip getFromPlayerLook(EntityPlayer player) {
		// find out what entity the player is looking at
		Vec3 eyePos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
		
		final double toRadians = Math.PI / 180.0;
		float pitch = (float) (player.rotationPitch * toRadians);
		float yaw = (float) (player.rotationYaw * toRadians);
		float cosYaw = MathHelper.cos(-yaw - (float)Math.PI);
		float sinYaw = MathHelper.sin(-yaw - (float)Math.PI);
		float cosPitch = MathHelper.cos(-pitch);
		float sinPitch = MathHelper.sin(-pitch);
		
		double reachDistance = EntityUtils.getPlayerReachDistance(player);
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
		for (EntityShip ship : findShipsInBox(player.worldObj, queryBox)) {
			if (ship.boundingBox.isVecInside(eyePos) || ship.boundingBox.isVecInside(targetPos) || ship.boundingBox.calculateIntercept(eyePos, targetPos) != null) {
				return ship;
			}
		}
		
		return null;
	}
	
	public static List<EntityShip> getFromEntityLocation(Entity entity) {
		// make the query box slightly larger than the player
		final double delta = 0.5;
		AxisAlignedBB queryBox = entity.boundingBox.expand(delta, delta, delta);
		
		return findShipsInBox(entity.worldObj, queryBox);
	}
	
	public static List<EntityShip> findShipsInBox(World world, AxisAlignedBB box) {
		// sadly, we can't use World.getEntitiesWithinAABB() because ship entities are too big.
		// It will only return entities whose positions are within near the chunk of the query box.
		// it doesn't do a global box-to-box test. =(
		List<EntityShip> ships = new ArrayList<EntityShip>();
		for (EntityShip ship : getShips(world)) {
			if (ship.boundingBox.intersectsWith(box)) {
				ships.add(ship);
			}
		}
		return ships;
	}
}
