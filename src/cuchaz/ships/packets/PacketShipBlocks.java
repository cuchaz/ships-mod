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
package cuchaz.ships.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipWorldPersistence;

public class PacketShipBlocks extends Packet<PacketShipBlocks> {
	
	private int m_entityId;
	private byte[] m_shipData;
	
	public PacketShipBlocks() {
		// for registration
	}
	
	public PacketShipBlocks(EntityShip ship) {
		m_entityId = ship.getEntityId();
		m_shipData = ShipWorldPersistence.writeNewestVersion(ship.getShipWorld(), true);
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_entityId);
		buf.writeInt(m_shipData.length);
		buf.writeBytes(m_shipData);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_entityId = buf.readInt();
		m_shipData = new byte[buf.readInt()];
		buf.readBytes(m_shipData);
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketShipBlocks,IMessage> getClientHandler() {
		return new IMessageHandler<PacketShipBlocks,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketShipBlocks message, MessageContext ctx) {
				return message.onReceivedClient(ctx.getClientHandler());
			}
		};
	}
	
	@SideOnly(Side.CLIENT)
	private IMessage onReceivedClient(NetHandlerPlayClient netClient) {
		// get the ship
		EntityShip ship = ShipLocator.getShip(Minecraft.getMinecraft().theWorld, m_entityId);
		if (ship == null) {
			return null;
		}
		
		// send the blocks to the ship
		if (ship.getShipWorld() == null) {
			try {
				ship.setShipWorld(ShipWorldPersistence.readAnyVersion(ship.worldObj, m_shipData, true));
			} catch (PersistenceException ex) {
				Ships.logger.warning(ex, "Unable to read ship! Ship will be removed from world");
				ship.setDead();
			}
		}
		
		return null;
	}
}
