package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;

import org.lwjgl.opengl.GL11;

public class RenderShip extends Render
{
	private RenderBlocks m_renderBlocks;
	
	public RenderShip( )
	{
		m_renderBlocks = new RenderBlocks();
	}
	
	@Override
	public void doRender( Entity entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		doRender( (EntityShip)entity, x, y, z, yaw, partialTickTime );
	}
	
	public void doRender( EntityShip ship, double x, double y, double z, float yaw, float partialTickTime )
	{
		m_renderBlocks.blockAccess = ship.getBlocks();
		
		// TEMP: just draw one block for now
		ChunkCoordinates coords = ship.getBlocks().blocks().iterator().next();
		Block block = Block.blocksList[ship.getBlocks().getBlockId( coords )];
		
		GL11.glPushMatrix();
		GL11.glTranslatef( (float)x, (float)y + 2.0f /* TEMP */, (float)z );
		loadTexture( "/terrain.png" );
		GL11.glDisable( GL11.GL_LIGHTING );
		m_renderBlocks.setRenderBoundsFromBlock( block );
		boolean renderSuccess = m_renderBlocks.renderBlockByRenderType( block, coords.posX, coords.posY, coords.posZ );
		GL11.glEnable( GL11.GL_LIGHTING );
		GL11.glPopMatrix();
		
		// TEMP
		System.out.println( "RenderShip.doRender()! " + ship.getBlocks().getBlockId( coords ) + " " + ( renderSuccess ? "SUCCESS" : "FAIL" ) );
	}
}
