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
package cuchaz.ships.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.Coords;

@SideOnly(Side.CLIENT)
public class ShipDebugRenderInfo {
	
	private boolean m_isRendered;
	private BlockSet m_collidedCoords;
	private Map<Entity,AxisAlignedBB> m_queryBoxes;
	
	public ShipDebugRenderInfo() {
		m_isRendered = false;
		m_collidedCoords = new BlockSet();
		m_queryBoxes = new HashMap<Entity,AxisAlignedBB>();
	}
	
	public static boolean isDebugRenderingOn() {
		return Minecraft.getMinecraft().gameSettings.showDebugInfo;
		// here's the flag for the game's built-in debug rendering
		// RenderManager.field_85095_o;
	}
	
	public BlockSet getCollidedCoords() {
		return m_collidedCoords;
	}
	
	public Iterable<AxisAlignedBB> getQueryBoxes() {
		return m_queryBoxes.values();
	}
	
	public void setRendered() {
		m_isRendered = true;
	}
	
	public void addCollidedCoord(Coords coords) {
		if (m_isRendered) {
			reset();
		}
		m_collidedCoords.add(coords);
	}
	
	public void setQueryBox(Entity entity, AxisAlignedBB box) {
		if (m_isRendered) {
			reset();
		}
		m_queryBoxes.put(entity, box);
	}
	
	private void reset() {
		m_isRendered = false;
		m_collidedCoords.clear();
		m_queryBoxes.clear();
	}
}
