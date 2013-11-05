/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.ColorUtils;

public class RenderShip2D
{
	private static final int WaterColor = ColorUtils.getColor( 43, 99, 225 );
	private static final ResourceLocation ShipTexture = TextureMap.locationBlocksTexture;
	
	public static void drawWater( BlockArray envelope, Double waterHeight, int x, int y, double z, int maxWidth, int maxHeight )
	{
		double blockSize = getBlockSize( maxWidth, maxHeight, envelope );
		double shipHeight = (double)envelope.getHeight()*blockSize;
		
		// compute the water height
		int waterRectHeight = maxHeight;
		if( waterHeight != null )
		{
			waterRectHeight = (int)( ( waterHeight - envelope.getVMin() )*blockSize + ( maxHeight - shipHeight )/2.0 );
			waterRectHeight = Math.min( maxHeight, waterRectHeight );
		}
		
		// draw the water rect
		drawWater( x, y + maxHeight - waterRectHeight, z, maxWidth, waterRectHeight );
	}
	
	public static void drawWater( int x, int y, double z, int maxWidth, int maxHeight )
	{
		drawColoredBlock( x, y, z, maxWidth, maxHeight, WaterColor );
	}
	
	public static void drawShip( BlockArray envelope, World world, int x, int y, double z, int maxWidth, int maxHeight )
	{
		double blockSize = getBlockSize( maxWidth, maxHeight, envelope );
		double shipWidth = (double)envelope.getWidth()*blockSize;
		double shipHeight = (double)envelope.getHeight()*blockSize;
		
		// draw the ship blocks
		Minecraft.getMinecraft().getTextureManager().bindTexture( ShipTexture );
		for( int u=envelope.getUMin(); u<=envelope.getUMax(); u++ )
		{
			for( int v=envelope.getVMin(); v<=envelope.getVMax(); v++ )
			{
				ChunkCoordinates coords = envelope.getBlock( u, v );
				if( coords == null )
				{
					continue;
				}
				
				// get the block texture
				Block block = Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )];
				int meta = world.getBlockMetadata( coords.posX, coords.posY, coords.posZ );
				Icon icon = block.getBlockTexture( world, coords.posX, coords.posY, coords.posZ, meta );
				
				drawTexturedBlock(
					( maxWidth - shipWidth )/2.0 + x + ( envelope.toZeroBasedU( u ) )*blockSize,
					( maxHeight - shipHeight )/2.0 + y + ( envelope.getHeight() - envelope.toZeroBasedV( v ) - 1 )*blockSize,
					z,
					blockSize,
					blockSize,
					icon
				);
			}
		}
	}
	
	public static void drawShipAsColor( BlockArray envelope, int color, int x, int y, double z, int maxWidth, int maxHeight )
	{
		double blockSize = getBlockSize( maxWidth, maxHeight, envelope );
		double shipWidth = (double)envelope.getWidth()*blockSize;
		double shipHeight = (double)envelope.getHeight()*blockSize;
		
		for( int u=envelope.getUMin(); u<=envelope.getUMax(); u++ )
		{
			for( int v=envelope.getVMin(); v<=envelope.getVMax(); v++ )
			{
				ChunkCoordinates coords = envelope.getBlock( u, v );
				if( coords == null )
				{
					continue;
				}
				
				drawColoredBlock(
					( maxWidth - shipWidth )/2.0 + x + ( envelope.toZeroBasedU( u ) )*blockSize,
					( maxHeight - shipHeight )/2.0 + y + ( envelope.getHeight() - envelope.toZeroBasedV( v ) - 1 )*blockSize,
					z,
					blockSize,
					blockSize,
					color
				);
			}
		}
	}
	
	private static double getBlockSize( int maxWidth, int maxHeight, BlockArray envelope )
	{
		double blockSize = Math.min(
			(double)maxWidth/(double)envelope.getWidth(),
			(double)maxHeight/(double)envelope.getHeight()
		);
		
		// shink the block size so we have some border
		blockSize *= 0.8;
		
		return blockSize;
	}
	
	public static void drawColoredBlock( double x, double y, double z, double width, double height, int color )
	{
        GL11.glDisable( GL11.GL_TEXTURE_2D );
        GL11.glColor4f(
			ColorUtils.getRedf( color ),
			ColorUtils.getGreenf( color ),
			ColorUtils.getBluef( color ),
			ColorUtils.getAlphaf( color )
		);
        
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// upper left corner, then clockwise
		tessellator.addVertex( x + 0, y + height, z );
		tessellator.addVertex( x + width, y + height, z );
		tessellator.addVertex( x + width, y + 0, z );
		tessellator.addVertex( x + 0, y + 0, z );
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
	}
	
	public static void drawTexturedBlock( double x, double y, double z, double width, double height, Icon icon )
	{
		// get the texture u/v for this icon
		double minU = (double)icon.getInterpolatedU( 0.0 );
		double maxU = (double)icon.getInterpolatedU( 16.0 );
		double minV = (double)icon.getInterpolatedV( 0.0 );
		double maxV = (double)icon.getInterpolatedV( 16.0 );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// upper left corner, then clockwise
		tessellator.addVertexWithUV( x + 0, y + height, z, minU, maxV );
		tessellator.addVertexWithUV( x + width, y + height, z, maxU, maxV );
		tessellator.addVertexWithUV( x + width, y + 0, z, maxU, minV );
		tessellator.addVertexWithUV( x + 0, y + 0, z, minU, minV );
		
		tessellator.draw();
	}
}
