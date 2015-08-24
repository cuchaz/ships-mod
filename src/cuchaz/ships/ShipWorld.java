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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.packets.PacketChangedBlocks;
import cuchaz.ships.packets.PacketShipBlockEvent;

public class ShipWorld extends DetachedWorld {
	
	// NOTE: this member var is essentially cache. It works as long as the client/server are single-threaded
	private Coords m_lookupCoords = new Coords(0, 0, 0);
	
	private EntityShip m_ship;
	private BlocksStorage m_storage;
	private BlockMap<TileEntity> m_tileEntities;
	private BlockMap<EntityHanging> m_hangingEntities;
	private BlockSet m_changedBlocks;
	private boolean m_needsRenderUpdate;
	private int m_biomeId;
	
	public ShipWorld(World world) {
		super(world, "Ship");
		
		// init defaults
		m_ship = null;
		m_storage = new BlocksStorage();
		m_tileEntities = new BlockMap<TileEntity>();
		m_hangingEntities = new BlockMap<EntityHanging>();
		m_changedBlocks = new BlockSet();
		m_biomeId = 0;
	}
	
	public ShipWorld(World world, BlocksStorage storage, BlockMap<TileEntity> tileEntities, BlockMap<EntityHanging> hangingEntities, int biomeId) {
		this(world);
		
		// save args
		m_storage = storage;
		m_tileEntities = tileEntities;
		m_hangingEntities = hangingEntities;
		m_biomeId = biomeId;
		
		// init the tile entities in the world
		for (TileEntity tileEntity : m_tileEntities.values()) {
			tileEntity.setWorldObj(this);
			tileEntity.validate();
		}
		
		// init the hanging entities in the world
		for (EntityHanging hangingEntity : m_hangingEntities.values()) {
			hangingEntity.setWorld(this);
		}
	}
	
	public ShipWorld(World world, Coords originCoords, BlockSet blocks) {
		this(world);
		
		// copy the blocks
		m_storage.readFromWorld(world, originCoords, blocks);
		
		// copy the tile entities
		for (Coords worldCoords : blocks) {
			// does this block have a tile entity?
			TileEntity tileEntity = world.getTileEntity(worldCoords.x, worldCoords.y, worldCoords.z);
			if (tileEntity == null) {
				continue;
			}
			
			Coords relativeCoords = new Coords(
				worldCoords.x - originCoords.x,
				worldCoords.y - originCoords.y,
				worldCoords.z - originCoords.z
			);
			
			try {
				// copy the tile entity
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT(nbt);
				TileEntity tileEntityCopy = TileEntity.createAndLoadEntity(nbt);
				
				// initialize the tile entity
				tileEntityCopy.setWorldObj(this);
				tileEntityCopy.xCoord = relativeCoords.x;
				tileEntityCopy.yCoord = relativeCoords.y;
				tileEntityCopy.zCoord = relativeCoords.z;
				tileEntityCopy.validate();
				
				// save it to the ship world
				m_tileEntities.put(relativeCoords, tileEntityCopy);
			} catch (Exception ex) {
				Ships.logger.warning(ex, "Tile entity %s at (%d,%d,%d) didn't like being moved to the ship. The block was moved, the but tile entity was not moved.", tileEntity.getClass().getName(), worldCoords.x, worldCoords.y, worldCoords.z);
			}
		}
		
		// copy hanging entities
		for (Map.Entry<Coords,EntityHanging> entry : getNearbyHangingEntities(world, blocks).entrySet()) {
			Coords worldCoords = entry.getKey();
			EntityHanging hangingEntity = entry.getValue();
			
			Coords relativeCoords = new Coords(
				worldCoords.x - originCoords.x,
				worldCoords.y - originCoords.y,
				worldCoords.z - originCoords.z
			);
			
			try {
				// copy the hanging entity
				NBTTagCompound nbt = new NBTTagCompound();
				hangingEntity.writeToNBTOptional(nbt);
				EntityHanging hangingEntityCopy = (EntityHanging)EntityList.createEntityFromNBT(nbt, this);
				
				// save it to the ship world
				hangingEntityCopy.field_146063_b = relativeCoords.x;
				hangingEntityCopy.field_146064_c = relativeCoords.y;
				hangingEntityCopy.field_146062_d = relativeCoords.z;
				hangingEntityCopy.posX -= originCoords.x;
				hangingEntityCopy.posY -= originCoords.y;
				hangingEntityCopy.posZ -= originCoords.z;
				m_hangingEntities.put(relativeCoords, hangingEntityCopy);
				
				// reset the hanging entity's bounding box after the move
				hangingEntityCopy.setDirection(hangingEntityCopy.hangingDirection);
			} catch (Exception ex) {
				Ships.logger.warning(ex, "Hanging entity %s at (%d,%d,%d) didn't like being moved to the ship. The block was moved, the but hanging entity was not moved.", hangingEntity.getClass().getName(), worldCoords.x, worldCoords.y, worldCoords.z);
			}
		}
		
		// copy the biome
		m_biomeId = world.getBiomeGenForCoords(originCoords.x, originCoords.z).biomeID;
	}
	
