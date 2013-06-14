package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockShip extends Block
{
	protected BlockShip( int blockId )
	{
		super( blockId, Material.iron );
		
		setHardness( 5.0F );
		setResistance( 10.0F );
		setStepSound( soundMetalFootstep );
		setUnlocalizedName( "blockShip" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public Icon getIcon( int side, int meta )
	{
		return null;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		blockIcon = iconRegister.registerIcon( "ships:shipTop" );
	}
	
}
