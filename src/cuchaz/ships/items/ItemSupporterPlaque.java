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
package cuchaz.ships.items;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemHangingEntity;

public class ItemSupporterPlaque extends ItemHangingEntity
{
	public ItemSupporterPlaque( int itemId )
	{
		super( itemId,  );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabDecorations );
		setUnlocalizedName( "supporterPlaque" );
		
		// UNDONE: make sure textures work
		setTextureName( "painting" );
	}
	
	@Override
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:supporterPlaque" );
	}
}
