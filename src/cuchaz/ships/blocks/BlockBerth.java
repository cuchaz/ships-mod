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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.PlayerRespawner;
import cuchaz.ships.Ships;

public class BlockBerth extends BlockBed
{
	public BlockBerth( int blockId )
	{
		super( blockId );
		
	    setHardness( 0.2F );
	    disableStats();
	    setTextureName( "bed" ); // UNDONE: get a new texture for berths
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
	
	@Override
	public boolean isBed( World world, int x, int y, int z, EntityLivingBase player )
	{
		return true;
	}
	
	@Override
	public boolean onBlockActivated( World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ )
	{
		if( world.isRemote )
		{
			return true;
		}
		
		// get the meta of the head block
		int meta = world.getBlockMetadata( x, y, z );
		if( !isBlockHeadOfBed( meta ) )
		{
			int direction = getDirection( meta );
			x += footBlockToHeadBlockMap[direction][0];
			z += footBlockToHeadBlockMap[direction][1];
			
			if( world.getBlockId( x, y, z ) != blockID )
			{
				return true;
			}
			
			meta = world.getBlockMetadata( x, y, z );
		}
		
		// NOTE: x,y,z is now the berth head
		
		if( !world.provider.canRespawnHere() )
		{
			// blow up the berth
			world.newExplosion( null, x + 0.5, y + 0.5, z + 0.5, 5, true, true );
			return true;
		}
		
		// handle berth occupancy
		if( isBedOccupied( meta ) )
		{
			if( PlayerRespawner.isPlayerInBerth( world, x, y, z ) )
			{
				player.addChatMessage( "tile.bed.occupied" );
				return true;
			}
			else
			{
				setBedOccupied( world, x, y, z, false );
			}
		}
		
		// start sleeping in the berth
		switch( PlayerRespawner.sleepInBerthAt( world, x, y, z, player ) )
		{
			case OK:
				setBedOccupied( world, x, y, z, true );
			break;
			
			case NOT_POSSIBLE_NOW:
				player.addChatMessage( "tile.bed.noSleep" );
			break;
			
			case NOT_SAFE:
				player.addChatMessage( "tile.bed.notSafe" );
			break;
			
			case NOT_POSSIBLE_HERE:
			case OTHER_PROBLEM:
			case TOO_FAR_AWAY:
				player.addChatMessage( "Something bad happened with this berth..." );
		}
		
		return true;
	}
}
