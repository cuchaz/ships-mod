package cuchaz.ships.render;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly( Side.CLIENT )
public class TileEntityHelmRenderer extends TileEntitySpecialRenderer
{
	private static final ResourceLocation Texture = new ResourceLocation( "ships", "/textures/models/helm.png" );
	
	private ModelHelm m_model;
	
	public TileEntityHelmRenderer( )
	{
		m_model = new ModelHelm();
	}
	
	@Override
	public void renderTileEntityAt( TileEntity tileEntity, double x, double y, double z, float partialTickTime )
	{
		RenderManager.instance.renderEngine.func_110577_a( Texture );
		
		GL11.glPushMatrix();
        GL11.glTranslatef( (float)x + 0.5f, (float)y + 1.5f, (float)z + 0.5f );
        GL11.glScalef( 1.0f, -1.0f, -1.0f );
		m_model.renderAll();
		GL11.glPopMatrix();
	}
}
