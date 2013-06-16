package cuchaz.ships;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;

public class GuiShip extends GuiContainer
{
	private ShipBuilder m_shipBuilder;
	
	private static final int LeftMargin = 8;
	private static final int TopMargin = 6;
	private static final int LineSpacing = 2;
	
	public GuiShip( Container container, ShipBuilder shipBuilder )
	{
		super( container );
		
		m_shipBuilder = shipBuilder;
		m_shipBuilder.build();
		
		xSize = 166;
		ySize = 153;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		final int LineHeight = fontRenderer.FONT_HEIGHT + LineSpacing;
		
		int color = ColorUtils.getGrey( 64 );
		fontRenderer.drawString( GuiString.ShipTitle.getLocalizedText(), LeftMargin, TopMargin, color );
		
		if( m_shipBuilder.isValidShip() )
		{
			// show the number of blocks
			fontRenderer.drawString( String.format( "%s: %d / %d",
				GuiString.ShipNumBlocks.getLocalizedText(),
				m_shipBuilder.getNumBlocks(),
				m_shipBuilder.getMaxNumBlocks()
			), LeftMargin, TopMargin + LineHeight*1, color );
			
			// NEXTTIME: show the make ship button!
		}
		else
		{
			// ship is too large
			fontRenderer.drawString( String.format( "%s: %s",
				GuiString.ShipNumBlocks.getLocalizedText(),
				GuiString.ShipTooLarge.getLocalizedText()
			), LeftMargin, TopMargin + LineHeight*1, color );
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float renderPartialTicks, int mouseX, int mouseY )
	{
		// render the GUI background
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        mc.renderEngine.bindTexture( Ships.TexturesPath + "/gui/ship.png" );
        int x = ( width - xSize )/2;
        int y = ( height - ySize )/2;
        drawTexturedModalRect( x, y, 0, 0, xSize, ySize );
	}
}
