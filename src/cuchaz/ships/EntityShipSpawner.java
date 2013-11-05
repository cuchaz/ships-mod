/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import com.google.common.base.Function;

import cpw.mods.fml.common.network.EntitySpawnPacket;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.packets.PacketShipBlocksRequest;

@SideOnly( Side.CLIENT )
public class EntityShipSpawner implements Function<EntitySpawnPacket,Entity>
{
	@Override
	public Entity apply( EntitySpawnPacket packet )
	{
		World world = Minecraft.getMinecraft().theWorld;
		
		// create the ship
		EntityShip ship = new EntityShip( world );
        ship.entityId = packet.entityId;
        ship.setLocationAndAngles( packet.scaledX, packet.scaledY, packet.scaledZ, packet.scaledYaw, packet.scaledPitch );
        
        // request blocks for the ship
		PacketDispatcher.sendPacketToServer( new PacketShipBlocksRequest( ship ).getCustomPacket() );
		
        return ship;
	}
}
