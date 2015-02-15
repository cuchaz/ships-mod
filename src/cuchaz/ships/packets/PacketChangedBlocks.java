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
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;

public class PacketChangedBlocks extends Packet<PacketChangedBlocks> {
	
	private BlockSet m_changedBlocks;
	private EntityShip m_ship;
	private int m_entityId;
	private int m_numChangedBlocks;
	private int[] m_x;
	private int[] m_y;
	private int[] m_z;
	private Block[] m_blocks;
	private int[] m_meta;
	
	public PacketChangedBlocks() {
		// for registration
	}
	
	public PacketChangedBlocks(EntityShip ship, BlockSet changedBlocks) {
		m_changedBlocks = changedBlocks;
		m_ship = ship;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		ShipWorld world = m_ship.getShipWorld();
		buf.writeInt(m_ship.getEntityId());
		buf.writeInt(m_changedBlocks.size());
		for (Coords coords : m_changedBlocks) {
			buf.writeShort(coords.x);
			buf.writeShort(coords.y);
			buf.writeShort(coords.z);
			// TODO: I think blockid and beta can both be compacted into a short
			buf.writeShort(Block.getIdFromBlock(world.getBlock(coords)));
			buf.writeByte(world.getBlockMetadata(coords));
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		// read the header
		m_entityId = buf.readInt();
		m_numChangedBlocks = buf.readInt();
		
		// allocate space for the rest
		m_x = new int[m_numChangedBlocks];
		m_y = new int[m_numChangedBlocks];
		m_z = new int[m_numChangedBlocks];
		m_blocks = new Block[m_numChangedBlocks];
		m_meta = new int[m_numChangedBlocks];
		
		// read the changes into a buffer
		for (int i = 0; i < m_numChangedBlocks; i++) {
			m_x[i] = buf.readShort();
			m_y[i] = buf.readShort();
			m_z[i] = buf.readShort();
			m_blocks[i] = Block.getBlockById(buf.readShort());
			m_meta[i] = buf.readByte();
		}
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketChangedBlocks,IMessage> getClientHandler() {
		return new IMessageHandler<PacketChangedBlocks,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketChangedBlocks message, MessageContext ctx) {
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
		
		// apply the block changes
		ShipWorld world = ship.getShipWorld();
		for (int i = 0; i < m_numChangedBlocks; i++) {
			world.applyBlockChange(m_x[i], m_y[i], m_z[i], m_blocks[i], m_meta[i]);
		}
		
		return null;
	}
}
