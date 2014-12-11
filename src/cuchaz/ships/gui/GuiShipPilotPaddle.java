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
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class GuiShipPilotPaddle extends GuiShipPilot
{
	private static final ResourceLocation BackgroundTexture = new ResourceLocation( "ships", "textures/gui/shipPaddle.png" );
	private static final int TextureWidth = 128;
	private static final int TextureHeight = 32;
	
	public GuiShipPilotPaddle( Container container, EntityShip ship, EntityPlayer player )
	{
		super(
			container,
			ship,
			player,
			Arrays.asList( PilotAction.Forward, PilotAction.Backward, PilotAction.Left, PilotAction.Right ),
			ForwardSideMethod.ByPlayerLook
		);
		
		xSize = 110;
		ySize = 25;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		int keyForward = mc.gameSettings.keyBindForward.getKeyCode();
		int keyBack = mc.gameSettings.keyBindBack.getKeyCode();
		int keyLeft = mc.gameSettings.keyBindLeft.getKeyCode();
		int keyRight = mc.gameSettings.keyBindRight.getKeyCode();
		
		// draw the key binds
		int textColor = ColorUtils.getGrey( 64 );
		this.mc.fontRenderer.drawString( Keyboard.getKeyName( keyForward ), 11, 8, textColor );
		this.mc.fontRenderer.drawString( Keyboard.getKeyName( keyBack ), 46, 8, textColor );
		this.mc.fontRenderer.drawString( Keyboard.getKeyName( keyLeft ), 61, 8, textColor );
		this.mc.fontRenderer.drawString( Keyboard.getKeyName( keyRight ), 95, 8, textColor );
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
	{
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
		mc.getTextureManager().bindTexture( BackgroundTexture );
		
        double umax = (double)xSize/TextureWidth;
        double vmax = (double)ySize/TextureHeight;
		
		Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop + ySize ), (double)zLevel, 0,    vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop + ySize ), (double)zLevel, umax, vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop ),         (double)zLevel, umax, 0 );
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop ),         (double)zLevel, 0,    0 );
        tessellator.draw();
	}
}
