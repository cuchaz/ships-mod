
package cuchaz.ships.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public class ModelHelm extends ModelBase
{
	ModelRenderer Shaft;
	ModelRenderer Core;
	ModelRenderer Spoke1;
	ModelRenderer Spoke2;
	ModelRenderer Spoke3;
	ModelRenderer Strut1;
	ModelRenderer Strut2;
	ModelRenderer Strut3;
	ModelRenderer Strut4;
	ModelRenderer Strut5;
	ModelRenderer Strut6;
	ModelRenderer StandFront1;
	ModelRenderer StandFront2;
	ModelRenderer StandFrontCore;
	ModelRenderer StandBase1;
	ModelRenderer StandBase2;
	ModelRenderer StandBack2;
	ModelRenderer StandBack1;
	ModelRenderer StandBackCore;
	ModelRenderer Box;
	
	public ModelHelm( )
	{
		textureWidth = 64;
		textureHeight = 32;
		
		Shaft = new ModelRenderer( this, 13, 0 );
		Shaft.addBox( -0.5F, -0.5F, 1F, 1, 1, 6 );
		Shaft.setRotationPoint( 0F, 13F, -8F );
		Shaft.setTextureSize( 64, 32 );
		Shaft.mirror = true;
		setRotation( Shaft, 0F, 0F, 0F );
		Core = new ModelRenderer( this, 11, 24 );
		Core.addBox( -1F, -1F, -1F, 2, 2, 2 );
		Core.setRotationPoint( 0F, 13F, -4F );
		Core.setTextureSize( 64, 32 );
		Core.mirror = true;
		setRotation( Core, 0F, 0F, 0F );
		Spoke1 = new ModelRenderer( this, 0, 0 );
		Spoke1.addBox( -0.5F, -8F, -0.5F, 1, 16, 1 );
		Spoke1.setRotationPoint( 0F, 13F, -4F );
		Spoke1.setTextureSize( 64, 32 );
		Spoke1.mirror = true;
		setRotation( Spoke1, 0F, 0F, 0F );
		Spoke2 = new ModelRenderer( this, 0, 0 );
		Spoke2.addBox( -0.5F, -8F, -0.5F, 1, 16, 1 );
		Spoke2.setRotationPoint( 0F, 13F, -4F );
		Spoke2.setTextureSize( 64, 32 );
		Spoke2.mirror = true;
		setRotation( Spoke2, 0F, 0F, 2.094395F );
		Spoke3 = new ModelRenderer( this, 0, 0 );
		Spoke3.addBox( -0.5F, -8F, -0.5F, 1, 16, 1 );
		Spoke3.setRotationPoint( 0F, 13F, -4F );
		Spoke3.setTextureSize( 64, 32 );
		Spoke3.mirror = true;
		setRotation( Spoke3, 0F, 0F, -2.094395F );
		Strut1 = new ModelRenderer( this, 5, 0 );
		Strut1.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut1.setRotationPoint( 0F, 13F, -4F );
		Strut1.setTextureSize( 64, 32 );
		Strut1.mirror = true;
		setRotation( Strut1, 0F, 0F, 0F );
		Strut2 = new ModelRenderer( this, 5, 0 );
		Strut2.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut2.setRotationPoint( 0F, 13F, -4F );
		Strut2.setTextureSize( 64, 32 );
		Strut2.mirror = true;
		setRotation( Strut2, 0F, 0F, 1.047198F );
		Strut3 = new ModelRenderer( this, 5, 0 );
		Strut3.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut3.setRotationPoint( 0F, 13F, -4F );
		Strut3.setTextureSize( 64, 32 );
		Strut3.mirror = true;
		setRotation( Strut3, 0F, 0F, 2.094395F );
		Strut4 = new ModelRenderer( this, 5, 0 );
		Strut4.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut4.setRotationPoint( 0F, 13F, -4F );
		Strut4.setTextureSize( 64, 32 );
		Strut4.mirror = true;
		setRotation( Strut4, 0F, 0F, 3.141593F );
		Strut5 = new ModelRenderer( this, 5, 0 );
		Strut5.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut5.setRotationPoint( 0F, 13F, -4F );
		Strut5.setTextureSize( 64, 32 );
		Strut5.mirror = true;
		setRotation( Strut5, 0F, 0F, -2.094395F );
		Strut6 = new ModelRenderer( this, 5, 0 );
		Strut6.addBox( 5F, -3.5F, -1F, 1, 7, 2 );
		Strut6.setRotationPoint( 0F, 13F, -4F );
		Strut6.setTextureSize( 64, 32 );
		Strut6.mirror = true;
		setRotation( Strut6, 0F, 0F, -1.047198F );
		StandFront1 = new ModelRenderer( this, 0, 29 );
		StandFront1.addBox( -1F, -1F, -0.5F, 12, 2, 1 );
		StandFront1.setRotationPoint( 0F, 13F, -1F );
		StandFront1.setTextureSize( 64, 32 );
		StandFront1.mirror = true;
		setRotation( StandFront1, 0F, 0F, 2.094395F );
		StandFront2 = new ModelRenderer( this, 0, 29 );
		StandFront2.addBox( -1F, -1F, -0.5F, 12, 2, 1 );
		StandFront2.setRotationPoint( 0F, 13F, -1F );
		StandFront2.setTextureSize( 64, 32 );
		StandFront2.mirror = true;
		setRotation( StandFront2, 0F, 0F, 1.047198F );
		StandFrontCore = new ModelRenderer( this, 0, 23 );
		StandFrontCore.addBox( -1.5F, -1.5F, -1F, 3, 3, 2 );
		StandFrontCore.setRotationPoint( 0F, 13F, -7F );
		StandFrontCore.setTextureSize( 64, 32 );
		StandFrontCore.mirror = true;
		setRotation( StandFrontCore, 0F, 0F, 0F );
		StandBase1 = new ModelRenderer( this, 28, 0 );
		StandBase1.addBox( 0F, 0F, 0F, 3, 2, 15 );
		StandBase1.setRotationPoint( 3.7F, 22F, -8F );
		StandBase1.setTextureSize( 64, 32 );
		StandBase1.mirror = true;
		setRotation( StandBase1, 0F, 0F, 0F );
		StandBase2 = new ModelRenderer( this, 28, 0 );
		StandBase2.addBox( -3F, 0F, 0F, 3, 2, 15 );
		StandBase2.setRotationPoint( -3.6F, 22F, -8F );
		StandBase2.setTextureSize( 64, 32 );
		StandBase2.mirror = true;
		setRotation( StandBase2, 0F, 0F, 0F );
		StandBack2 = new ModelRenderer( this, 0, 29 );
		StandBack2.addBox( -1F, -1F, -0.5F, 12, 2, 1 );
		StandBack2.setRotationPoint( 0F, 13F, -7F );
		StandBack2.setTextureSize( 64, 32 );
		StandBack2.mirror = true;
		setRotation( StandBack2, 0F, 0F, 1.047198F );
		StandBack1 = new ModelRenderer( this, 0, 29 );
		StandBack1.addBox( -1F, -1F, -0.5F, 12, 2, 1 );
		StandBack1.setRotationPoint( 0F, 13F, -7F );
		StandBack1.setTextureSize( 64, 32 );
		StandBack1.mirror = true;
		setRotation( StandBack1, 0F, 0F, 2.094395F );
		StandBackCore = new ModelRenderer( this, 0, 23 );
		StandBackCore.addBox( -1.5F, -1.5F, -1F, 3, 3, 2 );
		StandBackCore.setRotationPoint( 0F, 13F, -1F );
		StandBackCore.setTextureSize( 64, 32 );
		StandBackCore.mirror = true;
		setRotation( StandBackCore, 0F, 0F, 0F );
		Box = new ModelRenderer( this, 26, 21 );
		Box.addBox( 0F, 0F, 0F, 12, 4, 7 );
		Box.setRotationPoint( -6F, 20F, 0F );
		Box.setTextureSize( 64, 32 );
		Box.mirror = true;
		setRotation( Box, 0F, 0F, 0F );
	}
	
	public void renderAll( )
	{
		// scale from model space to world space
		float scaleFactor = 1/16.0f;
		
		Shaft.render( scaleFactor );
		Core.render( scaleFactor );
		Spoke1.render( scaleFactor );
		Spoke2.render( scaleFactor );
		Spoke3.render( scaleFactor );
		Strut1.render( scaleFactor );
		Strut2.render( scaleFactor );
		Strut3.render( scaleFactor );
		Strut4.render( scaleFactor );
		Strut5.render( scaleFactor );
		Strut6.render( scaleFactor );
		StandFront1.render( scaleFactor );
		StandFront2.render( scaleFactor );
		StandFrontCore.render( scaleFactor );
		StandBase1.render( scaleFactor );
		StandBase2.render( scaleFactor );
		StandBack2.render( scaleFactor );
		StandBack1.render( scaleFactor );
		StandBackCore.render( scaleFactor );
		Box.render( scaleFactor );
	}
	
	private void setRotation( ModelRenderer model, float x, float y, float z )
	{
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
