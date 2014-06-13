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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.EntityAccessor;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerAccessor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EnumStatus;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PlayerRespawner
{
	private static class BerthCoords
	{
		World world;
		int x;
		int y;
		int z;
		
		public BerthCoords( World world, int x, int y, int z )
		{
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private static Map<Integer,BerthCoords> m_playerSleptInBerth;
	private static Map<String,BerthCoords> m_playerSavedBerths;
	
	static
	{
		m_playerSleptInBerth = new TreeMap<Integer,BerthCoords>();
		m_playerSavedBerths = new TreeMap<String,BerthCoords>();
	}
	
	public static EnumStatus sleepInBedAt( World world, int x, int y, int z, EntityPlayer player )
    {
		// sadly, I have to re-implement some logic from EntityPlayer.sleepInBed() to get this to work...
		
		if( !world.isRemote )
		{
			// on the server, check for some conditions
            if( player.isPlayerSleeping() || !player.isEntityAlive() )
            {
                return EnumStatus.OTHER_PROBLEM;
            }
            if( !world.provider.isSurfaceWorld() )
            {
                return EnumStatus.NOT_POSSIBLE_HERE;
            }
			if( world.isDaytime() )
			{
				return EnumStatus.NOT_POSSIBLE_NOW;
			}
			
			// get the position of the player in the coordinate system of the blocks
			Vec3 playerPos = Vec3.createVectorHelper( player.posX, player.posY, player.posZ );
			if( world instanceof ShipWorld )
			{
				ShipWorld shipWorld = (ShipWorld)world;
				EntityShip ship = shipWorld.getShip();
				ship.worldToShip( playerPos );
				ship.shipToBlocks( playerPos );
			}
			
			if( Math.abs( playerPos.xCoord - x ) > 3 || Math.abs( playerPos.yCoord - y ) > 2 || Math.abs( playerPos.zCoord - z ) > 3 )
			{
				return EnumStatus.TOO_FAR_AWAY;
			}
			
			// are there any mobs nearby?
			// NOTE: cheat here and use the player position instead of the block position
			int dXZ = 8;
			int dY = 5;
			@SuppressWarnings( "unchecked" )
			List<EntityMob> mobs = (List<EntityMob>)world.getEntitiesWithinAABB(
				EntityMob.class,
				AxisAlignedBB.getAABBPool().getAABB(
					player.posX - dXZ, player.posY - dY, player.posZ - dXZ,
					player.posX + dXZ, player.posY + dY, player.posZ + dXZ
				)
			);
			if( !mobs.isEmpty() )
			{
				return EnumStatus.NOT_SAFE;
			}
		}
		
		// NOTE: at this point, we're committed to sleeping
		
		if( player.isRiding() )
		{
			// stop riding
			player.mountEntity( null );
		}
		
		// move the player to the sleeping position
		EntityAccessor.setSize( player, 0.2F, 0.2F );
		player.yOffset = 0.2F;
		
		if( world.blockExists( x, y, z ) )
		{
			// move the player into the bed
			
			int meta = world.getBlockMetadata( x, y, z );
			int direction = BlockBed.getDirection( meta );
			Block block = Block.blocksList[world.getBlockId( x, y, z )];
			if( block != null )
			{
				direction = block.getBedDirection( world, x, y, z );
			}
			float dx = 0.5F;
			float dz = 0.5F;
			
			switch( direction )
			{
				case 0:
					dz = 0.9F;
				break;
				case 1:
					dx = 0.1F;
				break;
				case 2:
					dz = 0.1F;
				break;
				case 3:
					dx = 0.9F;
			}
			
			// I can't call this private method and I don't know what it does
			// let's try not calling it instead. =P
			//player.func_71013_b( direction );
			
			player.setPosition( x + dx, y + 0.9375F, z + dz );
		}
		else
		{
			// umm... we couldn't find the bed. Just make something up
			player.setPosition( x + 0.5F, y + 0.9375F, z + 0.5F );
		}
		
		EntityPlayerAccessor.setSleeping( player, true );
		player.sleepTimer = 0;
		player.motionX = 0;
		player.motionZ = 0;
		player.motionY = 0;
		
		if( !world.isRemote )
		{
			world.updateAllPlayersSleepingFlag();
		}
		
		// save this player and berth so we can find it again when the player wakes up
		m_playerSleptInBerth.put( player.entityId, new BerthCoords( world, x, y, z ) );
		
		// UNDONE: remove player bed location
		
		return EnumStatus.OK;
    }
	
	public static void onPlayerWakeUp( EntityPlayer player, boolean wasSleepSuccessful )
	{
		// ignore on clients
		World world = player.worldObj;
		if( world.isRemote )
		{
			return;
		}
		
		// ignore interrupted sleep
		if( !wasSleepSuccessful )
		{
			return;
		}
		
		// what was the last berth the player slept in?
		BerthCoords coords = m_playerSleptInBerth.get( player.entityId );
		if( coords == null )
		{
			return;
		}
		m_playerSleptInBerth.remove( player.entityId );
		
		// is there a berth there?
		Block block = Block.blocksList[coords.world.getBlockId( coords.x, coords.y, coords.z )];
		if( block != null && block.blockID == Ships.m_blockBerth.blockID )
		{
			// save the berth coords
			m_playerSavedBerths.put( player.username, coords );
		}
	}
	
	public static void onPlayerRespawn( EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, int dimension )
	{
		// ignore on clients
		if( oldPlayer.worldObj.isRemote )
		{
			return;
		}
		
		if( oldPlayer.getBedLocation( dimension ) != null )
		{
			// since we remove bed info when a player sleeps on a ship,
			// the fact that bed info is here means a player slept on a bed outside of a ship
			// which means the player will respawn at that bed and we shouldn't do anything
			return;
		}
		
		// TEMP
		Ships.logger.info( "\n\nRespawn!\n\n" );
		
		BerthCoords coords = m_playerSavedBerths.get( newPlayer.username );
		if( coords == null )
		{
			return;
		}
		
		// get the player position in world coords
		Vec3 p = Vec3.createVectorHelper( coords.x, coords.y, coords.z );
		if( coords.world instanceof ShipWorld )
		{
			ShipWorld shipWorld = (ShipWorld)coords.world;
			EntityShip ship = shipWorld.getShip();
			ship.blocksToShip( p );
			ship.shipToWorld( p );
		}
		
		// respawn at the berth position
		newPlayer.setLocationAndAngles( p.xCoord, p.yCoord, p.zCoord, 0, 0 );
	}
}
