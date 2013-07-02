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
import cuchaz.modsShared.ColorUtils;

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
		
		GL11.glPushMatrix();
		GL11.glTranslated( x, y, z );
		GL11.glRotatef( ship.rotationYaw, 0.0f, 1.0f, 0.0f );
		GL11.glTranslated( ship.blocksToShipX( 0 ), ship.blocksToShipY( 0 ), ship.blocksToShipZ( 0 ) );
		loadTexture( "/terrain.png" );
		
		// draw all the blocks!
		for( ChunkCoordinates coords : ship.getBlocks().coords() )
		{
			Block block = Block.blocksList[ship.getBlocks().getBlockId( coords )];
			block.setBlockBoundsBasedOnState( m_renderBlocks.blockAccess, coords.posX, coords.posY, coords.posZ );
			m_renderBlocks.setRenderBoundsFromBlock( block );
			renderUnlitBlock( block, coords.posX, coords.posY, coords.posZ );
		}
		
		GL11.glPopMatrix();
		
		// render the block entity bounding boxes
		if( true )
		{
			GL11.glPushMatrix();
			GL11.glTranslated( x, y, z );
			GL11.glTranslated( -ship.posX, -ship.posY, -ship.posZ );
			
			renderAxis( ship );
			renderHitbox( ship );
			
			for( ChunkCoordinates coords : ship.getBlocks().coords() )
			{
				renderPosition( ship.getBlockEntity( coords ) );
				renderHitbox( ship.getBlockEntity( coords ) );
			}
			
			GL11.glPopMatrix();
		}
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
	
	private void renderPosition( Entity entity )
	{
		final double halfwidth = 0.05;
		
		renderBox(
			entity.posX - halfwidth,
			entity.posX + halfwidth,
			entity.posY - halfwidth,
			entity.posY + halfwidth,
			entity.posZ - halfwidth,
			entity.posZ + halfwidth,
			ColorUtils.getColor( 0, 255, 0 )
		);
	}
	
	private void renderAxis( Entity entity )
	{
		final double halfwidth = 0.05;
		final double halfHeight = 2.0;
		
		renderBox(
			entity.posX - halfwidth,
			entity.posX + halfwidth,
			entity.posY - halfHeight,
			entity.posY + halfHeight,
			entity.posZ - halfwidth,
			entity.posZ + halfwidth,
			ColorUtils.getColor( 0, 0, 255 )
		);
	}
	
	private void renderHitbox( Entity entity )
	{
		renderBox(
			entity.boundingBox.minX,
			entity.boundingBox.maxX,
			entity.boundingBox.minY,
			entity.boundingBox.maxY,
			entity.boundingBox.minZ,
			entity.boundingBox.maxZ,
			ColorUtils.getColor( 255, 0, 0 )
		);
	}
	
	private void renderBox( double xm, double xp, double ym, double yp, double zm, double zp, int color )
	{
		/*
		System.out.println( String.format(
			"renderBox [%.2f,%.2f] [%.2f,%.2f] [%.2f,%.2f]",
			xm, xp, ym, yp, zm, zp
		) );
		*/
		
		GL11.glDepthMask( false );
		GL11.glDisable( GL11.GL_TEXTURE_2D );
		GL11.glDisable( GL11.GL_LIGHTING );
		GL11.glDisable( GL11.GL_CULL_FACE );
		GL11.glEnable( GL11.GL_BLEND );
		GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_I( color, 128 );
		
		tessellator.addVertex( xm, yp, zm );
		tessellator.addVertex( xm, ym, zm );
		tessellator.addVertex( xp, ym, zm );
		tessellator.addVertex( xp, yp, zm );
		
		tessellator.addVertex( xp, yp, zp );
		tessellator.addVertex( xp, ym, zp );
		tessellator.addVertex( xm, ym, zp );
		tessellator.addVertex( xm, yp, zp );
		
		tessellator.addVertex( xp, yp, zm );
		tessellator.addVertex( xp, ym, zm );
		tessellator.addVertex( xp, ym, zp );
		tessellator.addVertex( xp, yp, zp );
		
		tessellator.addVertex( xm, yp, zp );
		tessellator.addVertex( xm, ym, zp );
		tessellator.addVertex( xm, ym, zm );
		tessellator.addVertex( xm, yp, zm );
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
		GL11.glEnable( GL11.GL_LIGHTING );
		GL11.glEnable( GL11.GL_CULL_FACE );
		GL11.glDisable( GL11.GL_BLEND );
		GL11.glDepthMask( true );
	}
}
