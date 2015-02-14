/*******************************************************************************
 * Copyright (c) 2014 jeff.
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

/**
 * projector - cuchaz Created using Tabula 4.0.2
 */
public class ModelProjector extends ModelBase {
	
	public ModelRenderer tip1;
	public ModelRenderer strut2;
	public ModelRenderer strut1;
	public ModelRenderer tip2;
	public ModelRenderer tip3;
	public ModelRenderer box;
	public ModelRenderer tip4;
	
	public ModelProjector() {
		this.textureWidth = 64;
		this.textureHeight = 32;
		this.tip1 = new ModelRenderer(this, 1, 1);
		this.tip1.setRotationPoint(-1.0F, 24.0F, -7.0F);
		this.tip1.addBox(0.0F, -2.0F, 0.0F, 2, 2, 1);
		this.tip4 = new ModelRenderer(this, 25, 1);
		this.tip4.setRotationPoint(-7.0F, 24.0F, -1.0F);
		this.tip4.addBox(0.0F, -2.0F, 0.0F, 1, 2, 2);
		this.tip3 = new ModelRenderer(this, 25, 1);
		this.tip3.setRotationPoint(6.0F, 24.0F, -1.0F);
		this.tip3.addBox(0.0F, -2.0F, 0.0F, 1, 2, 2);
		this.box = new ModelRenderer(this, 6, 24);
		this.box.setRotationPoint(-2.5F, 24.0F, -2.5F);
		this.box.addBox(0.0F, -2.0F, 0.0F, 5, 2, 5);
		this.strut1 = new ModelRenderer(this, 0, 17);
		this.strut1.setRotationPoint(-7.0F, 24.0F, -0.5F);
		this.strut1.addBox(0.0F, -1.0F, 0.0F, 14, 1, 1);
		this.tip2 = new ModelRenderer(this, 1, 1);
		this.tip2.setRotationPoint(-1.0F, 24.0F, 6.0F);
		this.tip2.addBox(0.0F, -2.0F, 0.0F, 2, 2, 1);
		this.strut2 = new ModelRenderer(this, 0, 1);
		this.strut2.setRotationPoint(-0.5F, 24.0F, -7.0F);
		this.strut2.addBox(0.0F, -1.0F, 0.0F, 1, 1, 14);
	}
	
	public void render(float f5) {
		this.tip2.render(f5);
		this.tip1.render(f5);
		this.strut1.render(f5);
		this.tip3.render(f5);
		this.strut2.render(f5);
		this.tip4.render(f5);
		this.box.render(f5);
	}
	
	/**
	 * This is a helper function from Tabula to set the rotation of model parts
	 */
	public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}
}
