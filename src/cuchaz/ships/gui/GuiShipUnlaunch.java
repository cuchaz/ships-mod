package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipUnlauncher;
import cuchaz.ships.ShipUnlauncher.UnlaunchFlag;
import cuchaz.ships.packets.PacketUnlaunchShip;

public class GuiShipUnlaunch extends GuiShip
{
	private EntityShip m_ship;
	private ShipUnlauncher m_unlauncher;
	private GuiButton m_buttonUnlaunchShip;
	
	public GuiShipUnlaunch( Container container, EntityShip ship )
	{
		super( container );
		
		m_ship = ship;
		m_unlauncher = new ShipUnlauncher( ship );
		
		m_buttonUnlaunchShip = null;
	}
	
	@Override
	@SuppressWarnings( "unchecked" )
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonUnlaunchShip = new GuiButton( 
			0, guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.ShipUnlaunch.getLocalizedText()
		);
		m_buttonUnlaunchShip.enabled = m_unlauncher.isUnlaunchable();
		buttonList.add( m_buttonUnlaunchShip );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonUnlaunchShip.id )
		{
			// tell the server to unlaunch the ship
			PacketUnlaunchShip packet = new PacketUnlaunchShip( m_ship.entityId );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawText( GuiString.ShipDashboard.getLocalizedText(), 0 );
		
		// draw the unlaunch flags
		drawYesNoText(
			GuiString.ShipAlignedToDirection.getLocalizedText(),
			m_unlauncher.getUnlaunchFlag( UnlaunchFlag.AlignedToDirection ),
			2
		);
		drawYesNoText(
			GuiString.ShipAwayFromBlocks.getLocalizedText(),
			m_unlauncher.getUnlaunchFlag( UnlaunchFlag.TouchingOnlySeparatorBlocks ),
			3
		);
	}
}
