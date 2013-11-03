package cuchaz.ships;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
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
	@SideOnly( Side.CLIENT )
	public boolean shouldSideBeRendered( IBlockAccess world, int x, int y, int z, int side )
	{
		return false;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerIcons( IconRegister iconRegister )
	{
		// do nothing
	}
}
