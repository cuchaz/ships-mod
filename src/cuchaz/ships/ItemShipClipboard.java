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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.ships.gui.GuiString;

public class ItemShipClipboard extends Item
{
	public ItemShipClipboard( int itemId )
	{
		super( itemId );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabTools );
		setUnlocalizedName( "shipClipboard" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:shipClipboard" );
	}
	
	@Override
	public ItemStack onItemRightClick( ItemStack itemStack, World world, EntityPlayer player )
    {
		// client only
		if( !world.isRemote )
		{
			return itemStack;
		}
		
		// find out where we're aiming
		final boolean IntersectWater = true;
		MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer( world, player, IntersectWater );
		if( movingobjectposition == null || movingobjectposition.typeOfHit != EnumMovingObjectType.TILE )
		{
			return itemStack;
		}
		int x = movingobjectposition.blockX;
		int y = movingobjectposition.blockY;
		int z = movingobjectposition.blockZ;
		
		// did we use the item on a ship block?
		int blockId = world.getBlockId( x, y, z );
		if( blockId == Block.waterStill.blockID || blockId == Block.waterMoving.blockID )
		{
			pasteShip( world, player, x, y, z );
		}
		else
		{
			message( player, GuiString.ClipboardUsage );
		}
		
		return itemStack;
    }
	
	@Override
	public boolean onItemUseFirst( ItemStack itemStack, EntityPlayer player, final World world, int blockX, int blockY, int blockZ, int side, float hitX, float hitY, float hitZ )
    {
		// only on the client
		if( !world.isRemote )
		{
			return false;
		}
		
		int blockId = world.getBlockId( blockX, blockY, blockZ );
		if( blockId == Ships.m_blockShip.blockID )
		{
			return copyShip( world, player, blockX, blockY, blockZ );
		}
		return false;
    }
	
