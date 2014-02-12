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
		data.writeInt( hangingDirection );
		data.writeInt( m_supporterId );
	}

	@Override
	public void readSpawnData( ByteArrayDataInput data )
	{
		// UNDONE: not getting world position somehow...
		// look at FMLClientHandler.spawnEntityIntoClientWorld() for info about what info gets copied to the client
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
		
		// TEMP
		Ships.logger.info( "Init plaque: %s at (%.1f,%.1f,%.1f)", Supporters.getName( m_supporterId ), posX, posY, posZ );
        Ships.logger.info( "Plaque direction: %d", hangingDirection );
        Ships.logger.info( "Plaque box: [%.2f,%.2f][%.2f,%.2f][%.2f,%.2f]",
        	boundingBox.minX, boundingBox.maxX,
        	boundingBox.minY, boundingBox.maxY,
        	boundingBox.minZ, boundingBox.maxZ
        );
        Thread.dumpStack();
	}
	
	@Override
	public int getWidthPixels( )
	{
		return 16;
	}

	@Override
	public int getHeightPixels( )
	{
		return 16;
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
	
	// TEMP
	@Override
	public void onUpdate( )
	{
		Ships.logger.info( "onUpdate plaque box: [%.2f,%.2f][%.2f,%.2f][%.2f,%.2f]",
			boundingBox.minX, boundingBox.maxX,
			boundingBox.minY, boundingBox.maxY,
			boundingBox.minZ, boundingBox.maxZ
        );
	}
}
