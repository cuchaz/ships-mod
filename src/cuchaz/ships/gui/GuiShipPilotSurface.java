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

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.math.CircleRange;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class GuiShipPilotSurface extends GuiShipPilot {
	
	private static final int TextureWidth = 256;
	private static final int TextureHeight = 256;
	private static final int CompassHeight = 12;
	private static final int CompassY = 52;
	private static final int CompassFrameX = 156;
	private static final int CompassFrameY = 6;
	private static final int CompassFrameWidth = 93;
	private static final int CompassNorthOffset = 5;
	private static final int CompassMarkerX = 201;
	private static final int CompassMarkerY = 31;
	private static final int CompassMarkerWidth = 3;
	private static final int CompassMarkerHeight = 4;
	private static final int CompassRangeX = 8;
	private static final int CompassRangeY = 39;
	private static final int CompassRangeWidth = 16;
	private static final int ThrottleX = 8;
	private static final int ThrottleY = 26;
	private static final int ThrottleZero = 12;
	private static final int ThrottleWidth = 37;
	private static final int ThrottleHeight = 12;
	private static final int ThrottleFrameX = 8;
	private static final int ThrottleFrameY = 6;
	
	public GuiShipPilotSurface(EntityShip ship, EntityPlayer player) {
		super(
			256, 25, new ResourceLocation("ships", "textures/gui/shipPilotSurface.png"),
			ship, player,
			Arrays.asList(PilotAction.ThrottleUp, PilotAction.ThrottleDown, PilotAction.Left, PilotAction.Right),
			ForwardSideMethod.ByHelm
		);
	}
	
	@Override
	protected void drawForeground(int mouseX, int mouseY, float partialTickTime) {
		
		// do we have a valid ship to pilot?
		// issue 78 says sometimes we don't. Can't reproduce. Better to handle it just in case though
		BlockSide forwardSide = getForwardSide();
		if (forwardSide == null) {
			this.mc.fontRenderer.drawString("Ship is corrupt! Can't pilot!", 48, 8, ColorUtils.getColor(255, 0, 0));
			return;
		}
		
		KeyBinding keyForward = mc.gameSettings.keyBindForward;
		KeyBinding keyBack = mc.gameSettings.keyBindBack;
		KeyBinding keyLeft = mc.gameSettings.keyBindLeft;
		KeyBinding keyRight = mc.gameSettings.keyBindRight;
		
		// draw the key binds
		int textColor = ColorUtils.getGrey(64);
		final int TextOffset = 44;
		this.mc.fontRenderer.drawString(getKeyName(keyForward), TextOffset + 11, 8, textColor);
		this.mc.fontRenderer.drawString(getKeyName(keyBack), TextOffset + 46, 8, textColor);
		this.mc.fontRenderer.drawString(getKeyName(keyLeft), TextOffset + 61, 8, textColor);
		this.mc.fontRenderer.drawString(getKeyName(keyRight), TextOffset + 95, 8, textColor);
		
		bindBackgroundTexture();
		
		// determine the compass offsets
		double shipDirectionOffset = (double)forwardSide.getXZOffset() / 4;
		double shipYawOffset = CircleRange.mapZeroTo360(getShip().rotationYaw) / 360.0f;
		double compassFrameOffset = (double)CompassFrameWidth / 2 / TextureWidth;
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		double z = zLevel;
		
		// draw the compass
		double umin = (double)CompassNorthOffset / TextureWidth + shipDirectionOffset - shipYawOffset - compassFrameOffset;
		double umax = umin + (double)CompassFrameWidth / TextureWidth;
		double vmin = (double)CompassY / TextureHeight;
		double vmax = (double)(CompassY + CompassHeight) / TextureHeight;
		double x = CompassFrameX;
		double y = CompassFrameY;
		tessellator.addVertexWithUV(x, y + CompassHeight, z, umin, vmax);
		tessellator.addVertexWithUV(x + CompassFrameWidth, y + CompassHeight, z, umax, vmax);
		tessellator.addVertexWithUV(x + CompassFrameWidth, y, z, umax, vmin);
		tessellator.addVertexWithUV(x, y, z, umin, vmin);
		
		// draw the compass range
		umin = (double) (CompassRangeX + CompassRangeWidth / 2) / TextureWidth - shipYawOffset - compassFrameOffset;
		umax = umin + (double)CompassFrameWidth / TextureWidth;
		vmin = (double)CompassRangeY / TextureHeight;
		vmax = vmin + (double)CompassHeight / TextureHeight;
		x = CompassFrameX;
		y = CompassFrameY;
		tessellator.addVertexWithUV(x, y + CompassHeight, z, umin, vmax);
		tessellator.addVertexWithUV(x + CompassFrameWidth, y + CompassHeight, z, umax, vmax);
		tessellator.addVertexWithUV(x + CompassFrameWidth, y, z, umax, vmin);
		tessellator.addVertexWithUV(x, y, z, umin, vmin);
		
		// draw the compass marker
		umin = (double)CompassMarkerX / TextureWidth;
		umax = umin + (double)CompassMarkerWidth / TextureWidth;
		vmin = (double)CompassMarkerY / TextureHeight;
		vmax = vmin + (double)CompassMarkerHeight / TextureHeight;
		x = CompassFrameX + CompassFrameWidth / 2 - CompassMarkerWidth / 2;
		y = CompassFrameY - 2;
		tessellator.addVertexWithUV(x, y + CompassMarkerHeight, z, umin, vmax);
		tessellator.addVertexWithUV(x + CompassMarkerWidth, y + CompassMarkerHeight, z, umax, vmax);
		tessellator.addVertexWithUV(x + CompassMarkerWidth, y, z, umax, vmin);
		tessellator.addVertexWithUV(x, y, z, umin, vmin);
		
		// draw the throttle
		if (getShip().linearThrottle > 0) {
			double fullWidth = ThrottleWidth - (ThrottleZero - ThrottleX) - 1;
			double throttleWidth = fullWidth * getShip().linearThrottle / EntityShip.LinearThrottleMax;
			umin = (double) (ThrottleZero + 1) / TextureWidth;
			umax = umin + (double)throttleWidth / TextureWidth;
			vmin = (double)ThrottleY / TextureHeight;
			vmax = vmin + (double)ThrottleHeight / TextureHeight;
			x = ThrottleFrameX + (ThrottleZero - ThrottleX) + 1;
			y = ThrottleFrameY;
			tessellator.addVertexWithUV(x, y + ThrottleHeight, z, umin, vmax);
			tessellator.addVertexWithUV(x + throttleWidth, y + ThrottleHeight, z, umax, vmax);
			tessellator.addVertexWithUV(x + throttleWidth, y, z, umax, vmin);
			tessellator.addVertexWithUV(x, y, z, umin, vmin);
		} else if (getShip().linearThrottle < 0) {
			double fullWidth = ThrottleZero - ThrottleX;
			double throttleWidth = fullWidth * getShip().linearThrottle / EntityShip.LinearThrottleMin;
			umax = (double) (ThrottleZero - 1) / TextureWidth;
			umin = umax - (double) (throttleWidth - 1) / TextureWidth;
			vmin = (double)ThrottleY / TextureHeight;
			vmax = vmin + (double)ThrottleHeight / TextureHeight;
			x = ThrottleFrameX + fullWidth - throttleWidth;
			y = ThrottleFrameY;
			tessellator.addVertexWithUV(x, y + ThrottleHeight, z, umin, vmax);
			tessellator.addVertexWithUV(x + throttleWidth, y + ThrottleHeight, z, umax, vmax);
			tessellator.addVertexWithUV(x + throttleWidth, y, z, umax, vmin);
			tessellator.addVertexWithUV(x, y, z, umin, vmin);
		}
		
		tessellator.draw();
	}

	private String getKeyName(KeyBinding key) {
		return GameSettings.getKeyDisplayString(key.getKeyCode());
	}
}
