/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.render;

import java.util.Arrays;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.math.BoxCorner;
import cuchaz.modsShared.math.CompareReal;
import cuchaz.modsShared.math.Matrix3;
import cuchaz.modsShared.math.RotatedBB;
import cuchaz.modsShared.math.Vector3;

public class RenderUtils {
	
	public static void renderPosition(Entity entity) {
		renderPoint(entity.posX, entity.posY, entity.posZ, ColorUtils.getColor(0, 255, 0));
	}
	
	public static void renderPoint(double x, double y, double z, int color) {
		final double Halfwidth = 0.05;
		
		renderBox(x - Halfwidth, x + Halfwidth, y - Halfwidth, y + Halfwidth, z - Halfwidth, z + Halfwidth, color);
	}
	
	public static void renderAxis(Entity entity) {
		final double Halfwidth = 0.05;
		final double HalfHeight = 2.0;
		
		renderBox(entity.posX - Halfwidth, entity.posX + Halfwidth, entity.posY - HalfHeight, entity.posY + HalfHeight, entity.posZ - Halfwidth, entity.posZ + Halfwidth, ColorUtils.getGrey(180));
	}
	
	public static void renderHitbox(RotatedBB box, int color) {
		// pad the hitbox by a small delta to avoid rendering glitches
		final double delta = 0.01;
		box.getAABox().minX -= delta;
		box.getAABox().minY -= delta;
		box.getAABox().minZ -= delta;
		box.getAABox().maxX += delta;
		box.getAABox().maxY += delta;
		box.getAABox().maxZ += delta;
		
		renderRotatedBox(box, color);
		
		// undo the pad
		box.getAABox().minX += delta;
		box.getAABox().minY += delta;
		box.getAABox().minZ += delta;
		box.getAABox().maxX -= delta;
		box.getAABox().maxY -= delta;
		box.getAABox().maxZ -= delta;
	}
	
	public static void renderHitbox(AxisAlignedBB box, int color) {
		// pad the hitbox by a small delta to avoid rendering glitches
		final double delta = 0.01;
		renderBox(box.minX - delta, box.maxX + delta, box.minY - delta, box.maxY + delta, box.minZ - delta, box.maxZ + delta, color);
	}
	
	public static void renderBox(AxisAlignedBB box, int color, double delta) {
		renderBox(box.minX - delta, box.maxX + delta, box.minY - delta, box.maxY + delta, box.minZ - delta, box.maxZ + delta, color);
	}
	
	public static void renderBox(double xm, double xp, double ym, double yp, double zm, double zp, int color) {
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_I(color, 128);
		
		// south
		tessellator.addVertex(xm, yp, zm);
		tessellator.addVertex(xm, ym, zm);
		tessellator.addVertex(xp, ym, zm);
		tessellator.addVertex(xp, yp, zm);
		
		// north
		tessellator.addVertex(xp, yp, zp);
		tessellator.addVertex(xp, ym, zp);
		tessellator.addVertex(xm, ym, zp);
		tessellator.addVertex(xm, yp, zp);
		
		// east
		tessellator.addVertex(xp, yp, zm);
		tessellator.addVertex(xp, ym, zm);
		tessellator.addVertex(xp, ym, zp);
		tessellator.addVertex(xp, yp, zp);
		
		// west
		tessellator.addVertex(xm, yp, zp);
		tessellator.addVertex(xm, ym, zp);
		tessellator.addVertex(xm, ym, zm);
		tessellator.addVertex(xm, yp, zm);
		
		// top
		tessellator.addVertex(xm, yp, zm);
		tessellator.addVertex(xp, yp, zm);
		tessellator.addVertex(xp, yp, zp);
		tessellator.addVertex(xm, yp, zp);
		
		// bottom
		tessellator.addVertex(xm, ym, zm);
		tessellator.addVertex(xp, ym, zm);
		tessellator.addVertex(xp, ym, zp);
		tessellator.addVertex(xm, ym, zp);
		
		tessellator.draw();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
	}
	
	public static void renderRotatedBox(RotatedBB box, int color) {
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_I(color, 128);
		
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		for (BlockSide side : Arrays.asList(BlockSide.North, BlockSide.South, BlockSide.East, BlockSide.West)) {
			for (BoxCorner corner : side.getCorners()) {
				box.getCorner(p, corner);
				tessellator.addVertex(p.xCoord, p.yCoord, p.zCoord);
			}
		}
		
		tessellator.draw();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
	}
	
	public static void renderVector(double x, double y, double z, double dx, double dy, double dz, int color) {
		// get the vector in world-space
		Vector3 v = new Vector3(dx, dy, dz);
		
		// does this vector even have length?
		if (CompareReal.eq(v.getSquaredLength(), 0)) {
			return;
		}
		
		// build the vector geometry in an arbitrary space
		double halfWidth = 0.2;
		Vector3[] vertices = { new Vector3(halfWidth, halfWidth, 0), new Vector3(-halfWidth, halfWidth, 0), new Vector3(-halfWidth, -halfWidth, 0), new Vector3(halfWidth, -halfWidth, 0), new Vector3(0, 0, v.getLength()) };
		
		// compute a basis so we can transform from parameter space to world space
		v.normalize();
		Matrix3 basis = new Matrix3();
		Matrix3.getArbitraryBasisFromZ(basis, v);
		
		// transform the vertices
		for (Vector3 p : vertices) {
			// rotate
			basis.multiply(p);
			
			// translate
			p.x += x;
			p.y += y;
			p.z += z;
		}
		
		// render the geometry
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawing(6); // triangle fan
		tessellator.setColorRGBA_I(color, 128);
		
		tessellator.addVertex(vertices[4].x, vertices[4].y, vertices[4].z);
		tessellator.addVertex(vertices[0].x, vertices[0].y, vertices[0].z);
		tessellator.addVertex(vertices[1].x, vertices[1].y, vertices[1].z);
		tessellator.addVertex(vertices[2].x, vertices[2].y, vertices[2].z);
		tessellator.addVertex(vertices[3].x, vertices[3].y, vertices[3].z);
		
		tessellator.draw();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
	}
}