	public void restoreToWorld(World world, Map<Coords,Coords> correspondence, int waterHeightInBlockSpace) {
		// restore the blocks
		m_storage.writeToWorld(world, correspondence);
		
		// bail out the boat if needed (it might have water in the trapped air blocks)
		for (Coords coordsShip : getDisplacement().getTrappedAirFromWaterHeight(waterHeightInBlockSpace)) {
			Coords coordsWorld = correspondence.get(coordsShip);
			
			// is this block actually water?
			Block block = world.getBlock(coordsWorld.x, coordsWorld.y, coordsWorld.z);
			if (BlockProperties.isWater(block) || block == Ships.m_blockAirWall) {
				BlockUtils.removeBlockWithoutNotifyingIt(world, coordsWorld.x, coordsWorld.y, coordsWorld.z, UpdateRules.UpdateClients);
			}
		}
		
		// restore the tile entities
		for (Map.Entry<Coords,TileEntity> entry : m_tileEntities.entrySet()) {
			Coords coordsShip = entry.getKey();
			Coords coordsWorld = correspondence.get(coordsShip);
			TileEntity tileEntity = entry.getValue();
			
			try {
				NBTTagCompound nbt = new NBTTagCompound();
				tileEntity.writeToNBT(nbt);
				TileEntity tileEntityCopy = TileEntity.createAndLoadEntity(nbt);
				tileEntityCopy.setWorldObj(world);
				tileEntityCopy.xCoord = coordsWorld.x;
				tileEntityCopy.yCoord = coordsWorld.y;
				tileEntityCopy.zCoord = coordsWorld.z;
				tileEntityCopy.validate();
				
				world.setTileEntity(coordsWorld.x, coordsWorld.y, coordsWorld.z, tileEntityCopy);
			} catch (Exception ex) {
				// remove the tile entity
				world.removeTileEntity(coordsWorld.x, coordsWorld.y, coordsWorld.z);
				
				Ships.logger.warning(ex, "Tile entity %s at (%d,%d,%d) didn't like being moved to the world. The tile entity has been removed from its block to prevent further errors.", tileEntity.getClass().getName(), coordsWorld.x, coordsWorld.y, coordsWorld.z);
			}
		}
		
		// compute the translation from block space to world space
		Coords translation = correspondence.get(new Coords(0, 0, 0));
		
		// restore hanging entities (on the server only)
		if (!world.isRemote) {
			for (Map.Entry<Coords,EntityHanging> entry : m_hangingEntities.entrySet()) {
				Coords coordsShip = entry.getKey();
				Coords coordsWorld = correspondence.get(coordsShip);
				EntityHanging hangingEntity = entry.getValue();
				
				try {
					// copy the hanging entity
					NBTTagCompound nbt = new NBTTagCompound();
					hangingEntity.writeToNBTOptional(nbt);
					EntityHanging hangingEntityCopy = (EntityHanging)EntityList.createEntityFromNBT(nbt, world);
					
					// save it to the ship world
					hangingEntityCopy.field_146063_b = coordsWorld.x;
					hangingEntityCopy.field_146064_c = coordsWorld.y;
					hangingEntityCopy.field_146062_d = coordsWorld.z;
					hangingEntityCopy.posX += translation.x;
					hangingEntityCopy.posY += translation.y;
					hangingEntityCopy.posZ += translation.z;
					
					// reset the hanging entity's bounding box after the move
					hangingEntityCopy.setDirection(hangingEntityCopy.hangingDirection);
					
					// then spawn it
					world.spawnEntityInWorld(hangingEntityCopy);
				} catch (Exception ex) {
					Ships.logger.warning(ex, "Hanging entity %s at (%d,%d,%d) didn't like being moved to the world. The block was moved, the but hanging entity was not moved.", hangingEntity.getClass().getName(), coordsWorld.x, coordsWorld.y, coordsWorld.z);
				}
			}
		}
	}
	
