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
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.items.ItemShipEraser;

public class PacketEraseShip extends Packet<PacketEraseShip> {
	
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketEraseShip() {
		// for registration
	}
	
	public PacketEraseShip(int x, int y, int z) {
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_x);
		buf.writeInt(m_y);
		buf.writeInt(m_z);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_x = buf.readInt();
		m_y = buf.readInt();
		m_z = buf.readInt();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketEraseShip,IMessage> getServerHandler() {
		return new IMessageHandler<PacketEraseShip,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketEraseShip message, MessageContext ctx) {
				return message.onReceivedServer(ctx.getServerHandler());
			}
		};
	}
	
	private IMessage onReceivedServer(NetHandlerPlayServer netServer) {
		ItemShipEraser.eraseShip(netServer.playerEntity.worldObj, netServer.playerEntity, m_x, m_y, m_z);
		
		return null;
	}
}
