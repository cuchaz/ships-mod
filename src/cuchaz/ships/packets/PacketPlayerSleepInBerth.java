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
package cuchaz.ships.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PlayerRespawner;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.ShipWorld;

public class PacketPlayerSleepInBerth extends Packet<PacketPlayerSleepInBerth> {
	
	private static final int NotAShip = -1;
	
	private int m_playerEntityId;
	private int m_shipEntityId;
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketPlayerSleepInBerth() {
		// for registration
	}
	
	public PacketPlayerSleepInBerth(EntityPlayer player, World world, int x, int y, int z) {
		m_playerEntityId = player.getEntityId();
		m_shipEntityId = NotAShip;
		m_x = x;
		m_y = y;
		m_z = z;
		
		// is this on a ship?
		if (world instanceof ShipWorld) {
			EntityShip ship = ((ShipWorld)world).getShip();
			m_shipEntityId = ship.getEntityId();
		}
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(m_playerEntityId);
		buf.writeInt(m_shipEntityId);
		buf.writeInt(m_x);
		buf.writeInt(m_y);
		buf.writeInt(m_z);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		m_playerEntityId = buf.readInt();
		m_shipEntityId = buf.readInt();
		m_x = buf.readInt();
		m_y = buf.readInt();
		m_z = buf.readInt();
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketPlayerSleepInBerth,IMessage> getClientHandler() {
		return new IMessageHandler<PacketPlayerSleepInBerth,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketPlayerSleepInBerth message, MessageContext ctx) {
				return message.onReceivedClient(ctx.getClientHandler());
			}
		};
	}
	
	@SideOnly(Side.CLIENT)
	private IMessage onReceivedClient(NetHandlerPlayClient netClient) {
		// NOTE: the sleeping player isn't necessarily the player that received the packet
		// but they are in the same world
		World world = Minecraft.getMinecraft().theWorld;
		
		// get the sleeping player
		Entity entity = world.getEntityByID(m_playerEntityId);
		if (entity == null || ! (entity instanceof EntityPlayer)) {
			return null;
		}
		EntityPlayer sleepingPlayer = (EntityPlayer)entity;
		
		if (m_shipEntityId == NotAShip) {
			// sleep in the berth in the world
			PlayerRespawner.sleepInBerthAt(world, m_x, m_y, m_z, sleepingPlayer);
		} else {
			// get the ship
			EntityShip ship = ShipLocator.getShip(world, m_shipEntityId);
			if (ship == null) {
				return null;
			}
			
			// sleep in the berth on the ship
			PlayerRespawner.sleepInBerthAt(ship.getShipWorld(), m_x, m_y, m_z, sleepingPlayer);
		}
		
		return null;
	}
}
