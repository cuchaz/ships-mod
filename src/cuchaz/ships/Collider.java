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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class Collider {
	
	public static void onEntityMove(Entity entity, double dx, double dy, double dz) {
		// no clip? Then it's easy
		if (entity.noClip) {
			entity.moveEntity(dx, dy, dz);
			return;
		}
		
		// save the pre-movement entity position
		double oldX = entity.posX;
		double oldY = entity.posY;
		double oldZ = entity.posZ;
		double oldYSize = entity.ySize;
		
		// NOTE: crouching players on ships will not be moved by entity.moveEntity()
		// because edge walk-over prevention doesn't know about ship blocks
		// it always sees the player as already over the edge, so any movement is prevented
		boolean isPlayerCrouching = entity.onGround && entity.isSneaking() && entity instanceof EntityPlayer;
		if (isPlayerCrouching && isEntityOnAnyShip(entity)) {
			// move the entity against the world without edge walk-over protections
			entity.onGround = false;
			entity.moveEntity(dx, dy, dz);
			entity.onGround = true;
		} else {
			// collide with the world normally
			entity.moveEntity(dx, dy, dz);
		}
		
		for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
			// collide with the ships
			ship.getCollider().onNearbyEntityMoved(oldX, oldY, oldZ, oldYSize, entity);
		}
	}
	
	public static boolean isEntityOnShipLadder(EntityLivingBase entity) {
		for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
			if (ship.getCollider().isEntityOnLadder(entity)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isEntityOnAnyShip(Entity entity) {
		for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
			if (ship.getCollider().isEntityAboard(entity)) {
				return true;
			}
		}
		return false;
	}
}
