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
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHangingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.EntitySupporterPlaque;
import cuchaz.ships.Supporters;

public class ItemSupporterPlaque extends ItemHangingEntity
{
	public ItemSupporterPlaque( int itemId )
	{
		super( itemId, EntitySupporterPlaque.class );
		
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
	
	@Override
	public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int sideId, float xHit, float yHit, float zHit )
	{
		// is this player not a supporter?
		if( Supporters.getId( player.username ) == Supporters.InvalidSupporterId /* TEMP */ && false )
		{
			return false;
		}
		
		// was the plaque placed on the top or bottom of a block?
		BlockSide side = BlockSide.getById( sideId );
		if( side == BlockSide.Bottom || side == BlockSide.Top )
		{
			return false;
		}
		
		// create the entity
		EntityHanging entity = new EntitySupporterPlaque( world, player, x, y, z, Direction.facingToDirection[sideId] );
		if( entity.onValidSurface() )
		{
			if( !world.isRemote )
			{
				world.spawnEntityInWorld( entity );
			}
			
			// use the item
			--itemStack.stackSize;
			
			return true;
		}
		
		return false;
	}
}
