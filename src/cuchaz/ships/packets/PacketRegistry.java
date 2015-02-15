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

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketRegistry {
	
	private SimpleNetworkWrapper m_network;
	private int m_nextId;
	
	public PacketRegistry(String modId) {
		m_nextId = 0;
		
		m_network = NetworkRegistry.INSTANCE.newSimpleChannel(modId);
		
		register(new PacketLaunchShip());
		register(new PacketShipLaunched());
		register(new PacketUnlaunchShip());
		register(new PacketRequestShipBlocks());
		register(new PacketShipBlocks());
		register(new PacketPilotShip());
		register(new PacketShipBlockEvent());
		register(new PacketChangedBlocks());
		register(new PacketPasteShip());
		register(new PacketEraseShip());
		register(new PacketPlayerSleepInBerth());
		register(new PacketBlockPropertiesOverrides());
		register(new PacketPlaceProjector());
	}
	
	private <T extends Packet<T>> void register(T packet) {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)packet.getClass();
		
		IMessageHandler<T,IMessage> clientHandler = packet.getClientHandler();
		if (clientHandler != null) {
			m_network.registerMessage(clientHandler, c, m_nextId, Side.CLIENT);
		}
		
		IMessageHandler<T,IMessage> serverHandler = packet.getServerHandler();
		if (serverHandler != null) {
			m_network.registerMessage(serverHandler, c, m_nextId, Side.SERVER);
		}
		
		m_nextId++;
	}
	
	public SimpleNetworkWrapper getDispatch() {
		return m_network;
	}
}
