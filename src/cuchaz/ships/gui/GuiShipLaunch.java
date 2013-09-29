package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLauncher.LaunchFlag;
import cuchaz.ships.packets.PacketLaunchShip;
import cuchaz.ships.render.RenderShip2D;

public class GuiShipLaunch extends GuiShip
{
	private ShipLauncher m_shipLauncher;
	private GuiButton m_buttonLaunchShip;
	
	public GuiShipLaunch( Container container, ShipLauncher shipLauncher )
	{
		super( container );
		
		m_shipLauncher = shipLauncher;
		
		m_buttonLaunchShip = null;
	}
	
	@Override
	@SuppressWarnings( "unchecked" )
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonLaunchShip = new GuiButton( 
			0, guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.ShipLaunch.getLocalizedText()
		);
		m_buttonLaunchShip.enabled = m_shipLauncher.isLaunchable();
		buttonList.add( m_buttonLaunchShip );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonLaunchShip.id )
		{
			// tell the server to spawn a ship
			PacketLaunchShip packet = new PacketLaunchShip( m_shipLauncher.getX(), m_shipLauncher.getY(), m_shipLauncher.getZ() );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawHeaderText( GuiString.ShipConstruction.getLocalizedText(), 0 );
		
		String valueText;
		
		// right number of blocks
		if( m_shipLauncher.getLaunchFlag( LaunchFlag.RightNumberOfBlocks ) )
		{
			valueText = String.format( "%d / %d",
				m_shipLauncher.getNumBlocks(),
				m_shipLauncher.getShipType().getMaxNumBlocks()
			);
		}
		else
		{
			valueText = GuiString.ShipTooLarge.getLocalizedText();
		}
		drawLabelValueText( GuiString.ShipNumBlocks.getLocalizedText(), valueText, 1 );
		
		// draw the launch flags
		drawYesNoText(
			GuiString.ShipInOrAboveWater.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.HasWaterBelow ),
			2
		);
		drawYesNoText(
			GuiString.ShipHasAirAbove.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.HasAirAbove ),
			3
		);
		drawYesNoText(
			GuiString.ShipFoundWaterHeight.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.FoundWaterHeight ),
			4
		);
		drawYesNoText(
			GuiString.ShipWillItFloat.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.WillItFloat ),
			5
		);
		
		// draw the ship and show the water height
		BlockSide shipSide = m_shipLauncher.getShipSide();
		if( shipSide != null )
		{
			BlockArray envelope = m_shipLauncher.getShipEnvelope( shipSide );
			int x = LeftMargin;
			int y = getLineY( 6 );
			int width = xSize - LeftMargin*2;
			int height = 64;
			RenderShip2D.drawWater(
				envelope,
				m_shipLauncher.getEquilibriumWaterHeight(),
				x, y, zLevel, width, height
			);
			RenderShip2D.drawShip(
				envelope,
				m_shipLauncher.getShipWorld(),
				x, y, zLevel, width, height
			);
		}
	}
}