	private boolean copyShip( final World world, EntityPlayer player, int blockX, int blockY, int blockZ )
	{
		// get the ship type from the block
		ShipType shipType = Ships.m_blockShip.getShipType( world, blockX, blockY, blockZ );
		
		// find all the blocks connected to the ship block
		List<ChunkCoordinates> blocks = BlockUtils.searchForBlocks(
			blockX, blockY, blockZ,
			shipType.getMaxNumBlocks(),
			new BlockUtils.BlockValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
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
		
		// also add the ship block
		ChunkCoordinates shipCoords = new ChunkCoordinates( blockX, blockY, blockZ );
		blocks.add( shipCoords );
		
		try
		{
			// encode the blocks into a string
			BlocksStorage storage = new BlocksStorage();
			storage.readFromWorld( world, shipCoords, blocks );
			String encodedBlocks = storage.writeToString();
			
			// save the string to the clipboard
			StringSelection selection = new StringSelection( encodedBlocks );
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents( selection, selection );
			
			message( player, GuiString.CopiedShip );
			return true;
		}
		catch( IOException ex )
		{
			Ships.logger.log( Level.WARNING, "Unable to copy ship!", ex );
			message( player, GuiString.ErrorCheckLogForDetails );
			return false;
		}
    }
	
	private boolean pasteShip( World world, EntityPlayer player, int blockX, int blockY, int blockZ )
	{
		// make sure we're in creative mode
		if( !player.capabilities.isCreativeMode )
		{
			message( player, GuiString.PastingOnlyCreative );
			return false;
		}
		
		try
		{
			// get the contents of the clipboard
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents = clipboard.getContents( null );
			String encodedBlocks = null;
			if( contents.isDataFlavorSupported( DataFlavor.stringFlavor ) )
			{
				encodedBlocks = (String)contents.getTransferData( DataFlavor.stringFlavor );
			}
			if( encodedBlocks == null )
			{
				message( player, GuiString.NoShipOnClipboard );
				return false;
			}
			
			// decode the ship
			BlocksStorage storage = new BlocksStorage();
			storage.readFromString( encodedBlocks );
			
			// how big is the ship?
			BoundingBoxInt box = new BoundingBoxInt( storage.getBoundingBox() );
			int dx = box.getDx();
			int dy = box.getDy();
			int dz = box.getDz();
			
			// look for a place to put the ship
			box.minY = blockY + 1;
			box.maxY = blockY + dy;
			for( int x=0; x<dx; x++ )
			{
				box.minX = blockX - x;
				box.maxX = blockX + dx - 1;
				for( int z=0; z<dz; z++ )
				{
					box.minZ = blockZ - z;
					box.maxZ = blockZ + dz - 1;
					if( isBoxAndShellEmpty( world, box ) )
					{
						placeShip( world, box, storage );
						return true;
					}
				}
			}
			
			message( player, GuiString.NoRoomToPasteShip, dx, dy, dz );
			return false;
		}
		catch( IOException ex )
		{
			message( player, GuiString.NoShipOnClipboard );
			return false;
		}
		catch( UnsupportedFlavorException ex )
		{
			message( player, GuiString.NoShipOnClipboard );
			return false;
		}
	}
	
	private boolean isBoxAndShellEmpty( World world, BoundingBoxInt box )
	{
		// check each block in the box, and also a shell of size 1 around the box
		for( int x=box.minX-1; x<=box.maxX+1; x++ )
		{
			for( int y=box.minY-1; y<=box.maxY+1; y++ )
			{
				for( int z=box.minZ-1; z<=box.maxZ+1; z++ )
				{
					int blockId = world.getBlockId( x, y, z );
					if( blockId != 0 && blockId != Block.waterStill.blockID && blockId != Block.waterMoving.blockID )
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void placeShip( World world, BoundingBoxInt targetBox, BlocksStorage storage )
	{
		// compute the translation
		BoundingBoxInt sourceBox = storage.getBoundingBox();
		// TEMP
		System.out.println( String.format( "Source box: [%d,%d][%d,%d][%d,%d]",
			sourceBox.minX, sourceBox.maxX,
			sourceBox.minY, sourceBox.maxY,
			sourceBox.minZ, sourceBox.maxZ
		) );
		
		int tx = targetBox.minX - sourceBox.minX;
		int ty = targetBox.minY - sourceBox.minY;
		int tz = targetBox.minZ - sourceBox.minZ;
		
		// TEMP
		Ships.logger.info( String.format( "translation: [%d,%d][%d,%d][%d,%d] -> [%d,%d][%d,%d][%d,%d] => t=(%d,%d,%d)",
			sourceBox.minX, sourceBox.maxX, sourceBox.minY, sourceBox.maxY, sourceBox.minZ, sourceBox.maxZ,
			targetBox.minX, targetBox.maxX, targetBox.minY, targetBox.maxY, targetBox.minZ, targetBox.maxZ,
			tx, ty, tz
		) );
		
		Map<ChunkCoordinates,ChunkCoordinates> correspondence = new TreeMap<ChunkCoordinates,ChunkCoordinates>();
		for( ChunkCoordinates shipCoords : storage.coords() )
		{
			ChunkCoordinates worldCoords = new ChunkCoordinates(
				shipCoords.posX + tx,
				shipCoords.posY + ty,
				shipCoords.posZ + tz
			);
			// TEMP
			Ships.logger.info( String.format( "Correspondence: (%d,%d,%d) -> (%d,%d,%d)",
				shipCoords.posX, shipCoords.posY, shipCoords.posZ,
				worldCoords.posX, worldCoords.posY, worldCoords.posZ
			) );
			correspondence.put( shipCoords, worldCoords );
		}
		
		storage.writeToWorld( world, correspondence );
	}
	
	private void message( EntityPlayer player, GuiString text, Object ... args )
	{
		// only send messages on the client
		if( player.worldObj.isRemote )
		{
			player.addChatMessage( String.format( text.getLocalizedText(), args ) );
		}
	}
}
