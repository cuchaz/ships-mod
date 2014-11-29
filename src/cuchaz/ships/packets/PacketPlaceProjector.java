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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import cuchaz.ships.ShipClipboard;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.items.ItemProjector;
import cuchaz.ships.persistence.PersistenceException;

public class PacketPlaceProjector extends Packet
{
	public static final String Channel = "placeProjector";
	
	private String m_encodedBlocks;
	private int m_x;
	private int m_y;
	private int m_z;
	
	public PacketPlaceProjector( )
	{
		super( Channel );
	}
	
	public PacketPlaceProjector( String encodedBlocks, int x, int y, int z )
	{
		this();
		
		m_encodedBlocks = encodedBlocks;
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	@Override
	public void writeData( DataOutputStream out )
	throws IOException
	{
		out.writeUTF( m_encodedBlocks );
		out.writeInt( m_x );
		out.writeInt( m_y );
		out.writeInt( m_z );
	}
	
	@Override
	public void readData( DataInputStream in )
	throws IOException
	{
		m_encodedBlocks = in.readUTF();
		m_x = in.readInt();
		m_y = in.readInt();
		m_z = in.readInt();
	}
	
	@Override
	public void onPacketReceived( EntityPlayer player )
	{
		if( m_encodedBlocks == null )
		{
			return;
		}
		
		// place the projector
		try
		{
			ShipWorld shipWorld = ShipClipboard.createShipWorld( player.worldObj, m_encodedBlocks );
			ItemProjector.placeProjector( player.worldObj, m_x, m_y, m_z, shipWorld );
			
			// if the player is holding a projector and we're not in creative mode, use it
			if( !player.capabilities.isCreativeMode )
			{
				ItemStack heldItem = player.getHeldItem();
				if( heldItem.itemID == Ships.m_itemProjector.itemID )
				{
					heldItem.stackSize--;
					if( heldItem.stackSize <= 0 )
					{
						player.inventory.mainInventory[player.inventory.currentItem] = null;
						MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, heldItem));
					}
				}
			}
		}
		catch( PersistenceException ex )
		{
			Ships.logger.error( ex, "Unable to place ship projector!" );
		}
	}
}