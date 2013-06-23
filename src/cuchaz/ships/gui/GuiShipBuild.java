package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.ShipBuilder;
import cuchaz.ships.ShipBuilder.BuildFlag;
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
	@SuppressWarnings( "unchecked" )
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
		m_buttonBuild.enabled = m_shipBuilder.isValidToBuild();
		buttonList.add( m_buttonBuild );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonBuild.id )
		{
			// tell the server to spawn a ship
			PacketBuildShip packet = new PacketBuildShip( m_shipBuilder.getX(), m_shipBuilder.getY(), m_shipBuilder.getZ() );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		int textColor = ColorUtils.getGrey( 64 );
		
		drawText( GuiString.ShipConstruction.getLocalizedText(), 0, textColor );
		
		String valueText;
		
		// right number of blocks
		if( m_shipBuilder.getBuildFlag( BuildFlag.RightNumberOfBlocks ) )
		{
			valueText = String.format( "%d / %d",
				m_shipBuilder.getNumBlocks(),
				m_shipBuilder.getShipType().getMaxNumBlocks()
			);
		}
		else
		{
			valueText = GuiString.ShipTooLarge.getLocalizedText();
		}
		drawText( String.format( "%s: %s", GuiString.ShipNumBlocks.getLocalizedText(), valueText ), 1, textColor );
		
		// has water below
		valueText = getYesNoText( m_shipBuilder.getBuildFlag( BuildFlag.HasWaterBelow ) );
		drawText( String.format( "%s: %s", GuiString.ShipInOrAboveWater.getLocalizedText(), valueText ), 2, textColor );
		
		// has air above
		valueText = getYesNoText( m_shipBuilder.getBuildFlag( BuildFlag.HasAirAbove ) );
		drawText( String.format( "%s: %s", GuiString.ShipHasAirAbove.getLocalizedText(), valueText ), 3, textColor );
		
		// found water height
		valueText = getYesNoText( m_shipBuilder.getBuildFlag( BuildFlag.FoundWaterHeight ) );
		drawText( String.format( "%s: %s", GuiString.ShipFoundWaterHeight.getLocalizedText(), valueText ), 4, textColor );
		
		// draw the ship and show the water height
		//drawShip( m_shipBuilder.get );
		
		// UNDONE: choose a ship name
	}

	protected String getYesNoText( boolean flag )
	{
		return flag ? GuiString.Yes.getLocalizedText() : GuiString.No.getLocalizedText();
	}
}
