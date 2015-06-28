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
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAccessor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.math.BoxCorner;
import cuchaz.modsShared.math.RotatedBB;
import cuchaz.ships.render.ShipDebugRenderInfo;

public class ShipCollider {
	
	private static class PossibleCollision {
		
		public Coords coords;
		public AxisAlignedBB box;
		
		public PossibleCollision(Coords coords, AxisAlignedBB box) {
			this.coords = coords;
			this.box = box;
		}
	}
	
	private static class CollisionResult {
		
		public double scaling;
		public int numCollidingBoxes;
	}
	
	private EntityShip m_ship;
	@SideOnly(Side.CLIENT)
	private ShipDebugRenderInfo m_debugRenderInfo;
	
	public ShipCollider(EntityShip ship) {
		m_ship = ship;
		
		if (Environment.isClient()) {
			m_debugRenderInfo = new ShipDebugRenderInfo();
		}
	}
	
	@SideOnly(Side.CLIENT)
	public ShipDebugRenderInfo getDebugRenderInfo() {
		return m_debugRenderInfo;
	}
	
	public void computeShipBoundingBox(AxisAlignedBB box, double x, double y, double z, float yaw) {
		ShipWorld blocks = m_ship.getShipWorld();
		if (blocks == null) {
			return;
		}
		
		// make an un-rotated box in world-space
		box.minX = x + m_ship.blocksToShipX(blocks.getBoundingBox().minX);
		box.minY = y + m_ship.blocksToShipY(blocks.getBoundingBox().minY);
		box.minZ = z + m_ship.blocksToShipZ(blocks.getBoundingBox().minZ);
		box.maxX = x + m_ship.blocksToShipX(blocks.getBoundingBox().maxX + 1);
		box.maxY = y + m_ship.blocksToShipY(blocks.getBoundingBox().maxY + 1);
		box.maxZ = z + m_ship.blocksToShipZ(blocks.getBoundingBox().maxZ + 1);
		
		// now rotate by the yaw
		// UNDONE: optimize out the new
		RotatedBB rotatedBox = new RotatedBB(box.copy(), yaw, x, z);
		
		// compute the new xz bounds
		box.minX = Integer.MAX_VALUE;
		box.maxX = Integer.MIN_VALUE;
		box.minZ = Integer.MAX_VALUE;
		box.maxZ = Integer.MIN_VALUE;
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		for (BoxCorner corner : BlockSide.Top.getCorners()) {
			rotatedBox.getCorner(p, corner);
			
			box.minX = Math.min(box.minX, p.xCoord);
			box.maxX = Math.max(box.maxX, p.xCoord);
			box.minZ = Math.min(box.minZ, p.zCoord);
			box.maxZ = Math.max(box.maxZ, p.zCoord);
		}
	}
	
