package cuchaz.ships;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemPaddle extends Item
{
	public ItemPaddle( int itemId )
	{
		super( itemId );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabTools );
		setUnlocalizedName( "paddle" );
	}
	
	@Override
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:paddle" );
	}
}
