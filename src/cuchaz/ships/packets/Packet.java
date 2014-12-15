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

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;

public abstract class Packet<T extends Packet<T>> implements IMessage
{
	public IMessageHandler<T,IMessage> getClientHandler( )
	{
		return null;
	}
	
	public IMessageHandler<T,IMessage> getServerHandler( )
	{
		return null;
	}
}
