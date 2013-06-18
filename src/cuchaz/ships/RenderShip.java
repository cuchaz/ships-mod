package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Icon;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.BlockSide;

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
	
	public void doRender( EntityShip ship, double cameraX, double cameraY, double cameraZ, float yaw, float partialTickTime )
	{
		m_renderBlocks.blockAccess = ship.getBlocks();
		
		GL11.glPushMatrix();
		GL11.glTranslatef( (float)cameraX, (float)cameraY, (float)cameraZ );
		loadTexture( "/terrain.png" );
		
		Tessellator tessellator = Tessellator.instance;
		
		// draw all the blocks!
		for( ChunkCoordinates coords : ship.getBlocks().blocks() )
		{
			Block block = Block.blocksList[ship.getBlocks().getBlockId( coords )];
			block.setBlockBoundsBasedOnState( m_renderBlocks.blockAccess, coords.posX, coords.posY, coords.posZ );
			m_renderBlocks.setRenderBoundsFromBlock( block );
			renderUnlitBlock( block, coords.posX, coords.posY, coords.posZ );
		}
		
		tessellator.setTranslation( 0, 0, 0 );
		
		GL11.glPopMatrix();
	}
	
	public void renderUnlitBlock( Block block, int x, int y, int z )
	{
		Tessellator tessellator = Tessellator.instance;
		for( BlockSide side : BlockSide.values() )
		{
			if( m_renderBlocks.renderAllFaces || block.shouldSideBeRendered( m_renderBlocks.blockAccess, x + side.getDx(), y + side.getDy(), z + side.getDz(), side.getId() ) )
			{
				Icon icon = m_renderBlocks.getBlockIcon( block, m_renderBlocks.blockAccess, x, y, z, side.getId() );
				
				tessellator.startDrawingQuads();
				tessellator.setNormal( (float)side.getDx(), (float)side.getDy(), (float)side.getDz() );
				side.renderSide( m_renderBlocks, block, (double)x, (double)y, (double)z, icon );
				tessellator.draw();
			}
		}
	}
}
