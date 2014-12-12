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
package cuchaz.ships;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ShipItemBlock extends ItemBlock
{
	public ShipItemBlock( )
	{
		super( Ships.m_blockShip );
		setHasSubtypes( true );
		setUnlocalizedName( "shipBlock" );
	}
	
	@Override
	public int getMetadata( int damageValue )
	{
		return damageValue;
	}
	
	@Override
	public String getUnlocalizedName( ItemStack itemstack )
	{
		return getUnlocalizedName() + ShipType.getByMeta( itemstack.getItemDamage() ).name();
	}
}
