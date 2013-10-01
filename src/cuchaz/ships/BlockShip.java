package cuchaz.ships;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.gui.Gui;

public class BlockShip extends Block
{
	protected BlockShip( int blockId )
	{
		super( blockId, Material.iron );
		
		setHardness( 5.0F );
		setResistance( 10.0F );
		setStepSound( soundMetalFootstep );
		setUnlocalizedName( "blockShip" );
		setCreativeTab( CreativeTabs.tabTransport );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public Icon getIcon( int side, int meta )
	{
		switch( BlockSide.getById( side ) )
		{
			case North:
			case South:
			case East:
			case West:
				return ShipType.getByMeta( meta ).getIcon();
			
			default:
				return blockIcon;
		}
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		blockIcon = iconRegister.registerIcon( "ships:shipTop" );
		ShipType.registerIcons( iconRegister );
	}
	
	@Override
	public boolean onBlockActivated( World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset )
	{
		// are we in the world, or on the ship?
		if( world instanceof ShipWorld )
		{
			// can the player paddle this ship?
			boolean isPaddleEquipped = player.getCurrentEquippedItem() != null
				&& player.getCurrentEquippedItem().getItem().itemID == Ships.m_itemPaddle.itemID;
			ShipWorld shipWorld = (ShipWorld)world;
			if( isPaddleEquipped && shipWorld.getShipType().isPaddleable() && shipWorld.getShip().isEntityCloseEnoughToRide( player ) )
			{
				Gui.PaddleShip.open( player, world, x, y, z );
			}
			else
			{
				Gui.UnbuildShip.open( player, world, x, y, z );
			}
		}
		else
		{
			Gui.BuildShip.open( player, world, x, y, z );
		}
		return true;
	}
	
	public ShipType getShipType( World world, int x, int y, int z )
	{
		return ShipType.getByMeta( world.getBlockMetadata( x, y, z ) );
	}
	
	@Override
	public int damageDropped( int metadata )
	{
		return metadata;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public void getSubBlocks( int itemId, CreativeTabs tabs, List subItems )
	{
		for( ShipType type : ShipType.values() )
		{
			subItems.add( type.newItemStack() );
		}
	}
}