	public BlockMap<EntityHanging> getNearbyHangingEntities(World world, BlockSet blocks) {
		// get the bounding box of the blocks
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		blocks.getBoundingBox().toAxisAlignedBB(box);
		
		// collect the hanging entities that are hanging on a block in the block set
		BlockMap<EntityHanging> out = new BlockMap<EntityHanging>();
		Coords entityBlock = new Coords(0, 0, 0);
		@SuppressWarnings("unchecked")
		List<EntityHanging> hangingEntities = (List<EntityHanging>)world.getEntitiesWithinAABB(EntityHanging.class, box);
		for (EntityHanging hangingEntity : hangingEntities) {
			// is this hanging entity actually on a ship block?
			entityBlock.set(hangingEntity.field_146063_b, hangingEntity.field_146064_c, hangingEntity.field_146062_d);
			if (blocks.contains(entityBlock)) {
				out.put(new Coords(entityBlock), hangingEntity);
			}
		}
		return out;
	}
	
	public BlocksStorage getBlocksStorage() {
		return m_storage;
	}
	
	public EntityShip getShip() {
		return m_ship;
	}
	
	public void setShip(EntityShip val) {
		m_ship = val;
	}
	
	public ShipType getShipType() {
		return ShipType.getByMeta(getBlockMetadata(0, 0, 0));
	}
	
	public boolean isValid() {
		return m_storage.getNumBlocks() > 0;
	}
	
	public int getNumBlocks() {
		return m_storage.getNumBlocks();
	}
	
	public Set<Coords> coords() {
		return m_storage.coords();
	}
	
	public BlockMap<TileEntity> tileEntities() {
		return m_tileEntities;
	}
	
	public BlockMap<EntityHanging> hangingEntities() {
		return m_hangingEntities;
	}
	
	public int getBiomeId() {
		return m_biomeId;
	}
	
	public ShipGeometry getGeometry() {
		return m_storage.getGeometry();
	}
	
	public ShipDisplacement getDisplacement() {
		return m_storage.getDisplacement();
	}
	
	public BoundingBoxInt getBoundingBox() {
		return m_storage.getGeometry().getEnvelopes().getBoundingBox();
	}
	
	public BlockStorage getBlockStorage(int x, int y, int z) {
		m_lookupCoords.set(x, y, z);
		return getBlockStorage(m_lookupCoords);
	}
	
	public BlockStorage getBlockStorage(Coords coords) {
		return m_storage.getBlock(coords);
	}
	
	@Override
	public Block getBlock(int x, int y, int z) {
		m_lookupCoords.set(x, y, z);
		return getBlock(m_lookupCoords);
	}
	
	public Block getBlock(Coords coords) {
		return getBlockStorage(coords).block;
	}
	
	@Override
	public boolean blockExists(int x, int y, int z) {
		// always return true. a block outside the ship will still exist, even if it's just air
		return true;
	}
	
	@Override
	public TileEntity getTileEntity(int x, int y, int z) {
		m_lookupCoords.set(x, y, z);
		return getTileEntity(m_lookupCoords);
	}
	
	public TileEntity getTileEntity(Coords coords) {
		return m_tileEntities.get(coords);
	}
	
	@Override
	public int getBlockMetadata(int x, int y, int z) {
		m_lookupCoords.set(x, y, z);
		return getBlockMetadata(m_lookupCoords);
	}
	
	public int getBlockMetadata(Coords coords) {
		return getBlockStorage(coords).meta;
	}
	
