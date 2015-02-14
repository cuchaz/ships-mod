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
package cuchaz.ships.core;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.EntityShip;
import cuchaz.ships.HitList;

public class ShipIntermediaryClient {
	
	public static final String Path = "cuchaz/ships/core/ShipIntermediaryClient";
	
	@SideOnly(Side.CLIENT)
	public static void onFoundHit() {
		// NOTE: this is called at the end of EntityRenderer.getMouseOver()
		// if the targeted thing is a ship, then run our world block detection logic
		
		// get the current client state
		MovingObjectPosition currentHit = Minecraft.getMinecraft().objectMouseOver;
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		double reachDist = Minecraft.getMinecraft().playerController != null ? Minecraft.getMinecraft().playerController.getBlockReachDistance() : 0;
		
		// if there's no player, something weird is going on. Bail.
		if (player == null) {
			return;
		}
		
		// did we hit a ship?
		if (currentHit != null && currentHit.typeOfHit == MovingObjectType.ENTITY && currentHit.entityHit != null && currentHit.entityHit instanceof EntityShip) {
			EntityShip ship = (EntityShip)currentHit.entityHit;
			
			// check for hits again (this time we're aware of ship blocks)
			HitList hits = new HitList();
			hits.addHits(ship, player, reachDist);
			hits.addHits(player.worldObj, player, reachDist);
			HitList.Entry hit = hits.getClosestHit();
			
			// did we hit a world block?
			if (hit != null && hit.type == HitList.Type.World) {
				Minecraft.getMinecraft().objectMouseOver = hit.hit;
			}
		}
	}
}
