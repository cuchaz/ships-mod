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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntitySupporterPlaque extends EntityHanging
{
	private int m_supporterId;
	
	public EntitySupporterPlaque( World world )
	{
		super( world );
		// called by entity loader. don't set anything
	}
	
	public EntitySupporterPlaque( World world, EntityPlayer player, int x, int y, int z, int direction )
	{
		super( world, x, y, z, direction );
		setDirection( direction );
		
		// get the supporter id
		m_supporterId = Supporters.getId( player.username );
		
		// TEMP
		m_supporterId = Supporters.getId( "MagisterXero" );
		
		// if this player isn't a supporter, kill the plaque
		if( m_supporterId == Supporters.InvalidSupporterId )
		{
			setDead();
			return;
		}
		
		// TEMP
		Ships.logger.info( "Created plaque: " + Supporters.getName( m_supporterId ) );
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
		
		// if this player isn't a supporter, kill the plaque
		if( m_supporterId == Supporters.InvalidSupporterId )
		{
			setDead();
			return;
		}
		
		// TEMP
		Ships.logger.info( "Loaded plaque: " + Supporters.getName( m_supporterId ) );
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
}
