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

import javax.swing.Icon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Direction;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.PlayerRespawner;
import cuchaz.ships.Ships;

public class BlockBerth extends BlockBed
{
	@SideOnly( Side.CLIENT )
	private IIcon m_iconFeetTop;
	@SideOnly( Side.CLIENT )
	private IIcon m_iconFeetEnd;
	@SideOnly( Side.CLIENT )
	private IIcon m_iconFeetSide;
	
	@SideOnly( Side.CLIENT )
	private IIcon m_iconHeadTop;
	@SideOnly( Side.CLIENT )
	private IIcon m_iconHeadEnd;
	@SideOnly( Side.CLIENT )
	private IIcon m_iconHeadSide;
	
	public BlockBerth( )
	{
	    setHardness( 0.2F );
	    disableStats();
	    setUnlocalizedName( "berth" );
	    setTextureName( "berth" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public void registerBlockIcons( IIconRegister iconRegister )
	{
		m_iconFeetTop = iconRegister.registerIcon( "ships:berthFeetTop" );
		m_iconFeetEnd = iconRegister.registerIcon( "ships:berthFeetEnd" );
		m_iconFeetSide = iconRegister.registerIcon( "ships:berthFeetSide" );
		
		m_iconHeadTop = iconRegister.registerIcon( "ships:berthHeadTop" );
		m_iconHeadEnd = iconRegister.registerIcon( "ships:berthHeadEnd" );
		m_iconHeadSide = iconRegister.registerIcon( "ships:berthHeadSide" );
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public IIcon getIcon( int sideId, int meta )
	{
		// bottom side is easy
		BlockSide side = BlockSide.getById( sideId );
		if( side == BlockSide.Bottom )
		{
			return Block.planks.getBlockTextureFromSide( sideId );
		}
		
		// the other sides are trickier...
		int bedDirection = Direction.bedDirection[getDirection( meta )][sideId];
		if( isBlockHeadOfBed( meta ) )
		{
			if( bedDirection == 2 )
			{
				return m_iconHeadEnd;
			}
			else if( bedDirection == 4 || bedDirection == 5 )
			{
				return m_iconHeadSide;
			}
			else
			{
				return m_iconHeadTop;
			}
		}
		else
		{
			if( bedDirection == 3 )
			{
				return m_iconFeetEnd;
			}
			else if( bedDirection == 4 || bedDirection == 5 )
			{
				return m_iconFeetSide;
			}
			else
			{
				return m_iconFeetTop;
			}
		}
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
