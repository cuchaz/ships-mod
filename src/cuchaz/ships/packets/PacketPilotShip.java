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
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.Ships;

public class PacketPilotShip extends Packet<PacketPilotShip> {
	
	private int m_entityId;
	private int m_actions;
	private BlockSide m_sideShipForward;
	private int m_linearThrottle;
	private int m_angularThrottle;
	
	public PacketPilotShip() {
		// for registration
	}
	
	public PacketPilotShip(int entityId, int actions, BlockSide sideFacingPlayer, int linearThrottle, int angularThrottle) {
		m_entityId = entityId;
		m_actions = actions;
		m_sideShipForward = sideFacingPlayer;
		m_linearThrottle = linearThrottle;
		m_angularThrottle = angularThrottle;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_entityId);
		buf.writeInt(m_actions);
		buf.writeByte(m_sideShipForward.ordinal());
		buf.writeByte(m_linearThrottle);
		buf.writeByte(m_angularThrottle);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_entityId = buf.readInt();
		m_actions = buf.readInt();
		m_sideShipForward = BlockSide.values()[buf.readByte()];
		m_linearThrottle = buf.readByte();
		m_angularThrottle = buf.readByte();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketPilotShip,IMessage> getServerHandler() {
		return new IMessageHandler<PacketPilotShip,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketPilotShip message, MessageContext ctx) {
				return message.onReceivedServer(ctx.getServerHandler());
			}
		};
	}
	
	private IMessage onReceivedServer(NetHandlerPlayServer netServer) {
		// get the ship
		EntityShip ship = ShipLocator.getShip(netServer.playerEntity.worldObj, m_entityId);
		if (ship == null) {
			return null;
		}
		
		applyActions(ship);
		
		// relay the actions to the clients
		Ships.net.getDispatch().sendToAllAround(this, new TargetPoint(netServer.playerEntity.worldObj.provider.dimensionId, ship.posX, ship.posY, ship.posZ, 100));
		
		return null;
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketPilotShip,IMessage> getClientHandler() {
		return new IMessageHandler<PacketPilotShip,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketPilotShip message, MessageContext ctx) {
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
		
		applyActions(ship);
		
		return null;
	}
	
	private void applyActions(EntityShip ship) {
		ship.setPilotActions(m_actions, m_sideShipForward, false);
		ship.linearThrottle = m_linearThrottle;
		ship.angularThrottle = m_angularThrottle;
	}
}
