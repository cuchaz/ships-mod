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

import java.util.TreeMap;

import net.minecraft.client.network.NetHandlerPlayClient;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipWorldPersistence;

public class PacketShipLaunched extends Packet<PacketShipLaunched> {
	
	private int m_entityId;
	private byte[] m_shipData;
	private int m_launchX;
	private int m_launchY;
	private int m_launchZ;
	
	public static PacketShipLaunched instance = null;
	
	private TreeMap<Integer,PacketShipLaunched> m_packets;
	
	public PacketShipLaunched() {
		// for registration
		m_packets = new TreeMap<Integer,PacketShipLaunched>();
		instance = this;
	}
	
	public PacketShipLaunched(EntityShip ship, Coords shipBlock) {
		m_entityId = ship.getEntityId();
		m_shipData = ShipWorldPersistence.writeNewestVersion(ship.getShipWorld(), true);
		m_launchX = shipBlock.x;
		m_launchY = shipBlock.y;
		m_launchZ = shipBlock.z;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_entityId);
		buf.writeInt(m_shipData.length);
		buf.writeBytes(m_shipData);
		buf.writeInt(m_launchX);
		buf.writeInt(m_launchY);
		buf.writeInt(m_launchZ);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_entityId = buf.readInt();
		m_shipData = new byte[buf.readInt()];
		buf.readBytes(m_shipData);
		m_launchX = buf.readInt();
		m_launchY = buf.readInt();
		m_launchZ = buf.readInt();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketShipLaunched,IMessage> getClientHandler() {
		return new IMessageHandler<PacketShipLaunched,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketShipLaunched message, MessageContext ctx) {
				return message.onReceivedClient(ctx.getClientHandler());
			}
		};
	}
	
	@SideOnly(Side.CLIENT)
	private IMessage onReceivedClient(NetHandlerPlayClient netClient) {
		// save the packet for later
		m_packets.put(m_entityId, this);
		
		return null;
	}
	
	public PacketShipLaunched getPacket(EntityShip ship) {
		PacketShipLaunched packet = m_packets.get(ship.getEntityId());
		if (packet != null) {
			m_packets.remove(ship.getEntityId());
		}
		return packet;
	}
	
	public void process(EntityShip ship) {
		try {
			ShipWorld shipWorld = ShipWorldPersistence.readAnyVersion(ship.worldObj, m_shipData, true);
			ShipLauncher.initShip(ship, shipWorld, new Coords(m_launchX, m_launchY, m_launchZ));
		} catch (PersistenceException ex) {
			Ships.logger.warning(ex, "Unable to read ship! Ship will be removed from world");
			ship.setDead();
		}
	}
}
