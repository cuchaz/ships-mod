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

import net.minecraft.client.gui.GuiButton;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipUnlauncher;
import cuchaz.ships.ShipUnlauncher.UnlaunchFlag;
import cuchaz.ships.Ships;
import cuchaz.ships.packets.PacketUnlaunchShip;

public class GuiShipUnlaunch extends GuiShip {
	
	private EntityShip m_ship;
	private ShipUnlauncher m_unlauncher;
	private GuiButton m_buttonUnlaunchShip;
	private GuiButton m_buttonOverride;
	
	public GuiShipUnlaunch(EntityShip ship) {
		m_ship = ship;
		m_unlauncher = new ShipUnlauncher(ship);
		
		m_buttonUnlaunchShip = null;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();
		
		// add the buttons
		m_buttonUnlaunchShip = newGuiButton(0,
			LeftMargin,
			m_height - TopMargin - 20,
			80, 20,
			GuiString.ShipUnlaunch.getLocalizedText()
		);
		m_buttonUnlaunchShip.enabled = false;
		
		m_buttonOverride = newGuiButton(1,
			LeftMargin,
			m_height - TopMargin - 20,
			120, 20,
			GuiString.ShipUnlaunchOverride.getLocalizedText()
		);
		m_buttonOverride.enabled = false;
		
		if (m_unlauncher.isUnlaunchable()) {
			m_buttonUnlaunchShip.enabled = true;
			buttonList.add(m_buttonUnlaunchShip);
		} else if (m_unlauncher.isUnlaunchable(true)) {
			m_buttonOverride.enabled = true;
			buttonList.add(m_buttonOverride);
		} else {
			buttonList.add(m_buttonUnlaunchShip);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void actionPerformed(GuiButton button) {
		if (button.id == m_buttonUnlaunchShip.id) {
			// tell the server to unlaunch the ship
			Ships.net.getDispatch().sendToServer(new PacketUnlaunchShip(m_ship.getEntityId()));
			close();
		} else if (button.id == m_buttonOverride.id) {
			// show the unlaunch button
			m_buttonUnlaunchShip.enabled = true;
			m_buttonOverride.enabled = false;
			buttonList.clear();
			buttonList.add(m_buttonUnlaunchShip);
		}
	}
	
	@Override
	protected void drawForeground(int mouseX, int mouseY, float partialTickTime) {
		drawHeaderText(GuiString.ShipDashboard.getLocalizedText(), 0);
		
		// draw the unlaunch flags
		drawYesNoText(GuiString.ShipAlignedToDirection.getLocalizedText(), m_unlauncher.getUnlaunchFlag(UnlaunchFlag.AlignedToDirection), 2);
		drawYesNoText(GuiString.ShipAwayFromBlocks.getLocalizedText(), m_unlauncher.getUnlaunchFlag(UnlaunchFlag.TouchingOnlySeparatorBlocks), 3);
		
		if (m_buttonOverride.enabled) {
			drawWrappedText(GuiString.ShipUnlaunchOverrideWarning.getLocalizedText(), 10, 160);
		}
	}
}
