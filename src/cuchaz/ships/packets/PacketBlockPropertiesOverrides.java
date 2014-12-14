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
import net.minecraft.client.network.NetHandlerPlayClient;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cuchaz.ships.config.BlockProperties;

public class PacketBlockPropertiesOverrides extends Packet<PacketBlockPropertiesOverrides>
{
	private String m_overrides;
	
	public PacketBlockPropertiesOverrides( )
	{
		// for registration
	}
	
	public PacketBlockPropertiesOverrides( String overrides )
	{
		m_overrides = overrides;
	}

	@Override
	public void toBytes( ByteBuf buf )
	{
		ByteBufUtils.writeUTF8String( buf, m_overrides );
	}

	@Override
	public void fromBytes( ByteBuf buf )
	{
		m_overrides = ByteBufUtils.readUTF8String( buf );
	}

	@Override
	protected IMessage onReceivedClient( NetHandlerPlayClient netClient )
	{
		// received on the client
		// save the block properties overrides
		BlockProperties.setOverrides( m_overrides );
		
		return null;
	}
}
