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

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.TileEntityProjector;

@SideOnly( Side.CLIENT )
public class TileEntityProjectorRenderer extends TileEntitySpecialRenderer
{
	private static final ResourceLocation Texture = new ResourceLocation( "ships", "textures/models/projector.png" );
	
	private RenderShip m_shipRenderer;
	private ModelProjector m_model;
	private RenderBlocks m_renderBlocks;
	
	public TileEntityProjectorRenderer( RenderShip shipRenderer )
	{
		m_shipRenderer = shipRenderer;
		m_model = new ModelProjector();
		m_renderBlocks = new RenderBlocks();
	}
	
	@Override
	public void renderTileEntityAt( TileEntity tileEntity, double x, double y, double z, float partialTickTime )
	{
		renderTileEntityAt( (TileEntityProjector)tileEntity, x, y, z, partialTickTime );
	}
	
	public void renderTileEntityAt( TileEntityProjector projector, double x, double y, double z, float partialTickTime )
	{
		RenderManager.instance.renderEngine.bindTexture( Texture );
		
		// scale from model space to world space
		float scaleFactor = 1/16.0f;
		
		GL11.glPushMatrix();
		GL11.glTranslated( x + 0.5, y + 1.5, z + 0.5 );
		GL11.glScalef( 1.0f, -1.0f, -1.0f );
		
		// render the block
		m_model.render( scaleFactor );
		
		GL11.glPopMatrix();
		
		// now render the ship too
		ShipWorld shipWorld = projector.getShipWorld();
		if( shipWorld != null )
		{
			// prep for rendering in blocks space
			GL11.glPushMatrix();
			GL11.glTranslated( x, y, z );
			Coords translation = projector.getShipTranslation();
			GL11.glTranslatef( translation.x, translation.y, translation.z );
			RenderManager.instance.renderEngine.bindTexture( TextureMap.locationBlocksTexture );
			
			// render ship effects
			// TODO: figure out blending!
			GL11.glColor4ub( (byte)0, (byte)0, (byte)255, (byte)0 );
			GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE );
			GL11.glEnable( GL11.GL_BLEND );
			
			// render the display list
			m_renderBlocks.renderAllFaces = true;
			GL11.glCallList( m_shipRenderer.getDisplayList( m_renderBlocks, shipWorld ) );
			
			GL11.glPopMatrix();
		}
	}
}
