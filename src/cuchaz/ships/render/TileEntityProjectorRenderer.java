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

import java.util.Random;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.TileEntityProjector;

@SideOnly(Side.CLIENT)
public class TileEntityProjectorRenderer extends TileEntitySpecialRenderer {
	
	private static final ResourceLocation Texture = new ResourceLocation("ships", "textures/models/projector.png");
	
	private RenderShip m_shipRenderer;
	private ModelProjector m_model;
	private RenderBlocks m_renderBlocks;
	private Random m_rand;
	
	public TileEntityProjectorRenderer(RenderShip shipRenderer) {
		m_shipRenderer = shipRenderer;
		m_model = new ModelProjector();
		m_renderBlocks = new RenderBlocks();
		m_rand = new Random();
	}
	
	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTickTime) {
		renderTileEntityAt((TileEntityProjector)tileEntity, x, y, z, partialTickTime);
	}
	
	public void renderTileEntityAt(TileEntityProjector projector, double x, double y, double z, float partialTickTime) {
		int pass = MinecraftForgeClient.getRenderPass();
		
		if (pass == 0) {
			// scale from model space to world space
			float scaleFactor = 1 / 16.0f;
			
			// render the block
			bindTexture(Texture);
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5, y + 1.5, z + 0.5);
			GL11.glScalef(1f, -1f, -1f);
			m_model.render(scaleFactor);
			GL11.glPopMatrix();
		}
		
		// calculate timing for flicker
		float alpha = 0.5f;
		if (m_rand.nextFloat() > 0.01) {
			alpha = 1.0f;
		}
		
		if (pass == 1) {
			GL11.glEnable(GL11.GL_BLEND);
			
			// render the ship
			ShipWorld shipWorld = projector.getShipWorld();
			if (shipWorld != null) {
				// prep for rendering in blocks space
				bindTexture(TextureMap.locationBlocksTexture);
				GL11.glPushMatrix();
				GL11.glTranslated(x, y, z);
				Coords translation = projector.getShipTranslation();
				GL11.glTranslatef(translation.x, translation.y, translation.z);
				
				// render ship effects
				// NOTE: can't use glColor here since we're rendering a call list
				// the color was already applied when we built the call list
				GL14.glBlendColor(49f / 255f * alpha, 136f / 255f * alpha, alpha, 0f);
				GL11.glBlendFunc(GL11.GL_CONSTANT_COLOR, GL11.GL_ONE_MINUS_CONSTANT_COLOR);
				
				// render the display list
				m_renderBlocks.renderAllFaces = true;
				GL11.glCallList(m_shipRenderer.getDisplayList(m_renderBlocks, shipWorld));
				
				// render the tile entities
				for (Coords coords : shipWorld.coords()) {
					TileEntity tileEntity = shipWorld.getTileEntity(coords);
					if (tileEntity != null && TileEntityRendererDispatcher.instance.hasSpecialRenderer(tileEntity)) {
						TileEntityRendererDispatcher.instance.renderTileEntityAt(tileEntity, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, partialTickTime);
					}
				}
				
				GL11.glPopMatrix();
			}
			
			// render glow on a billboard
			bindTexture(Texture);
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5, y + 1.25, z + 0.5);
			GL11.glRotatef((float)Math.toDegrees(Math.atan2(x + 0.5, z + 0.5)), 0, 1, 0);
			GL11.glScalef(-1, -1, 1);
			GL11.glColor4f(1, 1, 1, alpha);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-0.5, 0, 0, 0.5, 0);
			tessellator.addVertexWithUV(-0.5, 1, 0, 0.5, 1);
			tessellator.addVertexWithUV(0.5, 1, 0, 1, 1);
			tessellator.addVertexWithUV(0.5, 0, 0, 1, 0);
			tessellator.draw();
			GL11.glPopMatrix();
			
			GL11.glDisable(GL11.GL_BLEND);
		}
	}
}
