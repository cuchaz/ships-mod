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

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntitySupporterPlaque;

public class RenderSupporterPlaque extends Render
{
	@Override
	public void doRender( Entity entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		doRender( (EntitySupporterPlaque)entity, x, y, z, yaw, partialTickTime );
	}
	
	public void doRender( EntitySupporterPlaque entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		// TEMP
		GL11.glPushMatrix();
		GL11.glTranslatef( (float)x, (float)y, (float)z );
		GL11.glTranslated( -entity.posX, -entity.posY, -entity.posZ );
		System.out.println( "Rendering, bee-yotch!" );
		RenderUtils.renderHitbox( entity.boundingBox, ColorUtils.getColor( 0, 255, 0 ) );
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef( (float)x, (float)y, (float)z );
		GL11.glRotatef( yaw, 0, 1, 0 );
		GL11.glEnable( GL12.GL_RESCALE_NORMAL );
		bindEntityTexture( entity );
		float scale = 1f / 16f;
		GL11.glScalef( scale, scale, scale );
		renderPlaque( entity );
		GL11.glDisable( GL12.GL_RESCALE_NORMAL );
		GL11.glPopMatrix();
	}
	
	private void renderPlaque( EntitySupporterPlaque entity )
	{
		/*
		int numXBlocks = entity.getWidthPixels()/16;
		int numYBlocks = entity.getHeightPixels()/16;
		for( int x=0; x<numXBlocks; x++ )
		{
			for( int y=0; y<numYBlocks; y++ )
			{
				setColorAndLightness( entity, x, y );
				
				Tessellator tessellator = Tessellator.instance;
				tessellator.startDrawingQuads();
				
				float minU = x*16;
				float maxU = ( x + 1 )*16;
				float minV = y*16;
				float maxV = ( y + 1 )*16;
				
				// draw a face
				tessellator.setNormal( 0, 0, 1 );
				tessellator.addVertexWithUV( 0, 0, 0, minU, minV );
				tessellator.addVertexWithUV( 1, 0, 0, maxU, minV );
				tessellator.addVertexWithUV( 1, 1, 0, maxU, maxV );
				tessellator.addVertexWithUV( 0, 1, 0, minU, maxV );
				
				tessellator.draw();
			}
		}
		*/
	}
	
	@Override
	protected ResourceLocation getEntityTexture( Entity entity )
	{
		return new ResourceLocation( "ships", "textures/blocks/supporterPlaque.png" ); 
	}
	
	private void setColorAndLightness( EntityHanging entity, int x, int y )
	{
		int i = MathHelper.floor_double( entity.posX );
		int j = MathHelper.floor_double( entity.posY + x );
		int k = MathHelper.floor_double( entity.posZ );
		
		if( entity.hangingDirection == 2 )
		{
			i = MathHelper.floor_double( entity.posX + x );
		}
		if( entity.hangingDirection == 1 )
		{
			k = MathHelper.floor_double( entity.posZ - x );
		}
		if( entity.hangingDirection == 0 )
		{
			i = MathHelper.floor_double( entity.posX - x );
		}
		if( entity.hangingDirection == 3 )
		{
			k = MathHelper.floor_double( entity.posZ + x );
		}
		
		int l = renderManager.worldObj.getLightBrightnessForSkyBlocks( i, j, k, 0 );
		int i1 = l % 65536;
		int j1 = l / 65536;
		OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, (float)i1, (float)j1 );
		GL11.glColor3f( 1.0F, 1.0F, 1.0F );
	}
}
