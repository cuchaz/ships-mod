package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.LineSpacing;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.ShipBuilder;
import cuchaz.ships.packets.PacketBuildShip;

public class GuiShipBuild extends GuiShip
{
	private ShipBuilder m_shipBuilder;
	private GuiButton m_buttonBuild;
	
	public GuiShipBuild( Container container, ShipBuilder shipBuilder )
	{
		super( container );
		
		m_shipBuilder = shipBuilder;
		
		m_buttonBuild = null;
	}
	
	@Override
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonBuild = new GuiButton( 
			0, guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.ShipBuild.getLocalizedText()
		);
		m_buttonBuild.enabled = m_shipBuilder.isValidShip();
		buttonList.add( m_buttonBuild );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonBuild.id )
		{
			// tell the server to spawn a ship
			PacketBuildShip packet = new PacketBuildShip( m_shipBuilder.x, m_shipBuilder.y, m_shipBuilder.z );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		final int LineHeight = fontRenderer.FONT_HEIGHT + LineSpacing;
		
		int textColor = ColorUtils.getGrey( 64 );
		fontRenderer.drawString( GuiString.ShipConstruction.getLocalizedText(), LeftMargin, TopMargin, textColor );
		
		if( m_shipBuilder.isValidShip() )
		{
			// show the number of blocks
			fontRenderer.drawString( String.format( "%s: %d / %d",
				GuiString.ShipNumBlocks.getLocalizedText(),
				m_shipBuilder.getNumBlocks(),
				m_shipBuilder.getMaxNumBlocks()
			), LeftMargin, TopMargin + LineHeight*1, textColor );
		}
		else
		{
			// ship is too large
			fontRenderer.drawString( String.format( "%s: %s",
				GuiString.ShipNumBlocks.getLocalizedText(),
				GuiString.ShipTooLarge.getLocalizedText()
			), LeftMargin, TopMargin + LineHeight*1, textColor );
		}
		
		// UNDONE: choose a ship name
	}
}