	public void onNearbyEntityMoved(double oldX, double oldY, double oldZ, double oldYSize, Entity entity) {
		
		if (entity.noClip) {
			// skip entities that don't collide
			return;
		}
		
		if (entity instanceof EntityShip) {
			// nothing should be calling moveEntity() for ships, so we can ignore this
			return;
		}
		
		// get a box for the entity's original positions
		AxisAlignedBB oldEntityBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getEntityBoxInBlockSpace(oldEntityBox, entity, Vec3.createVectorHelper(oldX, oldY, oldZ));
		
		// to make collisions for standing on blocks more robust, if the old box is JUST beneath the top of a block, pop it up.
		final double Epsilon = 1e-1;
		double distToNextTop = MathHelper.ceiling_double_int(oldEntityBox.minY) - oldEntityBox.minY;
		if (distToNextTop <= Epsilon) {
			oldEntityBox.minY += distToNextTop;
			oldEntityBox.maxY += distToNextTop;
		}
		
		// get a box for the entity's current position
		AxisAlignedBB newEntityBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getEntityBoxInBlockSpace(newEntityBox, entity);
		
		// adjust the old box for the old ySize
		double dYSize = entity.ySize - oldYSize;
		oldEntityBox.maxY -= dYSize;
		oldEntityBox.minY -= dYSize;
		
		if (Environment.isClient() && ShipDebugRenderInfo.isDebugRenderingOn() && entity instanceof EntityLivingBase) {
			m_debugRenderInfo.setQueryBox(entity, oldEntityBox);
		}
		
		// get the deltas in blocks coordinates
		double originalDx = newEntityBox.minX - oldEntityBox.minX;
		double originalDy = newEntityBox.minY - oldEntityBox.minY;
		double originalDz = newEntityBox.minZ - oldEntityBox.minZ;
		
		double dx = originalDx;
		double dy = originalDy;
		double dz = originalDz;
		
		boolean isPlayerCrouching = entity.onGround && entity.isSneaking() && entity instanceof EntityPlayer;
		if (isPlayerCrouching) {
			// for walk-over prevention, move the query box just a little bit farther out to avoid precision/roundoff problems
			final double BufferSize = 0.05;
			double bufferX = dx > 0 ? BufferSize : -BufferSize;
			double bufferZ = dz > 0 ? BufferSize : -BufferSize;
			
			// reduce the movement delta to ensure player is always standing on a ship block
			final double StepSize = 0.05;
			while (dx != 0 && m_ship.getShipWorld().getGeometry().rangeQuery(oldEntityBox.getOffsetBoundingBox(dx + bufferX, -1.0, 0.0)).isEmpty()) {
				dx = stepTowardsZero(dx, StepSize);
			}
			while (dz != 0 && m_ship.getShipWorld().getGeometry().rangeQuery(oldEntityBox.getOffsetBoundingBox(0.0, -1.0, dz + bufferZ)).isEmpty()) {
				dz = stepTowardsZero(dz, StepSize);
			}
			while (dx != 0 && dz != 0 && m_ship.getShipWorld().getGeometry().rangeQuery(oldEntityBox.getOffsetBoundingBox(dx + bufferX, -1.0, dz + bufferZ)).isEmpty()) {
				dx = stepTowardsZero(dx, StepSize);
				dz = stepTowardsZero(dz, StepSize);
			}
			
			// update the new entity box position
			newEntityBox.minX = oldEntityBox.minX + dx;
			newEntityBox.maxX = oldEntityBox.maxX + dx;
			newEntityBox.minZ = oldEntityBox.minZ + dz;
			newEntityBox.maxZ = oldEntityBox.maxZ + dz;
		}
		
		List<PossibleCollision> possibleCollisions = trajectoryQuery(oldEntityBox, newEntityBox);
		
		if (Environment.isClient() && ShipDebugRenderInfo.isDebugRenderingOn() && entity instanceof EntityLivingBase) {
			for (PossibleCollision collision : possibleCollisions) {
				m_debugRenderInfo.addCollidedCoord(collision.coords);
			}
		}
		
		// no collisions? No changes needed
		if (possibleCollisions.isEmpty()) {
			return;
		}
		
		// calculate the actual collision
		// move along the manhattan path, stopping at the first collision
		// y first, then x, then z
		// different orders should give different collisions,
		// but for a small enough d vector, the difference should be un-noticeable
		for (PossibleCollision collision : possibleCollisions) {
			dy = collision.box.calculateYOffset(oldEntityBox, dy);
		}
		dy = applyBackoff(dy, originalDy);
		oldEntityBox.offset(0, dy, 0);
		
		for (PossibleCollision collision : possibleCollisions) {
			dx = collision.box.calculateXOffset(oldEntityBox, dx);
		}
		dx = applyBackoff(dx, originalDx);
		oldEntityBox.offset(dx, 0, 0);
		
		for (PossibleCollision collision : possibleCollisions) {
			dz = collision.box.calculateZOffset(oldEntityBox, dz);
		}
		dz = applyBackoff(dz, originalDz);
		oldEntityBox.offset(0, 0, dz);
		
		// handle stairs/slabs
		if (entity.stepHeight > 0 && (originalDx != dx || originalDz != dz) && originalDy != dy) {
			// stupid roundoff error grumble grumble...
			final double EpsilonStairs = 1e-6;
			
			// pop up the target over the step height
			newEntityBox.minY = oldEntityBox.minY + entity.stepHeight + EpsilonStairs;
			newEntityBox.maxY = oldEntityBox.maxY + entity.stepHeight + EpsilonStairs;
			possibleCollisions = trajectoryQuery(oldEntityBox, newEntityBox);
			
			// what's the rest of the distance to the target?
			double originalStairsDx = newEntityBox.minX - oldEntityBox.minX;
			double originalStairsDy = newEntityBox.minY - oldEntityBox.minY;
			double originalStairsDz = newEntityBox.minZ - oldEntityBox.minZ;
			
			double stairsDx = originalStairsDx;
			double stairsDy = originalStairsDy;
			double stairsDz = originalStairsDz;
			
			AxisAlignedBB tempBox = oldEntityBox.copy();
			if (!possibleCollisions.isEmpty()) {
				for (PossibleCollision collision : possibleCollisions) {
					stairsDy = collision.box.calculateYOffset(tempBox, stairsDy);
				}
				stairsDy = applyBackoff(stairsDy, originalDy);
				tempBox.offset(0, stairsDy, 0);
				
				for (PossibleCollision collision : possibleCollisions) {
					stairsDx = collision.box.calculateXOffset(tempBox, stairsDx);
				}
				stairsDx = applyBackoff(stairsDx, originalDx);
				tempBox.offset(stairsDx, 0, 0);
				
				for (PossibleCollision collision : possibleCollisions) {
					stairsDz = collision.box.calculateZOffset(tempBox, stairsDz);
				}
				stairsDz = applyBackoff(stairsDz, originalDz);
				tempBox.offset(0, 0, stairsDz);
			} else {
				tempBox.offset(stairsDx, stairsDy, stairsDz);
			}
			
			// did we step up?
			if (Math.abs(stairsDx) > EpsilonStairs || Math.abs(stairsDz) > EpsilonStairs) {
				// apply the change
				dx += stairsDx;
				dy += stairsDy;
				dz += stairsDz;
				oldEntityBox.setBB(tempBox);
			}
		}
		
		// translate back into world coordinates
		Vec3 newPos = Vec3.createVectorHelper( (oldEntityBox.minX + oldEntityBox.maxX) / 2, oldEntityBox.minY, (oldEntityBox.minZ + oldEntityBox.maxZ) / 2);
		m_ship.blocksToShip(newPos);
		m_ship.shipToWorld(newPos);
		
		// update the entity properties
		entity.setPosition(newPos.xCoord, newPos.yCoord + entity.yOffset - entity.ySize, newPos.zCoord);
		entity.isCollidedHorizontally = originalDx != dx || originalDz != dz;
		entity.isCollidedVertically = originalDy != dy;
		entity.onGround = entity.isCollidedVertically && originalDy < 0;
		entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
		
		// if we collided, kill the velocity
		if (originalDx != dx) {
			entity.motionX = 0;
		}
		if (originalDy != dy) {
			entity.motionY = 0;
		}
		if (originalDz != dz) {
			entity.motionZ = 0;
		}
		
		// update fall state. Sadly, we can't just call this:
		// entity.updateFallState( dy, entity.onGround );
		// so we're going have to do it using package injection
		EntityAccessor.updateFallState(entity, dy, entity.onGround);
	}
	
