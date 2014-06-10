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

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EnumStatus;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;

public class PlayerRespawner
{
	public static EnumStatus sleepInBedAt( int x, int y, int z )
    {
		return null;
		/*
        PlayerSleepInBedEvent event = new PlayerSleepInBedEvent(this, x, y, z);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.result != null)
        {
            return event.result;
        }
        if (!this.worldObj.isRemote)
        {
            if (this.isPlayerSleeping() || !this.isEntityAlive())
            {
                return EnumStatus.OTHER_PROBLEM;
            }

            if (!this.worldObj.provider.isSurfaceWorld())
            {
                return EnumStatus.NOT_POSSIBLE_HERE;
            }

            if (this.worldObj.isDaytime())
            {
                return EnumStatus.NOT_POSSIBLE_NOW;
            }

            if (Math.abs(this.posX - (double)x) > 3.0D || Math.abs(this.posY - (double)y) > 2.0D || Math.abs(this.posZ - (double)z) > 3.0D)
            {
                return EnumStatus.TOO_FAR_AWAY;
            }

            double d0 = 8.0D;
            double d1 = 5.0D;
            List list = this.worldObj.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.getAABBPool().getAABB((double)x - d0, (double)y - d1, (double)z - d0, (double)x + d0, (double)y + d1, (double)z + d0));

            if (!list.isEmpty())
            {
                return EnumStatus.NOT_SAFE;
            }
        }

        if (this.isRiding())
        {
            this.mountEntity((Entity)null);
        }

        this.setSize(0.2F, 0.2F);
        this.yOffset = 0.2F;

        if (this.worldObj.blockExists(x, y, z))
        {
            int l = this.worldObj.getBlockMetadata(x, y, z);
            int i1 = BlockBed.getDirection(l);
            Block block = Block.blocksList[worldObj.getBlockId(x, y, z)];
            if (block != null)
            {
                i1 = block.getBedDirection(worldObj, x, y, z);
            }
            float f = 0.5F;
            float f1 = 0.5F;

            switch (i1)
            {
                case 0:
                    f1 = 0.9F;
                    break;
                case 1:
                    f = 0.1F;
                    break;
                case 2:
                    f1 = 0.1F;
                    break;
                case 3:
                    f = 0.9F;
            }

            this.func_71013_b(i1);
            this.setPosition((double)((float)x + f), (double)((float)y + 0.9375F), (double)((float)z + f1));
        }
        else
        {
            this.setPosition((double)((float)x + 0.5F), (double)((float)y + 0.9375F), (double)((float)z + 0.5F));
        }

        this.sleeping = true;
        this.sleepTimer = 0;
        this.playerLocation = new ChunkCoordinates(x, y, z);
        this.motionX = this.motionZ = this.motionY = 0.0D;

        if (!this.worldObj.isRemote)
        {
            this.worldObj.updateAllPlayersSleepingFlag();
        }

        return EnumStatus.OK;
		*/
    }
	
	public static void onPlayerWakeUp( EntityPlayer player, boolean wasSleepSuccessful )
	{
		// ignore on clients
		World world = player.worldObj;
		if( world.isRemote )
		{
			return;
		}
		
		// ignore interruped sleep
		if( !wasSleepSuccessful )
		{
			return;
		}
		
		// is the player at a bed in the world?
		if( player.playerLocation != null )
		{
			int x = player.playerLocation.posX;
			int y = player.playerLocation.posY;
			int z = player.playerLocation.posZ;
			Block block = Block.blocksList[world.getBlockId( x, y, z )];
			if( block != null && block.isBed( world, x, y, z, player ) )
			{
				saveBedPointer( player, world, x, y, z );
				return;
			}
		}
		
		// is the player at a bed on a ship?
		for( EntityShip ship : ShipLocator.getFromEntityLocation( player ) )
		{
			
		}
	}
	
	private static void saveBedPointer( EntityPlayer player, World world, int x, int y, int z )
	{
		// TODO Auto-generated method stub
		
	}
	
	private static void saveBedPointer( EntityPlayer player, EntityShip ship, int x, int y, int z )
	{
		// TODO Auto-generated method stub
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
