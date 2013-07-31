package cuchaz.ships;

import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
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
			Tessellator tessellator = Tessellator.instance;
			for( BlockSide side : BlockSide.values() )
			{
				if( rb.renderAllFaces || block.shouldSideBeRendered( rb.blockAccess, x + side.getDx(), y + side.getDy(), z + side.getDz(), side.getId() ) )
				{
					Icon icon = rb.getBlockIcon( block, rb.blockAccess, x, y, z, side.getId() );
					tessellator.startDrawingQuads();
					tessellator.setNormal( (float)side.getDx(), (float)side.getDy(), (float)side.getDz() );
					side.renderSide( rb, block, (double)x, (double)y, (double)z, icon );
					tessellator.draw();
				}
			}
		}
	},
	Torch( 2 )
	{
		@Override
		public void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
		{
			// UNDONE
		}
	},
	Chest( 22 )
	{
		@Override
		public void render( RenderBlocks rb, Block block, int x, int y, int z, float partialTickTime )
		{
			renderTileEntity( rb, block, x, y, z, partialTickTime );
		}
	};
	
	private int m_id;
	
	private static TreeMap<Integer,RenderBlockType> m_map;
	
	static
	{
		m_map = new TreeMap<Integer,RenderBlockType>();
		for( RenderBlockType type : values() )
		{
			m_map.put( type.m_id, type );
		}
	}
	
	private RenderBlockType( int id )
	{
		m_id = id;
	}
	
	public int getId( )
	{
		return m_id;
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