	public AxisAlignedBB getBlockBoxInBlockSpace(Coords coords) {
		Block block = m_ship.getShipWorld().getBlock(coords);
		
		// if this is an air block, then use another block to get bounds
		if (block == null) {
			block = Blocks.stone;
		}
		
		block.setBlockBoundsBasedOnState(m_ship.getShipWorld(), coords.x, coords.y, coords.z);
		return AxisAlignedBB.getBoundingBox(
			block.getBlockBoundsMinX() + coords.x, block.getBlockBoundsMinY() + coords.y, block.getBlockBoundsMinZ() + coords.z,
			block.getBlockBoundsMaxX() + coords.x, block.getBlockBoundsMaxY() + coords.y, block.getBlockBoundsMaxZ() + coords.z
		);
	}
	
	public void getCollisionBoxesInBlockSpace(List<AxisAlignedBB> out, Coords coords, AxisAlignedBB box) {
		Block block = m_ship.getShipWorld().getBlock(coords);
		block.addCollisionBoxesToList(m_ship.getShipWorld(), coords.x, coords.y, coords.z, box, out, null);
	}
	
	public RotatedBB getBlockBoxInWorldSpace(Coords coords) {
		return m_ship.blocksToWorld(getBlockBoxInBlockSpace(coords));
	}
	
