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
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLauncher.LaunchFlag;
import cuchaz.ships.Ships;

public class PacketLaunchShip extends Packet<PacketLaunchShip> {
	
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketLaunchShip() {
		// for registration
	}
	
	public PacketLaunchShip(Coords coords) {
		m_x = coords.x;
		m_y = coords.y;
		m_z = coords.z;
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
	public IMessageHandler<PacketLaunchShip,IMessage> getServerHandler() {
		return new IMessageHandler<PacketLaunchShip,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketLaunchShip message, MessageContext ctx) {
				return message.onReceivedServer(ctx.getServerHandler());
			}
		};
	}
	
	private IMessage onReceivedServer(NetHandlerPlayServer netServer) {
		World world = netServer.playerEntity.worldObj;
		
		// spawn the ship
		ShipLauncher launcher = new ShipLauncher(world, new Coords(m_x, m_y, m_z));
		if (launcher.isLaunchable()) {
			launcher.launch();
		} else {
			// debug info
			Ships.logger.warning("Server can't launch ship at: (%d,%d,%d)", m_x, m_y, m_z);
			for (LaunchFlag flag : LaunchFlag.values()) {
				Ships.logger.warning("\t" + flag.name() + ": " + launcher.getLaunchFlag(flag));
			}
		}
		
		return null;
	}
}
