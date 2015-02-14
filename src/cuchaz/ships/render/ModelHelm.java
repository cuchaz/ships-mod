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

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelHelm extends ModelBase {
	
	private ModelRenderer Shaft;
	private ModelRenderer Core;
	private ModelRenderer Spoke1;
	private ModelRenderer Spoke2;
	private ModelRenderer Spoke3;
	private ModelRenderer Strut1;
	private ModelRenderer Strut2;
	private ModelRenderer Strut3;
	private ModelRenderer Strut4;
	private ModelRenderer Strut5;
	private ModelRenderer Strut6;
	private ModelRenderer StandFront1;
	private ModelRenderer StandFront2;
	private ModelRenderer StandFrontCore;
	private ModelRenderer StandBase1;
	private ModelRenderer StandBase2;
	private ModelRenderer StandBack2;
	private ModelRenderer StandBack1;
	private ModelRenderer StandBackCore;
	private ModelRenderer Box;
	
	private float m_wheelAngleRadians;
	
	public ModelHelm() {
		textureWidth = 64;
		textureHeight = 32;
		
		m_wheelAngleRadians = 0.0f;
		
		Shaft = new ModelRenderer(this, 13, 0);
		Shaft.addBox(-0.5F, -0.5F, 1F, 1, 1, 6);
		Shaft.setRotationPoint(0F, 13F, -8F);
		Shaft.setTextureSize(64, 32);
		Shaft.mirror = true;
		setRotation(Shaft, 0F, 0F, 0F);
		Core = new ModelRenderer(this, 11, 24);
		Core.addBox(-1F, -1F, -1F, 2, 2, 2);
		Core.setRotationPoint(0F, 13F, -4F);
		Core.setTextureSize(64, 32);
		Core.mirror = true;
		Spoke1 = new ModelRenderer(this, 0, 0);
		Spoke1.addBox(-0.5F, -8F, -0.5F, 1, 16, 1);
		Spoke1.setRotationPoint(0F, 13F, -4F);
		Spoke1.setTextureSize(64, 32);
		Spoke1.mirror = true;
		Spoke2 = new ModelRenderer(this, 0, 0);
		Spoke2.addBox(-0.5F, -8F, -0.5F, 1, 16, 1);
		Spoke2.setRotationPoint(0F, 13F, -4F);
		Spoke2.setTextureSize(64, 32);
		Spoke2.mirror = true;
		Spoke3 = new ModelRenderer(this, 0, 0);
		Spoke3.addBox(-0.5F, -8F, -0.5F, 1, 16, 1);
		Spoke3.setRotationPoint(0F, 13F, -4F);
		Spoke3.setTextureSize(64, 32);
		Spoke3.mirror = true;
		Strut1 = new ModelRenderer(this, 5, 0);
		Strut1.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut1.setRotationPoint(0F, 13F, -4F);
		Strut1.setTextureSize(64, 32);
		Strut1.mirror = true;
		Strut2 = new ModelRenderer(this, 5, 0);
		Strut2.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut2.setRotationPoint(0F, 13F, -4F);
		Strut2.setTextureSize(64, 32);
		Strut2.mirror = true;
		Strut3 = new ModelRenderer(this, 5, 0);
		Strut3.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut3.setRotationPoint(0F, 13F, -4F);
		Strut3.setTextureSize(64, 32);
		Strut3.mirror = true;
		Strut4 = new ModelRenderer(this, 5, 0);
		Strut4.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut4.setRotationPoint(0F, 13F, -4F);
		Strut4.setTextureSize(64, 32);
		Strut4.mirror = true;
		Strut5 = new ModelRenderer(this, 5, 0);
		Strut5.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut5.setRotationPoint(0F, 13F, -4F);
		Strut5.setTextureSize(64, 32);
		Strut5.mirror = true;
		Strut6 = new ModelRenderer(this, 5, 0);
		Strut6.addBox(5F, -3.5F, -1F, 1, 7, 2);
		Strut6.setRotationPoint(0F, 13F, -4F);
		Strut6.setTextureSize(64, 32);
		Strut6.mirror = true;
		StandFront1 = new ModelRenderer(this, 0, 29);
		StandFront1.addBox(-1F, -1F, -0.5F, 12, 2, 1);
		StandFront1.setRotationPoint(0F, 13F, -1F);
		StandFront1.setTextureSize(64, 32);
		StandFront1.mirror = true;
		setRotation(StandFront1, 0F, 0F, 2.094395F);
		StandFront2 = new ModelRenderer(this, 0, 29);
		StandFront2.addBox(-1F, -1F, -0.5F, 12, 2, 1);
		StandFront2.setRotationPoint(0F, 13F, -1F);
		StandFront2.setTextureSize(64, 32);
		StandFront2.mirror = true;
		setRotation(StandFront2, 0F, 0F, 1.047198F);
		StandFrontCore = new ModelRenderer(this, 0, 23);
		StandFrontCore.addBox(-1.5F, -1.5F, -1F, 3, 3, 2);
		StandFrontCore.setRotationPoint(0F, 13F, -7F);
		StandFrontCore.setTextureSize(64, 32);
		StandFrontCore.mirror = true;
		setRotation(StandFrontCore, 0F, 0F, 0F);
		StandBase1 = new ModelRenderer(this, 28, 0);
		StandBase1.addBox(0F, 0F, 0F, 3, 2, 15);
		StandBase1.setRotationPoint(3.7F, 22F, -8F);
		StandBase1.setTextureSize(64, 32);
		StandBase1.mirror = true;
		setRotation(StandBase1, 0F, 0F, 0F);
		StandBase2 = new ModelRenderer(this, 28, 0);
		StandBase2.addBox(-3F, 0F, 0F, 3, 2, 15);
		StandBase2.setRotationPoint(-3.6F, 22F, -8F);
		StandBase2.setTextureSize(64, 32);
		StandBase2.mirror = true;
		setRotation(StandBase2, 0F, 0F, 0F);
		StandBack2 = new ModelRenderer(this, 0, 29);
		StandBack2.addBox(-1F, -1F, -0.5F, 12, 2, 1);
		StandBack2.setRotationPoint(0F, 13F, -7F);
		StandBack2.setTextureSize(64, 32);
		StandBack2.mirror = true;
		setRotation(StandBack2, 0F, 0F, 1.047198F);
		StandBack1 = new ModelRenderer(this, 0, 29);
		StandBack1.addBox(-1F, -1F, -0.5F, 12, 2, 1);
		StandBack1.setRotationPoint(0F, 13F, -7F);
		StandBack1.setTextureSize(64, 32);
		StandBack1.mirror = true;
		setRotation(StandBack1, 0F, 0F, 2.094395F);
		StandBackCore = new ModelRenderer(this, 0, 23);
		StandBackCore.addBox(-1.5F, -1.5F, -1F, 3, 3, 2);
		StandBackCore.setRotationPoint(0F, 13F, -1F);
		StandBackCore.setTextureSize(64, 32);
		StandBackCore.mirror = true;
		setRotation(StandBackCore, 0F, 0F, 0F);
		Box = new ModelRenderer(this, 26, 21);
		Box.addBox(0F, 0F, 0F, 12, 4, 7);
		Box.setRotationPoint(-6F, 20F, 0F);
		Box.setTextureSize(64, 32);
		Box.mirror = true;
		setRotation(Box, 0F, 0F, 0F);
	}
	
	public void renderAll() {
		// update the rotations
		setRotation(Core, 0F, 0F, m_wheelAngleRadians + 0F);
		setRotation(Spoke1, 0F, 0F, m_wheelAngleRadians + 0F);
		setRotation(Spoke2, 0F, 0F, m_wheelAngleRadians + 2.094395F);
		setRotation(Spoke3, 0F, 0F, m_wheelAngleRadians + -2.094395F);
		setRotation(Strut1, 0F, 0F, m_wheelAngleRadians + 0F);
		setRotation(Strut2, 0F, 0F, m_wheelAngleRadians + 1.047198F);
		setRotation(Strut3, 0F, 0F, m_wheelAngleRadians + 2.094395F);
		setRotation(Strut4, 0F, 0F, m_wheelAngleRadians + 3.141593F);
		setRotation(Strut5, 0F, 0F, m_wheelAngleRadians + -2.094395F);
		setRotation(Strut6, 0F, 0F, m_wheelAngleRadians + -1.047198F);
		
		// scale from model space to world space
		float scaleFactor = 1 / 16.0f;
		
		Shaft.render(scaleFactor);
		Core.render(scaleFactor);
		Spoke1.render(scaleFactor);
		Spoke2.render(scaleFactor);
		Spoke3.render(scaleFactor);
		Strut1.render(scaleFactor);
		Strut2.render(scaleFactor);
		Strut3.render(scaleFactor);
		Strut4.render(scaleFactor);
		Strut5.render(scaleFactor);
		Strut6.render(scaleFactor);
		StandFront1.render(scaleFactor);
		StandFront2.render(scaleFactor);
		StandFrontCore.render(scaleFactor);
		StandBase1.render(scaleFactor);
		StandBase2.render(scaleFactor);
		StandBack2.render(scaleFactor);
		StandBack1.render(scaleFactor);
		StandBackCore.render(scaleFactor);
		Box.render(scaleFactor);
	}
	
	private void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
	
	public void setWheelAngle(float angleDegrees) {
		// convert to radians
		// why some of minecraft uses degress and some of minecraft uses radians, I will never know...
		m_wheelAngleRadians = (float)Math.toRadians(angleDegrees);
	}
}
