/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.EntityAccessor;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.entity.player.EntityPlayerAccessor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.blocks.BlockBerth;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketPlayerSleepInBerth;

public class PlayerRespawner {
	
	private static class BerthCoords {
		
		int dimensionId;
		UUID shipUuid;
		int x;
		int y;
		int z;
		
		public BerthCoords() {
			this.dimensionId = 0;
			this.shipUuid = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
		}
		
		public BerthCoords(World world, int x, int y, int z) {
			// sort out the real and ship worlds
			World realWorld = world;
			ShipWorld shipWorld = null;
			if (world instanceof ShipWorld) {
				shipWorld = (ShipWorld)world;
				realWorld = shipWorld.getShip().worldObj;
			}
			
			this.dimensionId = realWorld.provider.dimensionId;
			this.shipUuid = shipWorld != null ? shipWorld.getShip().getPersistentID() : null;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public boolean equals(World world, int x, int y, int z) {
			// easy part first, check the coords
			if (this.x != x || this.y != y || this.z != z) {
				return false;
			}
			
			// hard part last, check the dimension/ship
			if (world instanceof ShipWorld) {
				return this.shipUuid != null && this.shipUuid.equals( ((ShipWorld)world).getShip().getPersistentID());
			} else {
				return this.dimensionId == world.provider.dimensionId;
			}
		}
		
		public World getWorldOnServer() {
			if (this.shipUuid == null) {
				return DimensionManager.getWorld(this.dimensionId);
			}
			
			EntityShip ship = getShipOnServer();
			if (ship != null) {
				return ship.getShipWorld();
			}
			
			return null;
		}
		
		public EntityShip getShipOnServer() {
			if (this.shipUuid == null) {
				return null;
			}
			
			WorldServer realWorld = DimensionManager.getWorld(this.dimensionId);
			return ShipLocator.getShip(realWorld, this.shipUuid);
		}
		
		public void moveToShip(ShipWorld shipWorld, int x, int y, int z) {
			this.shipUuid = shipWorld.getShip().getPersistentID();
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void moveToWorld(int x, int y, int z) {
			this.shipUuid = null;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private static Integer m_serverInstanceId;
	private static Map<Integer,BerthCoords> m_sleepingBerths;
	private static Map<String,BerthCoords> m_playerSavedBerths;
	
	static {
		m_serverInstanceId = null;
		m_sleepingBerths = new TreeMap<Integer,BerthCoords>();
		m_playerSavedBerths = new TreeMap<String,BerthCoords>();
	}
	
	private static Map<String,BerthCoords> getSavedBerths(WorldServer worldServer) {
		checkServerInstance(worldServer);
		return m_playerSavedBerths;
	}
	
	private static void checkServerInstance(WorldServer worldServer) {
		int currentServerInstanceId = System.identityHashCode(worldServer.func_73046_m() /* getMinecraftServer() */);
		if (m_serverInstanceId == null || m_serverInstanceId != currentServerInstanceId) {
			m_sleepingBerths.clear();
			m_playerSavedBerths.clear();
			loadBerths();
		}
	}
	
	public static EnumStatus sleepInBerthAt(World world, int x, int y, int z, EntityPlayer player) {
		// sadly, I have to re-implement some logic from EntityPlayer.sleepInBed() to get this to work...
		
		World realWorld = player.worldObj;
		
		// get the berth position in world coords
		Vec3 bedPos = Vec3.createVectorHelper(x, y, z);
		if (world instanceof ShipWorld) {
			EntityShip ship = ((ShipWorld)world).getShip();
			ship.blocksToShip(bedPos);
			ship.shipToWorld(bedPos);
		}
		
		if (!realWorld.isRemote) {
			// on the server, check for some conditions
			if (player.isPlayerSleeping() || !player.isEntityAlive()) {
				return EnumStatus.OTHER_PROBLEM;
			}
			if (!realWorld.provider.isSurfaceWorld()) {
				return EnumStatus.NOT_POSSIBLE_HERE;
			}
			if (realWorld.isDaytime()) {
				return EnumStatus.NOT_POSSIBLE_NOW;
			}
			
			if (Math.abs(player.posX - bedPos.xCoord) > 3 || Math.abs(player.posY - bedPos.yCoord) > 2 || Math.abs(player.posZ - bedPos.zCoord) > 3) {
				return EnumStatus.TOO_FAR_AWAY;
			}
			
			// are there any mobs nearby?
			int dXZ = 8;
			int dY = 5;
			@SuppressWarnings("unchecked")
			List<EntityMob> mobs = (List<EntityMob>)realWorld.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.getBoundingBox(bedPos.xCoord - dXZ, bedPos.yCoord - dY, bedPos.zCoord - dXZ, bedPos.xCoord + dXZ, bedPos.yCoord + dY, bedPos.zCoord + dXZ));
			if (!mobs.isEmpty()) {
				return EnumStatus.NOT_SAFE;
			}
		}
		
		// NOTE: at this point, we're committed to sleeping
		
		if (player.isRiding()) {
			// stop riding
			player.mountEntity(null);
		}
		
		// move the player to the sleeping position
		EntityAccessor.setSize(player, 0.2F, 0.2F);
		player.yOffset = 0.2F;
		
		if (world.blockExists(x, y, z)) {
			// move the player into the bed
			
			int meta = world.getBlockMetadata(x, y, z);
			int direction = BlockBed.getDirection(meta);
			Block block = world.getBlock(x, y, z);
			if (block != Blocks.air) {
				direction = block.getBedDirection(world, x, y, z);
			}
			float dx = 0.5F;
			float dz = 0.5F;
			
			switch (direction) {
				case 0:
					dz = 0.9F;
				break;
				case 1:
					dx = 0.1F;
				break;
				case 2:
					dz = 0.1F;
				break;
				case 3:
					dx = 0.9F;
			}
			
			// this tweaks the player's render position slightly for sleeping
			// if we don't do it, it's not the end of the world
			// player.func_71013_b( direction );
			
			player.setPosition(bedPos.xCoord + dx, bedPos.yCoord + 0.9375F, bedPos.zCoord + dz);
		} else {
			// umm... we couldn't find the bed. Just make something up
			player.setPosition(bedPos.xCoord + 0.5F, bedPos.yCoord + 0.9375F, bedPos.zCoord + 0.5F);
		}
		
		// set sleeping flags
		EntityPlayerAccessor.setSleeping(player, true);
		setPlayerSleepTimer(player, 0);
		
		// stop all motion
		player.motionX = 0;
		player.motionZ = 0;
		player.motionY = 0;
		
		if (!realWorld.isRemote) {
			realWorld.updateAllPlayersSleepingFlag();
			
			// save this player and berth so we can find it again when the player wakes up
			m_sleepingBerths.put(player.getEntityId(), new BerthCoords(world, x, y, z));
			
			// tell all interested clients that the player started sleeping
			EntityPlayerMP playerServer = (EntityPlayerMP)player;
			PacketPlayerSleepInBerth packet = new PacketPlayerSleepInBerth(player, world, x, y, z);
			for (EntityPlayer trackingPlayer : playerServer.getServerForPlayer().getEntityTracker().getTrackingPlayers(player)) {
				Ships.net.getDispatch().sendTo(packet, (EntityPlayerMP)trackingPlayer);
			}
			playerServer.playerNetServerHandler.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
			Ships.net.getDispatch().sendTo(packet, playerServer);
		}
		
		return EnumStatus.OK;
	}
	
	private static void setPlayerSleepTimer(EntityPlayer player, int val) {
		// the field is private, so we need to hack
		String fieldName = Environment.isObfuscated() ? "field_71076_b" : "sleepTimer";
		try {
			Field field = EntityPlayer.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setInt(player, val);
		} catch (SecurityException ex) {
			throw new Error(ex);
		} catch (NoSuchFieldException ex) {
			throw new Error(ex);
		} catch (IllegalArgumentException ex) {
			throw new Error(ex);
		} catch (IllegalAccessException ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isPlayerInBerth(EntityPlayer player) {
		return m_sleepingBerths.get(player.getEntityId()) != null;
	}
	
	public static boolean isPlayerInBerth(World world, int x, int y, int z) {
		for (BerthCoords coords : m_sleepingBerths.values()) {
			if (coords.equals(world, x, y, z)) {
				return true;
			}
		}
		return false;
	}
	
	public static void onPlayerWakeUp(EntityPlayer player, boolean wasSleepSuccessful) {
		// ignore on clients
		if (player.worldObj.isRemote) {
			return;
		}
		WorldServer worldServer = (WorldServer)player.worldObj;
		
		// ignore interrupted sleep
		if (!wasSleepSuccessful) {
			return;
		}
		
		// what was the last berth the player slept in?
		BerthCoords coords = m_sleepingBerths.get(player.getEntityId());
		if (coords == null) {
			return;
		}
		m_sleepingBerths.remove(player.getEntityId());
		
		// is there a berth there?
		World berthWorld = coords.getWorldOnServer();
		if (berthWorld == null || berthWorld.getBlock(coords.x, coords.y, coords.z) != Ships.m_blockBerth) {
			return;
		}
		
		// save the berth coords
		getSavedBerths(worldServer).put(player.getCommandSenderName(), coords);
		saveBerths();
		
		// remove old spawn location
		player.setSpawnChunk(null, false);
		
		// let the player know the sleeping worked
		player.addChatMessage(new ChatComponentTranslation(GuiString.Slept.getKey()));
	}
	
	public static void onPlayerRespawn(EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, int dimension) {
		// NOTE: if we return without doing anything, the player will respawn at the last saved bed or the world spawn
		
		// ignore on clients
		if (oldPlayer.worldObj.isRemote) {
			return;
		}
		WorldServer worldServer = (WorldServer)newPlayer.worldObj;
		
		if (oldPlayer.getBedLocation(dimension) != null) {
			// since we remove bed info when a player sleeps on a ship,
			// the fact that bed info is here means a player slept on a bed outside of a ship
			// which means the player will respawn at that bed and we shouldn't do anything
			return;
		}
		
		BerthCoords coords = getSavedBerths(worldServer).get(newPlayer.getCommandSenderName());
		if (coords == null) {
			return;
		}
		
		if (coords.shipUuid == null) {
			// spawn at the world pos
			newPlayer.setLocationAndAngles(coords.x, coords.y, coords.z, 0, 0);
		} else {
			// convert the ship berth pos into a world pos and spawn there
			Vec3 p = Vec3.createVectorHelper(coords.x, coords.y, coords.z);
			EntityShip ship = coords.getShipOnServer();
			if (ship != null) {
				ship.blocksToShip(p);
				ship.shipToWorld(p);
				newPlayer.setLocationAndAngles(p.xCoord, p.yCoord, p.zCoord, 0, 0);
			} else {
				// the ship wasn't found =(
				newPlayer.addChatMessage(new ChatComponentTranslation(GuiString.BerthNotFound.getKey()));
			}
		}
	}
	
	public static void onShipLaunch(WorldServer worldServer, ShipWorld shipWorld, Coords shipBlock) {
		// this is only called on the server
		Map<String,BerthCoords> berths = getSavedBerths(worldServer);
		
		boolean changed = false;
		for (Coords coords : shipWorld.coords()) {
			// is this block a berth head?
			if (shipWorld.getBlock(coords) == Ships.m_blockBerth && BlockBerth.isBlockHeadOfBed(shipWorld.getBlockMetadata(coords))) {
				// this berth just launched into a ship
				
				// get the world coords where the berth used to be
				int worldX = shipBlock.x + coords.x;
				int worldY = shipBlock.y + coords.y;
				int worldZ = shipBlock.z + coords.z;
				
				// update any spawn points
				for (BerthCoords berth : berths.values()) {
					if (berth.equals(worldServer, worldX, worldY, worldZ)) {
						berth.moveToShip(shipWorld, coords.x, coords.y, coords.z);
						changed = true;
					}
				}
			}
		}
		
		if (changed) {
			saveBerths();
		}
	}
	
	public static void onShipDock(WorldServer worldServer, ShipWorld shipWorld, BlockMap<Coords> correspondence) {
		// this is only called on the server
		Map<String,BerthCoords> berths = getSavedBerths(worldServer);
		
		boolean changed = false;
		for (Coords coords : shipWorld.coords()) {
			// is this block a berth head?
			if (shipWorld.getBlock(coords) == Ships.m_blockBerth && BlockBerth.isBlockHeadOfBed(shipWorld.getBlockMetadata(coords))) {
				// this berth just docked to the world
				
				// update any spawn points
				for (BerthCoords berth : berths.values()) {
					if (berth.equals(shipWorld, coords.x, coords.y, coords.z)) {
						Coords worldCoords = correspondence.get(coords);
						berth.moveToWorld(worldCoords.x, worldCoords.y, worldCoords.z);
						changed = true;
					}
				}
			}
		}
		
		if (changed) {
			saveBerths();
		}
	}
	
	private static File getSaveFile() {
		return new File(DimensionManager.getCurrentSaveRootDirectory(), "berths.dat");
	}
	
	private static void saveBerths() {
		File file = getSaveFile();
		
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new FileOutputStream(file));
			out.writeInt(m_playerSavedBerths.size());
			for (Map.Entry<String,BerthCoords> entry : m_playerSavedBerths.entrySet()) {
				String username = entry.getKey();
				BerthCoords coords = entry.getValue();
				
				out.writeUTF(username);
				out.writeInt(coords.dimensionId);
				out.writeBoolean(coords.shipUuid != null);
				if (coords.shipUuid != null) {
					// write in big-endian order
					out.writeLong(coords.shipUuid.getMostSignificantBits());
					out.writeLong(coords.shipUuid.getLeastSignificantBits());
				}
				out.writeInt(coords.x);
				out.writeInt(coords.y);
				out.writeInt(coords.z);
			}
		} catch (IOException ex) {
			Ships.logger.error(ex, "Unable to save berths! Player spawn points on ships were not saved!");
		} finally {
			Util.closeSilently(out);
		}
	}
	
	private static void loadBerths() {
		File file = getSaveFile();
		if (!file.exists()) {
			return;
		}
		
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(file));
			int numRecords = in.readInt();
			for (int i = 0; i < numRecords; i++) {
				String username = in.readUTF();
				BerthCoords coords = new BerthCoords();
				coords.dimensionId = in.readInt();
				boolean hasShipId = in.readBoolean();
				if (hasShipId) {
					// read in big endian order
					coords.shipUuid = new UUID(in.readLong(), in.readLong());
				}
				coords.x = in.readInt();
				coords.y = in.readInt();
				coords.z = in.readInt();
				
				m_playerSavedBerths.put(username, coords);
			}
		} catch (IOException ex) {
			Ships.logger.error(ex, "Unable to load berths! Player spawn points on ships were not loaded!");
		} finally {
			Util.closeSilently(in);
		}
	}
}
