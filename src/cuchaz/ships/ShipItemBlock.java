package cuchaz.ships;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ShipItemBlock extends ItemBlock
{
	public ShipItemBlock( int blockId )
	{
		super( blockId );
		setHasSubtypes( true );
		setUnlocalizedName( "shipBlock" );
	}
	
	@Override
	public int getMetadata( int damageValue )
	{
		return damageValue;
	}
	
	@Override
	public String getUnlocalizedName( ItemStack itemstack )
	{
		return getUnlocalizedName() + ShipType.getByMeta( itemstack.getItemDamage() ).name();
	}
}
