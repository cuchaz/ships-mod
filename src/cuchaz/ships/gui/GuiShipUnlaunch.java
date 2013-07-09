package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.LineSpacing;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipUnbuilder;
import cuchaz.ships.packets.PacketUnbuildShip;

public class GuiShipUnlaunch extends GuiShip
{
	private EntityShip m_ship;
	private GuiButton m_buttonUnmakeShip;
	
	public GuiShipUnlaunch( Container container, EntityShip ship )
	{
		super( container );
		
		m_ship = ship;
		
		m_buttonUnmakeShip = null;
	}
	
	@Override
	@SuppressWarnings( "unchecked" )
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonUnmakeShip = new GuiButton( 
			0, guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.ShipUnlaunch.getLocalizedText()
		);
		ShipUnbuilder unbuilder = new ShipUnbuilder( m_ship );
		m_buttonUnmakeShip.enabled = unbuilder.isShipInValidUnbuildPosition();
		buttonList.add( m_buttonUnmakeShip );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonUnmakeShip.id )
		{
			// tell the server to unspwan the ship
			PacketUnbuildShip packet = new PacketUnbuildShip( m_ship.entityId );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		final int LineHeight = fontRenderer.FONT_HEIGHT + LineSpacing;
		
		int textColor = ColorUtils.getGrey( 64 );
		fontRenderer.drawString( GuiString.ShipDashboard.getLocalizedText(), LeftMargin, TopMargin, textColor );
		
		// UNDONE: show ship vitals
	}
}
