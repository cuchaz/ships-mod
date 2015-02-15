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

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class GuiShipPilotPaddle extends GuiShipPilot {
	
	public GuiShipPilotPaddle(EntityShip ship, EntityPlayer player) {
		super(
			110, 25, new ResourceLocation("ships", "textures/gui/shipPaddle.png"),
			ship,
			player,
			Arrays.asList(PilotAction.Forward, PilotAction.Backward, PilotAction.Left, PilotAction.Right),
			ForwardSideMethod.ByPlayerLook
		);
	}
	
	@Override
	protected void drawForeground(int mouseX, int mouseY, float partialTickTime) {
		
		int keyForward = mc.gameSettings.keyBindForward.getKeyCode();
		int keyBack = mc.gameSettings.keyBindBack.getKeyCode();
		int keyLeft = mc.gameSettings.keyBindLeft.getKeyCode();
		int keyRight = mc.gameSettings.keyBindRight.getKeyCode();
		
		// draw the key binds
		int textColor = ColorUtils.getGrey(64);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyForward), 11, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyBack), 46, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyLeft), 61, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyRight), 95, 8, textColor);
	}
}
