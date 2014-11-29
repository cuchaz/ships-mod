/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.Ships;
import cuchaz.ships.TileEntityProjector;

public class BlockProjector extends Block
{
	public BlockProjector( int blockId )
	{
		super( blockId, Material.circuits );
		
		setHardness( 2.0F );
		disableStats();
		setResistance( 5.0F );
		setStepSound( soundMetalFootstep );
	    setUnlocalizedName( "shipProjector" );
	    setTextureName( "projector" );
	}
	
	@Override
	public boolean isOpaqueCube( )
	{
		return false;
	}
	
	@Override
	public boolean renderAsNormalBlock( )
	{
		return false;
	}
	
	@Override
	public int getRenderType( )
	{
		return -1;
	}
	
	@Override
	public boolean hasTileEntity( int metadata )
	{
		return true;
	}
	
	@Override
	public TileEntity createTileEntity( World world, int metadata )
	{
		return new TileEntityProjector();
	}
	
	@Override
	@SideOnly( Side.CLIENT )
    public void registerIcons( IconRegister iconRegister )
    {
        this.blockIcon = iconRegister.registerIcon( "ships:projector" );
    }
	
	@Override
	public int idDropped( int meta, Random rand, int fortune )
    {
        return Ships.m_itemProjector.itemID;
    }
	
	@Override
	@SideOnly( Side.CLIENT )
	public int idPicked( World world, int x, int y, int z )
    {
        return Ships.m_itemProjector.itemID;
    }
	
	@Override
	public boolean onBlockActivated( World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset )
	{
		// TODO: show a gui
		//Gui.ShipPropulsion.open( player, world, x, y, z );
		return true;
	}
}
