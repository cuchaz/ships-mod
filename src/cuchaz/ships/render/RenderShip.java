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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BoxCorner;
import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.CompareReal;
import cuchaz.modsShared.Matrix3;
import cuchaz.modsShared.RotatedBB;
import cuchaz.modsShared.Vector3;
import cuchaz.ships.EntityShip;
import cuchaz.ships.HitList;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public class RenderShip extends Render
{
	private RenderBlocks m_renderBlocks;
	private Set<Integer> m_blacklistedBlocks;
	
	public RenderShip( )
	{
		m_renderBlocks = new RenderBlocks();
		m_blacklistedBlocks = new TreeSet<Integer>();
	}
	
	@Override
	protected ResourceLocation getEntityTexture( Entity entity )
	{
		// this isn't used, but it's required by subclasses of Render
		return null;
	}
	
	@Override
	public void doRender( Entity entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		doRender( (EntityShip)entity, x, y, z, yaw, partialTickTime );
	}
	
	public void doRender( EntityShip ship, double x, double y, double z, float yaw, float partialTickTime )
	{
		// NOTE: (x,y,z) is the vector from the camera to the entity origin
		// ie x = ship.posX - camera.posX
		
		m_renderBlocks.blockAccess = ship.getShipWorld();
		
		// render in blocks space
		GL11.glPushMatrix();
		GL11.glTranslated( x, y, z );
		GL11.glRotatef( yaw, 0.0f, 1.0f, 0.0f );
		GL11.glTranslated( ship.blocksToShipX( 0 ), ship.blocksToShipY( 0 ), ship.blocksToShipZ( 0 ) );
		
		RenderHelper.disableStandardItemLighting();
		
		/* UNDONE: enable frustum optimizations if they actually help
		// create the viewing frustum
		EntityLivingBase viewEntity = Minecraft.getMinecraft().renderViewEntity;
		Vec3 v = Vec3.createVectorHelper(
			viewEntity.lastTickPosX + ( viewEntity.posX - viewEntity.lastTickPosX ) * (double)partialTickTime,
	        viewEntity.lastTickPosY + ( viewEntity.posY - viewEntity.lastTickPosY ) * (double)partialTickTime,
	        viewEntity.lastTickPosZ + ( viewEntity.posZ - viewEntity.lastTickPosZ ) * (double)partialTickTime
		);
		ship.worldToShip( v );
		ship.shipToBlocks( v );
		Frustrum frustum = new Frustrum(); // lol, they spelled Frustum wrong
		frustum.setPosition( v.xCoord, v.yCoord, v.zCoord );
        */
		
		RenderManager.instance.renderEngine.bindTexture( TextureMap.locationBlocksTexture );
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// draw all the blocks (but save special tile entities for later)
		ShipWorld world = ship.getShipWorld();
		List<TileEntity> deferredTileEntities = new ArrayList<TileEntity>();
		for( ChunkCoordinates coords : ship.getShipWorld().coords() )
		{
			Block block = Block.blocksList[world.getBlockId( coords )];
			
			/* UNDONE: unable frustum optimizations if they actually help
			// is this block in the view frustum?
			boolean isInFrustum = frustum.isBoxInFrustum(
				block.getBlockBoundsMinX(),
				block.getBlockBoundsMinY(),
				block.getBlockBoundsMinZ(),
				block.getBlockBoundsMaxX(),
				block.getBlockBoundsMaxY(),
				block.getBlockBoundsMaxZ()
			);
			if( !isInFrustum )
			{
				continue;
			}
			*/
			
			// is this block blacklisted?
			if( m_blacklistedBlocks.contains( block.blockID ) )
			{
				// as a fallback, just try to render something
				try
				{
					block.setBlockBounds( 0, 0, 0, 1, 1, 1 );
					m_renderBlocks.setRenderBoundsFromBlock( block );
					m_renderBlocks.renderStandardBlockWithColorMultiplier( block, coords.posX, coords.posY, coords.posZ, 1, 1, 1 );
				}
				catch( Throwable t )
				{
					// this block is already a bad player. Just ignore any more problems
				}
				continue;
			}
			
			// mod blocks can do weird things and crash. We need to be careful here
			try
			{
				// get the block shape
				block.setBlockBoundsBasedOnState( world, coords.posX, coords.posY, coords.posZ );
				
				// do we have a tile entity that needs special rendering?
				TileEntity tileEntity = world.getBlockTileEntity( coords );
				if( tileEntity != null && TileEntityRenderer.instance.hasSpecialRenderer( tileEntity ) )
				{
					deferredTileEntities.add( tileEntity );
					continue;
				}
				
				if( block.getRenderType() == 0 )
				{
					// render a standard block
					// but use the color multiplier instead of ambient occlusion
					// AO just looks weird and I can't make it look good yet
					
					int colorMultiplier = block.colorMultiplier( world, coords.posX, coords.posY, coords.posZ );
			        float colorR = (float)( colorMultiplier >> 16 & 255 )/255.0F;
			        float colorG = (float)( colorMultiplier >> 8 & 255 )/255.0F;
			        float colorB = (float)( colorMultiplier & 255 )/255.0F;
					m_renderBlocks.setRenderBoundsFromBlock( block );
					m_renderBlocks.renderStandardBlockWithColorMultiplier( block, coords.posX, coords.posY, coords.posZ, colorR, colorG, colorB );
				}
				else
				{
					// render using the usual functions
					// they don't cause any issues with AO
					m_renderBlocks.renderBlockByRenderType( block, coords.posX, coords.posY, coords.posZ );
				}
			}
			catch( Throwable t )
			{
				// blacklist the block
				m_blacklistedBlocks.add( block.blockID );
				
				Ships.logger.log( Level.WARNING, "Block: " + block.getUnlocalizedName() + " couldn't render properly! Blocks of this type will not be rendered again.", t );
			}
		}
		
		tessellator.draw();
		
		// now render all the special tile entities
		for( TileEntity tileEntity : deferredTileEntities )
		{
			TileEntityRenderer.instance.renderTileEntityAt(
				tileEntity,
				tileEntity.xCoord,
				tileEntity.yCoord,
				tileEntity.zCoord,
				partialTickTime
			);
		}
		
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
		
		// render debug information
		if( ShipDebugRenderInfo.isDebugRenderingOn() )
		{
			ShipDebugRenderInfo info = ship.getCollider().getDebugRenderInfo();
			
			// render in world space
			GL11.glPushMatrix();
			GL11.glTranslated( x, y, z );
			GL11.glTranslated( -ship.posX, -ship.posY, -ship.posZ );
			
			renderAxis( ship );
			/*
			renderHitbox(
				ship.boundingBox,
				ColorUtils.getColor( 255, 255, 255 )
			);
			*/
			
			GL11.glPopMatrix();
			
			// render in blocks space
			GL11.glPushMatrix();
			GL11.glTranslated( x, y, z );
			GL11.glRotatef( yaw, 0.0f, 1.0f, 0.0f );
			GL11.glTranslated( ship.blocksToShipX( 0 ), ship.blocksToShipY( 0 ), ship.blocksToShipZ( 0 ) );
			
			// render all ship blocks
			List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
			for( ChunkCoordinates coords : ship.getShipWorld().coords() )
			{
				boxes.clear();
				ship.getCollider().getCollisionBoxesInBlockSpace( boxes, coords, ship.getCollider().getBlockBoxInBlockSpace( coords ) );
				int color;
				if( info.getCollidedCoords().contains( coords ) )
				{
					color = ColorUtils.getColor( 255, 255, 0 );
				}
				else
				{
					color = ColorUtils.getColor( 255, 0, 0 );
				}
				for( AxisAlignedBB box : boxes )
				{
					renderHitbox( box, color );
				}
			}
			
			// where is the camera?
			Vec3 camera = Vec3.createVectorHelper(
				RenderManager.renderPosX,
				RenderManager.renderPosY,
				RenderManager.renderPosZ
			);
			ship.worldToShip( camera );
			ship.shipToBlocks( camera );
			boolean isFirstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
			
			// show the query box
			for( AxisAlignedBB box : info.getQueryBoxes() )
			{
				// skip over boxes that surround the camera
				if( box.isVecInside( camera ) && isFirstPerson )
				{
					continue;
				}
				
				renderHitbox( box, ColorUtils.getColor( 0, 255, 0 ) );
			}
			
			// find out what ship block the player is looking at, if any
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			double reachDist = Minecraft.getMinecraft().playerController.getBlockReachDistance();
			HitList hits = new HitList();
			hits.addHits( ship, player, reachDist );
			hits.addHits( player.worldObj, player, reachDist );
			HitList.Entry hit = hits.getClosestHit();
			if( hit != null && hit.type == HitList.Type.Ship )
			{
				boxes.clear();
				ChunkCoordinates coords = new ChunkCoordinates( hit.hit.blockX, hit.hit.blockY, hit.hit.blockZ );
				ship.getCollider().getCollisionBoxesInBlockSpace( boxes, coords, ship.getCollider().getBlockBoxInBlockSpace( coords ) );
				for( AxisAlignedBB box : boxes )
				{
					renderHitbox( box, ColorUtils.getColor( 255, 255, 255 ) );
				}
			}
			
			GL11.glPopMatrix();
			
			info.setRendered();
		}
	}
	
	@Override
	public void doRenderShadowAndFire( Entity entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		// don't render entity shadows
	}
	
	public static void renderPosition( Entity entity )
	{
		renderPoint( entity.posX, entity.posY, entity.posZ, ColorUtils.getColor( 0, 255, 0 ) );
	}
	
	public static void renderPoint( double x, double y, double z, int color )
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
	
	public static void renderAxis( Entity entity )
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
			ColorUtils.getGrey( 180 )
		);
	}
	
	public static void renderHitbox( RotatedBB box, int color )
	{
		// pad the hitbox by a small delta to avoid rendering glitches
		final double delta = 0.01;
		box.getAABox().minX -= delta;
		box.getAABox().minY -= delta;
		box.getAABox().minZ -= delta;
		box.getAABox().maxX += delta;
		box.getAABox().maxY += delta;
		box.getAABox().maxZ += delta;
		
		renderRotatedBox( box, color );
		
		// undo the pad
		box.getAABox().minX += delta;
		box.getAABox().minY += delta;
		box.getAABox().minZ += delta;
		box.getAABox().maxX -= delta;
		box.getAABox().maxY -= delta;
		box.getAABox().maxZ -= delta;
	}
	
	public static void renderHitbox( AxisAlignedBB box, int color )
	{
		// pad the hitbox by a small delta to avoid rendering glitches
		final double delta = 0.01;
		renderBox(
			box.minX - delta,
			box.maxX + delta,
			box.minY - delta,
			box.maxY + delta,
			box.minZ - delta,
			box.maxZ + delta,
			color
		);
	}
	
	public static void renderBox( double xm, double xp, double ym, double yp, double zm, double zp, int color )
	{
		GL11.glDepthMask( false );
		GL11.glDisable( GL11.GL_TEXTURE_2D );
		GL11.glDisable( GL11.GL_LIGHTING );
		GL11.glDisable( GL11.GL_CULL_FACE );
		GL11.glEnable( GL11.GL_BLEND );
		GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_I( color, 128 );
		
		// south
		tessellator.addVertex( xm, yp, zm );
		tessellator.addVertex( xm, ym, zm );
		tessellator.addVertex( xp, ym, zm );
		tessellator.addVertex( xp, yp, zm );
		
		// north
		tessellator.addVertex( xp, yp, zp );
		tessellator.addVertex( xp, ym, zp );
		tessellator.addVertex( xm, ym, zp );
		tessellator.addVertex( xm, yp, zp );
		
		// east
		tessellator.addVertex( xp, yp, zm );
		tessellator.addVertex( xp, ym, zm );
		tessellator.addVertex( xp, ym, zp );
		tessellator.addVertex( xp, yp, zp );
		
		// west
		tessellator.addVertex( xm, yp, zp );
		tessellator.addVertex( xm, ym, zp );
		tessellator.addVertex( xm, ym, zm );
		tessellator.addVertex( xm, yp, zm );
		
		// top
		tessellator.addVertex( xm, yp, zm );
		tessellator.addVertex( xp, yp, zm );
		tessellator.addVertex( xp, yp, zp );
		tessellator.addVertex( xm, yp, zp );
		
		// bottom
		tessellator.addVertex( xm, ym, zm );
		tessellator.addVertex( xp, ym, zm );
		tessellator.addVertex( xp, ym, zp );
		tessellator.addVertex( xm, ym, zp );
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
		GL11.glEnable( GL11.GL_LIGHTING );
		GL11.glEnable( GL11.GL_CULL_FACE );
		GL11.glDisable( GL11.GL_BLEND );
		GL11.glDepthMask( true );
	}
	
	public static void renderRotatedBox( RotatedBB box, int color )
	{
		GL11.glDepthMask( false );
		GL11.glDisable( GL11.GL_TEXTURE_2D );
		GL11.glDisable( GL11.GL_LIGHTING );
		GL11.glDisable( GL11.GL_CULL_FACE );
		GL11.glEnable( GL11.GL_BLEND );
		GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_I( color, 128 );
		
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( BlockSide side : Arrays.asList( BlockSide.North, BlockSide.South, BlockSide.East, BlockSide.West ) )
		{
			for( BoxCorner corner : side.getCorners() )
			{
				box.getCorner( p, corner );
				tessellator.addVertex( p.xCoord, p.yCoord, p.zCoord );
			}
		}
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
		GL11.glEnable( GL11.GL_LIGHTING );
		GL11.glEnable( GL11.GL_CULL_FACE );
		GL11.glDisable( GL11.GL_BLEND );
		GL11.glDepthMask( true );
	}
	
	public static void renderVector( double x, double y, double z, double dx, double dy, double dz, int color )
	{
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
