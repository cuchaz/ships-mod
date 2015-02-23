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
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockArray;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.blocks.Envelopes;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.packets.PacketShipLaunched;

public class ShipLauncher {
	
	public static enum LaunchFlag {
		RightNumberOfBlocks {
			
			@Override
			public boolean computeValue(ShipLauncher launcher) {
				return launcher.m_blocks != null && !launcher.m_blocks.isEmpty() && launcher.m_blocks.size() <= launcher.m_shipType.getMaxNumBlocks() + 1; // +1 since the ship block is free
			}
		},
		WillItFloat {
			
			@Override
			public boolean computeValue(ShipLauncher launcher) {
				if (launcher.m_blocks == null || launcher.m_equilibriumWaterHeight == null) {
					return false;
				}
				
				return launcher.m_equilibriumWaterHeight < launcher.getShipBoundingBox().maxY + 1;
			}
		};
		
		public abstract boolean computeValue(ShipLauncher launcher);
	}
	
	public static final Neighbors ShipBlockNeighbors = Neighbors.Edges;
	
	private World m_world;
	private Coords m_shipBlock;
	private ShipType m_shipType;
	private BlockSet m_blocks; // NOTE: blocks are in world coordinates
	private List<Boolean> m_launchFlags;
	private ShipWorld m_shipWorld;
	private ShipPhysics m_shipPhysics;
	private Double m_equilibriumWaterHeight;
	private Integer m_sinkWaterHeight;
	private int m_numBlocksChecked;
	
	public ShipLauncher(final World world, Coords shipBlock) {
		m_world = world;
		m_shipBlock = shipBlock;
		
		// get the ship type from the block
		m_shipType = Ships.m_blockShip.getShipType(world, m_shipBlock.x, m_shipBlock.y, m_shipBlock.z);
		
		// determine how many blocks to check
		int numBlocksToCheck = getNumBlocksToCheck();
		
		// find all the blocks connected to the ship block
		m_blocks = BlockUtils.searchForBlocks(m_shipBlock.x, m_shipBlock.y, m_shipBlock.z, numBlocksToCheck, new BlockExplorer() {
			
			@Override
			public boolean shouldExploreBlock(Coords coords) {
				return !BlockProperties.isSeparator(world.getBlock(coords.x, coords.y, coords.z));
			}
		}, ShipBlockNeighbors);
		
		if (m_blocks != null) {
			m_numBlocksChecked = m_blocks.size();
			
			// did we find too many blocks?
			if (m_blocks.size() > m_shipType.getMaxNumBlocks()) {
				m_blocks = null;
			} else {
				// also add the ship block
				m_blocks.add(m_shipBlock);
				
				m_shipWorld = new ShipWorld(m_world, m_shipBlock, m_blocks);
				m_shipPhysics = new ShipPhysics(m_shipWorld.getBlocksStorage());
				m_equilibriumWaterHeight = m_shipPhysics.getEquilibriumWaterHeight();
				m_sinkWaterHeight = m_shipPhysics.getSinkWaterHeight();
			}
		} else {
			// we found WAY too many blocks
			m_shipWorld = null;
			m_shipPhysics = null;
			m_equilibriumWaterHeight = null;
			m_sinkWaterHeight = null;
			m_numBlocksChecked = numBlocksToCheck;
		}
		
		// compute the launch flags
		m_launchFlags = new ArrayList<Boolean>();
		for (LaunchFlag flag : LaunchFlag.values()) {
			m_launchFlags.add(flag.computeValue(this));
		}
	}
	
	public Coords getShipBlock() {
		return m_shipBlock;
	}
	
	public ShipType getShipType() {
		return m_shipType;
	}
	
	public ShipWorld getShipWorld() {
		return m_shipWorld;
	}
	
	public ShipPhysics getShipPhysics() {
		return m_shipPhysics;
	}
	
	public int getNumBlocks() {
		// don't count the ship block towards the size quota
		return m_blocks.size() - 1;
	}
	
	public int getNumBlocksChecked() {
		return m_numBlocksChecked;
	}
	
	public int getNumBlocksToCheck() {
		return Math.max(100, m_shipType.getMaxNumBlocks() * 2);
	}
	
	public boolean getLaunchFlag(LaunchFlag flag) {
		return m_launchFlags.get(flag.ordinal());
	}
	
	public boolean isLaunchable() {
		boolean isValid = true;
		for (LaunchFlag flag : LaunchFlag.values()) {
			isValid = isValid && getLaunchFlag(flag);
		}
		return isValid;
	}
	
	public BlockSide getShipSide() {
		if (getShipBoundingBox() == null) {
			return null;
		}
		
		// return the widest side of north,west
		if (getShipBoundingBox().getDx() > getShipBoundingBox().getDz()) {
			return BlockSide.North;
		}
		return BlockSide.West;
	}
	
	public BlockSide getShipFront() {
		if (getShipBoundingBox() == null) {
			return null;
		}
		
		// return the thinnest side of north,west
		if (getShipBoundingBox().getDx() > getShipBoundingBox().getDz()) {
			return BlockSide.North;
		}
		return BlockSide.West;
	}
	
	public BoundingBoxInt getShipBoundingBox() {
		if (m_shipWorld == null) {
			return null;
		}
		
		return m_shipWorld.getGeometry().getEnvelopes().getBoundingBox();
	}
	
	public BlockArray getShipEnvelope(BlockSide side) {
		if (m_shipWorld == null) {
			return null;
		}
		
		return m_shipWorld.getGeometry().getEnvelopes().getEnvelope(side);
	}
	
	public Double getEquilibriumWaterHeight() {
		return m_equilibriumWaterHeight;
	}
	
