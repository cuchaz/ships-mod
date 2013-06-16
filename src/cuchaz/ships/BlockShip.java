package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockSide;

public class BlockShip extends Block
{
	@SideOnly( Side.CLIENT )
	private Icon m_iconSide;
	
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
		switch( BlockSide.getById( side ) )
		{
			case North:
			case South:
			case East:
			case West:
				return m_iconSide;
			
			default:
				return blockIcon;
		}
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		blockIcon = iconRegister.registerIcon( "ships:shipTop" );
		m_iconSide = iconRegister.registerIcon( "ships:shipSide" );
	}
	
	@Override
	public boolean onBlockActivated( World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9 )
	{
		// show the ship UI
		Gui.Ship.open( player, x, y, z );
		return true;
	}
	
	@Override
	public void breakBlock( World world, int x, int y, int z, int side, int meta )
	{
		// UNDONE: destruct the ship
	}
}
