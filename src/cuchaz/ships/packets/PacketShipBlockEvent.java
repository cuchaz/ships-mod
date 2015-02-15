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
package cuchaz.ships.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
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

public class PacketShipBlockEvent extends Packet<PacketShipBlockEvent> {
	
	private int m_entityId;
	private int m_x;
	private int m_y;
	private int m_z;
	private int m_blockId;
	private int m_eventId;
	private int m_eventParam;
	
	public PacketShipBlockEvent() {
		// for registration
	}
	
	public PacketShipBlockEvent(int entityId, int x, int y, int z, Block block, int eventId, int eventParam) {
		m_entityId = entityId;
		m_x = x;
		m_y = y;
		m_z = z;
		m_blockId = Block.getIdFromBlock(block);
		m_eventId = eventId;
		m_eventParam = eventParam;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_entityId);
		buf.writeInt(m_x);
		buf.writeInt(m_y);
		buf.writeInt(m_z);
		buf.writeInt(m_blockId);
		buf.writeInt(m_eventId);
		buf.writeInt(m_eventParam);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_entityId = buf.readInt();
		m_x = buf.readInt();
		m_y = buf.readInt();
		m_z = buf.readInt();
		m_blockId = buf.readInt();
		m_eventId = buf.readInt();
		m_eventParam = buf.readInt();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketShipBlockEvent,IMessage> getClientHandler() {
		return new IMessageHandler<PacketShipBlockEvent,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketShipBlockEvent message, MessageContext ctx) {
				return message.onReceivedClient(ctx.getClientHandler());
			}
		};
	}
	
	@SideOnly(Side.CLIENT)
	private IMessage onReceivedClient(NetHandlerPlayClient netClient) {
		// get the ship
		EntityShip ship = ShipLocator.getShip(Minecraft.getMinecraft().theWorld, m_entityId);
		if (ship == null || ship.getShipWorld() == null) {
			return null;
		}
		
		// deliver the event
		Block block = Block.getBlockById(m_blockId);
		if (ship.getShipWorld().getBlock(m_x, m_y, m_z) == block) {
			block.onBlockEventReceived(ship.getShipWorld(), m_x, m_y, m_z, m_eventId, m_eventParam);
		}
		
		return null;
	}
}
