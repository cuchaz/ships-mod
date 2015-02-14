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

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.math.CircleRange;
import cuchaz.ships.config.BlockProperties;

public class ShipUnlauncher {
	
	public static enum UnlaunchFlag {
		AlignedToDirection(false) {
			
			@Override
			public boolean computeValue(ShipUnlauncher unlauncher) {
				// the yaw has to be within 10 degrees of zero
				return Math.abs(MathHelper.wrapAngleTo180_float(unlauncher.m_ship.rotationYaw)) < 10.0;
			}
		},
		TouchingOnlySeparatorBlocks(true) {
			
			@Override
			public boolean computeValue(ShipUnlauncher unlauncher) {
				// if any placed ship block has a neighbor that's not a ship block and not a separator block, the flag fails
				
				// put all the placed blocks into a data structure that has fast lookup
				BlockSet placedBlocks = new BlockSet();
				placedBlocks.addAll(unlauncher.m_correspondence.values());
				
				// check for the neighbors of each ship block
				Coords neighborCoords = new Coords(0, 0, 0);
				for (Coords coords : placedBlocks) {
					// for each neighbor
					for (BlockSide side : BlockSide.values()) {
						neighborCoords.x = coords.x + side.getDx();
						neighborCoords.y = coords.y + side.getDy();
						neighborCoords.z = coords.z + side.getDz();
						
						// skip ship blocks
						if (placedBlocks.contains(neighborCoords)) {
							continue;
						}
						
						if (!BlockProperties.isSeparator(getBlock(unlauncher.m_ship.worldObj, neighborCoords))) {
							return false;
						}
					}
				}
				
				return true;
			}
		};
		
		private boolean m_allowOverride;
		
		private UnlaunchFlag(boolean allowOverride) {
			m_allowOverride = allowOverride;
		}
		
		public boolean isOverrideAllowed() {
			return m_allowOverride;
		}
		
		public abstract boolean computeValue(ShipUnlauncher unlauncher);
		
		protected Block getBlock(IBlockAccess world, Coords coords) {
			return world.getBlock(coords.x, coords.y, coords.z);
		}
	}
	
	private EntityShip m_ship;
	private List<Boolean> m_unlaunchFlags;
	private BlockMap<Coords> m_correspondence;
	private int m_waterHeightInBlockSpace;
	private double m_deltaRotationRadians;
	private Vec3 m_deltaTranslation;
	
	public ShipUnlauncher(EntityShip ship) {
		m_ship = ship;
		
		// compute the block correspondence
		computeCorrespondence();
		
		// compute the unlaunch flags
		m_unlaunchFlags = new ArrayList<Boolean>();
		for (UnlaunchFlag flag : UnlaunchFlag.values()) {
			m_unlaunchFlags.add(flag.computeValue(this));
		}
	}
	
	private void computeCorrespondence() {
		// compute the block space translation
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		m_ship.blocksToShip(p);
		m_ship.shipToWorld(p);
		int tx = MathHelper.floor_double(p.xCoord + 0.5);
		int ty = MathHelper.ceiling_double_int(p.yCoord);
		int tz = MathHelper.floor_double(p.zCoord + 0.5);
		
		// compute the unlaunch delta
		m_deltaTranslation = Vec3.createVectorHelper(tx - p.xCoord, ty - p.yCoord, tz - p.zCoord);
		
		// determine the water surface level
		m_waterHeightInBlockSpace = MathHelper.floor_double(m_ship.getWaterHeight() + 0.5) - ty;
		
		// get the set of coords we care about
		BlockSet allCoords = new BlockSet();
		allCoords.addAll(m_ship.getShipWorld().coords());
		allCoords.addAll(m_ship.getShipWorld().getDisplacement().getTrappedAirFromWaterHeight(m_waterHeightInBlockSpace));
		
		// compute the snap rotation
		double yaw = CircleRange.mapZeroToTwoPi(Math.toRadians(m_ship.rotationYaw));
		int rotation = Util.realModulus((int) (yaw / Math.PI * 2 + 0.5), 4);
		m_deltaRotationRadians = Math.PI / 2 * rotation - yaw;
		int cos = new int[] { 1, 0, -1, 0 }[rotation];
		int sin = new int[] { 0, 1, 0, -1 }[rotation];
		
		// compute the actual correspondence
		m_correspondence = new BlockMap<Coords>();
		for (Coords coords : allCoords) {
			// rotate the coords
			int x = coords.x * cos + coords.z * sin;
			int z = -coords.x * sin + coords.z * cos;
			Coords worldCoords = new Coords(x, coords.y, z);
			
			// translate to the world
			worldCoords.x += tx;
			worldCoords.y += ty;
			worldCoords.z += tz;
			
			m_correspondence.put(coords, worldCoords);
		}
	}
	
	public boolean getUnlaunchFlag(UnlaunchFlag flag) {
		return m_unlaunchFlags.get(flag.ordinal());
	}
	
	public boolean isUnlaunchable() {
		return isUnlaunchable(false);
	}
	
	public boolean isUnlaunchable(boolean override) {
		boolean isValid = true;
		for (UnlaunchFlag flag : UnlaunchFlag.values()) {
			isValid = isValid && (getUnlaunchFlag(flag) || (override && flag.isOverrideAllowed()));
		}
		return isValid;
	}
	
	public void unlaunch() {
		// server only
		if (Environment.isClient()) {
			return;
		}
		
		// remove the ship entity
		m_ship.setDead();
		
		// restore all the blocks
		m_ship.getShipWorld().restoreToWorld(m_ship.worldObj, m_correspondence, m_waterHeightInBlockSpace);
		
		// update any saved berths
		PlayerRespawner.onShipDock((WorldServer)m_ship.worldObj, m_ship.getShipWorld(), m_correspondence);
	}
	
	public void snapToLaunchDirection() {
		m_ship.setPositionAndRotation(m_ship.posX, m_ship.posY, m_ship.posZ, 0, m_ship.rotationPitch);
	}
	
	public void applyUnlaunch(Entity entity) {
		// apply rotation of position relative to the ship center
		Vec3 p = Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
		p.xCoord = entity.posX + m_deltaTranslation.xCoord;
		p.zCoord = entity.posZ + m_deltaTranslation.zCoord;
		m_ship.worldToShip(p);
		double cos = Math.cos(m_deltaRotationRadians);
		double sin = Math.sin(m_deltaRotationRadians);
		double x = p.xCoord * cos + p.zCoord * sin;
		double z = -p.xCoord * sin + p.zCoord * cos;
		p.xCoord = x;
		p.zCoord = z;
		m_ship.shipToWorld(p);
		
		// apply the transformation
		entity.rotationYaw -= Math.toDegrees(m_deltaRotationRadians);
		entity.setPosition(p.xCoord, entity.posY + m_deltaTranslation.yCoord, p.zCoord);
	}
}
