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

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHangingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.EntitySupporterPlaque;
import cuchaz.ships.Supporters;
import cuchaz.ships.gui.GuiString;

public class ItemSupporterPlaque extends ItemHangingEntity
{
	public ItemSupporterPlaque( )
	{
		super( EntitySupporterPlaque.class );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabDecorations );
		setUnlocalizedName( "supporterPlaque" );
		setHasSubtypes( true );
		setMaxDamage( 0 );
	}
	
	@Override
	public void registerIcons( IIconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:supporterPlaque" );
	}
	
	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public void getSubItems( int itemId, CreativeTabs tabs, List items )
	{
		for( SupporterPlaqueType type : SupporterPlaqueType.values() )
		{
			items.add( type.newItemStack() );
		}
	}
	
	@Override
	public String getItemStackDisplayName( ItemStack itemStack )
	{
		return getItemDisplayName( itemStack );
	}
	
	@Override
	public String getItemDisplayName( ItemStack itemStack )
	{
		return SupporterPlaqueType.getByMeta( itemStack.getItemDamage() ).getItemName();
	}
	
	@Override
	public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int sideId, float xHit, float yHit, float zHit )
	{
		// get the type
		SupporterPlaqueType type = SupporterPlaqueType.getByMeta( itemStack.getItemDamage() );
		
		// is the player a supporter?
		if( !type.canUse( player ) )
		{
			if( Environment.isClient() )
			{
				player.addChatMessage( String.format( GuiString.NotASupporter.getLocalizedText() ) );
			}
			return false;
		}
		int supporterId = Supporters.getId( player.username );
		
		// was the plaque placed on the top or bottom of a block?
		BlockSide side = BlockSide.getById( sideId );
		if( side == BlockSide.Bottom || side == BlockSide.Top )
		{
			return false;
		}
		
		// create the entity
		EntityHanging entity = new EntitySupporterPlaque( world, type, supporterId, x, y, z, Direction.facingToDirection[sideId] );
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
