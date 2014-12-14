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

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public abstract class Packet<T extends Packet<T>> implements IMessage
{
	public IMessageHandler<T,IMessage> getClientHandler( )
	{
		return new IMessageHandler<T,IMessage>( )
		{
			@Override
			public IMessage onMessage( T message, MessageContext ctx )
			{
				return message.onReceivedClient( ctx.getClientHandler() );
			}
		};
	}
	
	public IMessageHandler<T,IMessage> getServerHandler( )
	{
		return new IMessageHandler<T,IMessage>( )
		{
			@Override
			public IMessage onMessage( T message, MessageContext ctx )
			{
				return message.onReceivedServer( ctx.getServerHandler() );
			}
		};
	}
	
	protected IMessage onReceivedClient( NetHandlerPlayClient netClient )
	{
		return null;
	}
	
	protected IMessage onReceivedServer( NetHandlerPlayServer netServer )
	{
		return null;
	}
}
