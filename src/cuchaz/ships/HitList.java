/*******************************************************************************
 * Copyright (c) 2014 jeff.
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
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.EntityUtils;

public class HitList {
	
	public static enum Type {
		Ship, World;
	}
	
	public class Entry implements Comparable<Entry> {
		
		public Type type;
		public double dist;
		public MovingObjectPosition hit;
		
		public Entry(Type type, double dist, MovingObjectPosition hit) {
			this.type = type;
			this.dist = dist;
			this.hit = hit;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				return equals((Entry)obj);
			}
			return false;
		}
		
		public boolean equals(Entry other) {
			return type == other.type && dist == other.dist && hit == other.hit;
		}
		
		@Override
		public int compareTo(Entry other) {
			return Double.compare(dist, other.dist);
		}
	}
	
	private TreeSet<Entry> m_entries;
	
	public HitList() {
		m_entries = new TreeSet<Entry>();
	}
	
	public Entry getClosestHit() {
		if (m_entries.isEmpty()) {
			return null;
		}
		return m_entries.first();
	}
	
	public List<Entry> toList() {
		return new ArrayList<Entry>(m_entries);
	}
	
	public void addHits(World world, Vec3 from, Vec3 to) {
		// NOTE: World.clip() changes its input vectors! Need to protect them since we use them later
		MovingObjectPosition hit = world.rayTraceBlocks(Vec3.createVectorHelper(from.xCoord, from.yCoord, from.zCoord), Vec3.createVectorHelper(to.xCoord, to.yCoord, to.zCoord));
		if (hit == null) {
			return;
		}
		
		m_entries.add(new Entry(Type.World, from.distanceTo(hit.hitVec), hit));
		
		/*
		 * TODO: that was blocks, now check entities AxisAlignedBB queryBox = AxisAlignedBB.getBoundingBox( from.xCoord, from.yCoord, from.zCoord, from.xCoord, from.yCoord, from.zCoord ); queryBox.expand( to.xCoord, to.yCoord, to.zCoord );
		 * 
		 * @SuppressWarnings( "unchecked" ) List<Entity> entities = (List<Entity>)world.getEntitiesWithinAABB( Entity.class, queryBox ); for( Entity entity : entities ) { // TODO: make a hit entry type for entities and check all these world entities }
		 */
	}
	
	public void addHits(EntityShip ship, Vec3 from, Vec3 to) {
		// convert the positions into blocks space
		Vec3 shipFrom = Vec3.createVectorHelper(from.xCoord, from.yCoord, from.zCoord);
		ship.worldToShip(shipFrom);
		ship.shipToBlocks(shipFrom);
		Vec3 shipTo = Vec3.createVectorHelper(to.xCoord, to.yCoord, to.zCoord);
		ship.worldToShip(shipTo);
		ship.shipToBlocks(shipTo);
		
		for (MovingObjectPosition hit : ship.getCollider().lineSegmentQuery(shipFrom, shipTo)) {
			// convert hit vec back to world coords
			ship.blocksToShip(hit.hitVec);
			ship.shipToWorld(hit.hitVec);
			
			m_entries.add(new Entry(Type.Ship, from.distanceTo(hit.hitVec), hit));
		}
	}
	
	public void addHits(World world, EntityPlayer player, double reachDist) {
		if (player == null) {
			throw new IllegalArgumentException("player cannot be null!");
		}
		
		Vec3 eyePos = EntityUtils.getPlayerEyePos(player);
		Vec3 lookDir = EntityUtils.getPlayerLookDirection(player);
		Vec3 targetPos = eyePos.addVector(lookDir.xCoord * reachDist, lookDir.yCoord * reachDist, lookDir.zCoord * reachDist);
		
		addHits(world, eyePos, targetPos);
	}
	
	public void addHits(EntityShip ship, EntityPlayer player, double reachDist) {
		if (player == null) {
			throw new IllegalArgumentException("player cannot be null!");
		}
		
		Vec3 eyePos = EntityUtils.getPlayerEyePos(player);
		Vec3 lookDir = EntityUtils.getPlayerLookDirection(player);
		Vec3 targetPos = eyePos.addVector(lookDir.xCoord * reachDist, lookDir.yCoord * reachDist, lookDir.zCoord * reachDist);
		
		addHits(ship, eyePos, targetPos);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (Entry entry : m_entries) {
			buf.append(entry.type);
			buf.append(":");
			buf.append(String.format("%.2f", entry.dist));
		}
		return buf.toString();
	}
}
