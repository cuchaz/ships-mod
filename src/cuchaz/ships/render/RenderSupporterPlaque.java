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
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

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
		GL11.glPushMatrix();
		GL11.glTranslatef( (float)x, (float)y, (float)z );
		GL11.glRotatef( yaw, 0, 1, 0 );
		GL11.glEnable( GL12.GL_RESCALE_NORMAL );
		bindEntityTexture( entity );
		float scale = 1f/16f;
		GL11.glScalef( scale, scale, scale );
		
		renderPlaque( entity, entity.getWidthPixels(), entity.getHeightPixels(), 0, 0 );
		
		GL11.glDisable( GL12.GL_RESCALE_NORMAL );
		GL11.glPopMatrix();
	}
	
	private void renderPlaque( EntitySupporterPlaque entity, int widthPixels, int heightPixels, int xOffsetPixels, int yOffsetPixels )
	{
		// UNDONE: render this BEE-YOTCH!!
		
		float f = (float)(-widthPixels) / 2.0F;
        float f1 = (float)(-heightPixels) / 2.0F;
        float f2 = 0.5F;
        float f3 = 0.75F;
        float f4 = 0.8125F;
        float f5 = 0.0F;
        float f6 = 0.0625F;
        float f7 = 0.75F;
        float f8 = 0.8125F;
        float f9 = 0.001953125F;
        float f10 = 0.001953125F;
        float f11 = 0.7519531F;
        float f12 = 0.7519531F;
        float f13 = 0.0F;
        float f14 = 0.0625F;

        for (int i1 = 0; i1 < widthPixels / 16; ++i1)
        {
            for (int j1 = 0; j1 < heightPixels / 16; ++j1)
            {
                float f15 = f + (float)((i1 + 1) * 16);
                float f16 = f + (float)(i1 * 16);
                float f17 = f1 + (float)((j1 + 1) * 16);
                float f18 = f1 + (float)(j1 * 16);
                this.func_77008_a(entity, (f15 + f16) / 2.0F, (f17 + f18) / 2.0F);
                float f19 = (float)(xOffsetPixels + widthPixels - i1 * 16) / 256.0F;
                float f20 = (float)(xOffsetPixels + widthPixels - (i1 + 1) * 16) / 256.0F;
                float f21 = (float)(yOffsetPixels + heightPixels - j1 * 16) / 256.0F;
                float f22 = (float)(yOffsetPixels + heightPixels - (j1 + 1) * 16) / 256.0F;
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)(-f2), (double)f20, (double)f21);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)(-f2), (double)f19, (double)f21);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)(-f2), (double)f19, (double)f22);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)(-f2), (double)f20, (double)f22);
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)f2, (double)f3, (double)f5);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)f2, (double)f4, (double)f5);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)f2, (double)f4, (double)f6);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)f2, (double)f3, (double)f6);
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)(-f2), (double)f7, (double)f9);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)(-f2), (double)f8, (double)f9);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)f2, (double)f8, (double)f10);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)f2, (double)f7, (double)f10);
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)f2, (double)f7, (double)f9);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)f2, (double)f8, (double)f9);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)(-f2), (double)f8, (double)f10);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)(-f2), (double)f7, (double)f10);
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)f2, (double)f12, (double)f13);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)f2, (double)f12, (double)f14);
                tessellator.addVertexWithUV((double)f15, (double)f18, (double)(-f2), (double)f11, (double)f14);
                tessellator.addVertexWithUV((double)f15, (double)f17, (double)(-f2), (double)f11, (double)f13);
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)(-f2), (double)f12, (double)f13);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)(-f2), (double)f12, (double)f14);
                tessellator.addVertexWithUV((double)f16, (double)f18, (double)f2, (double)f11, (double)f14);
                tessellator.addVertexWithUV((double)f16, (double)f17, (double)f2, (double)f11, (double)f13);
                tessellator.draw();
            }
        }
	}

	@Override
	protected ResourceLocation getEntityTexture( Entity entity )
	{
		// this isn't used, but it's required by subclasses of Render
		return null;
	}
	
    private void func_77008_a(EntityPainting par1EntityPainting, float par2, float par3)
    {
        int i = MathHelper.floor_double(par1EntityPainting.posX);
        int j = MathHelper.floor_double(par1EntityPainting.posY + (double)(par3 / 16.0F));
        int k = MathHelper.floor_double(par1EntityPainting.posZ);

        if (par1EntityPainting.hangingDirection == 2)
        {
            i = MathHelper.floor_double(par1EntityPainting.posX + (double)(par2 / 16.0F));
        }

        if (par1EntityPainting.hangingDirection == 1)
        {
            k = MathHelper.floor_double(par1EntityPainting.posZ - (double)(par2 / 16.0F));
        }

        if (par1EntityPainting.hangingDirection == 0)
        {
            i = MathHelper.floor_double(par1EntityPainting.posX - (double)(par2 / 16.0F));
        }

        if (par1EntityPainting.hangingDirection == 3)
        {
            k = MathHelper.floor_double(par1EntityPainting.posZ + (double)(par2 / 16.0F));
        }

        int l = this.renderManager.worldObj.getLightBrightnessForSkyBlocks(i, j, k, 0);
        int i1 = l % 65536;
        int j1 = l / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)i1, (float)j1);
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
    }

}
