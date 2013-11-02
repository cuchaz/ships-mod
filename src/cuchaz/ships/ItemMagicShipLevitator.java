package cuchaz.ships;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemMagicShipLevitator extends Item
{
	public ItemMagicShipLevitator( int itemId )
	{
		super( itemId );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabTools );
		setUnlocalizedName( "magicShipLevitator" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:magicShipLevitator" );
	}
	
	@Override
	public boolean onLeftClickEntity( ItemStack itemStack, EntityPlayer player, Entity entity )
	{
		// is the entity a ship?
		EntityShip ship = null;
		if( entity instanceof EntityShip )
		{
			ship = (EntityShip)entity;
		}
		else
		{
			return false;
		}
		
		// bump it up
		ship.posY += 6.0;
		
		return true;
	}
}
