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
package cuchaz.ships;

import cuchaz.modsShared.blocks.Coords;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerRespawner
{
	public static void onPlayerRespawn( EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, int dimension )
	{
		if( oldPlayer.getBedLocation( dimension ) != null )
		{
			// since we remove bed info when a player sleeps on a ship,
			// the fact that bed info is here means a player slept on a bed outside of a ship
			// which means the player will respawn at that bed and we shouldn't do anything
			return;
		}
		
		// TEMP
		Ships.logger.info( "\n\nRespawn!\n\n" );
		
		/*
		// check for a saved ship position
		// UNDONE: implement these things
		SavedSpawn savedSpawn = m_savedSpawns.lookup( newPlayer.username, dimension );
		if( savedSpawn == null )
		{
			return;
		}
		
		// respawn at the saved spawn position
		Coords spawnPos = savedSpawn.getSpawnPos();
		newPlayer.setLocationAndAngles( spawnPos.x, spawnPos.y, spawnPos.z, 0, 0 );
		*/
	}
}
