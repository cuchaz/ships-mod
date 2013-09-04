
package cuchaz.ships.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public class ModelHelm extends ModelBase
{
	private ModelRenderer Spoke1;
	private ModelRenderer Spoke2;
	private ModelRenderer Spoke3;
	private ModelRenderer Core;
	private ModelRenderer Wheel1;
	private ModelRenderer Wheel2;
	private ModelRenderer Wheel3;
	private ModelRenderer Wheel4;
	private ModelRenderer Wheel5;
	private ModelRenderer Wheel6;
	private ModelRenderer Shaft;
	private ModelRenderer Base_Top;
	private ModelRenderer Base_Bottom;
	private ModelRenderer Base_Trim;
	
	public ModelHelm( )
	{
		textureWidth = 64;
		textureHeight = 32;
		
		Spoke1 = new ModelRenderer( this, 0, 0 );
		Spoke1.addBox( -0.5F, -7F, -0.5F, 1, 14, 1 );
		Spoke1.setRotationPoint( 0F, 15F, -6F );
		Spoke1.setTextureSize( 64, 32 );
		setRotation( Spoke1, 0F, 0F, 0F );
		Spoke2 = new ModelRenderer( this, 0, 0 );
		Spoke2.addBox( -0.5F, -7F, -0.5F, 1, 14, 1 );
		Spoke2.setRotationPoint( 0F, 15F, -6F );
		Spoke2.setTextureSize( 64, 32 );
		setRotation( Spoke2, 0F, 0F, 2.094395F );
		Spoke3 = new ModelRenderer( this, 0, 0 );
		Spoke3.addBox( -0.5F, -7F, -0.5F, 1, 14, 1 );
		Spoke3.setRotationPoint( 0F, 15F, -6F );
		Spoke3.setTextureSize( 64, 32 );
		setRotation( Spoke3, 0F, 0F, -2.094395F );
		Core = new ModelRenderer( this, 0, 16 );
		Core.addBox( -1F, -1F, -1F, 2, 2, 2 );
		Core.setRotationPoint( 0F, 15F, -6F );
		Core.setTextureSize( 64, 32 );
		setRotation( Core, 0F, 0F, 0F );
		Wheel1 = new ModelRenderer( this, 5, 0 );
		Wheel1.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel1.setRotationPoint( -0.5F, 9.8F, -7F );
		Wheel1.setTextureSize( 64, 32 );
		setRotation( Wheel1, 0F, 0F, -1.047198F );
		Wheel2 = new ModelRenderer( this, 5, 0 );
		Wheel2.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel2.setRotationPoint( 4.19F, 12F, -7F );
		Wheel2.setTextureSize( 64, 32 );
		setRotation( Wheel2, 0F, 0F, 0F );
		Wheel3 = new ModelRenderer( this, 5, 0 );
		Wheel3.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel3.setRotationPoint( 4.7F, 17.1F, -7F );
		Wheel3.setTextureSize( 64, 32 );
		setRotation( Wheel3, 0F, 0F, 1.047198F );
		Wheel4 = new ModelRenderer( this, 5, 0 );
		Wheel4.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel4.setRotationPoint( -5.2F, 17.96F, -7F );
		Wheel4.setTextureSize( 64, 32 );
		setRotation( Wheel4, 0F, 0F, -1.047198F );
		Wheel5 = new ModelRenderer( this, 5, 0 );
		Wheel5.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel5.setRotationPoint( -5.18F, 12F, -7F );
		Wheel5.setTextureSize( 64, 32 );
		setRotation( Wheel5, 0F, 0F, 0F );
		Wheel6 = new ModelRenderer( this, 5, 0 );
		Wheel6.addBox( 0F, 0F, 0F, 1, 6, 2 );
		Wheel6.setRotationPoint( 0F, 8.95F, -7F );
		Wheel6.setTextureSize( 64, 32 );
		setRotation( Wheel6, 0F, 0F, 1.047198F );
		Shaft = new ModelRenderer( this, 11, 0 );
		Shaft.addBox( -0.5F, -0.5F, 0F, 1, 1, 10 );
		Shaft.setRotationPoint( 0F, 15F, -8F );
		Shaft.setTextureSize( 64, 32 );
		setRotation( Shaft, 0F, 0F, 0F );
		Base_Top = new ModelRenderer( this, 42, 21 );
		Base_Top.addBox( 0F, 0F, 0F, 6, 6, 5 );
		Base_Top.setRotationPoint( -3F, 12F, -4F );
		Base_Top.setTextureSize( 64, 32 );
		setRotation( Base_Top, 0F, 0F, 0F );
		Base_Bottom = new ModelRenderer( this, 34, 0 );
		Base_Bottom.addBox( 0F, 0F, 0F, 8, 5, 7 );
		Base_Bottom.setRotationPoint( -4F, 18F, -4F );
		Base_Bottom.setTextureSize( 64, 32 );
		setRotation( Base_Bottom, 0F, 0F, 0F );
		Base_Trim = new ModelRenderer( this, 0, 22 );
		Base_Trim.addBox( 0F, 0F, 0F, 10, 1, 9 );
		Base_Trim.setRotationPoint( -5F, 23F, -5F );
		Base_Trim.setTextureSize( 64, 32 );
		setRotation( Base_Trim, 0F, 0F, 0F );
	}
	
	public void renderAll( )
	{
		// scale from model space to world space
		float scaleFactor = 1/16.0f;
		
		Spoke1.render( scaleFactor );
		Spoke2.render( scaleFactor );
		Spoke3.render( scaleFactor );
		Core.render( scaleFactor );
		Wheel1.render( scaleFactor );
		Wheel2.render( scaleFactor );
		Wheel3.render( scaleFactor );
		Wheel4.render( scaleFactor );
		Wheel5.render( scaleFactor );
		Wheel6.render( scaleFactor );
		Shaft.render( scaleFactor );
		Base_Top.render( scaleFactor );
		Base_Bottom.render( scaleFactor );
		Base_Trim.render( scaleFactor );
	}
	
	private void setRotation( ModelRenderer model, float x, float y, float z )
	{
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