	public AxisAlignedBB getBlockWorldBoundingBox(AxisAlignedBB box, Coords coords) {
		return getBlockBoundingBox(box, coords, null);
	}
	
	public AxisAlignedBB getBlockBoundingBox(AxisAlignedBB box, Coords coords, EntityShip ship) {
		// transform the block center into world space
		Vec3 p = Vec3.createVectorHelper(coords.x + 0.5, coords.y + 0.5, coords.z + 0.5);
		m_ship.blocksToShip(p);
		m_ship.shipToWorld(p);
		
		if (ship != null) {
			// and then into ship space
			ship.worldToShip(p);
			ship.shipToBlocks(p);
		}
		
		// compute the halfwidth of the bounding box
		float yawRad = (float)Math.toRadians(m_ship.rotationYaw);
		if (ship != null) {
			yawRad -= (float)Math.toRadians(ship.rotationYaw);
		}
		double cos = MathHelper.cos(yawRad);
		double sin = MathHelper.sin(yawRad);
		double halfSize = Math.max(Math.abs(cos - sin), Math.abs(sin + cos)) / 2;
		
		return box.setBounds(
			p.xCoord - halfSize, p.yCoord - 0.5, p.zCoord - halfSize,
			p.xCoord + halfSize, p.yCoord + 0.5, p.zCoord + halfSize
		);
	}
	
	public AxisAlignedBB getBlockWorldBoundingBox(AxisAlignedBB box, Coords coords, double shipX, double shipY, double shipZ, float shipYaw) {
		return getBlockBoundingBox(box, coords, shipX, shipY, shipZ, shipYaw, null);
	}
	
	public AxisAlignedBB getBlockBoundingBox(AxisAlignedBB box, Coords coords, double shipX, double shipY, double shipZ, float shipYaw, EntityShip ship) {
		// temporarily place the ship at the new position
		double oldX = m_ship.posX;
		double oldY = m_ship.posY;
		double oldZ = m_ship.posZ;
		float oldYaw = m_ship.rotationYaw;
		
		// temporarily move the ship to the new location
		m_ship.posX = shipX;
		m_ship.posY = shipY;
		m_ship.posZ = shipZ;
		m_ship.rotationYaw = shipYaw;
		
		AxisAlignedBB blockWorldBox = getBlockBoundingBox(box, coords, ship);
		
		// restore the ship before anyone notices =P
		m_ship.posX = oldX;
		m_ship.posY = oldY;
		m_ship.posZ = oldZ;
		m_ship.rotationYaw = oldYaw;
		
		return blockWorldBox;
	}
	