	@Override
	public boolean setBlock(int x, int y, int z, Block newBlock, int newMeta, int ignored) {
		if (applyBlockChange(x, y, z, newBlock, newMeta)) {
			// on the client do nothing more
			// on the server, buffer the changes to be broadcast to the client
			if (Environment.isServer()) {
				m_changedBlocks.add(new Coords(x, y, z));
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void setTileEntity(int x, int y, int z, TileEntity tileEntity) {
		// do nothing. tile entities are handled differently
	}
	
	@Override
	public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int ignored) {
		if (applyBlockChange(x, y, z, getBlock(x, y, z), meta)) {
			// on the client do nothing more
			// on the server, buffer the changes to be broadcast to the client
			if (Environment.isServer()) {
				m_changedBlocks.add(new Coords(x, y, z));
			}
			return true;
		}
		return false;
	}
	
	public boolean needsRenderUpdate() {
		boolean val = m_needsRenderUpdate;
		m_needsRenderUpdate = false;
		return val;
	}
	
	public boolean applyBlockChange(int x, int y, int z, Block newBlock, int newMeta) {
		m_lookupCoords.set(x, y, z);
		return applyBlockChange(m_lookupCoords, newBlock, newMeta);
	}
	
	public boolean applyBlockChange(Coords coords, Block newBlock, int newMeta) {
		// lookup the affected block
		BlockStorage storage = getBlockStorage(coords);
		Block oldBlock = storage.block;
		
		// only allow benign changes to blocks
		boolean isAllowed = false
			// allow metadata changes
			|| (oldBlock == newBlock)
			// allow furnace block changes
			|| (oldBlock == Blocks.lit_furnace && newBlock == Blocks.furnace)
			|| (oldBlock == Blocks.furnace && newBlock == Blocks.lit_furnace);
		
		if (isAllowed) {
			// apply the change
			storage.block = newBlock;
			storage.meta = newMeta;
			
			// notify the tile entity if needed
			TileEntity tileEntity = getTileEntity(coords);
			if (tileEntity != null) {
				tileEntity.updateContainingBlockInfo();
			}
			
			m_needsRenderUpdate = true;
		}
		
		return isAllowed;
	}
	
	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
		m_lookupCoords.set(x, y, z);
		Block block = getBlock(m_lookupCoords);
		if (block == Blocks.air) {
			return defaultValue;
		}
		return block.isSideSolid(this, x, y, z, side);
	}
	
	@Override
	public boolean isBlockNormalCubeDefault(int x, int y, int z, boolean defaultValue) {
		m_lookupCoords.set(x, y, z);
		Block block = getBlock(m_lookupCoords);
		if (block == Blocks.air) {
			return defaultValue;
		}
		return block.isNormalCube(this, x, y, z);
	}
	
	@Override
	public int getLightBrightnessForSkyBlocks(int x, int y, int z, int blockBrightness) {
		// keep the ship fully sky-lit by default
		return 15 << 20 | 0 << 4;
		
		// TODO: lighting on ships
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public List selectEntitiesWithinAABB(Class theClass, AxisAlignedBB box, IEntitySelector selector) {
		// there are no entities in ship world
		return new ArrayList();
		
		// UNDONE: actually do this query?
		// get the AABB for the query box in world coords
		// get the entities from the real world
		// transform them into ship world
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box, IEntitySelector selector) {
		// there are no entities in ship world
		return new ArrayList();
	}
	
	@Override
	public void markTileEntityChunkModified(int x, int y, int z, TileEntity tileEntity) {
		// don't need to do anything
	}
	
	@Override
	public void updateEntities() {
		// update the tile entities
		Iterator<Map.Entry<Coords,TileEntity>> iter = m_tileEntities.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Coords,TileEntity> entry = iter.next();
			Coords coords = entry.getKey();
			TileEntity entity = entry.getValue();
			
			try {
				entity.updateEntity();
			} catch (Exception ex) {
				// remove the offending tile entity
				iter.remove();
				
				Ships.logger.warning(ex, "Tile entity %s at (%d,%d,%d) had a problem during an update! The tile entity has been removed from its block to prevent further errors.", entity.getClass().getName(), coords.x, coords.y, coords.z);
			}
		}
		
		// on the client, do random update ticks
		if (Environment.isClient() && m_ship != null) {
			updateEntitiesClient();
		}
		
		// on the server, push any accumulated changes to the client
		if (Environment.isServer() && !m_changedBlocks.isEmpty()) {
			pushBlockChangesToClients();
			m_changedBlocks.clear();
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void updateEntitiesClient() {
		// get the player position on the ship
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		Vec3 v = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
		m_ship.worldToShip(v);
		m_ship.shipToBlocks(v);
		int playerX = MathHelper.floor_double(v.xCoord);
		int playerY = MathHelper.floor_double(v.yCoord);
		int playerZ = MathHelper.floor_double(v.zCoord);
		
		Random random = new Random();
		for (int i = 0; i < 1000; i++) {
			int x = playerX + random.nextInt(16) - random.nextInt(16);
			int y = playerY + random.nextInt(16) - random.nextInt(16);
			int z = playerZ + random.nextInt(16) - random.nextInt(16);
			Block block = getBlock(x, y, z);
			if (block != Blocks.air) {
				
				// blocks can do all kinds of crazy things, be defensive
				try {
					block.randomDisplayTick(this, x, y, z, random);
				} catch (Throwable t) {
					Ships.logger.error(t, "Block threw up during random tick: %s", Block.blockRegistry.getNameForObject(block));
				}
			}
		}
	}
	
	private void pushBlockChangesToClients() {
		if (m_ship == null) {
			return;
		}
		
		Ships.net.getDispatch().sendToAllAround(
			new PacketChangedBlocks(m_ship, m_changedBlocks),
			new TargetPoint(m_ship.worldObj.provider.dimensionId, m_ship.posX, m_ship.posY, m_ship.posZ, 64)
		);
	}
	
	@Override
	public void addBlockEvent(int x, int y, int z, Block block, int eventId, int eventParam) {
		if (m_ship == null || block == Blocks.air || getBlock(x, y, z) != block) {
			return;
		}
		
		// on the client, just deliver to the block
		boolean eventWasAccepted = block.onBlockEventReceived(this, x, y, z, eventId, eventParam);
		
		// on the server, also send a packet to the client
		if (Environment.isServer() && eventWasAccepted) {
			// get the pos in world space
			Vec3 v = Vec3.createVectorHelper(x, y, z);
			m_ship.blocksToShip(v);
			m_ship.shipToWorld(v);
			
			Ships.net.getDispatch().sendToAllAround(
				new PacketShipBlockEvent(m_ship.getEntityId(), x, y, z, block, eventId, eventParam),
				new TargetPoint(m_ship.worldObj.provider.dimensionId, v.xCoord, v.yCoord, v.zCoord, 64)
			);
		}
	}
	
	@Override
	public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {
		if (sound == null) {
			return;
		}
		
		// on the server, send a packet to the clients
		if (Environment.isServer()) {
			// get the pos in world space
			Vec3 v = Vec3.createVectorHelper(x, y, z);
			m_ship.blocksToShip(v);
			m_ship.shipToWorld(v);
			
			m_ship.worldObj.playSoundEffect(v.xCoord, v.yCoord, v.zCoord, sound, volume, pitch);
		}
		
		// on the client, just ignore. Sounds actually get played by the packet handler
	}
	
	// NOTE: don't have to override playSoundToNearExcept() or playSoundAtEntity(), not called by blocks/tileEntities
	
	@Override
	public void playAuxSFXAtEntity(EntityPlayer player, int sfxID, int x, int y, int z, int auxData) {
		// on the server, send a packet to the clients
		if (Environment.isServer()) {
			// get the pos in world space
			Vec3 v = Vec3.createVectorHelper(x, y, z);
			m_ship.blocksToShip(v);
			m_ship.shipToWorld(v);
			
			m_ship.worldObj.playAuxSFXAtEntity(player, sfxID, MathHelper.floor_double(v.xCoord), MathHelper.floor_double(v.yCoord), MathHelper.floor_double(v.zCoord), auxData);
		}
		
		// on the client, just ignore. Sounds actually get played by the packet handler
	}
	
	@Override
	public void spawnParticle(String name, double x, double y, double z, double motionX, double motionY, double motionZ) {
		if (m_ship == null) {
			return;
		}
		
		// transform the position to world coordinates
		Vec3 v = Vec3.createVectorHelper(x, y, z);
		m_ship.blocksToShip(v);
		m_ship.shipToWorld(v);
		x = v.xCoord;
		y = v.yCoord;
		z = v.zCoord;
		
		// transform the velocity vector too
		v.xCoord = motionX;
		v.yCoord = motionY;
		v.zCoord = motionZ;
		m_ship.shipToWorldDirection(v);
		motionX = v.xCoord;
		motionY = v.yCoord;
		motionZ = v.zCoord;
		
		m_ship.worldObj.spawnParticle(name, x, y, z, motionX, motionY, motionZ);
	}
	
	@Override
	public boolean spawnEntityInWorld(Entity entity) {
		if (m_ship == null) {
			return false;
		}
		
		// transform the entity position to world coordinates
		Vec3 v = Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
		m_ship.blocksToShip(v);
		m_ship.shipToWorld(v);
		entity.posX = v.xCoord;
		entity.posY = v.yCoord;
		entity.posZ = v.zCoord;
		
		// transform the velocity vector too
		v.xCoord = entity.motionX;
		v.yCoord = entity.motionY;
		v.zCoord = entity.motionZ;
		m_ship.shipToWorldDirection(v);
		entity.motionX = v.xCoord;
		entity.motionY = v.yCoord;
		entity.motionZ = v.zCoord;
		
		// pass off to the outer world
		entity.worldObj = m_ship.worldObj;
		return m_ship.worldObj.spawnEntityInWorld(entity);
	}
	
	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return BiomeGenBase.getBiome(m_biomeId);
	}
}
