package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;
import cuchaz.modsShared.BlockUtils.Neighbors;
import cuchaz.ships.MaterialProperties;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.Ships;
import cuchaz.ships.render.RenderShip2D;

public class GuiShipPropulsion extends GuiShip
{
	private ShipLauncher m_shipLauncher;
	private BlockArray m_shipEnvelope;
	private BlockArray m_helmEnvelope;
	private BlockArray m_propulsionEnvelope;
	
	public GuiShipPropulsion( Container container, final World world, int helmX, int helmY, int helmZ )
	{
		super( container );
		
		// this should be the helm
		assert( world.getBlockId( helmX, helmY, helmZ ) == Ships.m_blockHelm.blockID );
		ChunkCoordinates helmCoords = new ChunkCoordinates( helmX, helmY, helmZ );
		
		// find the ship block
		ChunkCoordinates shipBlockCoords = BlockUtils.searchForBlock(
			helmX, helmY, helmZ,
			10000,
			new BlockConditionValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
				}
				
				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					return world.getBlockId( coords.posX, coords.posY, coords.posZ ) == Ships.m_blockShip.blockID;
				}
			},
			Neighbors.Edges
		);
		if( shipBlockCoords != null )
		{
			// get the ship definition and its top envelope
			m_shipLauncher = new ShipLauncher( world, shipBlockCoords.posX, shipBlockCoords.posY, shipBlockCoords.posZ );
			m_shipEnvelope = m_shipLauncher.getShipEnvelope( BlockSide.Top );
			
			// compute an envelope for the helm
			m_helmEnvelope = m_shipEnvelope.newEmptyCopy();
			m_helmEnvelope.setBlock( helmCoords.posX - shipBlockCoords.posX, helmCoords.posZ - shipBlockCoords.posZ, helmCoords );
			
			// UNDONE: compute an envelope for the propulsion systems
			m_propulsionEnvelope = m_shipEnvelope.newEmptyCopy();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawText( GuiString.ShipPropulsion.getLocalizedText(), 0 );
		
		if( m_shipLauncher != null )
		{
			int x = LeftMargin;
			int y = getLineY( 6 );
			int width = xSize - LeftMargin*2;
			int height = 64;
			
			RenderShip2D.drawWater( x, y, zLevel, width, height );
			
			if( m_shipEnvelope.getHeight() > m_shipEnvelope.getWidth() )
			{
				// rotate the envelopes so the long axis is across the GUI width
				m_shipEnvelope = BlockArray.Rotation.Ccw90.rotate( m_shipEnvelope );
				m_helmEnvelope = BlockArray.Rotation.Ccw90.rotate( m_helmEnvelope );
				m_propulsionEnvelope = BlockArray.Rotation.Ccw90.rotate( m_propulsionEnvelope );
			}
			
			// draw the ship slightly darker than normal
			GL11.glColor4f( 0.5f, 0.5f, 0.5f, 1.0f );
			RenderShip2D.drawShip(
				m_shipEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
			
			// draw the propulsion blocks at full brightness
			GL11.glColor4f( 0.5f, 0.5f, 0.5f, 1.0f );
			RenderShip2D.drawShip(
				m_propulsionEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
			
			// draw the helm at full brightness
			GL11.glColor4f( 0.5f, 0.5f, 0.5f, 1.0f );
			RenderShip2D.drawShip(
				m_helmEnvelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
		}
	}
}
