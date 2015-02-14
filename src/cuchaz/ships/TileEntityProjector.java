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

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipWorldPersistence;

public class TileEntityProjector extends TileEntity {
	
	private ShipWorld m_shipWorld;
	private byte[] m_encodedBlocksToLoad;
	private AxisAlignedBB m_boundingBox;
	private Coords m_shipTranslation;
	
	public TileEntityProjector() {
		m_shipWorld = null;
		m_encodedBlocksToLoad = null;
		m_boundingBox = null;
		m_shipTranslation = new Coords(0, 0, 0);
	}
	
	public ShipWorld getShipWorld() {
		return m_shipWorld;
	}
	
	public void setShipWorld(ShipWorld shipWorld) {
		m_shipWorld = shipWorld;
		
		// set the bounding box (center the ship over the projector)
		BoundingBoxInt box = m_shipWorld.getBoundingBox();
		m_shipTranslation.set(-box.minX - box.getDx() / 2, -box.minY + 2, -box.minZ - box.getDz() / 2);
		m_boundingBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
		m_shipWorld.getBoundingBox().toAxisAlignedBB(m_boundingBox);
		m_boundingBox.offset(xCoord + m_shipTranslation.x, yCoord + m_shipTranslation.y, zCoord + m_shipTranslation.z);
		// .getUnionBound()
		m_boundingBox.setBB(m_boundingBox.func_111270_a(getBlockType().getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord)));
	}
	
	public Coords getShipTranslation() {
		return m_shipTranslation;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		if (m_boundingBox != null) {
			return m_boundingBox;
		} else {
			return getBlockType().getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
		}
	}
	
	public void updateEntity() {
		// create the world if we don't have one yet
		if (m_encodedBlocksToLoad != null) {
			if (m_shipWorld == null) {
				try {
					setShipWorld(ShipWorldPersistence.readAnyVersion(worldObj, m_encodedBlocksToLoad));
				} catch (PersistenceException ex) {
					Ships.logger.error(ex, "Unable to restore ship projector");
				}
			}
			m_encodedBlocksToLoad = null;
		}
		
		// TODO: keep track of how much of the ship is built
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		m_encodedBlocksToLoad = nbt.getByteArray("shipWorld");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (m_shipWorld != null) {
			nbt.setByteArray("shipWorld", ShipWorldPersistence.writeNewestVersion(m_shipWorld));
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		readFromNBT(packet.func_148857_g());
		
		// re-render if we're on the client
		if (worldObj.isRemote) {
			// markBlockForRenderUpdate()
			worldObj.func_147479_m(xCoord, yCoord, zCoord);
		}
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		// only two passes: 0,1
		return true;
	}
}
