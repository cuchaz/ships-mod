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
package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;

public class BlockStorage
{
	public int id;
	public int meta;
	
	public BlockStorage( )
	{
		this( 0, 0 );
	}
	
	public BlockStorage( int id, int meta )
	{
		this.id = id;
		this.meta = meta;
	}
	
	public void readFromWorld( World world, Coords coords )
	{
		id = world.getBlockId( coords.x, coords.y, coords.z );
		meta = world.getBlockMetadata( coords.x, coords.y, coords.z );
	}
	
	public void writeToWorld( World world, Coords coords )
	{
		// is this actually a valid block id on this Minecraft instance?
		if( Block.blocksList[id] == null )
		{
			Ships.logger.warning( "Unrecognized block id: " + id + ". Cannot restore block" );
			return;
		}
		BlockUtils.changeBlockWithoutNotifyingIt( world, coords.x, coords.y, coords.z, id, meta, UpdateRules.UpdateClients );
	}
}
