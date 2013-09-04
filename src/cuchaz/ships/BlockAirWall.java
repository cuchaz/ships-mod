package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAirWall extends Block
{
	protected BlockAirWall( int blockId )
	{
		// an air block that stops flowing water
		super( blockId, Ships.m_materialAirWall );
		
		setUnlocalizedName( "blockAirWall" );
		setCreativeTab( CreativeTabs.tabBlock );
		
		setBlockBounds( 0, 0, 0, 1, 1, 1 );
	}
	
	@Override
	public boolean renderAsNormalBlock( )
	{
		return false;
	}
	
	@Override
	public boolean isOpaqueCube( )
	{
		return false;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool( World world, int x, int y, int z )
	{
		return null;
	}
	
	@Override
	public boolean canCollideCheck( int meta, boolean hitLiquids )
	{
		return hitLiquids;
	}
	
	@Override
	public boolean shouldSideBeRendered( IBlockAccess world, int x, int y, int z, int side )
	{
		return false;
	}
	
	@Override
	public void registerIcons( IconRegister iconRegister )
	{
		// do nothing
	}
}
