package cuchaz.ships;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.apache.commons.codec.binary.Base64OutputStream;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockUtils;
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
	public boolean onItemUseFirst( ItemStack itemStack, EntityPlayer player, final World world, int blockX, int blockY, int blockZ, int side, float hitX, float hitY, float hitZ )
    {
		// did we use the item on a ship block?
		int blockId = world.getBlockId( blockX, blockY, blockZ );
		if( blockId != Ships.m_blockShip.blockID )
		{
			return false;
		}
		
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
			String encodedBlocks = encodeBlocks( world, blocks );
			
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
	
	private String encodeBlocks( World world, List<ChunkCoordinates> blocks )
	throws IOException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream( new GZIPOutputStream( new Base64OutputStream( buffer ) ) );
		
		out.writeInt( blocks.size() );
		for( ChunkCoordinates coords : blocks )
		{
			out.writeInt( coords.posX );
			out.writeInt( coords.posY );
			out.writeInt( coords.posZ );
			out.writeInt( world.getBlockId( coords.posX, coords.posY, coords.posZ ) );
			out.writeInt( world.getBlockMetadata( coords.posX, coords.posY, coords.posZ ) );
		}
		
		out.close();
		return new String( buffer.toByteArray(), "UTF-8" );
	}
	
	private void message( EntityPlayer player, GuiString text )
	{
		// only send messages on the client
		if( player.worldObj.isRemote )
		{
			player.addChatMessage( text.getLocalizedText() );
		}
	}
}
