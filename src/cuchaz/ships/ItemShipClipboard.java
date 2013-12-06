/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.ships.gui.GuiString;

public class ItemShipClipboard extends Item
{
	private static final String Encoding = "UTF-8";
	
	private static class PersistableBlock implements Comparable<PersistableBlock>
	{
		public int x;
		public int y;
		public int z;
		public int blockId;
		public int meta;
		
		public PersistableBlock( int x, int y, int z, int blockId, int meta )
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.blockId = blockId;
			this.meta = meta;
		}
		
		public PersistableBlock( World world, ChunkCoordinates coord )
		{
			x = coord.posX;
			y = coord.posY;
			z = coord.posZ;
			blockId = world.getBlockId( x, y, z );
			meta = world.getBlockMetadata( x, y, z );
		}
		
		@Override
		public int compareTo( PersistableBlock other )
		{
			int val = x - other.x;
			if( val != 0 )
			{
				return val;
			}
			val = y - other.y;
			if( val != 0 )
			{
				return val;
			}
			return z - other.z;
		}
		
		@Override
		public boolean equals( Object other )
		{
			if( other instanceof PersistableBlock )
			{
				return equals( (PersistableBlock)other );
			}
			return false;
		}
		
		public boolean equals( PersistableBlock other )
		{
			return x == other.x && y == other.y && z == other.z;
		}
	}
	
	private static class PersistableBlocks implements Iterable<PersistableBlock>
	{
		private Set<PersistableBlock> m_blocks;
		
		public PersistableBlocks( World world, Iterable<ChunkCoordinates> coords )
		{
			m_blocks = new TreeSet<PersistableBlock>();
			for( ChunkCoordinates coord : coords )
			{
				m_blocks.add( new PersistableBlock( world, coord ) );
			}
		}
		
		public PersistableBlocks( String encodedBlocks )
		throws IOException
		{
			// STREAM MADNESS!!! @_@  MADNESS, I TELL YOU!!
			DataInputStream in = new DataInputStream( new GZIPInputStream( new Base64InputStream( new ByteArrayInputStream( encodedBlocks.getBytes( Encoding ) ) ) ) );
			
			int numBlocks = in.readInt();
			m_blocks = new TreeSet<PersistableBlock>();
			for( int i=0; i<numBlocks; i++ )
			{
				m_blocks.add( new PersistableBlock(
					in.readInt(),
					in.readInt(),
					in.readInt(),
					in.readInt(),
					in.readInt()
				) );
			}
			in.close();
		}
		
		public String getEncodedBlocks( )
		throws IOException
		{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream( new GZIPOutputStream( new Base64OutputStream( buffer ) ) );
			
			out.writeInt( m_blocks.size() );
			for( PersistableBlock block : m_blocks )
			{
				out.writeInt( block.x );
				out.writeInt( block.y );
				out.writeInt( block.z );
				out.writeInt( block.blockId );
				out.writeInt( block.meta );
			}
			
			out.close();
			return new String( buffer.toByteArray(), Encoding );
		}
		
		public BoundingBoxInt getBoundingBox( )
		{
			BoundingBoxInt box = new BoundingBoxInt();
			for( PersistableBlock block : m_blocks )
			{
				box.expandBoxToInclude( block.x, block.y, block.z );
			}
			return box;
		}

		@Override
		public Iterator<PersistableBlock> iterator( )
		{
			return m_blocks.iterator();
		}
	}
	
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
		blocks.add( new ChunkCoordinates( blockX, blockY, blockZ ) );
		
		try
		{
			// encode the blocks into a string
			String encodedBlocks = new PersistableBlocks( world, blocks ).getEncodedBlocks();
			
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
			PersistableBlocks blocks = new PersistableBlocks( encodedBlocks );
			
			// how big is the ship?
			BoundingBoxInt box = blocks.getBoundingBox();
			int dx = box.getDx();
			int dy = box.getDy();
			int dz = box.getDz();
			
			// look for a place to put the ship
			box.minY = blockY + 1;
			box.maxY = blockY + dy - 1;
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
						placeShip( world, box, blocks );
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
	
	private void placeShip( World world, BoundingBoxInt targetBox, PersistableBlocks blocks )
	{
		// compute the translation
		BoundingBoxInt sourceBox = blocks.getBoundingBox();
		int tx = targetBox.minX - sourceBox.minX;
		int ty = targetBox.minY - sourceBox.minY;
		int tz = targetBox.minZ - sourceBox.minZ;
		
		// paste the ship
		for( PersistableBlock block : blocks )
		{
			world.setBlock( block.x + tx, block.y + ty, block.z + tz, block.blockId, block.meta, 3 );
		}
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
