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

import net.minecraft.util.StatCollector;

public enum GuiString {
	Yes,
	No,
	ShipConstruction,
	ShipDashboard,
	ShipNumBlocks,
	ShipLaunch,
	ShipUnlaunch,
	ShipWillItFloat,
	ShipAlignedToDirection,
	ShipAwayFromBlocks,
	ShipPropulsion,
	NoShipBlock,
	InvalidShip,
	ClipboardUsage,
	NoShipWasFoundHere,
	CopiedShip,
	NoShipOnClipboard,
	NoRoomToPasteShip,
	PastedShip,
	OnlyCreative,
	NoPropulsion,
	FoundPropulsion,
	ShipUnlaunchOverride,
	ShipUnlaunchOverrideWarning,
	EraserUsage,
	ShipIsUnsinkable,
	ShipFloatsCloseToSinkLine,
	Sink,
	Slept,
	BerthNotFound,
	TryOnStillWater,
	ShipDataCorrupted;
	
	public String getKey() {
		return "cuchaz.ships." + name();
	}
	
	public String getLocalizedText() {
		String text = StatCollector.translateToLocal(getKey());
		if (text != null && text.length() > 0) {
			return text;
		}
		
		// if nothing worked, just return the name
		return name();
	}
}
