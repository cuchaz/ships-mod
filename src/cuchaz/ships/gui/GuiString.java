package cuchaz.ships.gui;

import cpw.mods.fml.common.registry.LanguageRegistry;

public enum GuiString
{
	Yes( "Yes" ),
	No( "No" ),
	ShipConstruction( "Ship Construction" ),
	ShipDashboard( "Ship Dashboard" ),
	ShipNumBlocks( "Blocks" ),
	ShipTooLarge( "Ship is too large!" ),
	ShipLaunch( "Launch Ship" ),
	ShipUnlaunch( "Dock Ship" ),
	ShipInOrAboveWater( "Ship in or above water" ),
	ShipHasAirAbove( "Ship has air above" ),
	ShipFoundWaterHeight( "Found water surface" ),
	ShipWillItFloat( "Will it float" ),
	ShipAlignedToDirection( "Aligned to launch direction" ),
	ShipAwayFromBlocks( "Away from solid blocks" ),
	ShipPropulsion( "Propulsion" ),
	NoShipBlock( "Couldn't find a ship nearby!" ),
	InvalidShip( "Ship is invalid!" );
	
	private String m_unlocalizedText;
	
	private GuiString( String unlocalizedText )
	{
		m_unlocalizedText = unlocalizedText;
	}
	
	public String getKey( )
	{
		return "cuchaz.ships." + name();
	}
	
	public String getUnlocalizedText( )
	{
		return m_unlocalizedText;
	}
	
	public String getLocalizedText( )
	{
		return LanguageRegistry.instance().getStringLocalization( getKey() );
	}
}
