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

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

public class EntitySupporterPlaque extends EntityHanging implements IEntityAdditionalSpawnData
{
	private int m_supporterId;
	
	public EntitySupporterPlaque( World world )
	{
		super( world );
		// called by deserializer. don't set anything here
	}
	
	public EntitySupporterPlaque( World world, EntityPlayer player, int x, int y, int z, int direction )
	{
		// NOTE: called when player spawns plaque via item
		super( world, x, y, z, direction );
		setDirection( direction );
		
		m_supporterId = Supporters.getId( player.username );
		initPlaque();
	}
	
	@Override
	public void writeEntityToNBT( NBTTagCompound nbt )
	{
        super.writeEntityToNBT( nbt );
		nbt.setInteger( "supporterId", m_supporterId );
	}
	
	@Override
	public void readEntityFromNBT( NBTTagCompound nbt )
	{
		super.readEntityFromNBT( nbt );
		m_supporterId = nbt.getInteger( "supporterId" );
		initPlaque();
	}
	
	@Override
	public void writeSpawnData( ByteArrayDataOutput data )
	{
		data.writeInt( xPosition );
		data.writeInt( yPosition );
		data.writeInt( zPosition );
		data.writeInt( hangingDirection );
		data.writeInt( m_supporterId );
	}

	@Override
	public void readSpawnData( ByteArrayDataInput data )
	{
		// need to read HangingEntity data too
		// since we're not using the usual HangingEntity spawn packet
		xPosition = data.readInt();
		yPosition = data.readInt();
		zPosition = data.readInt();
		setDirection( data.readInt() );
		
		m_supporterId = data.readInt();
		initPlaque();
	}
	
	private void initPlaque()
	{
		// TEMP
		m_supporterId = Supporters.getId( "MagisterXero" );
		
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
		return 32;
	}
	
	@Override
	public void onBroken( Entity entity )
	{
		// drop the item on break
		entityDropItem( new ItemStack( Ships.m_itemSupporterPlaque ), 0 );
	}
	
	@Override
	public void setPositionAndRotation2( double x, double y, double z, float yaw, float pitch, int alwaysThree )
	{
		// ignore position updates from the server
	}
}
