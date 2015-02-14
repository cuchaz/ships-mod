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
package cuchaz.ships.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPainting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntitySupporterPlaque;
import cuchaz.ships.Supporters;

public class RenderSupporterPlaque extends RenderPainting {
	
	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
		doRender((EntitySupporterPlaque)entity, x, y, z, yaw, partialTickTime);
	}
	
	public void doRender(EntitySupporterPlaque entity, double x, double y, double z, float yaw, float partialTickTime) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y, (float)z);
		GL11.glRotatef(yaw, 0, 1, 0);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		
		// shrink everything by a small amount
		final float Scale = 15f / 16;
		GL11.glScalef(Scale, Scale, 1);
		
		// flip the axes so the coordinates make sense
		GL11.glScalef(-1, -1, -1);
		
		// and translate to the lower-left corner of the plaque
		GL11.glTranslatef(-(float)entity.getWidthPixels() / 32, -(float)entity.getHeightPixels() / 32, 0);
		
		// set the texture
		bindTexture(entity.getType().getTexture());
		
		renderPlaque(entity);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}
	
	private void renderPlaque(EntitySupporterPlaque entity) {
		// render settings
		final double WallOffset = 1f / 16;
		
		// render variables
		int numXBlocks = entity.getWidthPixels() / 16;
		int numYBlocks = entity.getHeightPixels() / 16;
		float duBlock = 16f / entity.getWidthPixels();
		float dvBlock = 16f / entity.getHeightPixels();
		float duPixel = 1f / entity.getWidthPixels();
		float dvPixel = 1f / entity.getHeightPixels();
		
		// render the plaque
		Tessellator tessellator = Tessellator.instance;
		for (int x = 0; x < numXBlocks; x++) {
			for (int y = 0; y < numYBlocks; y++) {
				tessellator.startDrawingQuads();
				
				setColorAndLightness(entity, x, y);
				
				// NOTE: faces are in clockwise order
				// front
				tessellator.setNormal(0, 0, 1);
				tessellator.addVertexWithUV(x + 0, y + 0, WallOffset, (x + 0) * duBlock, (y + 0) * dvBlock);
				tessellator.addVertexWithUV(x + 0, y + 1, WallOffset, (x + 0) * duBlock, (y + 1) * dvBlock);
				tessellator.addVertexWithUV(x + 1, y + 1, WallOffset, (x + 1) * duBlock, (y + 1) * dvBlock);
				tessellator.addVertexWithUV(x + 1, y + 0, WallOffset, (x + 1) * duBlock, (y + 0) * dvBlock);
				
				// back
				tessellator.setNormal(0, 0, -1);
				tessellator.addVertexWithUV(x + 0, y + 0, 0, 0 * duPixel, 0 * dvPixel);
				tessellator.addVertexWithUV(x + 1, y + 0, 0, 1 * duPixel, 0 * dvPixel);
				tessellator.addVertexWithUV(x + 1, y + 1, 0, 1 * duPixel, 1 * dvPixel);
				tessellator.addVertexWithUV(x + 0, y + 1, 0, 0 * duPixel, 1 * dvPixel);
				
				if (x == 0) {
					// left
					tessellator.setNormal(-1, 0, 0);
					tessellator.addVertexWithUV(0, y + 0, 0 * WallOffset, 0 * duPixel, (y + 0) * dvBlock);
					tessellator.addVertexWithUV(0, y + 1, 0 * WallOffset, 0 * duPixel, (y + 1) * dvBlock);
					tessellator.addVertexWithUV(0, y + 1, 1 * WallOffset, 1 * duPixel, (y + 1) * dvBlock);
					tessellator.addVertexWithUV(0, y + 0, 1 * WallOffset, 1 * duPixel, (y + 0) * dvBlock);
				}
				if (x == numXBlocks - 1) {
					// right
					tessellator.setNormal(1, 0, 0);
					tessellator.addVertexWithUV(numXBlocks, y + 0, 1 * WallOffset, 1 - 1 * duPixel, (y + 0) * dvBlock);
					tessellator.addVertexWithUV(numXBlocks, y + 1, 1 * WallOffset, 1 - 1 * duPixel, (y + 1) * dvBlock);
					tessellator.addVertexWithUV(numXBlocks, y + 1, 0 * WallOffset, 1 - 0 * duPixel, (y + 1) * dvBlock);
					tessellator.addVertexWithUV(numXBlocks, y + 0, 0 * WallOffset, 1 - 0 * duPixel, (y + 0) * dvBlock);
				}
				if (y == 0) {
					// bottom
					tessellator.setNormal(0, -1, 0);
					tessellator.addVertexWithUV(x + 0, 0, 0 * WallOffset, (x + 0) * duBlock, 0 * dvPixel);
					tessellator.addVertexWithUV(x + 0, 0, 1 * WallOffset, (x + 0) * duBlock, 1 * dvPixel);
					tessellator.addVertexWithUV(x + 1, 0, 1 * WallOffset, (x + 1) * duBlock, 1 * dvPixel);
					tessellator.addVertexWithUV(x + 1, 0, 0 * WallOffset, (x + 1) * duBlock, 0 * dvPixel);
				}
				if (y == numYBlocks - 1) {
					// top
					tessellator.setNormal(0, 1, 0);
					tessellator.addVertexWithUV(x + 0, numYBlocks, 1 * WallOffset, (x + 0) * duBlock, 1 - 1 * dvPixel);
					tessellator.addVertexWithUV(x + 0, numYBlocks, 0 * WallOffset, (x + 0) * duBlock, 1 - 0 * dvPixel);
					tessellator.addVertexWithUV(x + 1, numYBlocks, 0 * WallOffset, (x + 1) * duBlock, 1 - 0 * dvPixel);
					tessellator.addVertexWithUV(x + 1, numYBlocks, 1 * WallOffset, (x + 1) * duBlock, 1 - 1 * dvPixel);
				}
				
				tessellator.draw();
			}
		}
		
		// render the text
		GL11.glPushMatrix();
		// for some reason, I can't turn off depth buffering
		// so just draw the text a little farther off the wall than the plaque
		GL11.glTranslatef(0f, 0f, (float)WallOffset + 0.001f);
		GL11.glNormal3f(0f, 0f, 1f);
		
		switch (entity.getType()) {
			case Small: {
				float smallHeight = 1f / 16;
				float mediumHeight = 1.5f / 16;
				float bigHeight = 2f / 16;
				
				drawCenteredLine(Supporters.getName(entity.getSupporterId()), 7f / 16, numXBlocks, bigHeight, ColorUtils.getGrey(0));
				drawCenteredLine("is a supporter of", 10f / 16, numXBlocks, smallHeight, ColorUtils.getGrey(64));
				drawCenteredLine("Cuchaz Interactive", 12f / 16, numXBlocks, mediumHeight, ColorUtils.getGrey(0));
			}
			break;
			
			case Large: {
				float smallHeight = 2f / 16;
				float mediumHeight = 3f / 16;
				float bigHeight = 4f / 16;
				
				drawCenteredLine(Supporters.getName(entity.getSupporterId()), 16f / 16, numXBlocks, bigHeight, ColorUtils.getGrey(0));
				drawCenteredLine("is a supporter of", 23f / 16, numXBlocks, smallHeight, ColorUtils.getGrey(64));
				drawCenteredLine("Cuchaz Interactive", 26f / 16, numXBlocks, mediumHeight, ColorUtils.getGrey(0));
			}
			break;
		}
		
		GL11.glPopMatrix();
	}
	
	private void drawCenteredLine(String text, float yPos, float plaqueSize, float height, int color) {
		// NOTE: all inputs are in world coords
		
		FontRenderer fontRenderer = getFontRendererFromRenderManager();
		
		GL11.glPushMatrix();
		
		// compute the scale so that the rendered font height matches the input height in block space
		int lineHeight = fontRenderer.FONT_HEIGHT + 1;
		float scaleFontToWorld = height / lineHeight;
		GL11.glScalef(scaleFontToWorld, scaleFontToWorld, 1f);
		
		// apply the scale to the arguments
		yPos /= scaleFontToWorld;
		plaqueSize /= scaleFontToWorld;
		
		int textWidth = fontRenderer.getStringWidth(text);
		int x = (int) (plaqueSize - textWidth) / 2;
		int y = (int)yPos;
		fontRenderer.drawString(text, x, y, color);
		
		GL11.glPopMatrix();
	}
	
	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		// not needed
		return null;
	}
	
	private void setColorAndLightness(EntityHanging entity, int x, int y) {
		int i = MathHelper.floor_double(entity.posX);
		int j = MathHelper.floor_double(entity.posY + x);
		int k = MathHelper.floor_double(entity.posZ);
		
		if (entity.hangingDirection == 2) {
			i = MathHelper.floor_double(entity.posX + x);
		}
		if (entity.hangingDirection == 1) {
			k = MathHelper.floor_double(entity.posZ - x);
		}
		if (entity.hangingDirection == 0) {
			i = MathHelper.floor_double(entity.posX - x);
		}
		if (entity.hangingDirection == 3) {
			k = MathHelper.floor_double(entity.posZ + x);
		}
		
		int l = renderManager.worldObj.getLightBrightnessForSkyBlocks(i, j, k, 0);
		int i1 = l % 65536;
		int j1 = l / 65536;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)i1, (float)j1);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
	}
}