	public void moveShip(double dx, double dy, double dz, float dYaw) {
		
		// compute the scaling of the delta (between 0 and 1) that avoids collisions
		double scaling = 1.0;
		int numCollidingBoxes = 0;
		CollisionResult collisionResult = new CollisionResult();
		for (Coords coords : m_ship.getShipWorld().coords()) {
			checkBlockCollision(collisionResult, coords, dx, dy, dz, dYaw);
			if (collisionResult.scaling < 1.0) {
				scaling = Math.min(scaling, collisionResult.scaling);
				numCollidingBoxes += collisionResult.numCollidingBoxes;
			}
		}
		
		// look for collisions with other ships
		AxisAlignedBB nextShipBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		computeShipBoundingBox(nextShipBox, m_ship.posX + dx, m_ship.posY + dy, m_ship.posZ + dz, m_ship.rotationYaw + dYaw);
		AxisAlignedBB queryBox = m_ship.boundingBox.func_111270_a(nextShipBox);
		@SuppressWarnings("unchecked")
		List<EntityShip> ships = (List<EntityShip>)m_ship.worldObj.getEntitiesWithinAABB(EntityShip.class, queryBox);
		for (EntityShip ship : ships) {
			if (ship == m_ship) {
				// skip self
				continue;
			}
			
			// handle inter-ship collisions
			for (Coords coords : m_ship.getShipWorld().coords()) {
				checkShipCollision(collisionResult, coords, dx, dy, dz, dYaw, ship);
				if (collisionResult.scaling < 1.0) {
					scaling = Math.min(scaling, collisionResult.scaling);
					numCollidingBoxes += collisionResult.numCollidingBoxes;
				}
			}
		}

		// avoid the collision
		dx *= scaling;
		dy *= scaling;
		dz *= scaling;
		
		// if there are any collisions, don't try to compute the dYaw that avoids the collision
		// just kill any rotation
		if (numCollidingBoxes > 0) {
			dYaw = 0;
		}
		
		// apply the new delta
		m_ship.rotationYaw += dYaw;
		m_ship.setPosition(m_ship.posX + dx, m_ship.posY + dy, m_ship.posZ + dz);
		
		// we just moved the ship. Push any colliding entities out of the way
		@SuppressWarnings("unchecked")
		List<Entity> entities = (List<Entity>)m_ship.worldObj.getEntitiesWithinAABB(Entity.class, m_ship.boundingBox);
		for (Entity entity : entities) {
			if (entity instanceof EntityShip) {
				// don't push ships here
				return;
			}
			
			// the src position is the current position, but moved by the delta
			double srcX = entity.posX + dx;
			double srcY = entity.posY + dy;
			double srcZ = entity.posZ + dz;
			double srcYSize = entity.ySize;
			
			// try to move the entity along the trajectory
			onNearbyEntityMoved(srcX, srcY, srcZ, srcYSize, entity);
		}
	}
	
	public List<Entity> getRiders() {
		// get all nearby entities
		AxisAlignedBB checkBox = m_ship.boundingBox.expand(1, 1, 1);
		@SuppressWarnings("unchecked")
		List<Entity> entities = m_ship.worldObj.getEntitiesWithinAABB(Entity.class, checkBox);
		
		// remove any entities from the list not close enough to be considered riding
		// also remove entities that are floating or moving upwards (e.g. jumping)
		Iterator<Entity> iter = entities.iterator();
		while (iter.hasNext()) {
			Entity entity = iter.next();
			if (entity instanceof EntityShip || !isEntityAboard(entity)) {
				iter.remove();
			}
		}
		return entities;
	}
	
	public boolean isEntityAboard(Entity entity) {
		if (entity instanceof EntityLivingBase) {
			return isEntityStandingOnBlock(entity) || isEntityOnLadder((EntityLivingBase)entity);
		}
		return isEntityStandingOnBlock(entity);
	}
	
	public boolean isEntityStandingOnBlock(Entity entity) {
		// get the bounding box of the entity in block space
		AxisAlignedBB checkBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getEntityBoxInBlockSpace(checkBox, entity);
		
		// change the box so it only occupies space JUST UNDER the entity
		checkBox.maxY = checkBox.minY;
		checkBox.minY -= 0.1;
		
		// get the list of nearby boxes that could be colliding
		List<AxisAlignedBB> nearbyBoxes = new ArrayList<AxisAlignedBB>();
		for (Coords coords : m_ship.getShipWorld().getGeometry().rangeQuery(checkBox.expand(0, 1, 0))) {
			getCollisionBoxesInBlockSpace(nearbyBoxes, coords, checkBox);
		}
		return !nearbyBoxes.isEmpty();
	}
	
