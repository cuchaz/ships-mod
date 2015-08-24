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

import org.apache.commons.codec.binary.Base64;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.ShipClipboard;
import cuchaz.ships.Ships;
import cuchaz.ships.persistence.PersistenceException;

public class PacketPasteShip extends Packet<PacketPasteShip> {
	
	public static final int MaxSize = 1024*1024; // chosen completely arbitrarily
	
	private byte[] m_encodedBlocks;
	private int m_dx;
	private int m_dy;
	private int m_dz;
	
	public PacketPasteShip() {
		// for registation
	}
	
	public PacketPasteShip(String encodedBlocks, int dx, int dy, int dz) {
		m_encodedBlocks = Base64.decodeBase64(encodedBlocks);
		if (m_encodedBlocks.length > MaxSize) {
			// this probably won't ever happen... right?
			throw new IllegalArgumentException("Ship description size exceeds " + MaxSize + " bytes. If this is a legitimate use, we need a bigger size");
		}
		m_dx = dx;
		m_dy = dy;
		m_dz = dz;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_encodedBlocks.length);
		buf.writeBytes(m_encodedBlocks);
		buf.writeInt(m_dx);
		buf.writeInt(m_dy);
		buf.writeInt(m_dz);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int dataSize = buf.readInt();
		if (dataSize <= MaxSize) {
			m_encodedBlocks = new byte[dataSize];
			buf.readBytes(m_encodedBlocks);
		}
		m_dx = buf.readInt();
		m_dy = buf.readInt();
		m_dz = buf.readInt();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketPasteShip,IMessage> getServerHandler() {
		return new IMessageHandler<PacketPasteShip,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketPasteShip message, MessageContext ctx) {
				return message.onReceivedServer(ctx.getServerHandler());
			}
		};
	}
	
	private IMessage onReceivedServer(NetHandlerPlayServer netServer) {
		if (m_encodedBlocks == null) {
			return null;
		}
		
		// restore the ship blocks on the server
		try {
			ShipClipboard.restoreShip(netServer.playerEntity.worldObj, Base64.encodeBase64String(m_encodedBlocks), new Coords(m_dx, m_dy, m_dz));
		} catch (PersistenceException ex) {
			Ships.logger.warning(ex, "Unable to restore ship!");
		}
		
		return null;
	}
}
