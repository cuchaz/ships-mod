/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.blocks;

import java.util.Random;

import net.minecraft.block.BlockBed;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.Ships;

public class BlockBerth extends BlockBed
{
	public BlockBerth( int blockId )
	{
		super( blockId );
		
	    setHardness( 0.2F );
	    disableStats();
	    setTextureName( "bed" );
	}
	
	@Override
	public int idDropped( int meta, Random rand, int fortune )
    {
        return isBlockHeadOfBed( meta ) ? 0 : Ships.m_itemBerth.itemID;
    }
	
	@Override
	@SideOnly( Side.CLIENT )
	public int idPicked( World world, int x, int y, int z )
    {
        return Ships.m_itemBerth.itemID;
    }
}
