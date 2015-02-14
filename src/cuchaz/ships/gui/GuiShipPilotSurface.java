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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.math.CircleRange;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class GuiShipPilotSurface extends GuiShipPilot {
	
	private static final ResourceLocation BackgroundTexture = new ResourceLocation("ships", "textures/gui/shipPilotSurface.png");
	private static final int TextureWidth = 256;
	private static final int TextureHeight = 64;
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
	
	public GuiShipPilotSurface(Container container, EntityShip ship, EntityPlayer player) {
		super(container, ship, player, Arrays.asList(PilotAction.ThrottleUp, PilotAction.ThrottleDown, PilotAction.Left, PilotAction.Right), ForwardSideMethod.ByHelm);
		
		xSize = 256;
		ySize = 25;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		// do we have a valid ship to pilot?
		// issue 78 says sometimes we don't. Can't reproduce. Better to handle it just in case though
		BlockSide forwardSide = getForwardSide();
		if (forwardSide == null) {
			this.mc.fontRenderer.drawString("Ship is corrupt! Can't pilot!", 48, 8, ColorUtils.getColor(255, 0, 0));
			return;
		}
		
		int keyForward = mc.gameSettings.keyBindForward.getKeyCode();
		int keyBack = mc.gameSettings.keyBindBack.getKeyCode();
		int keyLeft = mc.gameSettings.keyBindLeft.getKeyCode();
		int keyRight = mc.gameSettings.keyBindRight.getKeyCode();
		
		// draw the key binds
		int textColor = ColorUtils.getGrey(64);
		final int TextOffset = 44;
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyForward), TextOffset + 11, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyBack), TextOffset + 46, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyLeft), TextOffset + 61, 8, textColor);
		this.mc.fontRenderer.drawString(Keyboard.getKeyName(keyRight), TextOffset + 95, 8, textColor);
		
		loadTexture();
		
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
		double vmax = 1;
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
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		loadTexture();
		
		double umax = (double)xSize / TextureWidth;
		double vmax = (double)ySize / TextureHeight;
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV((double) (guiLeft), (double) (guiTop + ySize), (double)zLevel, 0, vmax);
		tessellator.addVertexWithUV((double) (guiLeft + xSize), (double) (guiTop + ySize), (double)zLevel, umax, vmax);
		tessellator.addVertexWithUV((double) (guiLeft + xSize), (double) (guiTop), (double)zLevel, umax, 0);
		tessellator.addVertexWithUV((double) (guiLeft), (double) (guiTop), (double)zLevel, 0, 0);
		tessellator.draw();
	}
	
	private void loadTexture() {
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		mc.getTextureManager().bindTexture(BackgroundTexture);
	}
}