	public Integer getSinkWaterHeight() {
		return m_sinkWaterHeight;
	}
	
	public EntityShip launch() {
		// currently, this is only called on the server
		assert (Environment.isServer());
		
		// spawn the ship
		EntityShip ship = new EntityShip(m_world);
		initShip(ship, m_shipWorld, m_shipBlock);
		
		if (!m_world.spawnEntityInWorld(ship)) {
			Ships.logger.warning("Could not spawn ship in world at (%.2f,%.2f,%.2f)", ship.posX, ship.posY, ship.posZ);
			return null;
		}
		
		// update any berths
		PlayerRespawner.onShipLaunch((WorldServer)m_world, m_shipWorld, m_shipBlock);
		
		// tell clients the ship launched
		Ships.net.getDispatch().sendToAllAround(
			new PacketShipLaunched(ship, m_shipBlock),
			new TargetPoint(ship.worldObj.provider.dimensionId, ship.posX, ship.posY, ship.posZ, 100)
		);
		
		return ship;
	}
	
	public static void initShip(EntityShip ship, ShipWorld shipWorld, Coords shipBlock) {
		Vec3 centerOfMass = new ShipPhysics(shipWorld.getBlocksStorage()).getCenterOfMass();
		
		// set ship properties
		ship.setPositionAndRotation(
			shipBlock.x + centerOfMass.xCoord,
			shipBlock.y + centerOfMass.yCoord,
			shipBlock.z + centerOfMass.zCoord,
			0, 0
		);
		ship.setShipWorld(shipWorld);
		
		removeShipFromWorld(ship.worldObj, shipWorld, shipBlock, UpdateRules.UpdateNoOne);
	}
	
	public static void removeShipFromWorld(World world, ShipWorld shipWorld, Coords shipBlock, UpdateRules updateRules) {
		// translate the ship blocks into world space
		BlockSet worldBlocks = new BlockSet();
		for (Coords blockCoords : shipWorld.coords()) {
			worldBlocks.add(new Coords(blockCoords.x + shipBlock.x, blockCoords.y + shipBlock.y, blockCoords.z + shipBlock.z));
		}
		
		// compute the water height
		Coords waterBlockPos = computeWaterBlock(world, shipWorld, shipBlock);
		Block waterBlock = world.getBlock(waterBlockPos.x, waterBlockPos.y, waterBlockPos.z);
		
		// add 1 to get the top of the water block
		int waterHeight = waterBlockPos.y + 1;
		
		// remove the world blocks, but don't tell the clients. They'll do it later when the ship blocks are sent over
		for (Coords cords : worldBlocks) {
			if (cords.y < waterHeight) {
				BlockUtils.changeBlockWithoutNotifyingIt(world, cords.x, cords.y, cords.z, waterBlock, 0, updateRules);
			} else {
				BlockUtils.removeBlockWithoutNotifyingIt(world, cords.x, cords.y, cords.z, updateRules);
			}
		}
		
		// restore the trapped air to water
		Coords worldCoords = new Coords(0, 0, 0);
		for (Coords blockCoords : shipWorld.getDisplacement().getTrappedAirFromWaterHeight(waterHeight - shipBlock.y)) {
			worldCoords.x = blockCoords.x + shipBlock.x;
			worldCoords.y = blockCoords.y + shipBlock.y;
			worldCoords.z = blockCoords.z + shipBlock.z;
			BlockUtils.changeBlockWithoutNotifyingIt(world, worldCoords.x, worldCoords.y, worldCoords.z, waterBlock, 0, updateRules);
		}
		
		// remove any hanging entities
		for (Map.Entry<Coords,EntityHanging> entry : shipWorld.getNearbyHangingEntities(world, worldBlocks).entrySet()) {
			EntityHanging hangingEntity = entry.getValue();
			
			// remove the hanging entity from the world
			world.removeEntity(hangingEntity);
		}
	}
	
	private static Coords computeWaterBlock(World world, ShipWorld shipWorld, Coords shipBlock) {
		
		Coords highestWaterPos = new Coords();
		
		// for each column in the ship or outside it
		Envelopes envelopes = shipWorld.getGeometry().getEnvelopes();
		for (int x = envelopes.getBoundingBox().minX - 1; x <= envelopes.getBoundingBox().maxX + 1; x++) {
			for (int z = envelopes.getBoundingBox().minZ - 1; z <= envelopes.getBoundingBox().maxZ + 1; z++) {
				int waterHeight = computeWaterHeight(world, shipWorld, shipBlock, x, z);
				if (waterHeight > highestWaterPos.y) {
					highestWaterPos.set(
						shipBlock.x + x,
						waterHeight,
						shipBlock.z + z
					);
				}
			}
		}
		
		return highestWaterPos;
	}
	
	private static int computeWaterHeight(World world, ShipWorld shipWorld, Coords shipBlock, int blockX, int blockZ) {
		
		// start at the top of the box
		Envelopes envelopes = shipWorld.getGeometry().getEnvelopes();
		int x = blockX + shipBlock.x;
		int y = envelopes.getBoundingBox().maxY + 1 + shipBlock.y;
		int z = blockZ + shipBlock.z;
		
		// drop until we hit air
		boolean foundAir = false;
		for (; y >= 0; y--) {
			if (world.getBlock(x, y, z).getMaterial() == Material.air) {
				foundAir = true;
				break;
			}
		}
		
		if (!foundAir) {
			return -1;
		}
		
		// keep dropping until we hit water
		for (; y >= 0; y--) {
			if (world.getBlock(x, y, z).getMaterial().isLiquid()) {
				return y;
			}
		}
		
		return -1;
	}
}
