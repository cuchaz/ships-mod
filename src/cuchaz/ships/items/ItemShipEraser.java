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
package cuchaz.ships.items;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.MaterialProperties;
import cuchaz.ships.ShipGeometry;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipType;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketEraseShip;

public class ItemShipEraser extends Item
{
	public ItemShipEraser( int itemId )
	{
		super( itemId );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabTools );
		setUnlocalizedName( "shipEraser" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:shipEraser" );
	}
	
	@Override
	public boolean onItemUseFirst( ItemStack itemStack, EntityPlayer player, final World world, int blockX, int blockY, int blockZ, int side, float hitX, float hitY, float hitZ )
    {
		// NOTE: this only happens on the client
		
		int blockId = world.getBlockId( blockX, blockY, blockZ );
		if( blockId == Ships.m_blockShip.blockID )
		{
			return eraseShip( world, player, blockX, blockY, blockZ );
		}
		else
		{
			message( player, GuiString.EraserUsage );
			return false;
		}
    }
	
	public static boolean eraseShip( final World world, EntityPlayer player, int blockX, int blockY, int blockZ )
	{
		// make sure we're in creative mode
		if( !player.capabilities.isCreativeMode )
		{
			message( player, GuiString.OnlyCreative );
			return false;
		}
		
		// get the ship type from the block
		ShipType shipType = Ships.m_blockShip.getShipType( world, blockX, blockY, blockZ );
		
		// find all the blocks connected to the ship block
		BlockSet blocks = BlockUtils.searchForBlocks(
			blockX, blockY, blockZ,
			shipType.getMaxNumBlocks(),
			new BlockExplorer( )
			{
				@Override
				public boolean shouldExploreBlock( Coords coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.x, coords.y, coords.z )] );
				}
			},
			ShipGeometry.ShipBlockNeighbors
		);
		
		// did we find too many blocks?
		if( blocks == null )
		{
			message( player, GuiString.NoShipWasFoundHere );
			return false;
		}
		
		if( Environment.isClient() )
		{
			PacketDispatcher.sendPacketToServer( new PacketEraseShip( blockX, blockY, blockZ ).getCustomPacket() );
		}
		else
		{
			// also add the ship block
			Coords shipCoords = new Coords( blockX, blockY, blockZ );
			blocks.add( shipCoords );
			
			// remove the ship
			ShipLauncher.removeShipFromWorld( world, new ShipWorld( world, shipCoords, blocks ), shipCoords, UpdateRules.UpdateClients );
		}
		return true;
    }
	
	private static void message( EntityPlayer player, GuiString text, Object ... args )
	{
		if( Environment.isClient() )
		{
			player.addChatMessage( String.format( text.getLocalizedText(), args ) );
		}
	}
}
