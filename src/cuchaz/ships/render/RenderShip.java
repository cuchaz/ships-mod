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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.EntityShip;
import cuchaz.ships.HitList;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public class RenderShip extends Render {
	
	private RenderBlocks m_renderBlocks;
	private Set<Integer> m_blacklistedBlocks;
	private Map<ShipWorld,Integer> m_displayListIds;
	
	public RenderShip() {
		m_renderBlocks = new RenderBlocks();
		m_blacklistedBlocks = new TreeSet<Integer>();
		m_displayListIds = new HashMap<ShipWorld,Integer>(); // fine to hash on instance
	}
	
	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		// this isn't needed here, but it's used by subclasses of Render
		return null;
	}
	
	@Override
	public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
		// don't render entity shadows
	}
	
	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
		doRender((EntityShip)entity, x, y, z, yaw, partialTickTime);
	}
	
	public void doRender(EntityShip ship, double x, double y, double z, float yaw, float partialTickTime) {
		// NOTE: (x,y,z) is the vector from the camera to the entity origin
		// ie x = ship.posX - camera.posX
		
		ShipWorld shipWorld = ship.getShipWorld();
		
		// prep for rendering in blocks space
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
		GL11.glTranslated(ship.blocksToShipX(0), ship.blocksToShipY(0), ship.blocksToShipZ(0));
		RenderHelper.disableStandardItemLighting();
		RenderManager.instance.worldObj = shipWorld;
		
		// handle the display list
		RenderManager.instance.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		GL11.glCallList(getDisplayList(m_renderBlocks, shipWorld));
		
		// draw all the hanging entities (in a different coord system);
		GL11.glPushMatrix();
		GL11.glTranslated(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
		for (EntityHanging hangingEntity : shipWorld.hangingEntities().values()) {
			RenderManager.instance.renderEntitySimple(hangingEntity, partialTickTime);
		}
		GL11.glPopMatrix();
		
		// now render all the special tile entities
		for (Coords coords : shipWorld.coords()) {
			TileEntity tileEntity = shipWorld.getTileEntity(coords);
			if (tileEntity != null && TileEntityRendererDispatcher.instance.hasSpecialRenderer(tileEntity)) {
				
				// blocks can do any crazy thing, be defensive
				try {
					TileEntityRendererDispatcher.instance.renderTileEntityAt(tileEntity, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, partialTickTime);
				} catch (Throwable t) {
					Ships.logger.error(t, "Tile entity threw up while rendering: %s", Block.blockRegistry.getNameForObject(tileEntity.getBlockType()));
				}
			}
		}
		
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
		
		// render debug information
		if (ShipDebugRenderInfo.isDebugRenderingOn()) {
			renderDebug(ship, x, y, z, yaw);
		}
	}
	
	public int getDisplayList(RenderBlocks renderBlocks, ShipWorld shipWorld) {
		// does the ship have a list already?
		Integer id = m_displayListIds.get(shipWorld);
		
		if (id != null && shipWorld.needsRenderUpdate()) {
			// invalidate the old list
			GL11.glDeleteLists(id, 1);
			id = null;
		}
		
		if (id == null) {
			// create a new list
			id = GLAllocation.generateDisplayLists(1);
			m_displayListIds.put(shipWorld, id);
			
			// build the list
			GL11.glNewList(id, GL11.GL_COMPILE);
			renderShip(renderBlocks, shipWorld);
			GL11.glEndList();
		}
		return id;
	}
	
	private void renderShip(RenderBlocks renderBlocks, ShipWorld shipWorld) {
		renderBlocks.blockAccess = shipWorld;
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// draw all the blocks (but defer special tile entities for later rendering)
		for (Coords coords : shipWorld.coords()) {
			Block block = shipWorld.getBlock(coords);
			
			// mod blocks can do weird things and crash. We need to be careful here
			try {
				if (m_blacklistedBlocks.contains(Block.getIdFromBlock(block))) {
					renderBlockFailsafe(renderBlocks, block, coords);
				} else {
					renderBlock(renderBlocks, shipWorld, block, coords);
				}
			} catch (Throwable t) {
				// blacklist the block
				m_blacklistedBlocks.add(Block.getIdFromBlock(block));
				
				Ships.logger.warning(t, "Block: %s couldn't render properly! Blocks of this type will not be rendered again.", block.getUnlocalizedName());
			}
		}
		
		tessellator.draw();
	}
	
	public static void renderBlock(RenderBlocks renderBlocks, ShipWorld shipWorld, Block block, Coords coords) {
		// get the block shape
		block.setBlockBoundsBasedOnState(shipWorld, coords.x, coords.y, coords.z);
		
		// do we have a tile entity that needs special rendering?
		TileEntity tileEntity = shipWorld.getTileEntity(coords);
		if (tileEntity != null && TileEntityRendererDispatcher.instance.hasSpecialRenderer(tileEntity)) {
			// skip this block
			return;
		}
		
		if (block.getRenderType() == 0) {
			// render a standard block
			// but use the color multiplier instead of ambient occlusion
			// AO just looks weird and I can't make it look good yet
			
			int colorMultiplier = block.colorMultiplier(shipWorld, coords.x, coords.y, coords.z);
			float colorR = (float) (colorMultiplier >> 16 & 255) / 255.0F;
			float colorG = (float) (colorMultiplier >> 8 & 255) / 255.0F;
			float colorB = (float) (colorMultiplier & 255) / 255.0F;
			renderBlocks.setRenderBoundsFromBlock(block);
			renderBlocks.renderStandardBlockWithColorMultiplier(block, coords.x, coords.y, coords.z, colorR, colorG, colorB);
		} else {
			// render using the usual functions
			// they don't cause any issues with AO
			renderBlocks.renderBlockByRenderType(block, coords.x, coords.y, coords.z);
		}
	}
	
	public static void renderBlockFailsafe(RenderBlocks renderBlocks, Block block, Coords coords) {
		// as a fallback, just try to render something
		try {
			block.setBlockBounds(0, 0, 0, 1, 1, 1);
			renderBlocks.setRenderBoundsFromBlock(block);
			renderBlocks.renderStandardBlockWithColorMultiplier(block, coords.x, coords.y, coords.z, 1, 1, 1);
		} catch (Throwable t) {
			// this block is already a bad player. Just ignore any more problems
		}
	}
	
	private void renderDebug(EntityShip ship, double x, double y, double z, float yaw) {
		ShipDebugRenderInfo info = ship.getCollider().getDebugRenderInfo();
		
		// render in world space
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslated(-ship.posX, -ship.posY, -ship.posZ);
		
		RenderUtils.renderAxis(ship);
		/*
		 * renderHitbox( ship.boundingBox, ColorUtils.getColor( 255, 255, 255 ) );
		 */
		
		GL11.glPopMatrix();
		
		// render in blocks space
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
		GL11.glTranslated(ship.blocksToShipX(0), ship.blocksToShipY(0), ship.blocksToShipZ(0));
		
		// render all ship blocks
		List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
		for (Coords coords : ship.getShipWorld().coords()) {
			boxes.clear();
			ship.getCollider().getCollisionBoxesInBlockSpace(boxes, coords, ship.getCollider().getBlockBoxInBlockSpace(coords));
			int color;
			if (info.getCollidedCoords().contains(coords)) {
				color = ColorUtils.getColor(255, 255, 0);
			} else {
				color = ColorUtils.getColor(255, 0, 0);
			}
			for (AxisAlignedBB box : boxes) {
				RenderUtils.renderHitbox(box, color);
			}
		}
		
		// render all the trapped air that displaced water
		double waterHeightInBlockSpace = ship.shipToBlocksY(ship.worldToShipY(ship.getWaterHeight()));
		for (Coords coords : ship.getShipWorld().getDisplacement().getTrappedAirFromWaterHeight(waterHeightInBlockSpace)) {
			AxisAlignedBB box = ship.getCollider().getBlockBoxInBlockSpace(coords);
			RenderUtils.renderBox(box, ColorUtils.getColor(0, 255, 0), -0.4);
		}
		
		// where is the camera?
		Vec3 camera = Vec3.createVectorHelper(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
		ship.worldToShip(camera);
		ship.shipToBlocks(camera);
		boolean isFirstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		
		// show the query box
		for (AxisAlignedBB box : info.getQueryBoxes()) {
			// skip over boxes that surround the camera
			if (box.isVecInside(camera) && isFirstPerson) {
				continue;
			}
			
			RenderUtils.renderHitbox(box, ColorUtils.getColor(0, 255, 0));
		}
		
		// find out what ship block the player is looking at, if any
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		double reachDist = Minecraft.getMinecraft().playerController.getBlockReachDistance();
		HitList hits = new HitList();
		hits.addHits(ship, player, reachDist);
		hits.addHits(player.worldObj, player, reachDist);
		HitList.Entry hit = hits.getClosestHit();
		if (hit != null && hit.type == HitList.Type.Ship) {
			boxes.clear();
			Coords coords = new Coords(hit.hit.blockX, hit.hit.blockY, hit.hit.blockZ);
			ship.getCollider().getCollisionBoxesInBlockSpace(boxes, coords, ship.getCollider().getBlockBoxInBlockSpace(coords));
			for (AxisAlignedBB box : boxes) {
				RenderUtils.renderHitbox(box, ColorUtils.getColor(255, 255, 255));
			}
		}
		
		GL11.glPopMatrix();
		
		info.setRendered();
	}
	
	/* frustum stuff for later reference
	
	// create the viewing frustum (NOTE: the frustum is in world space)
	EntityLivingBase viewEntity = Minecraft.getMinecraft().renderViewEntity;
	Vec3 v = Vec3.createVectorHelper(
		viewEntity.lastTickPosX + ( viewEntity.posX - viewEntity.lastTickPosX ) * (double)partialTickTime,
        viewEntity.lastTickPosY + ( viewEntity.posY - viewEntity.lastTickPosY ) * (double)partialTickTime,
        viewEntity.lastTickPosZ + ( viewEntity.posZ - viewEntity.lastTickPosZ ) * (double)partialTickTime
	);
	Frustrum frustum = new Frustrum(); // lol, they spelled Frustum wrong
	frustum.setPosition( v.xCoord, v.yCoord, v.zCoord );
	
	// is this block in the view frustum?
	ship.getCollider().getBlockWorldBoundingBox( worldBox, coords );
	boolean isInFrustum = frustum.isBoxInFrustum(
		worldBox.minX, worldBox.minY, worldBox.minZ,
		worldBox.maxX, worldBox.maxY, worldBox.maxZ
	);
	if( !isInFrustum )
	{
		continue;
	}
	*/
}