	public boolean isEntityOnLadder(EntityLivingBase entity) {
		ShipWorld shipWorld = m_ship.getShipWorld();
		AxisAlignedBB entityBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getEntityBoxInBlockSpace(entityBox, entity);
		for (Coords coords : shipWorld.getGeometry().rangeQuery(entityBox)) {
			Block block = shipWorld.getBlock(coords);
			if (block != Blocks.air && block.isLadder(shipWorld, coords.x, coords.y, coords.z, entity)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isColliding(AxisAlignedBB box) {
		// UNDONE: can optimize this by converting box into ship coords and doing a range query
		
		for (Coords coords : m_ship.getShipWorld().coords()) {
			AxisAlignedBB shipBlockBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
			getBlockWorldBoundingBox(shipBlockBox, coords);
			
			if (shipBlockBox.intersectsWith(box)) {
				return true;
			}
		}
		return false;
	}
	
	public List<MovingObjectPosition> lineSegmentQuery(final Vec3 from, Vec3 to) {
		// do a range query using the bounding box of the line segment
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
			Math.min(from.xCoord, to.xCoord), Math.min(from.yCoord, to.yCoord), Math.min(from.zCoord, to.zCoord),
			Math.max(from.xCoord, to.xCoord), Math.max(from.yCoord, to.yCoord), Math.max(from.zCoord, to.zCoord)
		);
		BlockSet nearbyBlocks = m_ship.getShipWorld().getGeometry().rangeQuery(box);
		
		// sort the boxes by their line/box intersection distance to the "from" point
		// throw out boxes that don't actually intersect the line segment
		List<MovingObjectPosition> intersections = new ArrayList<MovingObjectPosition>();
		for (Coords coords : nearbyBlocks) {
			// get the intersection point with the line segment
			Block block = m_ship.getShipWorld().getBlock(coords);
			MovingObjectPosition intersection = block.collisionRayTrace(m_ship.getShipWorld(), coords.x, coords.y, coords.z, from, to);
			if (intersection != null) {
				intersections.add(intersection);
			}
		}
		return intersections;
	}
	
	public double getDistanceSqToEntity(Entity entity) {
		// find the nearest neighbor block of entity
		// UNDONE: could optimize this with something like a kd tree, or an oct-tree,
		// but I'm lazy. This is probably fast enough for now
		
		// get the point of the entity in blocks space
		Vec3 p = Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
		m_ship.worldToShip(p);
		m_ship.shipToBlocks(p);
		
		double minDistSq = Double.POSITIVE_INFINITY;
		for (Coords coords : m_ship.getShipWorld().coords()) {
			double dx = coords.x - p.xCoord;
			double dy = coords.y - p.yCoord;
			double dz = coords.z - p.zCoord;
			double dist = dx * dx + dy * dy + dz * dz;
			minDistSq = Math.min(minDistSq, dist);
		}
		return Math.sqrt(minDistSq);
	}
	
	public void getIntersectingWorldBlocks(BlockSet worldBlocks, BlockSet shipBlocks) {
		getIntersectingWorldBlocks(worldBlocks, shipBlocks, 0, false);
	}
	
	public void getIntersectingWorldBlocks(BlockSet worldBlocks, BlockSet shipBlocks, double epsilon, boolean includeAir) {
		// find the world blocks that intersect the trapped air blocks
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		for (Coords coords : shipBlocks) {
			getBlockWorldBoundingBox(box, coords);
			
			// grow the bounding box just a bit so we get more robust collisions
			box = box.expand(epsilon, epsilon, epsilon);
			
			// query for all the world blocks that intersect it
			BlockUtils.worldRangeQuery(worldBlocks, m_ship.worldObj, box, includeAir);
		}
	}
	
	private void checkBlockCollision(CollisionResult result, Coords coords, double dx, double dy, double dz, float dYaw) {
		// get the current world bounding box for the ship block
		AxisAlignedBB shipBlockBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getBlockWorldBoundingBox(shipBlockBox, coords);
		
		// where would the ship block move to?
		double nextX = m_ship.posX + dx;
		double nextY = m_ship.posY + dy;
		double nextZ = m_ship.posZ + dz;
		float nextYaw = m_ship.rotationYaw + dYaw;
		AxisAlignedBB nextShipBlockBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getBlockWorldBoundingBox(nextShipBlockBox, coords, nextX, nextY, nextZ, nextYaw);
		
		// func_111270_a returns the bounding box of both boxes
		AxisAlignedBB combinedBlockBox = shipBlockBox.func_111270_a(nextShipBlockBox);
		
		// do a range query to get colliding world blocks
		BlockSet nearbyWorldBlocks = new BlockSet();
		BlockUtils.worldRangeQuery(nearbyWorldBlocks, m_ship.worldObj, combinedBlockBox);
		
		// get the scaling that avoids the collision
		result.scaling = 1;
		List<AxisAlignedBB> worldBlockBoxes = new ArrayList<AxisAlignedBB>();
		for (Coords worldCoords : nearbyWorldBlocks) {
			// get the block collision boxes
			Block worldBlock = m_ship.worldObj.getBlock(worldCoords.x, worldCoords.y, worldCoords.z);
			worldBlockBoxes.clear();
			worldBlock.addCollisionBoxesToList(m_ship.worldObj, worldCoords.x, worldCoords.y, worldCoords.z, combinedBlockBox, worldBlockBoxes, null);
			
			// determine the scaling for this block
			double blockScaling = 1;
			for (AxisAlignedBB worldBlockBox : worldBlockBoxes) {
				blockScaling = Math.min(blockScaling, getScalingToAvoidCollision(shipBlockBox, dx, dy, dz, worldBlockBox));
			}
			
			// did this block impede us? and should we break it?
			if (blockScaling < 1 && worldBlock instanceof BlockFlower) {
				// destroyBlock()
				m_ship.worldObj.func_147480_a(worldCoords.x, worldCoords.y, worldCoords.z, false);
			} else {
				result.scaling = Math.min(result.scaling, blockScaling);
			}
		}
		result.numCollidingBoxes = nearbyWorldBlocks.size();
	}
	
	private void checkShipCollision(CollisionResult result, Coords coords, double dx, double dy, double dz, float dYaw, EntityShip ship) {
		// NOTE: all inter-ship collision calculations take place in the other ship's coordinate system
		
		// get the current bounding box for the ship block
		AxisAlignedBB shipBlockBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getBlockBoundingBox(shipBlockBox, coords, ship);
		
		// where would the ship block move to?
		double nextX = m_ship.posX + dx;
		double nextY = m_ship.posY + dy;
		double nextZ = m_ship.posZ + dz;
		float nextYaw = m_ship.rotationYaw + dYaw;
		AxisAlignedBB nextShipBlockBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		getBlockBoundingBox(nextShipBlockBox, coords, nextX, nextY, nextZ, nextYaw, ship);
		
		// get the collisions with the other ship
		List<PossibleCollision> possibleCollisions = trajectoryQuery(shipBlockBox, nextShipBlockBox);
		
		// get the scaling that avoids the collisions
		result.scaling = 1;
		result.numCollidingBoxes = 0;
		for (PossibleCollision possibleCollision : possibleCollisions) {
			double scaling = getScalingToAvoidCollision(shipBlockBox, dx, dy, dz, possibleCollision.box);
			result.scaling = Math.min(result.scaling, scaling);
			if (scaling < 1) {
				result.numCollidingBoxes++;
			}
		}
	}
	
	private double getScalingToAvoidCollision(AxisAlignedBB box, double dx, double dy, double dz, AxisAlignedBB obstacleBox) {
		double sx = getScalingToAvoidCollision(dx, box.minX, box.maxX, obstacleBox.minX, obstacleBox.maxX);
		double sy = getScalingToAvoidCollision(dy, box.minY, box.maxY, obstacleBox.minY, obstacleBox.maxY);
		double sz = getScalingToAvoidCollision(dz, box.minZ, box.maxZ, obstacleBox.minZ, obstacleBox.maxZ);
		return Math.min(sx, Math.min(sy, sz));
	}
	
	private double getScalingToAvoidCollision(double delta, double selfMin, double selfMax, double obstacleMin, double obstacleMax) {
		// by default, don't scale the delta
		double scaling = 1;
		
		if (delta > 0) {
			double dist = obstacleMin - selfMax;
			if (dist >= 0) {
				return dist / delta;
			}
		} else if (delta < 0) {
			double dist = selfMin - obstacleMax;
			if (dist >= 0) {
				return dist / -delta;
			}
		}
		
		assert (scaling >= 0);
		return scaling;
	}
	
	private double applyBackoff(double d, double originalD) {
		// what is backoff and why do we need it?
		// the entity/world collision system doesn't use backoff...
		
		// Due to roundoff errors in world/blocks coordinate conversions,
		// sometimes collision calculations place entity JUST INSIDE an obstacle
		// Even though an intifintessimal translation would put the entity correctly outside of the obstacle,
		// once inside the obstacle, the entity can continue movement unimpeded.
		
		// either we have to detect (and prevent) when an entity is already colliding with an obstacle
		// (which might be preferable, but then players could get "stuck" in geometry),
		// or we have to place the entity so it is JUST OUTSIDE the obstacle after a collision,
		// instead of exactly on the boundary
		
		// distance in game coords, but small enough not to notice
		final double Backoff = 1e-3;
		
		if (d != 0 && d != originalD) {
			// apply the backoff in the opposite direction of the delta
			if (d > 0) {
				return d - Backoff;
			} else {
				return d + Backoff;
			}
		}
		return d;
	}
	
	private List<PossibleCollision> trajectoryQuery(AxisAlignedBB oldBox, AxisAlignedBB newBox) {
		// get a bounding box containing the entire entity trajectory
		AxisAlignedBB trajectoryBox = oldBox.func_111270_a(newBox);
		
		// collect the boxes for the blocks in the trajectory box
		List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
		List<PossibleCollision> collisions = new ArrayList<PossibleCollision>();
		if (m_ship != null && m_ship.getShipWorld() != null && m_ship.getShipWorld().getGeometry() != null)
		// NOTE: if one of these things is null, the ship probably hasn't loaded yet, so there shouldn't be any collisions
		{
			for (Coords coords : m_ship.getShipWorld().getGeometry().rangeQuery(trajectoryBox.expand(1, 1, 1)))
			// NOTE: expand trajectoryBox by 1 so we pick up boxes whose collision boxes are outside their bounding boxes
			{
				boxes.clear();
				getCollisionBoxesInBlockSpace(boxes, coords, trajectoryBox);
				for (AxisAlignedBB box : boxes) {
					collisions.add(new PossibleCollision(coords, box));
				}
			}
		}
		return collisions;
	}
	
	private void getEntityBoxInBlockSpace(AxisAlignedBB box, Entity entity) {
		getEntityBoxInBlockSpace(box, entity, Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ));
	}
	
	private void getEntityBoxInBlockSpace(AxisAlignedBB box, Entity entity, Vec3 pos) {
		// copy the vector since we need to modify it
		pos = Vec3.createVectorHelper(pos.xCoord, pos.yCoord, pos.zCoord);
		
		// transform to block coords
		m_ship.worldToShip(pos);
		m_ship.shipToBlocks(pos);
		
		// set the box here
		box.setBB(entity.boundingBox);
		box.offset(-entity.posX, -entity.posY, -entity.posZ);
		box.offset(pos.xCoord, pos.yCoord, pos.zCoord);
	}
	
	private double stepTowardsZero(double val, double epsilon) {
		if (val < epsilon && val >= -epsilon) {
			val = 0;
		} else if (val > 0) {
			val -= epsilon;
		} else {
			val += epsilon;
		}
		return val;
	}
}
