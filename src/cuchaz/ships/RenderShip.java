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
import cuchaz.modsShared.CompareReal;
import cuchaz.modsShared.Matrix3;
import cuchaz.modsShared.Vector3;

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
			renderVector( ship.posX, ship.posY + 2, ship.posZ, ship.motionX*10, ship.motionY*10, ship.motionZ*10, ColorUtils.getColor( 255, 255, 0 ) );
			
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
		renderPoint( entity.posX, entity.posY, entity.posZ, ColorUtils.getColor( 0, 255, 0 ) );
	}
	
	private void renderPoint( double x, double y, double z, int color )
	{
		final double Halfwidth = 0.05;
		
		renderBox(
			x - Halfwidth,
			x + Halfwidth,
			y - Halfwidth,
			y + Halfwidth,
			z - Halfwidth,
			z + Halfwidth,
			color
		);
	}
	
	private void renderAxis( Entity entity )
	{
		final double Halfwidth = 0.05;
		final double HalfHeight = 2.0;
		
		renderBox(
			entity.posX - Halfwidth,
			entity.posX + Halfwidth,
			entity.posY - HalfHeight,
			entity.posY + HalfHeight,
			entity.posZ - Halfwidth,
			entity.posZ + Halfwidth,
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
	
	private void renderVector( double x, double y, double z, double dx, double dy, double dz, int color )
	{
		// UNDONE: optimize out the new calls
		
		// get the vector in world-space
		Vector3 v = new Vector3( dx, dy, dz );
		
		// does this vector even have length?
		if( CompareReal.eq( v.getSquaredLength(), 0 ) )
		{
			return;
		}
		
		// build the vector geometry in an arbitrary space
		double halfWidth = 0.2;
		Vector3[] vertices = {
			new Vector3( halfWidth, halfWidth, 0 ),
			new Vector3( -halfWidth, halfWidth, 0 ),
			new Vector3( -halfWidth, -halfWidth, 0 ),
			new Vector3( halfWidth, -halfWidth, 0 ),
			new Vector3( 0, 0, v.getLength() )
		};
		
		// compute a basis so we can transform from parameter space to world space
		v.normalize();
		Matrix3 basis = new Matrix3();
		Matrix3.getArbitraryBasisFromZ( basis, v );
		
		// transform the vertices
		for( Vector3 p : vertices )
		{
			// rotate
			basis.multiply( p );
			
			// translate
			p.x += x;
			p.y += y;
			p.z += z;
		}
		
		// render the geometry
		GL11.glDepthMask( false );
		GL11.glDisable( GL11.GL_TEXTURE_2D );
		GL11.glDisable( GL11.GL_LIGHTING );
		GL11.glDisable( GL11.GL_CULL_FACE );
		GL11.glEnable( GL11.GL_BLEND );
		GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawing( 6 ); // triangle fan
		tessellator.setColorRGBA_I( color, 128 );
		
		tessellator.addVertex( vertices[4].x, vertices[4].y, vertices[4].z );
		tessellator.addVertex( vertices[0].x, vertices[0].y, vertices[0].z );
		tessellator.addVertex( vertices[1].x, vertices[1].y, vertices[1].z );
		tessellator.addVertex( vertices[2].x, vertices[2].y, vertices[2].z );
		tessellator.addVertex( vertices[3].x, vertices[3].y, vertices[3].z );
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
		GL11.glEnable( GL11.GL_LIGHTING );
		GL11.glEnable( GL11.GL_CULL_FACE );
		GL11.glDisable( GL11.GL_BLEND );
		GL11.glDepthMask( true );
	}
}
