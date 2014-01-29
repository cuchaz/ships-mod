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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public class ShipDebugRenderInfo
{
	private boolean m_isRendered;
	private Set<ChunkCoordinates> m_collidedCoords;
	private Map<Entity,AxisAlignedBB> m_queryBoxes;
	
	public ShipDebugRenderInfo( )
	{
		m_isRendered = false;
		m_collidedCoords = new TreeSet<ChunkCoordinates>();
		m_queryBoxes = new HashMap<Entity,AxisAlignedBB>();
	}
	
	public static boolean isDebugRenderingOn( )
	{
		return Minecraft.getMinecraft().gameSettings.showDebugInfo;
		// here's the flag for the game's built-in debug rendering
		// RenderManager.field_85095_o;
	}
	
	public Collection<ChunkCoordinates> getCollidedCoords( )
	{
		return m_collidedCoords;
	}
	
	public Iterable<AxisAlignedBB> getQueryBoxes( )
	{
		return m_queryBoxes.values();
	}
	
	public void setRendered()
	{
		m_isRendered = true;
	}
	
	public void addCollidedCoord( ChunkCoordinates coords )
	{
		if( m_isRendered )
		{
			reset();
		}
		m_collidedCoords.add( coords );
	}
	
	public void setQueryBox( Entity entity, AxisAlignedBB box )
	{
		if( m_isRendered )
		{
			reset();
		}
		m_queryBoxes.put( entity, box );
	}
	
	private void reset( )
	{
		m_isRendered = false;
		m_collidedCoords.clear();
		m_queryBoxes.clear();
	}
}
