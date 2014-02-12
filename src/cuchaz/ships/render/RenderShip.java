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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

import cuchaz.modsShared.ColorUtils;
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
				
				Ships.logger.warning( t, "Block: %s couldn't render properly! Blocks of this type will not be rendered again.", block.getUnlocalizedName() );
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
			
			RenderUtils.renderAxis( ship );
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
					RenderUtils.renderHitbox( box, color );
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
				
				RenderUtils.renderHitbox( box, ColorUtils.getColor( 0, 255, 0 ) );
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
					RenderUtils.renderHitbox( box, ColorUtils.getColor( 255, 255, 255 ) );
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
}
