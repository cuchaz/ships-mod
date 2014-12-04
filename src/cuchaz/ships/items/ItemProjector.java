package cuchaz.ships.items;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.ShipClipboard;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.TileEntityProjector;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketPlaceProjector;
import cuchaz.ships.persistence.PersistenceException;

public class ItemProjector extends Item
{
	public ItemProjector( )
	{
		setMaxStackSize( 1 );
		setCreativeTab( CreativeTabs.tabRedstone );
		setUnlocalizedName( "shipProjector" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IIconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:projector" );
	}
	
	@Override
	public ItemStack onItemRightClick( ItemStack itemStack, World world, EntityPlayer player )
    {
		// client only
		if( Environment.isServer() )
		{
			return itemStack;
		}
		
		try
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
			
			// only place on top of water
			int blockId = world.getBlockId( x, y, z );
			BlockSide side = BlockSide.getById(movingobjectposition.sideHit);
			if( blockId != Block.waterStill.blockID || side != BlockSide.Top )
			{
				clientMessage( player, GuiString.TryOnStillWater );
				return itemStack;
			}
			
			// there must be something on the ship clipboard
			String encodedBlocks = ShipClipboard.getBlocks();
			if( encodedBlocks == null )
			{
				clientMessage( player, GuiString.NoShipOnClipboard );
				return itemStack;
			}
			ShipWorld shipWorld = ShipClipboard.createShipWorld( world, encodedBlocks );
			placeProjector( world, x, y + 1, z, shipWorld );
			
			// tell the server
			PacketDispatcher.sendPacketToServer( new PacketPlaceProjector( encodedBlocks, x, y + 1, z ).getCustomPacket() );
			
			if( !player.capabilities.isCreativeMode )
			{
				// use the item
				itemStack.stackSize--;
			}
			return itemStack;
		}
		catch( PersistenceException ex )
		{
			clientMessage( player, GuiString.NoShipOnClipboard );
			return itemStack;
		}
    }
	
	public static void placeProjector( World world, int x, int y, int z, ShipWorld shipWorld )
	{
		// place the block
		world.setBlock( x, y, z, Ships.m_blockProjector.blockID );
		
		// tell the block about the ship
		TileEntityProjector tileEntity = (TileEntityProjector)world.getBlockTileEntity( x, y, z );
		tileEntity.setShipWorld( shipWorld );
	}
	
	@Override
	public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int sideId, float hitX, float hitY, float hitZ )
	{
		return false;
	}
	
	private void clientMessage( EntityPlayer player, GuiString text, Object ... args )
	{
		if( Environment.isClient() )
		{
			player.addChatMessage( String.format( text.getLocalizedText(), args ) );
		}
	}
}
