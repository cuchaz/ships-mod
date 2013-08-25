package cuchaz.ships;

import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import cuchaz.modsShared.BlockSide;

public enum RenderBlockType
{
	StandardBlock( 0 )
	{
		@Override
		public void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
		{
			// loads the terrain/blocks texture
			RenderManager.instance.renderEngine.func_110577_a( TextureMap.field_110575_b );

			block.setBlockBoundsBasedOnState( rb.blockAccess, x, y, z );
			rb.setRenderBoundsFromBlock( block );
			
			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			//rb.renderBlockByRenderType( block, x, y, z );
			rb.renderStandardBlockWithColorMultiplier( block, x, y, z, 1.0f, 1.0f, 1.0f );
			tessellator.draw();
		}
	},
	SpecialBlock( 38, 2 )
	{
		@Override
		public void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
		{
			// loads the terrain/blocks texture
			RenderManager.instance.renderEngine.func_110577_a( TextureMap.field_110575_b );
			
			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			rb.renderBlockByRenderType( block, x, y, z );
			tessellator.draw();
		}
	},
	TileEntity( 22 )
	{
		@Override
		public void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
		{
			renderTileEntity( rb, block, x, y, z, partialTickTime );
		}
	};
	
	private int[] m_ids;
	
	private static TreeMap<Integer,RenderBlockType> m_map;
	
	static
	{
		m_map = new TreeMap<Integer,RenderBlockType>();
		for( RenderBlockType type : values() )
		{
			for( int id : type.m_ids )
			{
				m_map.put( id, type );
			}
		}
	}
	
	private RenderBlockType( int ... ids )
	{
		m_ids = ids;
	}
	
	public int[] getIds( )
	{
		return m_ids;
	}
	
	public static RenderBlockType getById( int id )
	{
		RenderBlockType type = m_map.get( id );
		if( type == null )
		{
			type = RenderBlockType.StandardBlock;
		}
		return type;
	}
	
	public abstract void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime );
	
	private static void renderTileEntity( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
	{
		TileEntity tileEntity = rb.blockAccess.getBlockTileEntity( x, y, z );
		TileEntityRenderer.instance.renderTileEntityAt(
			tileEntity,
			(double)x,
			(double)y,
			(double)z,
			partialTickTime
		);
	}
}
