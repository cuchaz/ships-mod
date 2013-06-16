package cuchaz.ships;

import cpw.mods.fml.common.registry.LanguageRegistry;

public enum GuiString
{
	ShipTitle( "Ship Dashboard" ),
	ShipNumBlocks( "Blocks" ),
	ShipTooLarge( "Ship is too large!" );
	
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
