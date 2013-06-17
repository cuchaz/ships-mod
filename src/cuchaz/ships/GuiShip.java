package cuchaz.ships;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;

public class GuiShip extends GuiContainer
{
	private ShipBuilder m_shipBuilder;
	private GuiButton m_buttonMakeShip;
	
	private static final int LeftMargin = 8;
	private static final int TopMargin = 6;
	private static final int LineSpacing = 2;
	
	/* A Note about GUIs:
	 *    Different rendering functions appear to work in different coordinate spaces
	 *    Full screen coords:
	 *       buttons, textures
	 *    Window coords:
	 *       text
	 */
	
	public GuiShip( Container container, ShipBuilder shipBuilder )
	{
		super( container );
		
		m_shipBuilder = shipBuilder;
		m_shipBuilder.build();
		
		xSize = 166;
		ySize = 153;
		
		m_buttonMakeShip = null;
	}
	
	@Override
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonMakeShip = new GuiButton( 0, guiLeft + LeftMargin, guiTop + ySize - TopMargin - 20, 80, 20, "Make Ship" );
		m_buttonMakeShip.enabled = m_shipBuilder.isValidShip();
		buttonList.add( m_buttonMakeShip );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonMakeShip.id )
		{
			m_shipBuilder.makeShip();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		final int LineHeight = fontRenderer.FONT_HEIGHT + LineSpacing;
		
		int textColor = ColorUtils.getGrey( 64 );
		fontRenderer.drawString( GuiString.ShipTitle.getLocalizedText(), LeftMargin, TopMargin, textColor );
		
		if( m_shipBuilder.isValidShip() )
		{
			// show the number of blocks
			fontRenderer.drawString( String.format( "%s: %d / %d",
				GuiString.ShipNumBlocks.getLocalizedText(),
				m_shipBuilder.getNumBlocks(),
				m_shipBuilder.getMaxNumBlocks()
			), LeftMargin, TopMargin + LineHeight*1, textColor );
			
			// NEXTTIME: show the make ship button!
		}
		else
		{
			// ship is too large
			fontRenderer.drawString( String.format( "%s: %s",
				GuiString.ShipNumBlocks.getLocalizedText(),
				GuiString.ShipTooLarge.getLocalizedText()
			), LeftMargin, TopMargin + LineHeight*1, textColor );
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float renderPartialTicks, int mouseX, int mouseY )
	{
		// render the GUI background
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        mc.renderEngine.bindTexture( Ships.TexturesPath + "/gui/ship.png" );
        drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize );
	}
}
