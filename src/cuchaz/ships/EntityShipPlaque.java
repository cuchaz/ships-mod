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
package cuchaz.ships;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cuchaz.ships.gui.Gui;

public class EntityShipPlaque extends EntityHanging implements IEntityAdditionalSpawnData
{
	private String m_name;
	private int m_supporterId;
	
	public EntityShipPlaque( World world )
	{
		super( world );
		// called by deserializer. don't set anything here except default values
		
		m_name = "";
		m_supporterId = 0;
	}
	
	public EntityShipPlaque( World world, int supporterId, int x, int y, int z, int direction )
	{
		// NOTE: called when player spawns plaque via item
		super( world, x, y, z, direction );
		hangingDirection = direction;
		
		m_name = "";
		m_supporterId = supporterId;
		
		initPlaque();
	}
	
	public String getName( )
	{
		return m_name;
	}
	public void setName( String val )
	{
		m_name = val;
	}
	
	public int getSupporterId( )
	{
		return m_supporterId;
	}
	
	@Override
	public void writeEntityToNBT( NBTTagCompound nbt )
	{
        super.writeEntityToNBT( nbt );
        nbt.setString( "name", m_name );
		nbt.setInteger( "supporterId", m_supporterId );
	}
	
	@Override
	public void readEntityFromNBT( NBTTagCompound nbt )
	{
		super.readEntityFromNBT( nbt );
		m_name = nbt.getString( "name" );
		m_supporterId = nbt.getInteger( "supporterId" );
		initPlaque();
	}
	
	private void initPlaque()
	{
		// set the bounding box
		setDirection( hangingDirection );
		
		// if this supporter isn't valid, kill the plaque
		if( m_supporterId == Supporters.InvalidSupporterId )
		{
			setDead();
			return;
		}
	}
	
	@Override
	public int getWidthPixels( )
	{
		return 32;
	}

	@Override
	public int getHeightPixels( )
	{
		return 16;
	}
	
	@Override
	public void onBroken( Entity entity )
	{
		// don't do anything in creative mode
		if( entity instanceof EntityPlayer )
		{
			EntityPlayer entityplayer = (EntityPlayer)entity;
			if( entityplayer.capabilities.isCreativeMode )
			{
				return;
			}
		}
		
		// drop the item on break
		entityDropItem( new ItemStack( Ships.m_itemShipPlaque ), 0 );
	}
	
	@Override
	public void setPositionAndRotation2( double x, double y, double z, float yaw, float pitch, int alwaysThree )
	{
		// ignore position updates from the server
	}
	
	@Override
	public boolean interactFirst( EntityPlayer player )
    {
		// is this the player that placed the plaque?
		int supporterId = Supporters.getId( player.getCommandSenderName() );
		if( m_supporterId != supporterId )
		{
			return false;
		}
		
		// show the edit GUI
		Gui.ShipPlaque.open( player, player.worldObj, getEntityId(), 0, 0 );
		
		return true;
    }

	@Override
	public void writeSpawnData( ByteBuf buf )
	{
		// need to write HangingEntity data too
		// since we're not using the usual HangingEntity spawn packet
		buf.writeInt( field_146063_b ); // x
		buf.writeInt( field_146064_c ); // y
		buf.writeInt( field_146062_d ); // z
		buf.writeInt( hangingDirection );
		
		ByteBufUtils.writeUTF8String( buf, m_name );
		buf.writeInt( m_supporterId );
	}

	@Override
	public void readSpawnData( ByteBuf buf )
	{
		// need to read HangingEntity data too
		// since we're not using the usual HangingEntity spawn packet
		field_146063_b = buf.readInt(); // x
		field_146064_c = buf.readInt(); // y
		field_146062_d = buf.readInt(); // z
		hangingDirection = buf.readInt();
		
		m_name = ByteBufUtils.readUTF8String( buf );
		m_supporterId = buf.readInt();
		initPlaque();
	}
}
