/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.gui;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cuchaz.ships.Ships;

public enum GuiString
{
	Yes,
	No,
	ShipConstruction,
	ShipDashboard,
	ShipNumBlocks,
	ShipTooLarge,
	ShipLaunch,
	ShipUnlaunch,
	ShipInOrAboveWater,
	ShipHasAirAbove,
	ShipFoundWaterHeight,
	ShipWillItFloat,
	ShipAlignedToDirection,
	ShipAwayFromBlocks,
	ShipPropulsion,
	NoShipBlock,
	InvalidShip,
	ClipboardUsage,
	NoShipWasFoundHere,
	CopiedShip,
	ErrorCheckLogForDetails,
	NoShipOnClipboard,
	NoRoomToPasteShip,
	PastedShip,
	OnlyCreative,
	NoPropulsion,
	FoundPropulsion,
	ShipUnlaunchOverride,
	ShipUnlaunchOverrideWarning,
	ListOfSupporters,
	NotASupporter,
	EraserUsage,
	ShipPlaque,
	Done,
	ShipIsUnsinkable,
	ShipFloatsCloseToSinkLine,
	Sink,
	Slept,
	BerthNotFound,
	TryOnStillWater;
	
	public String getKey( )
	{
		return "cuchaz.ships." + name();
	}
	
	public String getLocalizedText( )
	{
		try
		{
			String text = LanguageRegistry.instance().getStringLocalization( getKey() );
			if( text != null && text.length() > 0 )
			{
				return text;
			}
		}
		catch( Exception ex )
		{
			// sometimes, the language registry doesn't work...
			Ships.logger.error( ex, "Unable to translate string: %s", name() );
		}
		
		// if nothing worked, just return the name
		return name();
	}
}
