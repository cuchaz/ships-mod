package cuchaz.ships;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemMagicBucket extends Item
{
	public ItemMagicBucket( int itemId )
	{
		super( itemId );
		
		maxStackSize = 1;
		setCreativeTab( CreativeTabs.tabTools );
		setUnlocalizedName( "magicBucket" );
	}
	
	@Override
	public void registerIcons( IconRegister iconRegister )
	{
		itemIcon = iconRegister.registerIcon( "ships:magicBucket" );
	}
	
	@Override
	public ItemStack onItemRightClick( ItemStack itemStack, World world, EntityPlayer player )
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
		
		if( !world.canMineBlock( player, x, y, z ) )
		{
			return itemStack;
		}
		if( !player.canPlayerEdit( x, y, z, movingobjectposition.sideHit, itemStack ) )
		{
			return itemStack;
		}
		
		// is it a liquid block?
		Material material = world.getBlockMaterial( x, y, z );
		if( ( material == Material.water || material == Material.lava ) && world.getBlockMetadata( x, y, z ) == 0 )
		{
			// make it an air wall!
			world.setBlock( x, y, z, Ships.BlockAirWall.blockID );
		}
		// is it an air wall?
		else if( material == Ships.MaterialAirWall )
		{
			// make it back to normal air!
			world.setBlockToAir( x, y, z );
		}
		
		return itemStack;
	}
}
