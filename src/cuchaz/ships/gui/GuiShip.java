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

import net.minecraft.util.ResourceLocation;
import cuchaz.modsShared.gui.GuiBase;

public abstract class GuiShip extends GuiBase {
	
	public GuiShip() {
		super(176, 166, new ResourceLocation("ships", "textures/gui/ship.png"), true);
	}
	
	protected String getYesNoText(boolean flag) {
		return flag ? GuiString.Yes.getLocalizedText() : GuiString.No.getLocalizedText();
	}
	
	protected void drawYesNoText(String labelText, boolean isYes, int lineNum) {
		drawYesNoText(labelText, getYesNoText(isYes), isYes, lineNum);
	}
}
